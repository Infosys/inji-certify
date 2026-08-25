/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */
package io.mosip.certify.services;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.inji.verify.dto.authorizationrequest.AuthorizationRequestResponseDto;
import io.inji.verify.dto.authorizationrequest.VPRequestCreateDto;
import io.inji.verify.dto.authorizationrequest.VPRequestResponseDto;
import io.inji.verify.dto.dcql.DCQLQueryDto;
import io.inji.verify.services.VerifiablePresentationRequestService;
import io.mosip.certify.config.VerifyServiceConfig;
import io.mosip.certify.core.dto.*;
import io.mosip.certify.core.exception.CertifyException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestTemplate;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

/**
 * Service for creating VP requests via the embedded inji-verify library.
 */
@Slf4j
@Service
public class IarVpRequestService {

    private final RestTemplate restTemplate;

    private final ObjectMapper objectMapper;

    private final VerifiablePresentationRequestService vpRequestService;

    @Value("${mosip.certify.vp-request.config-file-url:}")
    private String vpRequestConfigUrl;

    @Value("${mosip.certify.verify.service.verifier-client-id:}")
    private String verifierClientId;

    @Value("${mosip.certify.iae.response-mode.post:iae_post}")
    private String iaePostResponseMode;

    @Value("${mosip.certify.iae.response-mode.post-jwt:iae_post.jwt}")
    private String iaePostJwtResponseMode;

    @Value("${mosip.certify.oauth.interactive-authorization-endpoint:}")
    private String certifyIaeEndpoint;

    @Value("${spring.profiles.active:}")
    private String activeProfile;

    @Autowired
    public IarVpRequestService(RestTemplate restTemplate,
                                ObjectMapper objectMapper,
                                VerifiablePresentationRequestService vpRequestService) {
        this.restTemplate = restTemplate;
        this.objectMapper = objectMapper;
        this.vpRequestService = vpRequestService;
    }

    /**
     * Create VP request using the embedded inji-verify library.
     */
    public VerifyVpResponse createVpRequest(InteractiveAuthorizationRequest iarRequest) throws CertifyException {
        log.info("Creating VP request via library for wallet client_id: {} using verifier client_id: {}",
                 iarRequest.getClientId(), verifierClientId);

        validateConfiguration();

        VerifyServiceConfig verifyServiceConfig;
        try {
            log.info("Fetching VP Request Config from: {}", vpRequestConfigUrl);
            String vpRequestConfig;
            if (activeProfile != null && activeProfile.contains("local")) {
                Resource resource = new ClassPathResource(vpRequestConfigUrl);
                try (var inputStream = resource.getInputStream()) {
                    vpRequestConfig = new String(inputStream.readAllBytes(), java.nio.charset.StandardCharsets.UTF_8);
                }
            } else {
                vpRequestConfig = restTemplate.getForObject(vpRequestConfigUrl, String.class);
            }
            if (vpRequestConfig == null || vpRequestConfig.isBlank()) {
                throw new CertifyException("unknown_error", "VP request configuration is empty or unavailable");
            }
            verifyServiceConfig = objectMapper.readValue(vpRequestConfig, VerifyServiceConfig.class);
        } catch (IOException | org.springframework.web.client.RestClientException e) {
            log.error("Failed to load / parse vp request configuration", e);
            throw new CertifyException("unknown_error", "Failed to load / parse vp request configuration", e);
        }

        DCQLQueryDto dcqlQuery = verifyServiceConfig.getDcqlQuery();
        if (dcqlQuery == null) {
            throw new CertifyException("unknown_error",
                    "VP request configuration must contain a dcqlQuery block");
        }

        try {
            // Use config-level overrides if present (local testing with hardcoded vp_tokens),
            // otherwise fall back to property/defaults (production with real wallet flow)
            String effectiveClientId = StringUtils.hasText(verifyServiceConfig.getClientId())
                    ? verifyServiceConfig.getClientId() : verifierClientId;
            String effectiveNonce = verifyServiceConfig.getNonce();

            VPRequestCreateDto vpRequestCreateDto = new VPRequestCreateDto(
                    effectiveClientId,
                    null,
                    effectiveNonce,
                    dcqlQuery,
                    false
            );

            log.debug("Calling inji-verify library createAuthorizationRequest for verifier client_id: {}", effectiveClientId);
            VPRequestResponseDto vpAuthRequest = vpRequestService.createAuthorizationRequest(vpRequestCreateDto);

            if (vpAuthRequest == null) {
                throw new CertifyException("unknown_error", "Empty response from inji-verify library");
            }

            log.info("VP request created via library for client_id: {}, transactionId: {}",
                     iarRequest.getClientId(), vpAuthRequest.getTransactionId());

            return mapToVerifyVpResponse(vpAuthRequest);

        } catch (CertifyException e) {
            throw e;
        } catch (Exception e) {
            log.error("Failed to create VP request via library for client_id: {}", iarRequest.getClientId(), e);
            throw new CertifyException("unknown_error", "Failed to create VP request via library", e);
        }
    }

