package com.securefileshare.file.storage;

import com.securefileshare.common.exception.ApiException;
import io.minio.*;
import io.minio.errors.MinioException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.io.InputStream;
import java.security.DigestInputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;

/**
 * MinIO object storage streaming service.
 * WHAT: Streams ciphertext payloads into and out of MinIO object storage without in-memory buffering.
 * WHY: Enforces zero-knowledge streaming storage: prevents loading full multi-megabyte ciphertext into RAM,
 *      while recomputing SHA-256 checksums on the fly during upload.
 */
@Service
public class MinioStorageService {

    private static final Logger log = LoggerFactory.getLogger(MinioStorageService.class);

    // Minimum part size for MinIO multipart uploads when size is unknown (5MB)
    private static final long DEFAULT_PART_SIZE = 5 * 1024 * 1024L;

    private final MinioClient minioClient;
    private final String bucketName;

    public MinioStorageService(
            MinioClient minioClient,
            @Value("${minio.bucket-name:secure-files}") String bucketName) {
        this.minioClient = minioClient;
        this.bucketName = bucketName;
    }

    /**
     * Streams ciphertext directly into MinIO while computing SHA-256 on the fly.
     * WHAT: Pipes the InputStream directly into S3 object storage without buffering the full file into memory.
     * WHY: Prevents memory exhaustion (OOM) on large file uploads and generates an authenticated SHA-256 digest.
     *
     * @param objectKey   Unique MinIO object key (path)
     * @param inputStream Inbound stream of encrypted ciphertext bytes
     * @param sizeBytes   Total stream length in bytes (-1 if unknown)
     * @return 64-character lowercase hexadecimal SHA-256 digest of the streamed ciphertext
     */
    public String streamUpload(String objectKey, InputStream inputStream, long sizeBytes) {
        ensureBucket();

        try {
            // Place-level comment: Initialize SHA-256 message digest to calculate hash during streaming
            // Why: Avoids a second pass over the file, verifying integrity during the single storage upload stream
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            DigestInputStream digestInputStream = new DigestInputStream(inputStream, digest);

            // Configure stream size and part size for MinIO SDK
            // If size is known, partSize is -1 (MinIO auto-calculates). If unknown, partSize is 5MB minimum.
            long objectSize = sizeBytes > 0 ? sizeBytes : -1;
            long partSize = sizeBytes > 0 ? -1 : DEFAULT_PART_SIZE;

            log.debug("Streaming ciphertext to MinIO object '{}' (size: {} bytes)...", objectKey, sizeBytes);
            minioClient.putObject(
                    PutObjectArgs.builder()
                            .bucket(bucketName)
                            .object(objectKey)
                            .stream(digestInputStream, objectSize, partSize)
                            .contentType("application/octet-stream")
                            .build()
            );

            // Extract the computed 32-byte SHA-256 digest and format as 64-character lowercase hex string
            byte[] hashBytes = digestInputStream.getMessageDigest().digest();
            String computedSha256 = HexFormat.of().formatHex(hashBytes);
            log.debug("Streamed upload complete for object '{}'. Computed SHA-256: {}", objectKey, computedSha256);

            return computedSha256;
        } catch (NoSuchAlgorithmException ex) {
            throw new IllegalStateException("Standard SHA-256 algorithm missing from Java runtime", ex);
        } catch (MinioException ex) {
            log.error("MinIO storage error during upload of '{}': {}", objectKey, ex.getMessage(), ex);
            throw new ApiException(HttpStatus.INTERNAL_SERVER_ERROR, "Storage service error: " + ex.getMessage());
        } catch (Exception ex) {
            log.error("Failed to stream upload object '{}': {}", objectKey, ex.getMessage(), ex);
            throw new ApiException(HttpStatus.INTERNAL_SERVER_ERROR, "Failed to upload encrypted file to storage: " + ex.getMessage());
        }
    }

    /**
     * Retrieves an InputStream pointing directly to the ciphertext object in MinIO.
     * WHAT: Opens a streaming read connection from MinIO.
     * WHY: Powers GET /files/{id}/package streaming without loading the object into service memory.
     *
     * @param objectKey Unique MinIO object key
     * @return InputStream to stream ciphertext bytes to the client
     */
    public InputStream streamDownload(String objectKey) {
        ensureBucket();

        try {
            log.debug("Opening download stream for MinIO object '{}'...", objectKey);
            return minioClient.getObject(
                    GetObjectArgs.builder()
                            .bucket(bucketName)
                            .object(objectKey)
                            .build()
            );
        } catch (MinioException ex) {
            log.error("MinIO storage error during download of '{}': {}", objectKey, ex.getMessage(), ex);
            throw new ApiException(HttpStatus.INTERNAL_SERVER_ERROR, "Storage download error: " + ex.getMessage());
        } catch (Exception ex) {
            log.error("Failed to stream download object '{}': {}", objectKey, ex.getMessage(), ex);
            throw new ApiException(HttpStatus.INTERNAL_SERVER_ERROR, "Failed to retrieve encrypted file from storage: " + ex.getMessage());
        }
    }

    /**
     * Purges an object from MinIO.
     * WHAT: Deletes the specified object key from the storage bucket.
     * WHY: Used to immediately discard ciphertext if the recomputed SHA-256 does not match the client's hash,
     *      preventing fraudulent or corrupted ciphertext from remaining in storage.
     *
     * @param objectKey Unique MinIO object key to remove
     */
    public void deleteObject(String objectKey) {
        try {
            log.info("Deleting MinIO object '{}' from bucket '{}'...", objectKey, bucketName);
            minioClient.removeObject(
                    RemoveObjectArgs.builder()
                            .bucket(bucketName)
                            .object(objectKey)
                            .build()
            );
        } catch (Exception ex) {
            log.warn("Failed to delete MinIO object '{}': {}", objectKey, ex.getMessage());
        }
    }

    /**
     * Ensures the configured bucket exists before executing MinIO operations.
     */
    private void ensureBucket() {
        try {
            boolean exists = minioClient.bucketExists(BucketExistsArgs.builder().bucket(bucketName).build());
            if (!exists) {
                minioClient.makeBucket(MakeBucketArgs.builder().bucket(bucketName).build());
            }
        } catch (Exception ex) {
            log.warn("Could not ensure bucket '{}' exists: {}", bucketName, ex.getMessage());
        }
    }
}
