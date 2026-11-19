package com.securefileshare.auth.service;

import com.securefileshare.common.exception.ApiException;
import com.securefileshare.auth.entity.Invite;
import com.securefileshare.auth.entity.User;
import com.securefileshare.auth.repository.InviteRepository;
import com.securefileshare.auth.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.UUID;

/**
 * Service managing invite links and codes (Task 4).
 * WHAT: Generates random, unguessable, expiring, single-use or multi-use invite links/codes.
 * WHY: Enables inviting new or existing users securely.
 */
@Service
public class InviteService {

    private static final Logger log = LoggerFactory.getLogger(InviteService.class);

    private final InviteRepository inviteRepository;
    private final UserRepository userRepository;

    public InviteService(InviteRepository inviteRepository, UserRepository userRepository) {
        this.inviteRepository = inviteRepository;
        this.userRepository = userRepository;
    }

    @Transactional
    public Invite createInvite(UUID userId, int maxUses, int validityDays) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "User not found"));

        String inviteCode = "SFS-INV-" + UUID.randomUUID().toString().replace("-", "").substring(0, 16).toUpperCase();
        Instant expiresAt = Instant.now().plus(validityDays > 0 ? validityDays : 7, ChronoUnit.DAYS);

        Invite invite = new Invite(user, inviteCode, expiresAt, maxUses > 0 ? maxUses : 1);
        Invite saved = inviteRepository.save(invite);
        log.info("Created invite code {} for user {}", inviteCode, userId);
        return saved;
    }

    @Transactional(readOnly = true)
    public Invite validateAndGetInvite(String inviteCode) {
        Invite invite = inviteRepository.findByInviteCode(inviteCode)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Invite code not found or invalid"));

        if (invite.isRevoked()) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Invite link has been revoked");
        }

        if (Instant.now().isAfter(invite.getExpiresAt())) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Invite link has expired");
        }

        if (invite.useCount() >= invite.getMaxUses()) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Invite link has reached its maximum usage limit");
        }

        return invite;
    }

    @Transactional
    public void useInvite(String inviteCode) {
        Invite invite = validateAndGetInvite(inviteCode);
        invite.incrementUseCount();
        if (invite.useCount() >= invite.getMaxUses()) {
            invite.setRevoked(true); // auto-revoke single-use or exhausted invites
        }
        inviteRepository.save(invite);
        log.info("Invite code {} used successfully (uses: {}/{})", inviteCode, invite.useCount(), invite.getMaxUses());
    }

    @Transactional
    public void revokeInvite(UUID userId, UUID inviteId) {
        Invite invite = inviteRepository.findById(inviteId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Invite not found"));

        if (!invite.getUser().getId().equals(userId)) {
            throw new ApiException(HttpStatus.FORBIDDEN, "Not authorized to revoke this invite");
        }

        invite.setRevoked(true);
        inviteRepository.save(invite);
        log.info("User {} revoked invite ID {}", userId, inviteId);
    }
}
