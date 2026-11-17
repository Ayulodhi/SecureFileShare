package com.securefileshare.client.api;

import javax.net.ssl.X509TrustManager;
import java.security.MessageDigest;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.Base64;

/**
 * TrustManager implementing SHA-256 certificate public key pinning.
 * WHAT: Verifies server certificate chain against a pre-configured expected SHA-256 pin.
 * WHY: Prevents MITM attacks even if a compromised CA issues a valid cert.
 */
public class PinningTrustManager implements X509TrustManager {

    private final String expectedPin; // e.g. "sha256/Base64Hash"

    public PinningTrustManager(String expectedPin) {
        this.expectedPin = expectedPin;
    }

    @Override
    public void checkClientTrusted(X509Certificate[] chain, String authType) throws CertificateException {
        throw new CertificateException("Client certificates not supported");
    }

    @Override
    public void checkServerTrusted(X509Certificate[] chain, String authType) throws CertificateException {
        if (chain == null || chain.length == 0) {
            throw new CertificateException("Server certificate chain is empty");
        }

        X509Certificate serverCert = chain[0];
        String calculatedPin = calculatePin(serverCert);

        if (!calculatedPin.equals(expectedPin)) {
            throw new CertificateException("Certificate pinning verification failed! Expected pin: "
                    + expectedPin + ", but got: " + calculatedPin);
        }
    }

    @Override
    public X509Certificate[] getAcceptedIssuers() {
        return new X509Certificate[0];
    }

    public static String calculatePin(X509Certificate cert) throws CertificateException {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] der = cert.getPublicKey().getEncoded();
            byte[] digest = md.digest(der);
            return "sha256/" + Base64.getEncoder().encodeToString(digest);
        } catch (Exception e) {
            throw new CertificateException("Failed to calculate certificate pin", e);
        }
    }
}
