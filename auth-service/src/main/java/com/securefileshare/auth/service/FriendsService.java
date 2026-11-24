package com.securefileshare.auth.service;

import com.securefileshare.common.exception.ApiException;
import com.securefileshare.auth.entity.*;
import com.securefileshare.auth.repository.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Service managing friend requests, friendships, and invite links.
 * WHAT: Implements friend requests, state transitions, blocking, un friending, and secure invite code generation.
 * WHY: Powers the social graph and direct recipient sharing capabilities.
 */
@Service
public class FriendsService {

    private static final Logger log = LoggerFactory.getLogger(FriendsService.class);
    private static final long MAX_PENDING_REQUESTS = 20;

    private final UserRepository userRepository;
    private final FriendRequestRepository friendRequestRepository;
    private final FriendshipRepository friendshipRepository;
    private final InviteRepository inviteRepository;
    private final EmailService emailService;
    private final String appBaseUrl;

    public FriendsService(UserRepository userRepository,
                          FriendRequestRepository friendRequestRepository,
                          FriendshipRepository friendshipRepository,
                          InviteRepository inviteRepository,
                          EmailService emailService,
                          @Value("${app.base-url:http://localhost:3000}") String appBaseUrl) {
        this.userRepository = userRepository;
        this.friendRequestRepository = friendRequestRepository;
        this.friendshipRepository = friendshipRepository;
        this.inviteRepository = inviteRepository;
        this.emailService = emailService;
        this.appBaseUrl = appBaseUrl;
    }

    public FriendsService(UserRepository userRepository,
                          FriendRequestRepository friendRequestRepository,
                          FriendshipRepository friendshipRepository,
                          InviteRepository inviteRepository,
                          EmailService emailService) {
        this(userRepository, friendRequestRepository, friendshipRepository, inviteRepository, emailService, "http://localhost:3000");
    }

    @Transactional(readOnly = true)
    public User findByFriendCode(String friendCode) {
        return userRepository.findByFriendCode(friendCode)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "User not found with friend code: " + friendCode));
    }

    @Transactional
    public void sendFriendRequest(UUID requesterId, String emailOrFriendCode) {
        User requester = userRepository.findById(requesterId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Requester not found"));

        String identifier = emailOrFriendCode.toLowerCase().trim();
        Optional<User> addresseeOpt = userRepository.findByEmail(identifier)
                .or(() -> userRepository.findByFriendCode(emailOrFriendCode));

        if (addresseeOpt.isEmpty()) {
            try {
                String inviteCode = "SFS-INV-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
                Instant expiresAt = Instant.now().plusSeconds(7L * 24 * 60 * 60);
                Invite invite = new Invite(requester, inviteCode, expiresAt, 1);
                inviteRepository.save(invite);

                String inviteUrl = appBaseUrl + "/register?invite=" + inviteCode;
                emailService.sendFriendInviteEmail(emailOrFriendCode, requester.getUsername(), inviteUrl);
                log.info("Sent friend invite email with link to unregistered recipient {}", emailOrFriendCode);
            } catch (Exception e) {
                log.warn("Failed to dispatch friend invite email to {}: {}", emailOrFriendCode, e.getMessage());
            }
            return;
        }

        User addressee = addresseeOpt.get();

        if (requester.getId().equals(addressee.getId())) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Cannot send friend request to yourself");
        }

        // Check if addressee has blocked requester -> drop silently per rule 5
        Optional<FriendRequest> reverseBlock = friendRequestRepository.findByRequesterAndAddressee(addressee, requester);
        if (reverseBlock.isPresent() && reverseBlock.get().getStatus() == RequestStatus.BLOCKED) {
            log.info("Blocked user {} attempted to send request to {}", requesterId, addressee.getId());
            return; // Silent drop
        }

        // Check pending outgoing limit (max 20)
        long pendingCount = friendRequestRepository.countByRequesterAndStatus(requester, RequestStatus.PENDING);
        if (pendingCount >= MAX_PENDING_REQUESTS) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Maximum limit of 20 pending friend requests reached");
        }

        // Check if a request already exists between them
        Optional<FriendRequest> existing = friendRequestRepository.findByRequesterAndAddressee(requester, addressee);
        Optional<FriendRequest> reverseExisting = friendRequestRepository.findByRequesterAndAddressee(addressee, requester);

        if (reverseExisting.isPresent() && reverseExisting.get().getStatus() == RequestStatus.PENDING) {
            // Auto-accept if both sent requests to each other
            reverseExisting.get().setStatus(RequestStatus.ACCEPTED);
            friendRequestRepository.save(reverseExisting.get());
            friendshipRepository.save(new Friendship(requester, addressee));
            log.info("Auto-accepted reciprocal friend request between {} and {}", requesterId, addressee.getId());
            return;
        }

        if (existing.isPresent()) {
            FriendRequest req = existing.get();
            if (req.getStatus() == RequestStatus.ACCEPTED) {
                throw new ApiException(HttpStatus.BAD_REQUEST, "Already friends");
            }
            if (req.getStatus() == RequestStatus.PENDING) {
                throw new ApiException(HttpStatus.BAD_REQUEST, "Friend request already pending");
            }
            req.setStatus(RequestStatus.PENDING);
            friendRequestRepository.save(req);
        } else {
            friendRequestRepository.save(new FriendRequest(requester, addressee, RequestStatus.PENDING));
        }

        // Send friend request email notification
        try {
            emailService.sendFriendRequestEmail(addressee.getEmail(), requester.getUsername());
        } catch (Exception e) {
            log.warn("Failed to dispatch friend request email to {}: {}", addressee.getEmail(), e.getMessage());
        }

        log.info("Friend request sent from {} to {}", requesterId, addressee.getId());
    }

    @Transactional
    public void acceptFriendRequest(UUID userId, UUID requestId) {
        FriendRequest request = friendRequestRepository.findById(requestId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Friend request not found"));

        if (!request.getAddressee().getId().equals(userId)) {
            throw new ApiException(HttpStatus.FORBIDDEN, "Not authorized to accept this request");
        }

        if (request.getStatus() != RequestStatus.PENDING) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Friend request is not pending");
        }

        request.setStatus(RequestStatus.ACCEPTED);
        friendRequestRepository.save(request);

        // Save canonical friendship
        friendshipRepository.save(new Friendship(request.getRequester(), request.getAddressee()));
        log.info("User {} accepted friend request from {}", userId, request.getRequester().getId());
    }

    @Transactional
    public void unfriend(UUID userId, UUID friendId) {
        User user = userRepository.findById(userId).orElseThrow();
        User friend = userRepository.findById(friendId).orElseThrow();

        Optional<Friendship> friendship = friendshipRepository.findCanonical(user, friend);
        if (friendship.isPresent()) {
            friendshipRepository.delete(friendship.get());
        }

        // Also update any friend request records between them
        friendRequestRepository.findByRequesterAndAddressee(user, friend).ifPresent(friendRequestRepository::delete);
        friendRequestRepository.findByRequesterAndAddressee(friend, user).ifPresent(friendRequestRepository::delete);

        log.info("User {} unfriended {}", userId, friendId);
    }
}
