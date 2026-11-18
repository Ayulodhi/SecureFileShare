package com.securefileshare.share.service;

import com.securefileshare.common.exception.ApiException;
import com.securefileshare.share.client.AuditServiceClient;
import com.securefileshare.share.dto.CreateShareRequest;
import com.securefileshare.share.dto.ShareResponse;
import com.securefileshare.share.entity.ShareRecord;
import com.securefileshare.share.repository.ShareRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * Service managing cryptographic share link creation, retrieval, expiration, and revocation.
 * WHAT: Coordinates database persistence for shares, computes expiry durations, and verifies ownership.
 * WHY: Enforces zero-knowledge secure file sharing rules between users.
 */
@Service
public class ShareService {

    private static final Logger log = LoggerFactory.getLogger(ShareService.class);

    private final ShareRepository shareRepository;
    private final AuditServiceClient auditServiceClient;

    public ShareService(ShareRepository shareRepository, AuditServiceClient auditServiceClient) {
        this.shareRepository = shareRepository;
        this.auditServiceClient = auditServiceClient;
    }

    /**
     * Retrieves all share links created by the authenticated owner.
     */
    @Transactional(readOnly = true)
    public List<ShareResponse> getSharesCreated(UUID ownerId, String baseUrl) {
        log.debug("Listing shares created by user {}", ownerId);
        List<ShareRecord> records = shareRepository.findByOwnerIdOrderByCreatedAtDesc(ownerId);
        Instant now = Instant.now();
        for (ShareRecord record : records) {
            if ("ACTIVE".equals(record.getStatus()) && record.getExpiresAt().isBefore(now)) {
                record.setStatus("EXPIRED");
            }
        }
        return records.stream()
                .map(r -> ShareResponse.fromEntity(r, baseUrl))
                .toList();
    }

    /**
     * Creates a new cryptographically bound, expiring share link.
     */
    @Transactional
    public ShareResponse createShare(UUID ownerId, CreateShareRequest request, String baseUrl) {
        log.info("User {} creating share link for file {} to recipient {}", ownerId, request.getFileId(), request.getRecipientUsername());

        long hours = 24;
        String opt = request.getExpiryOption().toLowerCase();
        if (opt.contains("1 hour") || opt.contains("1h")) {
            hours = 1;
        } else if (opt.contains("24") || opt.contains("1 day")) {
            hours = 24;
        } else if (opt.contains("7") || opt.contains("week")) {
            hours = 168;
        } else if (opt.contains("30") || opt.contains("month")) {
            hours = 720;
        }

        Instant expiresAt = Instant.now().plusSeconds(hours * 3600);
        String shareToken = "sh_" + UUID.randomUUID().toString().replace("-", "") + Long.toHexString(System.currentTimeMillis());
        String shareId = "sr_" + UUID.randomUUID().toString().substring(0, 12);

        ShareRecord record = new ShareRecord(
                shareId,
                request.getFileId(),
                ownerId,
                request.getFileName(),
                request.getRecipientUsername(),
                shareToken,
                expiresAt
        );

        ShareRecord saved = shareRepository.save(record);
        auditServiceClient.logEvent(ownerId.toString(), "CREATE_SHARE", saved.getFileName());
        log.info("Share link created successfully with ID: {}", saved.getId());
        return ShareResponse.fromEntity(saved, baseUrl);
    }

    /**
     * Revokes an active share link (owner only).
     */
    @Transactional
    public void revokeShare(UUID callerId, String shareId) {
        log.info("User {} requesting revocation of share {}", callerId, shareId);
        ShareRecord record = shareRepository.findById(shareId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Share not found with ID: " + shareId));

        if (!record.getOwnerId().equals(callerId)) {
            log.warn("Unauthorized attempt by user {} to revoke share {} owned by {}", callerId, shareId, record.getOwnerId());
            throw new ApiException(HttpStatus.FORBIDDEN, "Only the file owner can revoke this share");
        }

        record.setStatus("REVOKED");
        shareRepository.save(record);
        auditServiceClient.logEvent(callerId.toString(), "REVOKE_SHARE", record.getFileName());
        log.info("Share {} successfully revoked", shareId);
    }

    /**
     * Resolves a public share token for recipient access.
     */
    @Transactional(readOnly = true)
    public ShareResponse getShareByToken(String token, String baseUrl) {
        log.debug("Resolving share token: {}", token);
        ShareRecord record = shareRepository.findByShareToken(token)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Share link not found or invalid"));

        if ("REVOKED".equals(record.getStatus())) {
            throw new ApiException(HttpStatus.FORBIDDEN, "This share link has been revoked by the owner");
        }

        if (record.getExpiresAt().isBefore(Instant.now())) {
            record.setStatus("EXPIRED");
            throw new ApiException(HttpStatus.FORBIDDEN, "This share link has expired");
        }

        return ShareResponse.fromEntity(record, baseUrl);
    }
}
