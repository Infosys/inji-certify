package io.mosip.certify.services;

import com.nimbusds.jose.Algorithm;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.jwk.Curve;
import com.nimbusds.jose.jwk.JWK;
import com.nimbusds.jose.jwk.KeyUse;
import com.nimbusds.jose.jwk.OctetKeyPair;
import com.nimbusds.jose.util.Base64URL;
import io.mosip.certify.core.constants.Constants;
import io.mosip.certify.core.exception.CertifyException;
import io.mosip.certify.core.spi.JwksService;
import io.mosip.certify.entity.CredentialConfig;
import io.mosip.certify.repository.CredentialConfigRepository;
import io.mosip.kernel.keymanagerservice.dto.AllCertificatesDataResponseDto;
import io.mosip.kernel.keymanagerservice.service.KeymanagerService;
import lombok.extern.slf4j.Slf4j;
import org.bouncycastle.asn1.ASN1InputStream;
import org.bouncycastle.asn1.ASN1Sequence;
import org.bouncycastle.asn1.DERBitString;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.io.ByteArrayInputStream;
import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.Security;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.security.interfaces.RSAPublicKey;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.*;

import static io.mosip.certify.core.constants.Constants.ED25519_REF_ID;

@Service
@Slf4j
public class JwksServiceImpl implements JwksService {

    static {
        Security.addProvider(new BouncyCastleProvider());
    }


    @Autowired
    private KeymanagerService keymanagerService;

    @Autowired
    private CredentialConfigRepository credentialConfigRepository;

    @Value("#{${mosip.certify.credential-config.credential-signing-alg-values-supported}}")
    private LinkedHashMap<String, List<String>> credentialSigningAlgValuesSupportedMap;

    @Value("#{${mosip.certify.signature-algo.key-alias-mapper:{}}}")
    private Map<String, List<List<String>>> signatureAlgoKeyAliasMapper;

    /**
     * Internal method to fetch JWK set - cached for performance
     * Only successful responses are cached (method returns non-null Map)
     */
    @Cacheable(value = "jwks", key = "'oauth-jwks'")
    public Map<String, Object> getJwks() {
        List<Map<String, Object>> jwkList = new ArrayList<>();

        // Fetch JWKs dynamically from configuration map
        signatureAlgoKeyAliasMapper.forEach((algo, keyAliases) -> {
            keyAliases.forEach(keyAlias -> {
                String appId = keyAlias.get(0);
                String refId = keyAlias.get(1);
                AllCertificatesDataResponseDto response = keymanagerService.getAllCertificates(appId, Optional.of(refId));
                jwkList.addAll(getJwks(response));
            });
        });

        Map<String, Object> response = new HashMap<>();
        response.put("keys", jwkList);

        return response;
    }

    private List<Map<String, Object>> getJwks(AllCertificatesDataResponseDto allCertificatesDataResponseDto) {
        List<Map<String, Object>> jwkList = new ArrayList<>();
        if (allCertificatesDataResponseDto != null && allCertificatesDataResponseDto.getAllCertificates() != null) {
            Arrays.stream(allCertificatesDataResponseDto.getAllCertificates())
                    .filter(dto -> dto != null
                            && StringUtils.hasText(dto.getKeyId())
                            && StringUtils.hasText(dto.getCertificateData()))
                    .forEach(dto -> {
                        try {
                            Map<String, Object> jwk = getJwk(dto.getKeyId(), dto.getCertificateData(), dto.getExpiryAt());
                            if (jwk != null) {
                                jwkList.add(jwk);
                                log.debug("Added JWK for keyId: {}", dto.getKeyId());
                            }
                        } catch (Exception e) {
                            log.error("Failed to parse the certificate data for keyId: {}", dto.getKeyId(), e);
                            // Continue processing other certificates
                        }
                    });
        } else {
            log.warn("No certificates found for CERTIFY_SERVICE_APP_ID");
        }
        return jwkList;
    }

