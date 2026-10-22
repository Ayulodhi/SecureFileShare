package com.securefileshare.auth.crypto;

import com.securefileshare.common.constant.SecurityConstants;
import com.securefileshare.common.exception.ApiException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

import java.security.KeyFactory;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.PublicKey;
import java.security.interfaces.ECPublicKey;
import java.security.interfaces.RSAPublicKey;
import java.security.spec.ECParameterSpec;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;

/**
 * Cryptographic validation utility component for public keys.
 * WHAT: Parses, validates, and computes SHA-256 fingerprints for RSA-4096 and EC P-256 public keys.
 * WHY: CRITICAL ZERO-KNOWLEDGE SECURITY GUARANTEE: Ensures submitted public keys meet strict cryptographic standards
 *      before registration, defending against weak-key attacks and ensuring wrapped file keys cannot be factored.
 */
@Component
public class KeyValidator {

    /**
     * Validates that the provided RSA public key is well-formed and exactly 4096 bits.
     *
     * @param pemOrBase64 PEM or base64 encoded X.509 SubjectPublicKeyInfo
     * @return Validated RSAPublicKey instance
     * @throws ApiException if key is invalid, malformed, or has modulus length != 4096
     */
    public RSAPublicKey validateRsaPublicKey(String pemOrBase64) {
        // Strip PEM headers and whitespace to isolate base64 encoded DER bytes
        byte[] derBytes = decodePemOrBase64(pemOrBase64);

        try {
            // Instantiate standard Java RSA KeyFactory
            KeyFactory keyFactory = KeyFactory.getInstance("RSA");
            X509EncodedKeySpec keySpec = new X509EncodedKeySpec(derBytes);
            PublicKey publicKey = keyFactory.generatePublic(keySpec);

            // Verify the generated key is an RSAPublicKey
            if (!(publicKey instanceof RSAPublicKey rsaKey)) {
                // Why: Defends against algorithm substitution attacks
                throw new ApiException(HttpStatus.BAD_REQUEST, "Provided key is not a valid RSA public key");
            }

            // Security Decision: Validate exact modulus bit length
            // Why: Prevents weak RSA keys (< 4096 bits) which are vulnerable to factorization attacks
            int bitLength = rsaKey.getModulus().bitLength();
            if (bitLength != SecurityConstants.REQUIRED_RSA_KEY_SIZE_BITS) {
                throw new ApiException(HttpStatus.BAD_REQUEST,
                        "RSA public key must be exactly 4096 bits. Provided: " + bitLength + " bits");
            }

            return rsaKey;
        } catch (ApiException ex) {
            throw ex;
        } catch (Exception ex) {
            // Why: Prevents ASN.1 parser stack traces from leaking internal cryptographic details
            throw new ApiException(HttpStatus.BAD_REQUEST, "Invalid or unparseable RSA public key: " + ex.getMessage());
        }
    }

    /**
     * Validates that the provided EC public key is well-formed and uses the NIST P-256 (secp256r1) curve.
     *
     * @param pemOrBase64 PEM or base64 encoded X.509 SubjectPublicKeyInfo
     * @return Validated ECPublicKey instance
     * @throws ApiException if key is invalid or uses an unsupported/weak elliptic curve
     */
    public ECPublicKey validateEcPublicKey(String pemOrBase64) {
        // Strip PEM headers and whitespace to isolate base64 encoded DER bytes
        byte[] derBytes = decodePemOrBase64(pemOrBase64);

        try {
            // Instantiate standard Java EC KeyFactory
            KeyFactory keyFactory = KeyFactory.getInstance("EC");
            X509EncodedKeySpec keySpec = new X509EncodedKeySpec(derBytes);
            PublicKey publicKey = keyFactory.generatePublic(keySpec);

            // Verify the generated key is an ECPublicKey
            if (!(publicKey instanceof ECPublicKey ecKey)) {
                // Why: Defends against key type substitution
                throw new ApiException(HttpStatus.BAD_REQUEST, "Provided key is not a valid EC public key");
            }

            // Verify curve parameters match 256-bit prime field
            // Why: NIST P-256 curve provides 128-bit security level for ECDSA signatures and prevents weak-curve attacks
            ECParameterSpec params = ecKey.getParams();
            int fieldSize = params.getCurve().getField().getFieldSize();
            if (fieldSize != 256) {
                throw new ApiException(HttpStatus.BAD_REQUEST,
                        "EC public key must use a 256-bit curve (secp256r1/NIST P-256). Provided: " + fieldSize + " bits");
            }

            return ecKey;
        } catch (ApiException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Invalid or unparseable EC public key: " + ex.getMessage());
        }
    }

    /**
     * Computes the SHA-256 fingerprint over the normalized DER encoded bytes of a public key.
     *
     * @param key Public key to fingerprint
     * @return 64-character lowercase hexadecimal SHA-256 digest
     */
    public String computeFingerprint(PublicKey key) {
        try {
            // Why: Hashing the raw DER encoded bytes ensures deterministic, format-independent fingerprints
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hashBytes = digest.digest(key.getEncoded());

            // Convert digest bytes into 64-character lowercase hex string
            StringBuilder hexString = new StringBuilder();
            for (byte b : hashBytes) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) {
                    hexString.append('0');
                }
                hexString.append(hex);
            }
            return hexString.toString();
        } catch (NoSuchAlgorithmException ex) {
            // SHA-256 is guaranteed to exist in standard Java SE runtime
            throw new IllegalStateException("Standard SHA-256 digest algorithm missing from runtime", ex);
        }
    }

    /**
     * Strips standard PEM headers/footers, carriage returns, and newlines, decoding remaining Base64 bytes.
     *
     * @param input Raw PEM or Base64 string
     * @return Decoded raw DER byte array
     */
    private byte[] decodePemOrBase64(String input) {
        if (input == null || input.isBlank()) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Public key cannot be null or blank");
        }

        // Clean out PEM headers, footers, carriage returns, and spaces
        String clean = input
                .replaceAll("-----BEGIN [A-Z ]+-----", "")
                .replaceAll("-----END [A-Z ]+-----", "")
                .replaceAll("\\s+", "");

        try {
            return Base64.getDecoder().decode(clean);
        } catch (IllegalArgumentException ex) {
            // Why: Sanitizes Base64 decoding errors
            throw new ApiException(HttpStatus.BAD_REQUEST, "Malformed Base64 payload in public key string");
        }
    }
}
