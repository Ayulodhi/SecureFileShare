package com.securefileshare.auth.entity;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class FriendshipTest {

    @Test
    @DisplayName("Should order user IDs canonically using unsigned UUID comparison matching PostgreSQL")
    void testCanonicalOrdering() {
        // Create UUIDs where signed vs unsigned comparison differs
        // e.g. UUID with high bit set vs UUID with 0
        UUID uLow = new UUID(0x0000000000000000L, 0x0000000000000001L);
        UUID uHigh = new UUID(0x8000000000000000L, 0x0000000000000000L);

        User userLow = new User("low", "low@test.com", "hash", Role.SENDER_RECIPIENT);
        userLow.setId(uLow);

        User userHigh = new User("high", "high@test.com", "hash", Role.SENDER_RECIPIENT);
        userHigh.setId(uHigh);

        // Regardless of argument order, userLow should always be userId1 and userHigh should be userId2
        Friendship f1 = new Friendship(userLow, userHigh);
        assertThat(f1.getUserId1().getId()).isEqualTo(uLow);
        assertThat(f1.getUserId2().getId()).isEqualTo(uHigh);

        Friendship f2 = new Friendship(userHigh, userLow);
        assertThat(f2.getUserId1().getId()).isEqualTo(uLow);
        assertThat(f2.getUserId2().getId()).isEqualTo(uHigh);
    }
}
