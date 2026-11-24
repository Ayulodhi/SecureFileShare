package com.securefileshare.auth.service;

import com.securefileshare.auth.entity.FriendRequest;
import com.securefileshare.auth.entity.RequestStatus;
import com.securefileshare.auth.entity.Role;
import com.securefileshare.auth.entity.User;
import com.securefileshare.auth.repository.FriendRequestRepository;
import com.securefileshare.auth.repository.FriendshipRepository;
import com.securefileshare.auth.repository.InviteRepository;
import com.securefileshare.auth.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.util.Optional;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit tests for FriendsService.
 * WHAT: Verifies friend request creation and email notification triggering.
 * WHY: Ensures sending a friend request successfully invokes EmailService.
 */
class FriendsServiceTest {

    private UserRepository userRepository;
    private FriendRequestRepository friendRequestRepository;
    private FriendshipRepository friendshipRepository;
    private InviteRepository inviteRepository;
    private EmailService emailService;
    private FriendsService friendsService;

    @BeforeEach
    void setUp() {
        userRepository = Mockito.mock(UserRepository.class);
        friendRequestRepository = Mockito.mock(FriendRequestRepository.class);
        friendshipRepository = Mockito.mock(FriendshipRepository.class);
        inviteRepository = Mockito.mock(InviteRepository.class);
        emailService = Mockito.mock(EmailService.class);

        friendsService = new FriendsService(
                userRepository,
                friendRequestRepository,
                friendshipRepository,
                inviteRepository,
                emailService
        );
    }

    @Test
    @DisplayName("Should send friend request email when friend request is sent by email")
    void testSendFriendRequestByEmailTriggersEmail() {
        UUID requesterId = UUID.randomUUID();
        String friendEmail = "bob@example.com";

        User requester = new User("alice", "alice@example.com", "hash", Role.SENDER_RECIPIENT);
        requester.setId(requesterId);
        User addressee = new User("bob", friendEmail, "hash", Role.SENDER_RECIPIENT);
        addressee.setId(UUID.randomUUID());

        when(userRepository.findById(requesterId)).thenReturn(Optional.of(requester));
        when(userRepository.findByEmail(friendEmail)).thenReturn(Optional.of(addressee));
        when(friendRequestRepository.countByRequesterAndStatus(requester, RequestStatus.PENDING)).thenReturn(0L);
        when(friendRequestRepository.findByRequesterAndAddressee(requester, addressee)).thenReturn(Optional.empty());
        when(friendRequestRepository.findByRequesterAndAddressee(addressee, requester)).thenReturn(Optional.empty());

        friendsService.sendFriendRequest(requesterId, friendEmail);

        verify(emailService).sendFriendRequestEmail(friendEmail, "alice");
    }

    @Test
    @DisplayName("Should send friend invite email when email is not registered")
    void testSendFriendRequestUnregisteredEmailTriggersInvite() {
        UUID requesterId = UUID.randomUUID();
        String unregisteredEmail = "stranger@example.com";

        User requester = new User("alice", "alice@example.com", "hash", Role.SENDER_RECIPIENT);
        requester.setId(requesterId);

        when(userRepository.findById(requesterId)).thenReturn(Optional.of(requester));
        when(userRepository.findByEmail(unregisteredEmail)).thenReturn(Optional.empty());
        when(userRepository.findByFriendCode(unregisteredEmail)).thenReturn(Optional.empty());

        friendsService.sendFriendRequest(requesterId, unregisteredEmail);

        verify(emailService).sendFriendInviteEmail(anyString(), anyString(), anyString());
    }
}