    /**
     * Convert certificate data to JWK format
     *
     * @param keyId Key identifier
     * @param certificateData PEM encoded certificate
     * @param expiryAt Certificate expiry date
     * @return JWK map, or null if certificate parsing fails or certificate is expired
     * @throws Exception if certificate parsing fails
     */
    private Map<String, Object> getJwk(String keyId, String certificateData, LocalDateTime expiryAt) throws Exception {
        if (!StringUtils.hasText(keyId) || !StringUtils.hasText(certificateData)) {
            throw new IllegalArgumentException("keyId or certificateData cannot be null or empty");
        }
        if (expiryAt != null && expiryAt.isBefore(LocalDateTime.now())) {
            log.debug("Certificate for keyId: {} has expired, skipping", keyId);
            return null;
        }

        // Parse PEM to X509Certificate
        CertificateFactory cf = CertificateFactory.getInstance("X.509");
        X509Certificate cert = (X509Certificate) cf.generateCertificate(
                new ByteArrayInputStream(certificateData.getBytes(StandardCharsets.UTF_8)));

        byte[] spkiBytes = cert.getPublicKey().getEncoded();  // SubjectPublicKeyInfo
        String sigAlg = cert.getSigAlgName().toUpperCase();

        Map<String, Object> map = new LinkedHashMap<>();
        map.put("kid", keyId);
        map.put("use", "sig");
        map.put("x5c", List.of(Base64.getEncoder().encodeToString(cert.getEncoded())));
        map.put("x5t#S256", computeSHA256Thumbprint(cert.getEncoded()));

        if (sigAlg.contains(JWSAlgorithm.Ed25519.getName().toUpperCase()) || sigAlg.contains(JWSAlgorithm.EdDSA.getName().toUpperCase())) {
            // Ed25519: Extract 32-byte 'x' from SPKI BIT STRING
            byte[] keyBytes = parseEd25519KeyBytes(spkiBytes);
            if (keyBytes == null || keyBytes.length != 32) {
                log.warn("Invalid Ed25519 key ({} bytes) for kid: {}",
                        keyBytes != null ? keyBytes.length : 0, keyId);
                return null;
            }

            // Correct Builder: Base64URL x, no private key or cert chain needed for public JWK
            Base64URL xParam = Base64URL.encode(keyBytes);
            OctetKeyPair okp = new OctetKeyPair.Builder(Curve.Ed25519, xParam)
                    .keyID(keyId)
                    .keyUse(KeyUse.SIGNATURE)
                    .algorithm(new Algorithm("EdDSA"))
                    .build();

            map.put("kty", "OKP");
            map.put("crv", "Ed25519");
            map.put("x", xParam);
            map.put("alg", "EdDSA");
        } else {
            // RSA/EC: Parse with NimbusDS
            JWK jwk = JWK.parseFromPEMEncodedX509Cert(certificateData);
            map.put("alg", jwk.getAlgorithm() != null ? jwk.getAlgorithm().getName() : null);
            map.put("kty", jwk.getKeyType().getValue());
            @SuppressWarnings("unchecked")
            Map<String, Object> params = (Map<String, Object>) jwk.toPublicJWK().getRequiredParams();
            if (params.containsKey("n")) map.put("n", params.get("n"));
            if (params.containsKey("e")) map.put("e", params.get("e"));
        }

        if (expiryAt != null) {
            map.put("exp", expiryAt.toEpochSecond(ZoneOffset.UTC));
        }
        return map;
    }

    private static byte[] parseEd25519KeyBytes(byte[] spkiBytes) {
        try (ASN1InputStream asn1In = new ASN1InputStream(spkiBytes)) {
            ASN1Sequence seq = (ASN1Sequence) asn1In.readObject();
            if (seq.size() != 2) return null;

            DERBitString bitString = (DERBitString) DERBitString.getInstance(seq.getObjectAt(1));
            if (bitString.getBytes().length != 32 || bitString.getPadBits() != 0) return null;

            return bitString.getBytes();  // Raw 32-byte Ed25519 public key
        } catch (Exception e) {
            log.error("Failed to parse Ed25519 SPKI (len={}): {}", spkiBytes.length, e.getMessage());
            return null;
        }
    }

    private static String computeSHA256Thumbprint(byte[] certBytes) throws Exception {
        MessageDigest md = MessageDigest.getInstance("SHA-256");
        byte[] digest = md.digest(certBytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(digest);
    }

}
