package com.securefileshare.file.crypto;

import com.securefileshare.common.exception.ApiException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.security.*;
import java.security.spec.ECGenParameterSpec;
import java.util.Base64;
import java.util.HexFormat;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Unit test for SignatureVerificationService.
 * WHAT: Verifies cryptographic NIST P-256 EC public key validation and ECDSA signature checking.
 * WHY: Ensures invalid curves, tampered hashes, and forged signatures are rejected before any file processing.
 */
public class SignatureVerificationServiceTest {

    private SignatureVerificationService service;
    private KeyPair ecKeyPair;
    private String ecPublicKeyPem;

    @BeforeEach
    void setUp() throws Exception {
        service = new SignatureVerificationService();

        KeyPairGenerator kpg = KeyPairGenerator.getInstance("EC");
        kpg.initialize(new ECGenParameterSpec("secp256r1"));
        ecKeyPair = kpg.generateKeyPair();

        ecPublicKeyPem = Base64.getEncoder().encodeToString(ecKeyPair.getPublic().getEncoded());
    }

    @Test
    @DisplayName("Valid signature passes verification")
    void testValidSignature() throws Exception {
        String hashHex = HexFormat.of().formatHex(
                MessageDigest.getInstance("SHA-256").digest("Sample Ciphertext".getBytes(StandardCharsets.UTF_8))
        );

        Signature sig = Signature.getInstance("SHA256withECDSA");
        sig.initSign(ecKeyPair.getPrivate());
        sig.update(hashHex.getBytes(StandardCharsets.UTF_8));
        String sigBase64 = Base64.getEncoder().encodeToString(sig.sign());

        boolean valid = service.verifySignature(ecPublicKeyPem, hashHex, sigBase64);
        assertThat(valid).isTrue();
    }

    @Test
    @DisplayName("Tampered hash fails verification")
    void testTamperedHashFails() throws Exception {
        String hashHex = HexFormat.of().formatHex(
                MessageDigest.getInstance("SHA-256").digest("Original".getBytes(StandardCharsets.UTF_8))
        );
        String tamperedHashHex = HexFormat.of().formatHex(
                MessageDigest.getInstance("SHA-256").digest("Tampered".getBytes(StandardCharsets.UTF_8))
        );

        Signature sig = Signature.getInstance("SHA256withECDSA");
        sig.initSign(ecKeyPair.getPrivate());
        sig.update(hashHex.getBytes(StandardCharsets.UTF_8));
        String sigBase64 = Base64.getEncoder().encodeToString(sig.sign());

        boolean valid = service.verifySignature(ecPublicKeyPem, tamperedHashHex, sigBase64);
        assertThat(valid).isFalse();
    }

    @Test
    @DisplayName("Forged key fails verification")
    void testForgedKeyFails() throws Exception {
        String hashHex = HexFormat.of().formatHex(
                MessageDigest.getInstance("SHA-256").digest("Original".getBytes(StandardCharsets.UTF_8))
        );

        KeyPairGenerator kpg = KeyPairGenerator.getInstance("EC");
        kpg.initialize(new ECGenParameterSpec("secp256r1"));
        KeyPair forgedKey = kpg.generateKeyPair();

        Signature sig = Signature.getInstance("SHA256withECDSA");
        sig.initSign(forgedKey.getPrivate());
        sig.update(hashHex.getBytes(StandardCharsets.UTF_8));
        String sigBase64 = Base64.getEncoder().encodeToString(sig.sign());

        boolean valid = service.verifySignature(ecPublicKeyPem, hashHex, sigBase64);
        assertThat(valid).isFalse();
    }

    @Test
    @DisplayName("Non-EC public key is rejected during parsing")
    void testNonEcKeyRejected() throws Exception {
        KeyPairGenerator rsaKpg = KeyPairGenerator.getInstance("RSA");
        rsaKpg.initialize(2048);
        KeyPair rsaKey = rsaKpg.generateKeyPair();
        String rsaPublicKeyPem = Base64.getEncoder().encodeToString(rsaKey.getPublic().getEncoded());

        assertThatThrownBy(() -> service.parseEcPublicKey(rsaPublicKeyPem))
                .isInstanceOf(ApiException.class)
                .hasMessageContaining("EC public key");
    }
}
