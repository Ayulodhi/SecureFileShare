package com.securefileshare.client.crypto;

import java.security.KeyPair;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.util.Base64;

/**
 * Keystore service managing local key pair storage and PEM formatting.
 * WHAT: Handles SPKI PEM exports and key fingerprint generation.
 * WHY: Enables zero-knowledge identity management on the desktop client.
 */
public class KeystoreService {

    public static String toPem(String type, byte[] derBytes) {
        String base64 = Base64.getEncoder().encodeToString(derBytes);
        StringBuilder sb = new StringBuilder();
        sb.append("-----BEGIN ").append(type).append("-----\n");
        int index = 0;
        while (index < base64.length()) {
            int endIndex = Math.min(index + 64, base64.length());
            sb.append(base64, index, endIndex).append("\n");
            index = endIndex;
        }
        sb.append("-----END ").append(type).append("-----\n");
        return sb.toString();
    }

    public static String computeFingerprint(PublicKey publicKey) throws Exception {
        byte[] encoded = publicKey.getEncoded();
        return CryptoEngine.computeSha256Hex(encoded);
    }
}
