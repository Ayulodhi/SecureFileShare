package com.securefileshare.client.api;

import org.junit.jupiter.api.Test;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;
import java.security.MessageDigest;
import java.security.cert.X509Certificate;
import java.util.Base64;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * TDD Test for TLS Certificate Pinning in Desktop Client API.
 * WHAT: Verifies that certificates with incorrect or unknown pins/fingerprints are rejected,
 *       while correctly pinned certificates are accepted.
 */
public class TlsPinningTest {

    @Test
    public void testPinningTrustManagerRejectsMismatchedCertificate() throws Exception {
        // Expected pinned SHA-256 fingerprint (mock expected value)
        String expectedPin = "sha256/AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA=";

        PinningTrustManager trustManager = new PinningTrustManager(expectedPin);

        // Generate a mock certificate chain
        X509Certificate mockCert = MockCertificateGenerator.generateMockCertificate("CN=SecureFileShare Test");
        X509Certificate[] chain = new X509Certificate[]{ mockCert };

        // Should throw CertificateException because fingerprint does not match expected pin
        assertThatThrownBy(() -> trustManager.checkServerTrusted(chain, "RSA"))
                .isInstanceOf(Exception.class)
                .hasMessageContaining("Certificate pinning verification failed");
    }

    @Test
    public void testPinningTrustManagerAcceptsMatchingCertificate() throws Exception {
        X509Certificate mockCert = MockCertificateGenerator.generateMockCertificate("CN=SecureFileShare Test");
        String calculatedPin = PinningTrustManager.calculatePin(mockCert);

        PinningTrustManager trustManager = new PinningTrustManager(calculatedPin);
        X509Certificate[] chain = new X509Certificate[]{ mockCert };

        // Should not throw when fingerprint matches
        trustManager.checkServerTrusted(chain, "RSA");
        assertThat(calculatedPin).startsWith("sha256/");
    }
}
