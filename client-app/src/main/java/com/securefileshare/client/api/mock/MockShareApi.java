package com.securefileshare.client.api.mock;

import com.securefileshare.client.api.ShareApi;
import com.securefileshare.client.model.ReceivedPackage;
import com.securefileshare.client.model.ShareLinkResult;
import com.securefileshare.client.model.ShareRecordDto;
import com.securefileshare.client.model.ShareStatus;

import java.io.File;
import java.io.FileWriter;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.UUID;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Mock implementation of ShareApi with sample share grants, link creation, revocation simulation,
 * and test cases for all recipient validation and download scenarios.
 * WHAT: Manages in-memory shares and provides mock ReceivedPackage bundles for all Phase C5 test cases.
 * WHY: Supports end-to-end verification of the 5-check receive pipeline without live network infrastructure.
 */
public class MockShareApi implements ShareApi {

    private final List<ShareRecordDto> shares = new CopyOnWriteArrayList<>();

    public MockShareApi() {
        seedSampleData();
    }

    private void seedSampleData() {
        shares.add(new ShareRecordDto(
                "share-101",
                UUID.randomUUID().toString(),
                "contract-final.pdf",
                "rahul",
                ShareStatus.ACTIVE,
                "Oct 20, 2026",
                "In 2 days (Oct 24)"
        ));
        shares.add(new ShareRecordDto(
                "share-102",
                UUID.randomUUID().toString(),
                "annual-report-draft.pdf",
                "priya",
                ShareStatus.EXPIRED,
                "Sep 15, 2026",
                "Expired Oct 15, 2026"
        ));
        shares.add(new ShareRecordDto(
                "share-103",
                UUID.randomUUID().toString(),
                "architecture-diagram.png",
                "dev-team",
                ShareStatus.REVOKED,
                "Oct 01, 2026",
                "Revoked Oct 10, 2026"
        ));
    }

    @Override
    public List<ShareRecordDto> getSharesCreated() throws Exception {
        Thread.sleep(150);
        return new ArrayList<>(shares);
    }

    @Override
    public void revokeShare(String shareId) throws Exception {
        Thread.sleep(150);
        for (ShareRecordDto share : shares) {
            if (share.getShareId().equals(shareId)) {
                share.setStatus(ShareStatus.REVOKED);
                return;
            }
        }
        throw new NoSuchElementException("Share record not found: " + shareId);
    }

    @Override
    public ShareLinkResult createShareLink(String fileId, String fileName, String recipientUsername, String expiryOption) throws Exception {
        Thread.sleep(200);

        Duration duration = switch (expiryOption != null ? expiryOption : "7 days") {
            case "1 hour" -> Duration.ofHours(1);
            case "24 hours" -> Duration.ofHours(24);
            case "30 days" -> Duration.ofDays(30);
            default -> Duration.ofDays(7);
        };

        Instant now = Instant.now();
        Instant expiresAt = now.plus(duration);

        String shareId = "share-" + UUID.randomUUID().toString().substring(0, 8);
        String token = UUID.randomUUID().toString().replace("-", "");
        String shareUrl = "https://securefileshare.io/share/" + token;

        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("MMM dd, yyyy HH:mm 'UTC'").withZone(ZoneId.of("UTC"));
        String formattedExpiry = "Expires: " + expiryOption + " (" + formatter.format(expiresAt) + ")";

        shares.add(0, new ShareRecordDto(
                shareId,
                fileId != null ? fileId : UUID.randomUUID().toString(),
                fileName,
                recipientUsername,
                ShareStatus.ACTIVE,
                formatter.format(now),
                formattedExpiry
        ));

        return new ShareLinkResult(
                shareId,
                fileId,
                fileName,
                recipientUsername,
                shareUrl,
                formattedExpiry,
                expiresAt
        );
    }

    @Override
    public ReceivedPackage validateAndDownloadPackage(String shareLink, String currentUsername) throws Exception {
        Thread.sleep(250);

        if (shareLink == null || shareLink.trim().isEmpty()) {
            throw new IllegalArgumentException("Share link cannot be empty");
        }

        String link = shareLink.toLowerCase().trim();

        // Failure Scenario 1: Expired link
        boolean isExpired = link.contains("expired");

        // Failure Scenario 2: Revoked share
        boolean isRevoked = link.contains("revoked");

        // Failure Scenario 3: Wrong recipient user
        String targetRecipient = link.contains("wrong-user") ? "other.user" : (currentUsername != null ? currentUsername : "rahul.sharma");

        // Failure Scenario 4: Corrupted signature
        boolean isCorruptedSig = link.contains("bad-sig");

        // Failure Scenario 5: Hash mismatch
        boolean isCorruptedHash = link.contains("bad-hash");

        // Failure Scenario 6: Infected malware
        boolean isInfected = link.contains("infected");

        // Create a mock temporary ciphertext file on disk
        File tempCipherfile = File.createTempFile("sfs_rx_enc_", ".tmp");
        try (FileWriter writer = new FileWriter(tempCipherfile)) {
            writer.write("Zero-Knowledge Encrypted Payload Content [Mock Ciphertext]");
        }

        return new ReceivedPackage(
                "share-rx-" + UUID.randomUUID().toString().substring(0, 6),
                "financial_statement_2026.pdf",
                2_458_112L,
                "priya.sharma",
                targetRecipient,
                tempCipherfile,
                Base64.getEncoder().encodeToString(new byte[12]),
                Base64.getEncoder().encodeToString("mock-wrapped-key-payload".getBytes()),
                Base64.getEncoder().encodeToString("mock-ecdsa-signature-payload".getBytes()),
                "e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855",
                isExpired,
                isRevoked,
                isCorruptedSig,
                isCorruptedHash,
                isInfected
        );
    }
}
