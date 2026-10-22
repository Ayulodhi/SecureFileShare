package com.securefileshare.file.dto;

import com.securefileshare.file.entity.FileRecord;
import java.io.InputStream;

/**
 * Data container representing a complete encrypted file package for retrieval.
 * WHAT: Bundles file metadata, the caller's wrapped AES key, and the open MinIO ciphertext InputStream.
 * WHY: Powers GET /files/{id}/package streaming without loading ciphertext bytes into memory.
 */
public class FilePackage {

    private final FileRecord fileRecord;
    private final String callerWrappedKey;
    private final InputStream ciphertextStream;

    public FilePackage(FileRecord fileRecord, String callerWrappedKey, InputStream ciphertextStream) {
        this.fileRecord = fileRecord;
        this.callerWrappedKey = callerWrappedKey;
        this.ciphertextStream = ciphertextStream;
    }

    public FileRecord getFileRecord() {
        return fileRecord;
    }

    public String getCallerWrappedKey() {
        return callerWrappedKey;
    }

    public InputStream getCiphertextStream() {
        return ciphertextStream;
    }
}
