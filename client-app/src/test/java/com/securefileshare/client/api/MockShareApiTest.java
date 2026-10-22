package com.securefileshare.client.api;

import com.securefileshare.client.api.mock.MockShareApi;
import com.securefileshare.client.model.ShareRecordDto;
import com.securefileshare.client.model.ShareStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.NoSuchElementException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Unit tests for MockShareApi.
 * WHAT: Asserts retrieval of active, expired, and revoked shares, and validates revocation logic.
 * WHY: Ensures that share lifecycle transitions and recipient access revocation work reliably.
 */
class MockShareApiTest {

    private MockShareApi mockShareApi;

    @BeforeEach
    void setUp() {
        mockShareApi = new MockShareApi();
    }

    @Test
    @DisplayName("getSharesCreated should return sample shares including Active, Expired, and Revoked")
    void testGetSharesCreated() throws Exception {
        List<ShareRecordDto> shares = mockShareApi.getSharesCreated();

        assertThat(shares).hasSize(3);

        ShareRecordDto s1 = shares.get(0);
        assertThat(s1.getFileName()).isEqualTo("contract-final.pdf");
        assertThat(s1.getRecipient()).isEqualTo("rahul");
        assertThat(s1.getStatus()).isEqualTo(ShareStatus.ACTIVE);
        assertThat(s1.canRevoke()).isTrue();

        ShareRecordDto s2 = shares.get(1);
        assertThat(s2.getStatus()).isEqualTo(ShareStatus.EXPIRED);
        assertThat(s2.canRevoke()).isFalse();

        ShareRecordDto s3 = shares.get(2);
        assertThat(s3.getStatus()).isEqualTo(ShareStatus.REVOKED);
        assertThat(s3.canRevoke()).isFalse();
    }

    @Test
    @DisplayName("revokeShare should transition active share to REVOKED status")
    void testRevokeShare() throws Exception {
        mockShareApi.revokeShare("share-101");

        List<ShareRecordDto> shares = mockShareApi.getSharesCreated();
        ShareRecordDto s1 = shares.stream()
                .filter(s -> "share-101".equals(s.getShareId()))
                .findFirst()
                .orElseThrow();

        assertThat(s1.getStatus()).isEqualTo(ShareStatus.REVOKED);
        assertThat(s1.canRevoke()).isFalse();
    }

    @Test
    @DisplayName("revokeShare should throw NoSuchElementException on non-existent shareId")
    void testRevokeNonExistentShare() {
        assertThatThrownBy(() -> mockShareApi.revokeShare("non-existent-share-999"))
                .isInstanceOf(NoSuchElementException.class);
    }
}
