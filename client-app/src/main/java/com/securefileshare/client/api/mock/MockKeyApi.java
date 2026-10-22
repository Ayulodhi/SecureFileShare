package com.securefileshare.client.api.mock;

import com.securefileshare.client.api.KeyApi;
import com.securefileshare.client.model.KeyDetailsDto;

/**
 * Mock implementation of KeyApi supplying user's cryptographic fingerprints.
 * WHAT: Provides pre-computed SHA-256 fingerprints for RSA-4096 and EC P-256 keys.
 * WHY: Powers the "My keys" panel display and verification controls without requiring live keystore unlocks.
 */
public class MockKeyApi implements KeyApi {

    @Override
    public KeyDetailsDto getMyKeys() throws Exception {
        // Artificial delay for loading states
        Thread.sleep(200);

        return new KeyDetailsDto(
                "rahul.sharma",
                "7B:A2:3F:89:4C:D1:6E:90:5B:3C:2A:F8:10:92:E4:67:38:D9:F0:12:88:AC:33:41:9A:E2:B5:C8:D3:5F:6A:11",
                "1C:89:D4:5E:2A:7B:60:F1:4D:33:9A:88:EE:12:45:90:7A:B2:6E:3F:55:18:29:CA:41:09:FE:23:44:81:AA:BC",
                "October 12, 2026, 09:30 AM UTC",
                "~/.securefileshare/keystore.p12 (PKCS12)"
        );
    }
}
