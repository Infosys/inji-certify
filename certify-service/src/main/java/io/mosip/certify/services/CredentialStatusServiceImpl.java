package io.mosip.certify.services;

import io.mosip.certify.core.constants.ErrorConstants;
import io.mosip.certify.core.dto.CredentialStatusResponse;
import io.mosip.certify.core.dto.UpdateCredentialStatusRequest;
import io.mosip.certify.core.exception.CertifyException;
import io.mosip.certify.core.spi.CredentialStatusService;
import io.mosip.certify.entity.CredentialStatusTransaction;
import io.mosip.certify.entity.StatusListCredential;
import io.mosip.certify.repository.CredentialStatusTransactionRepository;
import io.mosip.certify.repository.StatusListCredentialRepository;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.List;

@Slf4j
@Service
public class CredentialStatusServiceImpl implements CredentialStatusService {
    public static final int DEFAULT_STATUS_LIST_SIZE = 131072;
    @Autowired
    private CredentialStatusTransactionRepository credentialStatusTransactionRepository;

    @Autowired
    private StatusListCredentialRepository statusListCredentialRepository;

    @Value("#{${mosip.certify.data-provider-plugin.credential-status.allowed-status-purposes:[]}}")
    private List<String> allowedCredentialStatusPurposes;

    @Override
    public CredentialStatusResponse updateCredentialStatus(UpdateCredentialStatusRequest request) {

        String statusListCredentialId = request.getCredentialStatus().getStatusListCredential();
        Long statusListIndex = request.getCredentialStatus().getStatusListIndex();
        String id = request.getCredentialStatus().getId();

        if(id != null && !id.equals(statusListCredentialId)) {
            throw new CertifyException(ErrorConstants.STATUS_ID_MISMATCH, "Mismatch between credential status ID and Status List Credential.");
        }
        StatusListCredential statusListCredential = statusListCredentialRepository.findById(statusListCredentialId)
                .orElseThrow(() -> new CertifyException(ErrorConstants.STATUS_LIST_NOT_FOUND, "Status List Credential not found for ID: " + statusListCredentialId));

        String statusPurpose = validateAndGetStatusPurpose(
                request.getCredentialStatus().getStatusPurpose(), statusListCredential
        );
        validateStatusListIndex(statusListIndex);

        CredentialStatusTransaction transaction = new CredentialStatusTransaction();
        transaction.setStatusPurpose(statusPurpose);
        transaction.setStatusValue(request.getStatus());
        transaction.setStatusListCredentialId(statusListCredentialId);
        transaction.setStatusListIndex(statusListIndex);
        CredentialStatusTransaction savedTransaction =credentialStatusTransactionRepository.save(transaction);

        CredentialStatusResponse dto = new CredentialStatusResponse();
        dto.setStatusListCredentialUrl(transaction.getStatusListCredentialId());
        dto.setStatusListIndex(transaction.getStatusListIndex());
        dto.setStatusPurpose(transaction.getStatusPurpose());
        dto.setStatusTimestamp(savedTransaction.getCreatedDtimes());
        if(request.getCredentialStatus().getType() != null) {
            dto.setCredentialType(request.getCredentialStatus().getType());
        }
        return dto;
    }

    private String validateAndGetStatusPurpose(String statusPurpose, StatusListCredential statusListCredential) {
        // fallback to StatusListCredential's purpose when statusPurpose is missing or empty
        if(statusPurpose == null || statusPurpose.trim().isEmpty()) {
            return statusListCredential.getStatusPurpose();
        }
        String statusPurposeValue = statusPurpose.trim().toLowerCase();
        // validate against allowed purposes only if the list is configured
        if(allowedCredentialStatusPurposes != null && !allowedCredentialStatusPurposes.isEmpty()) {
            boolean isAllowed = allowedCredentialStatusPurposes.stream()
                    .anyMatch(allowed -> allowed.equalsIgnoreCase(statusPurposeValue));
            if (!isAllowed) {
                throw new CertifyException(ErrorConstants.INVALID_STATUS_PURPOSE,
                        "statusPurpose must be one of: " + allowedCredentialStatusPurposes);
            }
        }
        return statusPurposeValue;
    }

    private void validateStatusListIndex(Long statusListIndex) {
        if (statusListIndex == null) {
            throw new CertifyException(ErrorConstants.INVALID_STATUS_LIST_INDEX, "statusListIndex must not be null");
        }
        if(statusListIndex < 0) {
            throw new CertifyException(ErrorConstants.INVALID_STATUS_LIST_INDEX, "statusListIndex must be a non-negative integer");
        }
        if(statusListIndex >= DEFAULT_STATUS_LIST_SIZE) {
            String errorMsg = String.format("statusListIndex must be between 0 and %d", DEFAULT_STATUS_LIST_SIZE - 1);
            throw new CertifyException(ErrorConstants.STATUS_LIST_INDEX_OUT_OF_RANGE, errorMsg);
        }
    }
}
