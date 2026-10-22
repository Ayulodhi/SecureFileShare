package com.securefileshare.file.service;

import com.securefileshare.common.exception.ApiException;
import com.securefileshare.file.client.AuthServiceClient;
import com.securefileshare.file.crypto.SignatureVerificationService;
import com.securefileshare.file.dto.*;
import com.securefileshare.file.entity.FileKey;
import com.securefileshare.file.entity.FileRecord;
import com.securefileshare.file.entity.FileStatus;
import com.securefileshare.file.repository.FileKeyRepository;
import com.securefileshare.file.repository.FileRepository;
import com.securefileshare.file.storage.MinioStorageService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Core business service managing encrypted file operations.
 * WHAT: Coordinates zero-knowledge file upload streaming, integrity verification, signature validation,
 *      metadata persistence, package retrieval, and recipient key management.
 * WHY:
 * ====================================================================================================
 * ZERO-KNOWLEDGE ARCHITECTURAL GUARANTEE:
 * The server NEVER sees plaintext files and NEVER holds symmetric AES keys or user private keys.
 * 1. The client generates a random, single-use AES-256-GCM symmetric key on their device.
 * 2. The client encrypts the file locally with AES-256-GCM to produce ciphertext and IV.
 * 3. The client wraps the AES key with the recipient's RSA-4096 public key using OAEP padding.
 * 4. The client computes the SHA-256 hash of the ciphertext and signs it with their NIST P-256 EC private key.
 * 5. The server acts exclusively as an untrusted ciphertext repository: it recomputes the SHA-256 hash on
 *    the streaming ciphertext to verify data transit integrity, verifies the ECDSA signature against the owner's
 *    EC public key, and stores only opaque ciphertext, wrapped keys, and metadata.
 * 6. The server CANNOT decrypt anything because it does not possess any user's private key or unwrapped AES key.
 * ====================================================================================================
 */
@Service
public class FileService {

    private static final Logger log = LoggerFactory.getLogger(FileService.class);

    private final FileRepository fileRepository;
    private final FileKeyRepository fileKeyRepository;
    private final MinioStorageService minioStorageService;
    private final AuthServiceClient authServiceClient;
    private final SignatureVerificationService signatureVerificationService;
    private final long maxSizeBytes;

    public FileService(
            FileRepository fileRepository,
            FileKeyRepository fileKeyRepository,
            MinioStorageService minioStorageService,
            AuthServiceClient authServiceClient,
            SignatureVerificationService signatureVerificationService,
            @Value("${file.max-size-bytes:104857600}") long maxSizeBytes) {
        this.fileRepository = fileRepository;
        this.fileKeyRepository = fileKeyRepository;
        this.minioStorageService = minioStorageService;
        this.authServiceClient = authServiceClient;
        this.signatureVerificationService = signatureVerificationService;
        this.maxSizeBytes = maxSizeBytes;
    }

    /**
     * Uploads an encrypted file package from the owner.
     * WHAT: Validates signature, streams ciphertext directly into MinIO, verifies SHA-256 digest,
     *      stores file metadata, and saves the owner's wrapped AES key.
     * WHY: Guarantees zero-knowledge storage without loading large ciphertext into memory and sets
     *      initial file status to PENDING_SCAN for future malware scanning.
     *
     * @param ownerId         Authenticated owner UUID extracted from JWT
     * @param file            Multipart file containing encrypted ciphertext stream
     * @param originalName    Original filename (e.g. document.pdf)
     * @param iv              Initialization Vector used for AES-GCM (Base64)
     * @param ecdsaSignature  ECDSA digital signature over ciphertext SHA-256 hash (Base64)
     * @param ciphertextHash  Client-declared SHA-256 hash (64 hex characters)
     * @param ownerWrappedKey Owner's wrapped AES key (RSA-4096-OAEP)
     * @return FileMetadataResponse containing persisted file metadata
     */
    @Transactional
    public FileMetadataResponse uploadEncryptedFile(
            UUID ownerId,
            MultipartFile file,
            String originalName,
            String iv,
            String ecdsaSignature,
            String ciphertextHash,
            String ownerWrappedKey) {

        log.info("Processing encrypted file upload for owner {} (originalName: {})...", ownerId, originalName);

        // Security check: Validate file payload presence and enforce strict maximum file size boundary
        // Why: Prevents disk and memory denial-of-service (DoS) from oversized ciphertext uploads
        if (file == null || file.isEmpty()) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Ciphertext file payload is required");
        }
        if (file.getSize() > maxSizeBytes) {
            throw new ApiException(HttpStatus.BAD_REQUEST,
                    "Ciphertext size (" + file.getSize() + " bytes) exceeds maximum limit of " + maxSizeBytes + " bytes");
        }

