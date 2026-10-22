package com.securefileshare.auth.crypto;

import com.securefileshare.common.exception.ApiException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.SecureRandom;
import java.security.interfaces.ECPublicKey;
import java.security.interfaces.RSAPublicKey;
import java.security.spec.ECGenParameterSpec;
import java.util.Base64;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Unit tests for cryptographic key validation and SHA-256 fingerprint computation.
 * WHAT: Generates live RSA (4096-bit & 2048-bit) and EC (secp256r1) keypairs to verify acceptance and rejection criteria.
 * WHY: Enforces zero-knowledge requirements ensuring weak RSA keys and unsupported curves are rejected at the gate.
 */
class KeyValidatorTest {

    private KeyValidator keyValidator;

    @BeforeEach
    void setUp() {
        this.keyValidator = new KeyValidator();
    }

    @Test
    @DisplayName("Should accept valid 4096-bit RSA public key")
    void testAccept4096BitRsaKey() throws Exception {
        // Generate valid 4096-bit RSA key
        KeyPairGenerator generator = KeyPairGenerator.getInstance("RSA");
        generator.initialize(4096, new SecureRandom());
        KeyPair keyPair = generator.generateKeyPair();

        String pem = toPem("PUBLIC KEY", keyPair.getPublic().getEncoded());

        RSAPublicKey validatedKey = keyValidator.validateRsaPublicKey(pem);
        assertThat(validatedKey).isNotNull();
        assertThat(validatedKey.getModulus().bitLength()).isEqualTo(4096);
    }

    @Test
    @DisplayName("Should reject weak 2048-bit RSA public key")
    void testReject2048BitRsaKey() throws Exception {
        // Generate weak 2048-bit RSA key
        KeyPairGenerator generator = KeyPairGenerator.getInstance("RSA");
        generator.initialize(2048, new SecureRandom());
        KeyPair keyPair = generator.generateKeyPair();

        String pem = toPem("PUBLIC KEY", keyPair.getPublic().getEncoded());

        // Why: Prevents weak key usage susceptible to factorization attacks
        assertThatThrownBy(() -> keyValidator.validateRsaPublicKey(pem))
                .isInstanceOf(ApiException.class)
                .hasMessageContaining("must be exactly 4096 bits");
    }

    @Test
    @DisplayName("Should accept valid NIST P-256 (secp256r1) EC public key")
    void testAcceptP256EcKey() throws Exception {
        // Generate valid EC P-256 key
        KeyPairGenerator generator = KeyPairGenerator.getInstance("EC");
        generator.initialize(new ECGenParameterSpec("secp256r1"), new SecureRandom());
        KeyPair keyPair = generator.generateKeyPair();

        String pem = toPem("PUBLIC KEY", keyPair.getPublic().getEncoded());

        ECPublicKey validatedKey = keyValidator.validateEcPublicKey(pem);
        assertThat(validatedKey).isNotNull();
        assertThat(validatedKey.getParams().getCurve().getField().getFieldSize()).isEqualTo(256);
    }

    @Test
    @DisplayName("Should reject malformed public key string")
    void testRejectMalformedKey() {
        String malformedKey = "NotAValidKeyStringAtAll";

        assertThatThrownBy(() -> keyValidator.validateRsaPublicKey(malformedKey))
                .isInstanceOf(ApiException.class);

        assertThatThrownBy(() -> keyValidator.validateEcPublicKey(malformedKey))
                .isInstanceOf(ApiException.class);
    }

    @Test
    @DisplayName("Should compute deterministic SHA-256 fingerprint for public key")
    void testComputeFingerprintDeterminism() throws Exception {
        KeyPairGenerator generator = KeyPairGenerator.getInstance("EC");
        generator.initialize(new ECGenParameterSpec("secp256r1"), new SecureRandom());
        KeyPair keyPair = generator.generateKeyPair();

        String fp1 = keyValidator.computeFingerprint(keyPair.getPublic());
        String fp2 = keyValidator.computeFingerprint(keyPair.getPublic());

        // Why: Deterministic fingerprints allow senders to verify keys out-of-band against server substitution
        assertThat(fp1).isEqualTo(fp2);
        assertThat(fp1).hasSize(64); // SHA-256 hex digest is exactly 64 characters
    }

    private String toPem(String type, byte[] derBytes) {
        String base64 = Base64.getEncoder().encodeToString(derBytes);
        return "-----BEGIN " + type + "-----\n" + base64 + "\n-----END " + type + "-----\n";
    }
}
