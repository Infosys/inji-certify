package io.mosip.certify.proofgenerators;

import info.weboftrust.ldsignatures.LdProof;
import info.weboftrust.ldsignatures.canonicalizer.Canonicalizer;
import info.weboftrust.ldsignatures.canonicalizer.URDNA2015Canonicalizer;
import io.mosip.certify.core.constants.Constants;
import io.mosip.certify.core.constants.SignatureAlg;
import io.mosip.kernel.signature.dto.JWSSignatureRequestDto;
import io.mosip.kernel.signature.dto.JWTSignatureResponseDto;
import io.mosip.kernel.signature.service.SignatureService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ConditionalOnProperty(name = "mosip.certify.data-provider-plugin.issuer.vc-sign-algo", havingValue = SignatureAlg.ED25519_SIGNATURE_SUITE_2018)
class Ed25519Signature2018ProofGeneratorTest {

    @InjectMocks
    private Ed25519Signature2018ProofGenerator proofGenerator;

    @Mock
    private SignatureService signatureService;

    private Map<String, String> keyID;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        keyID = new HashMap<>();
        keyID.put(Constants.APPLICATION_ID, "app123");
        keyID.put(Constants.REFERENCE_ID, "ref456");
    }

    @Test
    void testGetName() {
        assertEquals("Ed25519Signature2018", proofGenerator.getName());
    }

    @Test
    void testGetCanonicalizer() {
        Canonicalizer canonicalizer = proofGenerator.getCanonicalizer();
        assertNotNull(canonicalizer);
        assertTrue(canonicalizer instanceof URDNA2015Canonicalizer);
    }

    @Test
    void testGenerateProofSuccess() {
        LdProof baseProof = new LdProof();
        String vcEncodedHash = "mockEncodedHash";
        JWTSignatureResponseDto responseDto = new JWTSignatureResponseDto();
        responseDto.setJwtSignedData("mockJwsData");

        when(signatureService.jwsSign(any(JWSSignatureRequestDto.class))).thenReturn(responseDto);

        LdProof result = proofGenerator.generateProof(baseProof, vcEncodedHash, keyID);

        assertNotNull(result);
        assertEquals("mockJwsData", result.getJws());

        ArgumentCaptor<JWSSignatureRequestDto> captor = ArgumentCaptor.forClass(JWSSignatureRequestDto.class);
        verify(signatureService).jwsSign(captor.capture());

        JWSSignatureRequestDto capturedPayload = captor.getValue();
        assertEquals(vcEncodedHash, capturedPayload.getDataToSign());
        assertEquals("app123", capturedPayload.getApplicationId());
        assertEquals("ref456", capturedPayload.getReferenceId());
        assertFalse(capturedPayload.getIncludePayload());
    }

    @Test
    void testGenerateProof_NullProof() {
        JWTSignatureResponseDto responseDto = new JWTSignatureResponseDto();
        responseDto.setJwtSignedData("mockJwsData");
        when(signatureService.jwsSign(any(JWSSignatureRequestDto.class))).thenReturn(responseDto);

        LdProof result = proofGenerator.generateProof(null, "mockEncodedHash", keyID);

        assertNotNull(result);
        assertEquals("mockJwsData", result.getJws());
    }

    @Test
    void testGenerateProof_EmptyKeyID() {
        JWTSignatureResponseDto responseDto = new JWTSignatureResponseDto();
        responseDto.setJwtSignedData("mockJwsData");
        when(signatureService.jwsSign(any(JWSSignatureRequestDto.class))).thenReturn(responseDto);

        Map<String, String> emptyKeyID = new HashMap<>();
        LdProof result = proofGenerator.generateProof(new LdProof(), "mockEncodedHash", emptyKeyID);

        assertNotNull(result);
        assertEquals("mockJwsData", result.getJws());
    }

    @Test
    void testGenerateProofSignature_ServiceFailure() {
        when(signatureService.jwsSign(any(JWSSignatureRequestDto.class))).thenThrow(new RuntimeException("Signature Failed"));

        Exception exception = assertThrows(RuntimeException.class, () -> {
            proofGenerator.generateProof(new LdProof(), "mockEncodedHash", keyID);
        });

        assertEquals("Signature Failed", exception.getMessage());
    }
}