        // Validate cryptographic parameters
        if (originalName == null || originalName.isBlank()) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Original file name cannot be blank");
        }
        if (iv == null || iv.isBlank()) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "AES-GCM initialization vector (IV) is required");
        }
        if (ecdsaSignature == null || ecdsaSignature.isBlank()) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "ECDSA digital signature is required");
        }
        if (ciphertextHash == null || ciphertextHash.isBlank() || ciphertextHash.length() != 64) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Ciphertext SHA-256 hash must be 64 hexadecimal characters");
        }
        if (ownerWrappedKey == null || ownerWrappedKey.isBlank()) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Owner wrapped AES key is required");
        }

        // Step 1: Fetch owner's registered EC public key from auth-service
        // Why: Authenticates the owner's cryptographic identity before consuming storage resources
        KeyRegistryResponseDto ownerKeys = authServiceClient.getActiveKeys(ownerId);
        String ecPublicKey = ownerKeys.getEcPublicKey();
        if (ecPublicKey == null || ecPublicKey.isBlank()) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Owner does not have a registered EC public key");
        }

        // Step 2: Verify the ECDSA signature against the declared ciphertext hash
        // Why: Proves that the ciphertext hash was authorized by the owner's private key before streaming
        boolean signatureValid = signatureVerificationService.verifySignature(ecPublicKey, ciphertextHash, ecdsaSignature);
        if (!signatureValid) {
            log.warn("ECDSA signature validation failed for owner {}", ownerId);
            throw new ApiException(HttpStatus.BAD_REQUEST,
                    "ECDSA signature verification failed: signature does not match owner's EC public key and ciphertext hash");
        }

        // Step 3: Generate unique object storage key
        String objectKey = ownerId + "/" + UUID.randomUUID();

        // Step 4: Stream ciphertext into MinIO while computing SHA-256 on the fly
        // ZERO-KNOWLEDGE: The server streams raw ciphertext without buffering or decrypting
        String computedSha256;
        try (InputStream inputStream = file.getInputStream()) {
            computedSha256 = minioStorageService.streamUpload(objectKey, inputStream, file.getSize());
        } catch (ApiException ex) {
            throw ex;
        } catch (Exception ex) {
            log.error("Failed to stream upload ciphertext to MinIO: {}", ex.getMessage(), ex);
            throw new ApiException(HttpStatus.INTERNAL_SERVER_ERROR, "Failed to stream ciphertext to storage: " + ex.getMessage());
        }

        // Step 5: Integrity check: compare recomputed SHA-256 with client's declared hash
        // Why: Prevents tampering during transit. If hash does not match, immediately purge from storage and reject.
        if (!computedSha256.equalsIgnoreCase(ciphertextHash)) {
            log.error("Ciphertext SHA-256 integrity mismatch for owner {}. Declared: {}, Computed: {}",
                    ownerId, ciphertextHash, computedSha256);
            minioStorageService.deleteObject(objectKey);
            throw new ApiException(HttpStatus.BAD_REQUEST,
                    "Ciphertext SHA-256 integrity mismatch: received data does not match declared hash");
        }

        // Step 6: Persist file metadata with status PENDING_SCAN
        // Why: Status starts as PENDING_SCAN; future Phase 5 scan worker will evaluate and update it
        FileRecord fileRecord = new FileRecord(
                ownerId,
                originalName,
                file.getSize(),
                computedSha256.toLowerCase(),
                iv,
                ecdsaSignature,
                objectKey
        );
        fileRecord.setStatus(FileStatus.PENDING_SCAN);
        FileRecord savedRecord = fileRepository.save(fileRecord);

        // Step 7: Store the owner's wrapped AES key in FILE_KEYS
        // Why: Stores one wrapped key per user who may decrypt the file, including owner (for re-sharing)
        FileKey ownerKey = new FileKey(savedRecord, ownerId, ownerWrappedKey);
        fileKeyRepository.save(ownerKey);

        log.info("Encrypted file {} saved successfully with status PENDING_SCAN (id: {})", originalName, savedRecord.getId());
        return FileMetadataResponse.fromEntity(savedRecord);
    }

    /**
     * Lists all file metadata records owned by the caller.
     * WHAT: Retrieves metadata for all files where owner_id matches caller UUID.
     * WHY: Powers the GET /files endpoint. Returns metadata only, never ciphertext.
     *
     * @param callerId Authenticated caller UUID
     * @return List of FileMetadataResponse DTOs
     */
    @Transactional(readOnly = true)
    public List<FileMetadataResponse> listFiles(UUID callerId) {
        log.debug("Listing files owned by user {}", callerId);
        List<FileRecord> records = fileRepository.findByOwnerIdOrderByCreatedAtDesc(callerId);
        return records.stream()
                .map(FileMetadataResponse::fromEntity)
                .toList();
    }

    /**
     * Retrieves the complete encrypted file package for decryption on the caller's device.
     * WHAT: Validates caller authorization (owner or valid recipient share), retrieves caller's wrapped AES key,
     *      and returns a streaming connection to the MinIO ciphertext.
     * WHY: Powers GET /files/{id}/package. Zero-knowledge: the caller uses their own RSA private key
     *      to unwrap the AES key, then decrypts the ciphertext locally.
     *
     * @param fileId   UUID of the target file
     * @param callerId Authenticated caller UUID
     * @return FilePackage containing file metadata, caller's wrapped key, and ciphertext InputStream
     */
    public FilePackage getFilePackage(UUID fileId, UUID callerId) {
        log.info("Retrieving encrypted package for file {} requested by user {}", fileId, callerId);

        // Fetch file record
        FileRecord record = fileRepository.findById(fileId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "File not found with ID: " + fileId));

        // Place-level comment: Access control check
        // Why: Enforces authorization boundary: caller must be the owner OR hold a valid share
        boolean isOwner = record.getOwnerId().equals(callerId);
        boolean hasValidShare = checkShareAccessPlaceholder(fileId, callerId);

        if (!isOwner && !hasValidShare) {
            log.warn("Access denied: user {} attempted to retrieve file {} owned by {}", callerId, fileId, record.getOwnerId());
            throw new ApiException(HttpStatus.FORBIDDEN,
                    "Access denied: caller is neither file owner nor authorized recipient");
        }

        // Fetch the caller's specific wrapped AES key
        // Why: Each authorized recipient receives the AES key wrapped specifically with their RSA public key
        FileKey callerKey = fileKeyRepository.findByFileRecord_IdAndRecipientId(fileId, callerId)
                .orElseThrow(() -> new ApiException(HttpStatus.FORBIDDEN,
                        "No wrapped key found for the requesting user on file " + fileId));

        // Open streaming download connection from MinIO
        InputStream ciphertextStream = minioStorageService.streamDownload(record.getObjectKey());

        return new FilePackage(record, callerKey.getWrappedKey(), ciphertextStream);
    }

    /**
     * Adds a wrapped AES key for a new recipient.
     * WHAT: Saves an additional FileKey record containing the AES key wrapped with the recipient's RSA-4096 key.
     * WHY: Powers POST /files/{id}/keys. Used when the file owner shares access with a new recipient.
     *
     * @param fileId   UUID of the target file
     * @param callerId Authenticated caller UUID (must be file owner)
     * @param request  AddFileKeyRequest containing recipient UUID and wrapped AES key
     * @return FileKeyResponse with registered key details
     */
    @Transactional
    public FileKeyResponse addRecipientKey(UUID fileId, UUID callerId, AddFileKeyRequest request) {
        log.info("Owner {} adding wrapped key for recipient {} on file {}", callerId, request.getRecipientId(), fileId);

        // Fetch target file
        FileRecord record = fileRepository.findById(fileId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "File not found with ID: " + fileId));

        // Security check: Only the file owner may add recipient keys
        // Why: Prevents unauthorized users from modifying key distribution on another user's file
        if (!record.getOwnerId().equals(callerId)) {
            log.warn("Unauthorized attempt by user {} to add recipient keys to file {} owned by {}",
                    callerId, fileId, record.getOwnerId());
            throw new ApiException(HttpStatus.FORBIDDEN, "Only the file owner can add wrapped keys for recipients");
        }

        // Check if a key already exists for this recipient
        Optional<FileKey> existingKey = fileKeyRepository.findByFileRecord_IdAndRecipientId(fileId, request.getRecipientId());
        FileKey savedKey;
        if (existingKey.isPresent()) {
            // Update existing wrapped key
            FileKey key = existingKey.get();
            key.setWrappedKey(request.getWrappedKey());
            savedKey = fileKeyRepository.save(key);
            log.info("Updated existing wrapped key for recipient {} on file {}", request.getRecipientId(), fileId);
        } else {
            // Create new wrapped key entry
            FileKey newKey = new FileKey(record, request.getRecipientId(), request.getWrappedKey());
            savedKey = fileKeyRepository.save(newKey);
            log.info("Saved new wrapped key for recipient {} on file {}", request.getRecipientId(), fileId);
        }

        return FileKeyResponse.fromEntity(savedKey);
    }

    /**
     * Placeholder access check for shared recipients.
     * WHAT: In Phase 2, verifies if a wrapped key exists for this recipient in the database.
     * WHY: Placeholder check that Phase 3 will replace by making an inter-service call to share-service.
     *
     * @param fileId   UUID of the file
     * @param callerId UUID of the user requesting access
     * @return true if caller holds a registered wrapped key
     */
    private boolean checkShareAccessPlaceholder(UUID fileId, UUID callerId) {
        // Place-level comment: Phase 2 placeholder check
        // Why: Verifies whether the recipient has already been granted access via a registered FileKey
        // Phase 3 will replace this by querying share-service for active, non-expired shares and permissions
        return fileKeyRepository.existsByFileRecord_IdAndRecipientId(fileId, callerId);
    }
}
