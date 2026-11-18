package com.securefileshare.file.service;

import com.securefileshare.common.exception.ApiException;
import com.securefileshare.file.repository.FileKeyRepository;
import com.securefileshare.file.repository.FileRepository;
import com.securefileshare.file.entity.FileRecord;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

import java.util.UUID;

/**
 * Centralized authorization component for file access control (Task 1).
 * WHAT: Enforces strict authorization rules for accessing file metadata, packages, and keys.
 * WHY: Replaces scattered checks and ensures unauthorized access attempts consistently return 404 Not Found.
 */
@Component
public class FileAccessService {

    private final FileRepository fileRepository;
    private final FileKeyRepository fileKeyRepository;

    public FileAccessService(FileRepository fileRepository, FileKeyRepository fileKeyRepository) {
        this.fileRepository = fileRepository;
        this.fileKeyRepository = fileKeyRepository;
    }

    /**
     * Verifies whether the caller is authorized to access the specified file.
     * Rules: Caller must be the owner OR hold a valid direct grant (file_key).
     * If unauthorized or file does not exist, throws ApiException(404 Not Found).
     */
    public FileRecord verifyAccessOrThrow(UUID fileId, UUID callerId) {
        FileRecord record = fileRepository.findById(fileId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "File not found"));

        boolean isOwner = record.getOwnerId().equals(callerId);
        boolean hasDirectGrant = fileKeyRepository.existsByFileRecord_IdAndRecipientId(fileId, callerId);

        if (!isOwner && !hasDirectGrant) {
            // Return 404 instead of 403 to prevent file ID enumeration / information disclosure
            throw new ApiException(HttpStatus.NOT_FOUND, "File not found");
        }

        return record;
    }

    /**
     * Verifies whether the caller is the exact owner of the specified file.
     */
    public FileRecord verifyOwnerOrThrow(UUID fileId, UUID callerId) {
        FileRecord record = fileRepository.findById(fileId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "File not found"));

        if (!record.getOwnerId().equals(callerId)) {
            throw new ApiException(HttpStatus.NOT_FOUND, "File not found");
        }

        return record;
    }
}
