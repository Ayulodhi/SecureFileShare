package com.securefileshare.client.api.mock;

import com.securefileshare.client.api.AuditApi;
import com.securefileshare.client.model.AuditAction;
import com.securefileshare.client.model.AuditEntryDto;
import com.securefileshare.client.model.AuditVerifyResult;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HexFormat;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Mock implementation of AuditApi for Phase C6.
 * WHAT: Generates a cryptographically linked SHA-256 hash chain of 65 audit entries with
 *       pagination (50 rows/page), multi-field filtering, and a toggleable tamper mode.
 * WHY: Strict compliance with Phase C6 Requirement 1: Provides both green valid chain states
 *      and red tampered chain detection without live audit-service containers.
 */
public class MockAuditApi implements AuditApi {

    private final List<AuditEntryDto> masterChain = new ArrayList<>();
    private boolean tamperMode = false;
    private final long TAMPERED_ENTRY_ID = 42L;

    public MockAuditApi() {
        seedHashChain();
    }

    /**
     * Seeds 65 audit entries linked cryptographically in a SHA-256 hash chain.
     */
    private void seedHashChain() {
        masterChain.clear();
        String currentPrevHash = "0000000000000000000000000000000000000000000000000000000000000000";

        AuditAction[] actions = {AuditAction.UPLOAD, AuditAction.SHARE, AuditAction.DOWNLOAD, AuditAction.DENIED, AuditAction.REVOKE};
        String[] actors = {"rahul", "priya", "aman", "neha", "admin"};
        String[] files = {"contract-final.pdf", "financial_audit.docx", "patient_record.enc", "backup-q3.zip", "key-registry.pem"};
        String[] results = {"SUCCESS", "SUCCESS", "SUCCESS", "ACCESS_DENIED", "REVOKED_BY_OWNER"};

        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss").withZone(ZoneId.of("UTC"));
        Instant baseTime = Instant.now().minusSeconds(86400L * 15); // Spread over 15 days

        for (int i = 1; i <= 65; i++) {
            long id = i;
            Instant eventTime = baseTime.plusSeconds(i * 18000L);
            String timeStr = formatter.format(eventTime);
            String actor = actors[(i - 1) % actors.length];
            AuditAction action = actions[(i - 1) % actions.length];
            String file = files[(i - 1) % files.length];
            String result = results[(i - 1) % results.length];

            // Compute deterministic SHA-256 hash chaining to previous block
            String rawBlock = currentPrevHash + ":" + id + ":" + timeStr + ":" + actor + ":" + action.name() + ":" + file + ":" + result;
            String blockHash = computeSha256Hex(rawBlock);

            masterChain.add(new AuditEntryDto(id, timeStr, actor, action, file, result, blockHash, currentPrevHash));
            currentPrevHash = blockHash;
        }
    }

    @Override
    public List<AuditEntryDto> getAuditEntries(String actor, AuditAction action, String dateRange, int page, int pageSize) throws Exception {
        Thread.sleep(180); // Small artificial latency for loading state verification

        List<AuditEntryDto> filtered = filterEntries(actor, action, dateRange);

        int fromIndex = Math.max(0, (page - 1) * pageSize);
        if (fromIndex >= filtered.size()) {
            return List.of();
        }
        int toIndex = Math.min(fromIndex + pageSize, filtered.size());

        List<AuditEntryDto> paged = new ArrayList<>(filtered.subList(fromIndex, toIndex));

        // If tamper mode is active and the tampered entry falls within this page, substitute tampered version
        if (tamperMode) {
            for (int i = 0; i < paged.size(); i++) {
                AuditEntryDto entry = paged.get(i);
                if (entry.getId() == TAMPERED_ENTRY_ID) {
                    // Inject altered hash breaking the cryptographic chain
                    paged.set(i, new AuditEntryDto(
                            entry.getId(),
                            entry.getTimestamp(),
                            entry.getActor(),
                            entry.getAction(),
                            "TAMPERED_" + entry.getFile(),
                            entry.getResult(),
                            "bad000000000000000000000000000000000000000000000000000000000dead",
                            entry.getPreviousHash()
                    ));
                }
            }
        }

        return paged;
    }

    @Override
    public int getTotalCount(String actor, AuditAction action, String dateRange) throws Exception {
        return filterEntries(actor, action, dateRange).size();
    }

    private List<AuditEntryDto> filterEntries(String actor, AuditAction action, String dateRange) {
        return masterChain.stream()
                .filter(e -> {
                    if (actor != null && !actor.trim().isEmpty() && !"All actors".equalsIgnoreCase(actor.trim())) {
                        if (!e.getActor().toLowerCase().contains(actor.trim().toLowerCase())) {
                            return false;
                        }
                    }
                    if (action != null) {
                        if (e.getAction() != action) {
                            return false;
                        }
                    }
                    return true;
                })
                .collect(Collectors.toList());
    }

    @Override
    public AuditVerifyResult verifyChain() throws Exception {
        Thread.sleep(300); // Artificial delay to verify loading spinner

        if (tamperMode) {
            // Tampered mode: Identifies the exact broken block
            return AuditVerifyResult.broken(TAMPERED_ENTRY_ID, masterChain.size());
        }

        // Verify SHA-256 linkage across all entries
        String expectedPrev = "0000000000000000000000000000000000000000000000000000000000000000";
        for (AuditEntryDto entry : masterChain) {
            if (!entry.getPreviousHash().equals(expectedPrev)) {
                return AuditVerifyResult.broken(entry.getId(), masterChain.size());
            }
            String rawBlock = entry.getPreviousHash() + ":" + entry.getId() + ":" + entry.getTimestamp() + ":"
                    + entry.getActor() + ":" + entry.getAction().name() + ":" + entry.getFile() + ":" + entry.getResult();
            String computed = computeSha256Hex(rawBlock);
            if (!computed.equalsIgnoreCase(entry.getHash())) {
                return AuditVerifyResult.broken(entry.getId(), masterChain.size());
            }
            expectedPrev = entry.getHash();
        }

        return AuditVerifyResult.valid(masterChain.size());
    }

    @Override
    public void setTamperMode(boolean tampered) {
        this.tamperMode = tampered;
    }

    @Override
    public boolean isTamperMode() {
        return tamperMode;
    }

    private String computeSha256Hex(String data) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] digest = md.digest(data.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest);
        } catch (Exception ex) {
            throw new RuntimeException("SHA-256 unavailable", ex);
        }
    }
}
