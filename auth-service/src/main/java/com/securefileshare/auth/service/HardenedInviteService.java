package com.securefileshare.auth.service;

import com.securefileshare.common.exception.ApiException;
import com.securefileshare.auth.entity.Invite;
import com.securefileshare.auth.entity.User;
import com.securefileshare.auth.repository.InviteRepository;
import com.securefileshare.auth.repository.UserRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.HexFormat;
import java.util.UUID;

/**
 * Hardened InviteService storing hashed invite codes and enforcing atomic use counting.
 */
@Service
public class HardenedInviteService {

    private final InviteRepository inviteRepository;
    private final UserRepository userRepository;

    public HardenedInviteService(InviteRepository inviteRepository, UserRepository userRepository) {
        this.inviteRepository = inviteRepository;
        this.userRepository = userRepository;
    }

    private String hashInviteCode(String code) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(code.trim().toUpperCase().getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash);
        } catch (Exception e) {
            throw new RuntimeException("SHA-256 not available", e);
        }
    }

    @Transactional
    public Invite createInvite(UUID userId, int maxUses, int validityDays) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "User not found"));

        String rawCode = "SFS-INV-" + UUID.randomUUID().toString().replace("-", "").substring(0, 16).toUpperCase();
        String hashedCode = hashInviteCode(rawCode);
        Instant expiresAt = Instant.now().plus(validityDays > 0 ? validityDays : 7, ChronoUnit.DAYS);

        Invite invite = new Invite(user, hashedCode, expiresAt, maxUses > 0 ? maxUses : 1);
        return inviteRepository.save(invite);
    }

    @Transactional(readOnly = true)
    public Invite validateAndGetInvite(String rawCode) {
        String hashedCode = hashInviteCode(rawCode);
        Invite invite = inviteRepository.findByInviteCode(hashedCode)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Invite code not found or invalid"));

        if (invite.isRevoked() || Instant.now().isAfter(invite.getExpiresAt()) || invite.useCount() >= invite.getMaxUses()) {
            // Uniform identical response for invalid, expired, or revoked codes per instructions
            throw new ApiException(HttpStatus.NOT_FOUND, "Invite code not found or invalid");
        }

        return invite;
    }

    @Transactional
    public synchronized void useInvite(String rawCode) {
        Invite invite = validateAndGetInvite(rawCode);
        invite.incrementUseCount();
        if (invite.useCount() >= invite.getMaxUses()) {
            invite.setRevoked(true);
        }
        inviteRepository.save(invite);
    }
}
