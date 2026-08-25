package io.mosip.certify.config;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.inji.verify.dto.dcql.DCQLQueryDto;
import lombok.Data;

/**
 * Configuration class for VP request configuration (vp_request_config.json).
 * Contains the DCQL query definition and optional override fields for local testing.
 */
@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class VerifyServiceConfig {
    @JsonProperty("dcqlQuery")
    private DCQLQueryDto dcqlQuery;

    @JsonProperty("clientId")
    private String clientId;

    @JsonProperty("nonce")
    private String nonce;
}

