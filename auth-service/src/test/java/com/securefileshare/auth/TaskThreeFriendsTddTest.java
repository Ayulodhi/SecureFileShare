package com.securefileshare.auth;

import com.securefileshare.auth.entity.RequestStatus;
import com.securefileshare.auth.entity.User;
import com.securefileshare.auth.entity.Role;
import com.securefileshare.auth.repository.FriendRequestRepository;
import com.securefileshare.auth.repository.UserRepository;
import com.securefileshare.auth.service.FriendsService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

import java.util.concurrent.*;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
public class TaskThreeFriendsTddTest extends BaseAuthIntegrationTest {

    @DynamicPropertySource
    static void configureExtraProperties(DynamicPropertyRegistry registry) {
        registry.add("security.admin.initial-username", () -> "admin");
        registry.add("security.admin.initial-email", () -> "admin@test.com");
        registry.add("security.admin.initial-password", () -> "AdminSecurePass2026!");
    }

    @Autowired
    private FriendsService friendsService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private FriendRequestRepository friendRequestRepository;

    private User alice;
    private User bob;

    @BeforeEach
    void setUp() {
        friendRequestRepository.deleteAll();
        userRepository.deleteAll();

        alice = userRepository.save(new User("alice", "alice@test.com", "hash", Role.SENDER_RECIPIENT));
        bob = userRepository.save(new User("bob", "bob@test.com", "hash", Role.SENDER_RECIPIENT));
    }

    @Test
    @DisplayName("Task 3 TDD: Friend request send, accept, and canonical friendship creation")
    void testFriendRequestSendAndAccept() {
        friendsService.sendFriendRequest(alice.getId(), bob.getFriendCode());
        var pending = friendRequestRepository.findByAddresseeAndStatus(bob, RequestStatus.PENDING);
        assertThat(pending).hasSize(1);

        friendsService.acceptFriendRequest(bob.getId(), pending.get(0).getId());
        var updated = friendRequestRepository.findById(pending.get(0).getId()).get();
        assertThat(updated.getStatus()).isEqualTo(RequestStatus.ACCEPTED);
    }

    @Test
    @DisplayName("Task 3 TDD: Self request throws exception")
    void testSelfRequestThrows() {
        assertThatThrownBy(() -> friendsService.sendFriendRequest(alice.getId(), alice.getFriendCode()))
                .isInstanceOf(RuntimeException.class);
    }
}
