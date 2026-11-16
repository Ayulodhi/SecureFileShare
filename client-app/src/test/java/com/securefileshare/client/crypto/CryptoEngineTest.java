package com.securefileshare.client.crypto;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import javax.crypto.SecretKey;
import java.security.KeyPair;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for desktop client CryptoEngine (Bouncy Castle).
 * WHAT: Verifies RSA key wrapping, ECDSA signing/verification, and SHA-256 hashing.
 * WHY: Ensures desktop client cryptographic operations match zero-knowledge standards.
 */
class CryptoEngineTest {

    @Test
    @DisplayName("RSA-4096 OAEP key wrapping and unwrapping round-trip")
    void testRsaKeyWrappingRoundTrip() throws Exception {
        KeyPair rsaPair = CryptoEngine.generateRsaKeyPair();
        String rsaPem = KeystoreService.toPem("PUBLIC KEY", rsaPair.getPublic().getEncoded());

        SecretKey aesKey = CryptoEngine.generateAesKey();
        String wrappedKey = CryptoEngine.wrapAesKey(aesKey, rsaPem);
        assertThat(wrappedKey).isNotBlank();

        SecretKey unwrapped = CryptoEngine.unwrapAesKey(wrappedKey, rsaPair.getPrivate());
        assertThat(unwrapped).isNotNull();
        assertThat(unwrapped.getEncoded()).isEqualTo(aesKey.getEncoded());
    }

    @Test
    @DisplayName("ECDSA P-256 signing and signature verification round-trip")
    void testEcdsaSigningAndVerification() throws Exception {
        KeyPair ecPair = CryptoEngine.generateEcKeyPair();
        String ecPem = KeystoreService.toPem("PUBLIC KEY", ecPair.getPublic().getEncoded());

        String messageHash = CryptoEngine.computeSha256Hex("Confidential File Payload".getBytes());
        String signature = CryptoEngine.signEcdsa(ecPair.getPrivate(), messageHash);
        assertThat(signature).isNotBlank();

        boolean verified = CryptoEngine.verifyEcdsa(ecPem, messageHash, signature);
        assertThat(verified).isTrue();
    }

    @Test
    @DisplayName("SHA-256 hash computation returns 64 hex characters")
    void testSha256Hex() throws Exception {
        String hash = CryptoEngine.computeSha256Hex("test data".getBytes());
        assertThat(hash).hasSize(64);
    }
}
