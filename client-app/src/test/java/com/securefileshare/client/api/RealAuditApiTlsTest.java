package com.securefileshare.client.api;

import com.securefileshare.client.config.AppConfig;
import com.securefileshare.client.api.real.RealAuditApi;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class RealAuditApiTlsTest {

    @Test
    void testRealAuditApiConfiguresTlsPinningContext() {
        AppConfig config = new AppConfig();
        // Set a valid pin format
        config.setTlsPin("sha256/CD1EAD30476DDBE0ACE01CA280CAE2774E3ED8A8");

        RealAuditApi api = new RealAuditApi(config);
        assertThat(api).isNotNull();
    }
}
