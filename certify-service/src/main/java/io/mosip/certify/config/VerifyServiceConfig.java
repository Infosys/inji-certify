package io.mosip.certify.config;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.inji.verify.dto.dcql.DCQLQueryDto;
import lombok.Data;

/**
 * Configuration class for VP request configuration (vp_request_config.json).
 * Only dcqlQuery is used by the service. Other fields (clientId, nonce,
 * responseCodeValidationRequired) may be present in the config file for
 * local testing purposes but are ignored at runtime.
 */
@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class VerifyServiceConfig {
    @JsonProperty("dcqlQuery")
    private DCQLQueryDto dcqlQuery;
}

