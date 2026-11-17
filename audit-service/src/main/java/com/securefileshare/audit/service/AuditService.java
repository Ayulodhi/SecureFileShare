package com.securefileshare.audit.service;

import com.securefileshare.audit.dto.AuditEntryDto;
import com.securefileshare.audit.dto.AuditVerifyResultDto;
import com.securefileshare.audit.dto.LogEventRequest;
import com.securefileshare.audit.entity.AuditRecord;
import com.securefileshare.audit.repository.AuditRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.HexFormat;
import java.util.List;
import java.util.Optional;

/**
 * Service managing append-only hash-chained audit logs and verification.
 * WHAT: Computes SHA-256 hash chains across audit events and validates log integrity.
 * WHY: Implements tamper-evident compliance tracking required by Phase C6.
 */
@Service
public class AuditService {

    private static final Logger log = LoggerFactory.getLogger(AuditService.class);
    private static final String GENESIS_PREV_HASH = "0000000000000000000000000000000000000000000000000000000000000000";

    private final AuditRepository auditRepository;

    public AuditService(AuditRepository auditRepository) {
        this.auditRepository = auditRepository;
    }

    /**
     * Retrieves paginated audit entries with optional actor and action filters.
     */
    @Transactional(readOnly = true)
    public List<AuditEntryDto> getAuditEntries(int page, int size, String actor, String action) {
        Pageable pageable = PageRequest.of(page, size);
        Page<AuditRecord> recordPage;

        boolean hasActor = actor != null && !actor.isBlank();
        boolean hasAction = action != null && !action.isBlank();

        if (hasActor && hasAction) {
            recordPage = auditRepository.findByActorContainingIgnoreCaseOrActionContainingIgnoreCaseOrderByIdDesc(actor, action, pageable);
        } else if (hasActor) {
            recordPage = auditRepository.findByActorContainingIgnoreCaseOrderByIdDesc(actor, pageable);
        } else if (hasAction) {
            recordPage = auditRepository.findByActionContainingIgnoreCaseOrderByIdDesc(action, pageable);
        } else {
            recordPage = auditRepository.findAllByOrderByIdDesc(pageable);
        }

        return recordPage.getContent().stream()
                .map(AuditEntryDto::fromEntity)
                .toList();
    }

    /**
     * Verifies the integrity of the entire audit hash chain.
     */
    @Transactional(readOnly = true)
    public AuditVerifyResultDto verifyChain() {
        List<AuditRecord> records = auditRepository.findAllByOrderByIdAsc();
        if (records.isEmpty()) {
            return new AuditVerifyResultDto(true, 0, null, "Audit log is empty. Chain valid.");
        }

        String expectedPrevHash = GENESIS_PREV_HASH;
        int checkedCount = 0;

        for (AuditRecord record : records) {
            if (!record.getPreviousHash().equals(expectedPrevHash)) {
                log.warn("Audit chain broken at entry #{}: previousHash mismatch", record.getId());
                return new AuditVerifyResultDto(
                        false,
                        checkedCount,
                        record.getId(),
                        "Chain broken at entry #" + record.getId() + ": previousHash does not match preceding entry hash"
                );
            }

            Instant normalizedTimestamp = record.getTimestamp().truncatedTo(ChronoUnit.MILLIS);
            int version = record.getHashVersion();
            String computedHash = computeEntryHash(
                    record.getPreviousHash(),
                    normalizedTimestamp,
                    record.getActor(),
                    record.getAction(),
                    record.getFileName(),
                    version
            );

            if (!computedHash.equals(record.getHash())) {
                log.warn("Audit chain broken at entry #{}: hash tampered", record.getId());
                return new AuditVerifyResultDto(
                        false,
                        checkedCount,
                        record.getId(),
                        "Chain broken at entry #" + record.getId() + ": entry hash has been modified"
                );
            }

            expectedPrevHash = record.getHash();
            checkedCount++;
        }

        return new AuditVerifyResultDto(
                true,
                checkedCount,
                null,
                "Chain valid, " + checkedCount + " entries checked"
        );
    }

    /**
     * Appends a new audit event to the hash chain.
     */
    @Transactional
    public synchronized AuditEntryDto logEvent(LogEventRequest request) {
        Optional<AuditRecord> lastRecord = auditRepository.findTopByOrderByIdDesc();
        String previousHash = lastRecord.map(AuditRecord::getHash).orElse(GENESIS_PREV_HASH);

        Instant timestamp = Instant.now().truncatedTo(ChronoUnit.MILLIS);
        String actor = request.getActor();
        String action = request.getAction();
        String fileName = request.getFileName();

        int version = 2;
        String hash = computeEntryHash(previousHash, timestamp, actor, action, fileName, version);

        AuditRecord record = new AuditRecord(timestamp, actor, action, fileName, hash, previousHash, version);
        AuditRecord saved = auditRepository.save(record);

        log.info("Logged audit event #{}: {} by {} on {}", saved.getId(), action, actor, fileName);
        return AuditEntryDto.fromEntity(saved);
    }

    private String computeEntryHash(String previousHash, Instant timestamp, String actor, String action, String fileName, int version) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            String payload;
            if (version == 1) {
                payload = previousHash + timestamp.toString() + actor + action + fileName;
            } else {
                payload = previousHash + "|" + timestamp.toString() + "|" + actor + "|" + action + "|" + fileName + "|v2";
            }
            byte[] hashBytes = digest.digest(payload.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hashBytes);
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("SHA-256 algorithm not available", e);
        }
    }
}
