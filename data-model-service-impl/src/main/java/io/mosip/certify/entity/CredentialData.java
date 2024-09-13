package io.mosip.certify.entity;


import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.persistence.Entity;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import javax.persistence.Id;

@Data
@Entity
@NoArgsConstructor
@AllArgsConstructor
public class CredentialData {
    @Id
    @NotBlank
    @NotNull
    private String id;

    @NotBlank(message = "Context is mandatory")
    private String context;

    @NotBlank(message = "Credential Type is mandatory")
    private String credentialType;

    @NotBlank(message = "Template is mandatory")
    private String template;

}
