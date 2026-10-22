package com.securefileshare.file.controller;

import com.securefileshare.common.dto.ApiResponse;
import com.securefileshare.file.dto.AddFileKeyRequest;
import com.securefileshare.file.dto.FileKeyResponse;
import com.securefileshare.file.dto.FileMetadataResponse;
import com.securefileshare.file.dto.FilePackage;
import com.securefileshare.file.service.FileService;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;

import java.io.InputStream;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/files")
public class FileController {

    private static final Logger log = LoggerFactory.getLogger(FileController.class);

    private final FileService fileService;

    public FileController(FileService fileService) {
        this.fileService = fileService;
    }

    /**
     * Uploads an encrypted file package.
     * WHAT: Accepts multipart form data with ciphertext stream, IV, signature, hash, and owner's wrapped key.
     * WHY: Powers POST /files. Performs zero-knowledge upload with on-the-fly integrity check and sets status PENDING_SCAN.
     *
     * @param file            Encrypted ciphertext multipart stream
     * @param originalName    Plaintext filename (for client reference)
     * @param iv              Base64 AES-GCM Initialization Vector
     * @param ecdsaSignature  Base64 ECDSA signature over ciphertext SHA-256 hash
     * @param ciphertextHash  64-hex SHA-256 hash of the ciphertext
     * @param ownerWrappedKey Base64 owner's wrapped AES key
     * @param authentication  Spring Security authentication holding caller UUID
     * @return ApiResponse containing FileMetadataResponse with status 201 Created
     */
    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ApiResponse<FileMetadataResponse>> uploadFile(
            @RequestParam("ciphertext") MultipartFile file,
            @RequestParam("originalName") String originalName,
            @RequestParam("iv") String iv,
            @RequestParam("ecdsaSignature") String ecdsaSignature,
            @RequestParam("ciphertextHash") String ciphertextHash,
            @RequestParam("ownerWrappedKey") String ownerWrappedKey,
            Authentication authentication) {

        UUID ownerId = (UUID) authentication.getPrincipal();
        log.info("Received encrypted file upload request from user {} for file {}", ownerId, originalName);

        FileMetadataResponse response = fileService.uploadEncryptedFile(
                ownerId,
                file,
                originalName,
                iv,
                ecdsaSignature,
                ciphertextHash,
                ownerWrappedKey
        );

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(ApiResponse.success("Encrypted file uploaded successfully", response));
    }

    /**
     * Lists all encrypted files uploaded by the authenticated caller.
     * WHAT: Returns metadata for files owned by the caller.
     * WHY: Powers GET /files. Returns metadata only; no ciphertext is transferred.
     *
     * @param authentication Spring Security authentication holding caller UUID
     * @return ApiResponse containing list of FileMetadataResponse
     */
    @GetMapping
    public ResponseEntity<ApiResponse<List<FileMetadataResponse>>> listFiles(Authentication authentication) {
        UUID callerId = (UUID) authentication.getPrincipal();
        log.debug("Listing files for caller {}", callerId);

        List<FileMetadataResponse> files = fileService.listFiles(callerId);
        return ResponseEntity.ok(ApiResponse.success("Files retrieved successfully", files));
    }

    /**
     * Streams the complete encrypted package (ciphertext and metadata) for client decryption.
     * WHAT: Returns ciphertext stream in response body and cryptographic parameters in custom HTTP response headers.
     * WHY: Powers GET /files/{id}/package. Allowed only for file owner or authorized recipients.
     *
     * @param id             Target file UUID
     * @param authentication Spring Security authentication holding caller UUID
     * @return ResponseEntity with StreamingResponseBody and cryptographic headers
     */
    @GetMapping("/{id}/package")
    public ResponseEntity<StreamingResponseBody> getFilePackage(
            @PathVariable UUID id,
            Authentication authentication) {

        UUID callerId = (UUID) authentication.getPrincipal();
        log.info("Streaming encrypted package for file {} to caller {}", id, callerId);

        FilePackage filePackage = fileService.getFilePackage(id, callerId);

        // Place-level comment: Return cryptographic parameters and caller's wrapped key in HTTP response headers
        // Why: Enables the client to receive metadata alongside the raw streaming ciphertext body
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_OCTET_STREAM);
        headers.setContentDispositionFormData("attachment", filePackage.getFileRecord().getOriginalName() + ".enc");
        headers.setContentLength(filePackage.getFileRecord().getSizeBytes());

        headers.add("X-File-Id", filePackage.getFileRecord().getId().toString());
        headers.add("X-File-Original-Name", filePackage.getFileRecord().getOriginalName());
        headers.add("X-File-IV", filePackage.getFileRecord().getIv());
        headers.add("X-File-Signature", filePackage.getFileRecord().getEcdsaSignature());
        headers.add("X-File-Hash", filePackage.getFileRecord().getCiphertextSha256());
        headers.add("X-File-Wrapped-Key", filePackage.getCallerWrappedKey());
        headers.add("X-File-Status", filePackage.getFileRecord().getStatus().name());

        // Stream ciphertext bytes directly from MinIO to the HTTP response output stream
        StreamingResponseBody responseBody = outputStream -> {
            try (InputStream is = filePackage.getCiphertextStream()) {
                byte[] buffer = new byte[8192];
                int bytesRead;
                while ((bytesRead = is.read(buffer)) != -1) {
                    outputStream.write(buffer, 0, bytesRead);
                }
                outputStream.flush();
            }
        };

        return new ResponseEntity<>(responseBody, headers, HttpStatus.OK);
    }

    /**
     * Adds an AES key wrapped for a new recipient.
     * WHAT: Allows the file owner to register a wrapped key for another user.
     * WHY: Powers POST /files/{id}/keys for zero-knowledge file sharing with a second person.
     *
     * @param id             Target file UUID
     * @param request        AddFileKeyRequest containing recipient UUID and wrapped AES key
     * @param authentication Spring Security authentication holding caller UUID
     * @return ApiResponse containing FileKeyResponse with status 201 Created
     */
    @PostMapping("/{id}/keys")
    public ResponseEntity<ApiResponse<FileKeyResponse>> addRecipientKey(
            @PathVariable UUID id,
            @Valid @RequestBody AddFileKeyRequest request,
            Authentication authentication) {

        UUID callerId = (UUID) authentication.getPrincipal();
        log.info("Owner {} adding wrapped key on file {} for recipient {}", callerId, id, request.getRecipientId());

        FileKeyResponse response = fileService.addRecipientKey(id, callerId, request);
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(ApiResponse.success("Recipient key added successfully", response));
    }
}
