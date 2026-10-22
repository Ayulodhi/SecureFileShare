package com.securefileshare.file.crypto;

import com.securefileshare.common.exception.ApiException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.security.*;
import java.security.interfaces.ECPublicKey;
import java.security.spec.ECParameterSpec;
import java.security.spec.X509EncodedKeySpec;
import java.util.Arrays;
import java.util.Base64;
import java.util.HexFormat;

/**
 * Service responsible for validating client ECDSA digital signatures.
 * WHAT: Verifies SHA256withECDSA signatures over ciphertext hashes using the owner's NIST P-256 EC public key.
 * WHY: Enforces cryptographic proof of file origin and non-repudiation: guarantees the file was encrypted and
 *      authorized by the private key holder without the server ever holding that private key.
 */
@Service
public class SignatureVerificationService {

    private static final Logger log = LoggerFactory.getLogger(SignatureVerificationService.class);

    /**
     * Verifies an ECDSA signature over the ciphertext SHA-256 hash using the owner's EC public key.
     * WHAT: Decodes public key and signature, then evaluates cryptographic validity.
     * WHY: Confirms that the ciphertext was signed by the registered file owner.
     *
     * @param ecPublicKeyPem  Owner's registered EC public key in PEM or Base64 format
     * @param ciphertextHash  64-character hexadecimal SHA-256 hash of the ciphertext
     * @param signatureBase64 Base64-encoded ECDSA signature
     * @return true if the signature is cryptographically valid
     */
    public boolean verifySignature(String ecPublicKeyPem, String ciphertextHash, String signatureBase64) {
        if (ecPublicKeyPem == null || ecPublicKeyPem.isBlank()) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "EC public key cannot be null or blank");
        }
        if (ciphertextHash == null || ciphertextHash.isBlank() || ciphertextHash.length() != 64) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Invalid ciphertext SHA-256 hash format (must be 64 hex characters)");
        }
        if (signatureBase64 == null || signatureBase64.isBlank()) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "ECDSA signature cannot be null or blank");
        }

        try {
            // Place-level comment: Parse the owner's EC public key and verify NIST P-256 parameters
            // Why: Prevents weak curve attacks and key substitution
            ECPublicKey publicKey = parseEcPublicKey(ecPublicKeyPem);

            // Decode the Base64 signature bytes
            byte[] rawSigBytes = Base64.getDecoder().decode(signatureBase64.replaceAll("\\s+", ""));

            // Convert IEEE P1363 (64 bytes: r||s) to ASN.1 DER if necessary
            // Why: WebCrypto in browsers produces 64-byte P1363 signatures, whereas Java Signature expects ASN.1 DER
            byte[] derSigBytes = isDerEncoded(rawSigBytes) ? rawSigBytes : convertP1363ToDer(rawSigBytes);

            byte[] hashHexBytes = ciphertextHash.getBytes(StandardCharsets.UTF_8);
            byte[] rawHashBytes = HexFormat.of().parseHex(ciphertextHash);

            // Attempt verification with SHA256withECDSA over hex string bytes
            if (tryVerify("SHA256withECDSA", publicKey, hashHexBytes, derSigBytes)) {
                return true;
            }

            // Attempt verification with SHA256withECDSA over raw digest bytes
            if (tryVerify("SHA256withECDSA", publicKey, rawHashBytes, derSigBytes)) {
                return true;
            }

            // Attempt verification with NONEwithECDSA over raw 32-byte digest
            if (tryVerify("NONEwithECDSA", publicKey, rawHashBytes, derSigBytes)) {
                return true;
            }

            log.warn("ECDSA signature verification failed against all canonical candidate formats");
            return false;
        } catch (ApiException ex) {
            throw ex;
        } catch (Exception ex) {
            log.error("Failed to verify ECDSA signature: {}", ex.getMessage());
            throw new ApiException(HttpStatus.BAD_REQUEST, "Invalid or unparseable ECDSA signature: " + ex.getMessage());
        }
    }

    /**
     * Executes a single Signature verification pass for a given algorithm.
     */
    private boolean tryVerify(String algorithm, PublicKey key, byte[] data, byte[] sigBytes) {
        try {
            Signature signature = Signature.getInstance(algorithm);
            signature.initVerify(key);
            signature.update(data);
            return signature.verify(sigBytes);
        } catch (Exception ex) {
            log.trace("Algorithm {} check failed or threw: {}", algorithm, ex.getMessage());
            return false;
        }
    }

    /**
     * Parses and validates a NIST P-256 EC public key from PEM or Base64 X.509 format.
     */
    public ECPublicKey parseEcPublicKey(String pemOrBase64) {
        String clean = pemOrBase64
                .replaceAll("-----BEGIN [A-Z ]+-----", "")
                .replaceAll("-----END [A-Z ]+-----", "")
                .replaceAll("\\s+", "");

        byte[] derBytes = Base64.getDecoder().decode(clean);

        try {
            KeyFactory keyFactory = KeyFactory.getInstance("EC");
            X509EncodedKeySpec keySpec = new X509EncodedKeySpec(derBytes);
            PublicKey publicKey = keyFactory.generatePublic(keySpec);

            if (!(publicKey instanceof ECPublicKey ecKey)) {
                throw new ApiException(HttpStatus.BAD_REQUEST, "Key is not a valid EC public key");
            }

            ECParameterSpec params = ecKey.getParams();
            int fieldSize = params.getCurve().getField().getFieldSize();
            if (fieldSize != 256) {
                throw new ApiException(HttpStatus.BAD_REQUEST,
                        "EC public key must use a 256-bit curve (NIST P-256). Provided: " + fieldSize + " bits");
            }

            return ecKey;
        } catch (ApiException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Unparseable EC public key: " + ex.getMessage());
        }
    }

    /**
     * Checks if the signature bytes start with ASN.1 SEQUENCE tag (0x30).
     */
    private boolean isDerEncoded(byte[] sigBytes) {
        return sigBytes.length > 0 && sigBytes[0] == 0x30;
    }

    /**
     * Converts an IEEE P1363 (64-byte r||s) ECDSA signature into ASN.1 DER format.
     * Sequence: 0x30 [len] 0x02 [r_len] [r_bytes] 0x02 [s_len] [s_bytes]
     */
    private byte[] convertP1363ToDer(byte[] p1363) {
        if (p1363.length != 64) {
            // Not a standard 64-byte P-256 P1363 signature; return as-is
            return p1363;
        }

        try {
            byte[] rBytes = Arrays.copyOfRange(p1363, 0, 32);
            byte[] sBytes = Arrays.copyOfRange(p1363, 32, 64);

            BigInteger r = new BigInteger(1, rBytes);
            BigInteger s = new BigInteger(1, sBytes);

            byte[] rDer = r.toByteArray();
            byte[] sDer = s.toByteArray();

            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            baos.write(0x30); // SEQUENCE tag
            baos.write(rDer.length + sDer.length + 4); // Total length
            baos.write(0x02); // INTEGER tag for r
            baos.write(rDer.length);
            baos.write(rDer);
            baos.write(0x02); // INTEGER tag for s
            baos.write(sDer.length);
            baos.write(sDer);

            return baos.toByteArray();
        } catch (IOException ex) {
            return p1363;
        }
    }
}
