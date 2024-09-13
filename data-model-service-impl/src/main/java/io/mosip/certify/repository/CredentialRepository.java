package io.mosip.certify.repository;

import io.mosip.certify.entity.CredentialData;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface CredentialRepository extends JpaRepository<CredentialData, String> {
    Optional<CredentialData> findByCredentialTypeAndContext(String credentialType, String context);
}
