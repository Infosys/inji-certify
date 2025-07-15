package io.mosip.certify.enums;

import io.mosip.certify.core.exception.CertifyException;

import java.util.Arrays;

public enum SignatureCryptoSuite {
    RSA_SIGNATURE_2018("RsaSignature2018"),
    ED25519_SIGNATURE_2018("Ed25519Signature2018"),
    ED25519_SIGNATURE_2020("Ed25519Signature2020"),
    ECDSA_KOBLITZ_2016("EcdsaKoblitzSignature2016"),
    ECDSA_SECP256K1_2019("EcdsaSecp256k1Signature2019"),
    ECDSA_SECP256R1_2019("EcdsaSecp256r1Signature2019"),
    ECDSA_RDFC_2019("ecdsa-rdfc-2019"),
    ECDSA_JCS_2019("ecdsa-jcs-2019"),
    EDDSA_RDFC_2019("eddsa-rdfc-2022"),
    EDDSA_JCS_2019("eddsa-jcs-2022");


    private final String cryptoSuite;

    private SignatureCryptoSuite(String cryptoSuite) {
        this.cryptoSuite = cryptoSuite;
    }

    public static SignatureCryptoSuite fromString(String value) {
        return Arrays.stream(SignatureCryptoSuite.values())
                .filter(suite -> suite.cryptoSuite.equals(value))
                .findFirst()
                .orElseThrow(() -> new CertifyException("Invalid SignatureCryptoSuite: " + value));
    }

    @Override
    public String toString() {
        return cryptoSuite;
    }
}
