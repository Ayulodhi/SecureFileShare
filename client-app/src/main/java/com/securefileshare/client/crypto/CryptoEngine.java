package com.securefileshare.client.crypto;

import org.bouncycastle.jce.provider.BouncyCastleProvider;

import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.*;
import java.security.spec.ECGenParameterSpec;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;
import java.util.HexFormat;

/**
 * Client-side cryptographic engine using Bouncy Castle for zero-knowledge encryption.
 * WHAT: Implements AES-256-GCM, RSA-4096-OAEP key wrapping, ECDSA P-256 signing, and SHA-256 hashing.
 * WHY: Provides robust cryptographic primitives matching the web client for desktop zero-knowledge operations.
 */
public class CryptoEngine {

    private static final String PROVIDER_NAME = BouncyCastleProvider.PROVIDER_NAME;

    static {
        if (Security.getProvider(PROVIDER_NAME) == null) {
            Security.addProvider(new BouncyCastleProvider());
        }
    }

    /**
     * Generates an RSA-4096 key pair for session key wrapping.
     */
    public static KeyPair generateRsaKeyPair() throws Exception {
        KeyPairGenerator kpg = KeyPairGenerator.getInstance("RSA");
        kpg.initialize(4096, new SecureRandom());
        return kpg.generateKeyPair();
    }

    /**
     * Generates an ECDSA P-256 key pair for file integrity signing.
     */
    public static KeyPair generateEcKeyPair() throws Exception {
        KeyPairGenerator kpg = KeyPairGenerator.getInstance("EC");
        kpg.initialize(new ECGenParameterSpec("secp256r1"), new SecureRandom());
        return kpg.generateKeyPair();
    }

    /**
     * Computes the lowercase 64-character SHA-256 hex digest of input bytes.
     */
    public static String computeSha256Hex(byte[] data) throws NoSuchAlgorithmException {
        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        return HexFormat.of().formatHex(digest.digest(data));
    }

    /**
     * Signs a message (usually ciphertext SHA-256 hash) with an ECDSA private key.
     */
    public static String signEcdsa(PrivateKey privateKey, String message) throws Exception {
        Signature signature = Signature.getInstance("SHA256withECDSA");
        signature.initSign(privateKey);
        signature.update(message.getBytes(StandardCharsets.UTF_8));
        return Base64.getEncoder().encodeToString(signature.sign());
    }

    /**
     * Verifies an ECDSA signature against a public key and message.
     */
    public static boolean verifyEcdsa(String publicKeyPem, String message, String signatureBase64) throws Exception {
        byte[] derBytes = Base64.getDecoder().decode(
                publicKeyPem.replace("-----BEGIN PUBLIC KEY-----", "")
                        .replace("-----END PUBLIC KEY-----", "")
                        .replaceAll("\\s", "")
        );
        KeyFactory kf = KeyFactory.getInstance("EC");
        PublicKey ecPublicKey = kf.generatePublic(new X509EncodedKeySpec(derBytes));

        Signature signature = Signature.getInstance("SHA256withECDSA");
        signature.initVerify(ecPublicKey);
        signature.update(message.getBytes(StandardCharsets.UTF_8));
        return signature.verify(Base64.getDecoder().decode(signatureBase64));
    }

    public static PublicKey parsePublicKey(String pem, String algorithm) throws Exception {
        byte[] derBytes = Base64.getDecoder().decode(
                pem.replace("-----BEGIN PUBLIC KEY-----", "")
                        .replace("-----END PUBLIC KEY-----", "")
                        .replaceAll("\\s", "")
        );
        KeyFactory kf = KeyFactory.getInstance(algorithm);
        return kf.generatePublic(new X509EncodedKeySpec(derBytes));
    }

    /**
     * Wraps an AES secret key with an RSA-4096 public key using OAEP padding with SHA-256.
     */
    public static String wrapAesKey(SecretKey aesKey, String recipientRsaPublicKeyPem) throws Exception {
        PublicKey pubKey = parsePublicKey(recipientRsaPublicKeyPem, "RSA");
        Cipher cipher = Cipher.getInstance("RSA/ECB/OAEPWithSHA-256AndMGF1Padding");
        cipher.init(Cipher.WRAP_MODE, pubKey);
        byte[] wrapped = cipher.wrap(aesKey);
        return Base64.getEncoder().encodeToString(wrapped);
    }

    /**
     * Unwraps an AES secret key with an RSA private key.
     */
    public static SecretKey unwrapAesKey(String wrappedKeyBase64, PrivateKey rsaPrivateKey) throws Exception {
        byte[] wrappedBytes = Base64.getDecoder().decode(wrappedKeyBase64);
        Cipher cipher = Cipher.getInstance("RSA/ECB/OAEPWithSHA-256AndMGF1Padding");
        cipher.init(Cipher.UNWRAP_MODE, rsaPrivateKey);
        Key key = cipher.unwrap(wrappedBytes, "AES", Cipher.SECRET_KEY);
        return new SecretKeySpec(key.getEncoded(), "AES");
    }

    public static SecretKey generateAesKey() throws Exception {
        KeyGenerator kg = KeyGenerator.getInstance("AES");
        kg.init(256, new SecureRandom());
        return kg.generateKey();
    }
}
