package io.mosip.certify.proof;

import com.nimbusds.jose.*;
import com.nimbusds.jose.crypto.ECDSASigner;
import com.nimbusds.jose.crypto.Ed25519Signer;
import com.nimbusds.jose.crypto.RSASSASigner;
import com.nimbusds.jose.jwk.*;
import com.nimbusds.jose.jwk.gen.ECKeyGenerator;
import com.nimbusds.jose.jwk.gen.OctetKeyPairGenerator;
import com.nimbusds.jose.jwk.gen.RSAKeyGenerator;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import io.mosip.certify.core.dto.CredentialProof;
import io.mosip.certify.core.exception.InvalidRequestException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import java.nio.charset.StandardCharsets;
import java.text.ParseException;
import java.util.*;

import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThrows;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.*;

class JwtProofValidatorTest {

    private JwtProofValidator jwtProofValidator;

    @Mock
    private CredentialProof credentialProof;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        jwtProofValidator = new JwtProofValidator();
        ReflectionTestUtils.setField(jwtProofValidator, "supportedAlgorithms", List.of("RS256", "ES256K", "Ed25519"));
        ReflectionTestUtils.setField(jwtProofValidator, "credentialIdentifier", "test-credential-id");
    }

    @Test
    void testValidateWithNullJwt() {

        when(credentialProof.getJwt()).thenReturn(null);


        boolean result = jwtProofValidator.validate("client-id", "nonce", credentialProof);


        assertFalse(result, "Expected validation to fail for null JWT");
    }

    @Test
    void testValidateWithBlankJwt() {

        when(credentialProof.getJwt()).thenReturn("");


        boolean result = jwtProofValidator.validate("client-id", "nonce", credentialProof);


        assertFalse(result, "Expected validation to fail for blank JWT");
    }

    @Test
    void testValidate_ValidJWT() throws Exception {
        String jwt = createValidJWT();
        CredentialProof credentialProof = new CredentialProof();
        credentialProof.setJwt(jwt);

        boolean result = jwtProofValidator.validate("test-client", "test-nonce", credentialProof);

        assertTrue(result, "JWT should be valid");
    }

    @Test
    void testValidate_InvalidJWT() {
        CredentialProof credentialProof = new CredentialProof();
        credentialProof.setJwt("invalid.jwt.token");

        boolean result = jwtProofValidator.validate("test-client", "test-nonce", credentialProof);

        assertFalse(result, "Invalid JWT should fail validation");
    }

    @Test
    void testGetKeyMaterial_ValidJWT() throws Exception {
        String jwt = createValidJWT();
        CredentialProof credentialProof = new CredentialProof();
        credentialProof.setJwt(jwt);

        String keyMaterial = jwtProofValidator.getKeyMaterial(credentialProof);
        assertNotNull(keyMaterial);
        assertTrue(keyMaterial.startsWith("did:jwk:"), "Key material should be prefixed with did:jwk");
    }

    @Test
    void testGetKeyMaterial_InvalidJWT() {
        CredentialProof credentialProof = new CredentialProof();
        credentialProof.setJwt("invalid.jwt.token");

        assertThrows(InvalidRequestException.class, () -> jwtProofValidator.getKeyMaterial(credentialProof));
    }

    private String createValidJWT() throws Exception {
        // Generate a 2048-bit RSA key pair
        RSAKey rsaJWK = new RSAKeyGenerator(2048)
                .keyID(UUID.randomUUID().toString())
                .generate();

        // Extract public key for embedding in the JWT header
        RSAKey rsaPublicJWK = rsaJWK.toPublicJWK();

        JWSHeader header = new JWSHeader.Builder(JWSAlgorithm.RS256)
                .type(new JOSEObjectType("openid4vci-proof+jwt"))
                .jwk(rsaPublicJWK)  // Embed the public JWK
                .build();

        // Build JWT claims
        SignedJWT jwt = new SignedJWT(header, new com.nimbusds.jwt.JWTClaimsSet.Builder()
                .audience("test-credential-id")
                .issuer("test-client")
                .claim("nonce", "test-nonce")
                .issueTime(new Date())
                .expirationTime(new Date(System.currentTimeMillis() + 60000)) // 1 min expiration
                .build());

        // Sign JWT using private key
        JWSSigner signer = new RSASSASigner(rsaJWK);
        jwt.sign(signer);

        return jwt.serialize();
    }

    @Test
    public void testValidate_InvalidJwt_MissingClaims() throws ParseException, JOSEException {
        RSAKey rsaJWK = new RSAKeyGenerator(2048)
                .keyID(UUID.randomUUID().toString())
                .generate();

        // Extract public key for embedding in the JWT header
        RSAKey rsaPublicJWK = rsaJWK.toPublicJWK();

        JWSHeader header = new JWSHeader.Builder(JWSAlgorithm.RS256)
                .type(new JOSEObjectType("openid4vci-proof+jwt"))
                .jwk(rsaPublicJWK)  // Embed the public JWK
                .build();

        // Build JWT claims
        SignedJWT jwt = new SignedJWT(header, new com.nimbusds.jwt.JWTClaimsSet.Builder()
                .audience("test-credential-id")
                .issuer("test-client")
                .claim("nonce", "test-nonce")
                .expirationTime(new Date(System.currentTimeMillis() + 60000)) // 1 min expiration
                .build());

        // Sign JWT using private key
        JWSSigner signer = new RSASSASigner(rsaJWK);
        jwt.sign(signer);

        String jwtStr = jwt.serialize();
        CredentialProof credentialProof = new CredentialProof();
        credentialProof.setJwt(jwtStr);

        boolean result = jwtProofValidator.validate("test-client", "test-nonce", credentialProof);

        assertFalse(result, "Missing iat from jwt claims");
    }

    @Test
    public void testValidate_Es256_InvalidAlgException() throws ParseException, JOSEException {
        // Generate a valid ECKey with ES256
        ECKey ecJWK = new ECKeyGenerator(Curve.P_256)
                .keyUse(KeyUse.SIGNATURE)
                .keyID(UUID.randomUUID().toString())
                .generate();

        // Create a public JWK from the private JWK
        JWK publicJwk = ecJWK.toPublicJWK();

        // Create JWS Header with public JWK
        JWSHeader header = new JWSHeader.Builder(JWSAlgorithm.ES256)
                .jwk(publicJwk)
                .type(new JOSEObjectType("openid4vci-proof+jwt"))
                .build();

        // Build JWT Claims
        JWTClaimsSet claims = new JWTClaimsSet.Builder()
                .audience("test-credential-id")
                .issuer("clientId")
                .claim("nonce", "someNonce")
                .issueTime(new Date())
                .expirationTime(new Date(System.currentTimeMillis() + 3600000)) // 1 hour from now
                .build();

        // Create and sign the JWT with the private EC key
        SignedJWT signedJWT = new SignedJWT(header, claims);
        JWSSigner signer = new ECDSASigner(ecJWK);
        signedJWT.sign(signer);

        // Serialize JWT
        String jwt = signedJWT.serialize();

        // Prepare CredentialProof object
        CredentialProof credentialProof = new CredentialProof();
        credentialProof.setJwt(jwt);

        // Validate JWT
        boolean result = jwtProofValidator.validate("clientId", "someNonce", credentialProof);

        assertFalse(result, "No algorithm found exception");
    }

    @Test
    void testValidate_ValidEd25519JWT() throws Exception {
        String jwt = createValidEd25519JWT();
        CredentialProof credentialProof = new CredentialProof();
        credentialProof.setJwt(jwt);

        boolean result = jwtProofValidator.validate("test-client", "test-nonce", credentialProof);

        assertTrue(result, "Ed25519 JWT should be valid");
    }

    private String createValidEd25519JWT() throws Exception {
        // Generate Ed25519 key pair
        OctetKeyPair edJWK = new OctetKeyPairGenerator(Curve.Ed25519)
                .keyID(UUID.randomUUID().toString())  // Use unique key ID
                .generate();

        // Create JWT header with Ed25519 algorithm and JWK
        JWSHeader header = new JWSHeader.Builder(JWSAlgorithm.Ed25519)
                .type(new JOSEObjectType("openid4vci-proof+jwt"))
                .keyID("did:jwk:" + Base64.getUrlEncoder().withoutPadding().encodeToString(edJWK.toPublicJWK().toJSONString().getBytes(StandardCharsets.UTF_8)))
                .build();

        // Create JWT claims
        JWTClaimsSet claims = new JWTClaimsSet.Builder()
                .audience("test-credential-id")
                .issuer("test-client")
                .claim("nonce", "test-nonce")
                .issueTime(new Date())
                .expirationTime(new Date(System.currentTimeMillis() + 60000))
                .build();

        // Sign JWT with Ed25519
        SignedJWT jwt = new SignedJWT(header, claims);
        JWSSigner signer = new Ed25519Signer(edJWK);
        jwt.sign(signer);

        return jwt.serialize();
    }
}