    /**
     * Convert verify response to OpenID4VP request.
     */
    public Object convertToOpenId4VpRequest(VerifyVpResponse verifyResponse, InteractiveAuthorizationRequest iarRequest) {
        Map<String, Object> openId4VpRequest = new HashMap<>();

        VerifyVpResponse.AuthorizationDetails authDetails = verifyResponse.getAuthorizationDetails();
        if (authDetails == null) {
            log.error("No authorization details found in library response");
            throw new CertifyException("unknown_error", "Invalid response from inji-verify library: missing authorization details");
        }

        openId4VpRequest.put("response_type", authDetails.getResponseType());
        openId4VpRequest.put("client_id", authDetails.getClientId() != null ? authDetails.getClientId() : iarRequest.getClientId());
        openId4VpRequest.put("nonce", authDetails.getNonce());
        log.info("Forwarding VP request nonce from library: {}", authDetails.getNonce());

        openId4VpRequest.put("dcql_query", authDetails.getDcqlQuery());

        String responseMode = authDetails.getResponseMode();
        if (!StringUtils.hasText(responseMode)) {
            throw new CertifyException("unknown_error", "Response mode is required");
        }
        if ("direct_post".equals(responseMode)) {
            responseMode = iaePostResponseMode;
        } else if ("direct_post.jwt".equals(responseMode)) {
            responseMode = iaePostJwtResponseMode;
        }
        openId4VpRequest.put("response_mode", responseMode);

        openId4VpRequest.put("response_uri", certifyIaeEndpoint);
        log.info("Using certify /iae endpoint for wallet VP submission: {}", certifyIaeEndpoint);

        log.info("Successfully converted library response to OpenId4VpRequest for client_id: {}", iarRequest.getClientId());
        return openId4VpRequest;
    }

    private VerifyVpResponse mapToVerifyVpResponse(VPRequestResponseDto dto) {
        VerifyVpResponse response = new VerifyVpResponse();
        response.setTransactionId(dto.getTransactionId());
        response.setRequestId(dto.getRequestId());
        response.setExpiresAt(dto.getExpiresAt());

        AuthorizationRequestResponseDto authorizationRequestResponse = dto.getAuthorizationDetails();
        if (authorizationRequestResponse != null) {
            VerifyVpResponse.AuthorizationDetails authorizationDetails = new VerifyVpResponse.AuthorizationDetails();
            authorizationDetails.setClientId(authorizationRequestResponse.getClientId());
            authorizationDetails.setNonce(authorizationRequestResponse.getNonce());
            authorizationDetails.setResponseUri(authorizationRequestResponse.getResponseUri());
            authorizationDetails.setResponseType(authorizationRequestResponse.getResponseType());
            authorizationDetails.setResponseMode(authorizationRequestResponse.getResponseMode());
            authorizationDetails.setIssuedAt(authorizationRequestResponse.getIssuedAt());
            authorizationDetails.setDcqlQuery(
                    objectMapper.convertValue(authorizationRequestResponse.getDcqlQuery(), Object.class));
            response.setAuthorizationDetails(authorizationDetails);
        }
        return response;
    }

    private void validateConfiguration() {
        if (!StringUtils.hasText(vpRequestConfigUrl)) {
            throw new IllegalStateException("mosip.certify.vp-request.config-file-url must be configured");
        }
        if (!StringUtils.hasText(verifierClientId)) {
            throw new IllegalStateException("mosip.certify.verify.service.verifier-client-id must be configured");
        }
        if (!StringUtils.hasText(certifyIaeEndpoint)) {
            throw new IllegalStateException("mosip.certify.oauth.interactive-authorization-endpoint must be configured");
        }
        log.info("IarVpRequestService configuration validation successful");
    }
}
