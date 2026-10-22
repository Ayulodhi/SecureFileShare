package com.securefileshare.client.config;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Properties;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for the AppConfig loader.
 * WHAT: Verifies properties loading from classpath, input streams, and programmatic Properties.
 * WHY: Validates configuration switch between mock and real modes and URL resolution.
 */
class AppConfigTest {

    @Test
    @DisplayName("AppConfig should load client.properties defaults from classpath")
    void testClasspathConfigLoading() {
        AppConfig config = new AppConfig();

        assertThat(config.getMode()).isEqualTo("mock");
        assertThat(config.isMockMode()).isTrue();
        assertThat(config.getAuthServiceUrl()).isEqualTo("http://localhost:8081");
        assertThat(config.getFileServiceUrl()).isEqualTo("http://localhost:8082");
        assertThat(config.getShareServiceUrl()).isEqualTo("http://localhost:8083");
        assertThat(config.getAuditServiceUrl()).isEqualTo("http://localhost:8084");
    }

    @Test
    @DisplayName("isMockMode should return true for 'mock' and false for 'real'")
    void testMockModeDetection() {
        AppConfig config = new AppConfig();

        config.setMode("mock");
        assertThat(config.isMockMode()).isTrue();

        config.setMode("MOCK");
        assertThat(config.isMockMode()).isTrue();

        config.setMode("real");
        assertThat(config.isMockMode()).isFalse();

        config.setMode("production");
        assertThat(config.isMockMode()).isFalse();
    }

    @Test
    @DisplayName("applyProperties should override configuration fields")
    void testApplyProperties() {
        AppConfig config = new AppConfig();

        Properties customProps = new Properties();
        customProps.setProperty(AppConfig.KEY_MODE, "real");
        customProps.setProperty(AppConfig.KEY_AUTH_URL, "https://auth.securefileshare.io");
        customProps.setProperty(AppConfig.KEY_FILE_URL, "https://file.securefileshare.io");
        customProps.setProperty(AppConfig.KEY_SHARE_URL, "https://share.securefileshare.io");
        customProps.setProperty(AppConfig.KEY_AUDIT_URL, "https://audit.securefileshare.io");

        config.applyProperties(customProps);

        assertThat(config.getMode()).isEqualTo("real");
        assertThat(config.isMockMode()).isFalse();
        assertThat(config.getAuthServiceUrl()).isEqualTo("https://auth.securefileshare.io");
        assertThat(config.getFileServiceUrl()).isEqualTo("https://file.securefileshare.io");
        assertThat(config.getShareServiceUrl()).isEqualTo("https://share.securefileshare.io");
        assertThat(config.getAuditServiceUrl()).isEqualTo("https://audit.securefileshare.io");
    }

    @Test
    @DisplayName("loadFromStream should parse properties format correctly")
    void testLoadFromStream() throws IOException {
        AppConfig config = new AppConfig();

        String sampleProperties = """
                client.mode=real
                service.auth.url=http://10.0.0.1:8081
                service.file.url=http://10.0.0.1:8082
                service.share.url=http://10.0.0.1:8083
                service.audit.url=http://10.0.0.1:8084
                """;

        try (var in = new ByteArrayInputStream(sampleProperties.getBytes(StandardCharsets.UTF_8))) {
            config.loadFromStream(in);
        }

        assertThat(config.isMockMode()).isFalse();
        assertThat(config.getAuthServiceUrl()).isEqualTo("http://10.0.0.1:8081");
        assertThat(config.getFileServiceUrl()).isEqualTo("http://10.0.0.1:8082");
    }
}
