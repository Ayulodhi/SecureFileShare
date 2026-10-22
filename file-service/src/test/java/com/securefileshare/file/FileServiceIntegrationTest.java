package com.securefileshare.file;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.securefileshare.file.client.AuthServiceClient;
import com.securefileshare.file.dto.AddFileKeyRequest;
import com.securefileshare.file.dto.KeyRegistryResponseDto;
import com.securefileshare.file.repository.FileKeyRepository;
import com.securefileshare.file.repository.FileRepository;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.security.*;
import java.security.spec.ECGenParameterSpec;
import java.time.Instant;
import java.util.*;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * End-to-end integration tests for file-service against containerized PostgreSQL and MinIO.
 * WHAT: Verifies zero-knowledge file upload streaming, SHA-256 integrity checks, tampered ciphertext rejection,
 *      ECDSA signature validation, package streaming retrieval, and multi-recipient key sharing.
 * WHY: Validates end-to-end correctness of Phase 2 file-service requirements against live containerized dependencies.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@AutoConfigureMockMvc
public class FileServiceIntegrationTest {

    private static final String TEST_JWT_SECRET = "super_secret_test_jwt_key_file_service_2026_at_least_32_chars!";

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        // Wire containerized PostgreSQL properties from running Docker environment
        String host = System.getenv().getOrDefault("POSTGRES_HOST", "localhost");
        String port = System.getenv().getOrDefault("POSTGRES_PORT", "5432");
        String user = System.getenv().getOrDefault("POSTGRES_USER", "sfs_admin");
        String pass = System.getenv().getOrDefault("POSTGRES_PASSWORD", "sfs_secure_pass_2026");

        registry.add("spring.datasource.url", () -> "jdbc:postgresql://" + host + ":" + port + "/sfs_file");
        registry.add("spring.datasource.username", () -> user);
        registry.add("spring.datasource.password", () -> pass);

        // Wire containerized MinIO properties from running Docker environment
        String minioEndpoint = System.getenv().getOrDefault("MINIO_ENDPOINT", "http://localhost:9000");
        String minioUser = System.getenv().getOrDefault("MINIO_ROOT_USER", "minio_admin");
        String minioPass = System.getenv().getOrDefault("MINIO_ROOT_PASSWORD", "minio_secret_pass_2026");

        registry.add("minio.endpoint", () -> minioEndpoint);
        registry.add("minio.access-key", () -> minioUser);
        registry.add("minio.secret-key", () -> minioPass);
        registry.add("minio.bucket-name", () -> "secure-files");

        // Wire security and file properties
        registry.add("security.jwt.secret", () -> TEST_JWT_SECRET);
        registry.add("security.jwt.expiration-ms", () -> 3600000L);
        registry.add("file.max-size-bytes", () -> 104857600L);
        registry.add("services.auth.url", () -> "http://localhost:8081");
    }

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private FileRepository fileRepository;

    @Autowired
    private FileKeyRepository fileKeyRepository;

    @MockBean
    private AuthServiceClient authServiceClient;

    private KeyPair ownerEcKeyPair;
    private UUID ownerId;
    private String ownerToken;
    private String ownerEcPublicKeyPem;

    @BeforeEach
    void setUp() throws Exception {
        // Generate cryptographic NIST P-256 EC key pair for the test owner
        KeyPairGenerator kpg = KeyPairGenerator.getInstance("EC");
        kpg.initialize(new ECGenParameterSpec("secp256r1"));
        ownerEcKeyPair = kpg.generateKeyPair();

        ownerEcPublicKeyPem = Base64.getEncoder().encodeToString(ownerEcKeyPair.getPublic().getEncoded());
        ownerId = UUID.randomUUID();
        ownerToken = generateTestJwt(ownerId, "testowner", "ROLE_SENDER_RECIPIENT");

        // Mock auth-service public key registry response for the test owner
        KeyRegistryResponseDto keyResponse = new KeyRegistryResponseDto(
                UUID.randomUUID(),
                ownerId,
                "dummy_rsa_public_key",
                ownerEcPublicKeyPem,
                "dummy_rsa_fp",
                "dummy_ec_fp",
                Instant.now(),
                false
        );
        when(authServiceClient.getActiveKeys(eq(ownerId))).thenReturn(keyResponse);
    }

    @Test
    @DisplayName("Upload valid encrypted file: streams to MinIO, recomputes SHA-256, verifies ECDSA, saves status PENDING_SCAN")
    void testUploadValidEncryptedFile() throws Exception {
        byte[] ciphertext = "Encrypted binary ciphertext payload for confidential document".getBytes(StandardCharsets.UTF_8);
        String ciphertextHash = computeSha256Hex(ciphertext);
        String ecdsaSignature = signWithEcdsa(ownerEcKeyPair.getPrivate(), ciphertextHash);
        String iv = Base64.getEncoder().encodeToString("12ByteGcmIV!!".getBytes(StandardCharsets.UTF_8));
        String ownerWrappedKey = Base64.getEncoder().encodeToString("WrappedAesKeyBytes4096Bits".getBytes(StandardCharsets.UTF_8));

        MockMultipartFile file = new MockMultipartFile(
                "ciphertext",
                "ciphertext.enc",
                MediaType.APPLICATION_OCTET_STREAM_VALUE,
                ciphertext
        );

        MvcResult result = mockMvc.perform(multipart("/files")
                        .file(file)
                        .param("originalName", "confidential_project.pdf")
                        .param("iv", iv)
                        .param("ecdsaSignature", ecdsaSignature)
                        .param("ciphertextHash", ciphertextHash)
                        .param("ownerWrappedKey", ownerWrappedKey)
                        .header("Authorization", "Bearer " + ownerToken))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.originalName").value("confidential_project.pdf"))
                .andExpect(jsonPath("$.data.status").value("PENDING_SCAN"))
                .andExpect(jsonPath("$.data.ciphertextSha256").value(ciphertextHash))
                .andExpect(jsonPath("$.data.sizeBytes").value(ciphertext.length))
                .andReturn();

        JsonNode json = objectMapper.readTree(result.getResponse().getContentAsString());
        UUID fileId = UUID.fromString(json.path("data").path("id").asText());

        // Verify database persistence
        assertThat(fileRepository.findById(fileId)).isPresent();
        assertThat(fileKeyRepository.findByFileRecord_IdAndRecipientId(fileId, ownerId)).isPresent();

        // Verify GET /files lists this file
        mockMvc.perform(get("/files")
                        .header("Authorization", "Bearer " + ownerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data[0].id").value(fileId.toString()))
                .andExpect(jsonPath("$.data[0].originalName").value("confidential_project.pdf"));

        // Verify GET /files/{id}/package streams the exact ciphertext and returns caller's wrapped key in headers
        MvcResult packageResult = mockMvc.perform(get("/files/{id}/package", fileId)
                        .header("Authorization", "Bearer " + ownerToken))
                .andExpect(status().isOk())
                .andExpect(header().string("X-File-Id", fileId.toString()))
                .andExpect(header().string("X-File-Original-Name", "confidential_project.pdf"))
                .andExpect(header().string("X-File-IV", iv))
                .andExpect(header().string("X-File-Hash", ciphertextHash))
                .andExpect(header().string("X-File-Wrapped-Key", ownerWrappedKey))
                .andExpect(header().string("X-File-Status", "PENDING_SCAN"))
                .andReturn();

        MvcResult dispatchedPackage = mockMvc.perform(asyncDispatch(packageResult))
                .andExpect(status().isOk())
                .andReturn();

        byte[] downloadedBytes = dispatchedPackage.getResponse().getContentAsByteArray();
        assertThat(downloadedBytes).isEqualTo(ciphertext);
    }

    @Test
    @DisplayName("Tampered ciphertext: recomputed SHA-256 does not match client hash, rejects with 400, purges MinIO")
    void testTamperedCiphertextRejected() throws Exception {
        byte[] originalCiphertext = "Authentic secret ciphertext data".getBytes(StandardCharsets.UTF_8);
        String authenticHash = computeSha256Hex(originalCiphertext);
        String validSignature = signWithEcdsa(ownerEcKeyPair.getPrivate(), authenticHash);

        // Tamper with the ciphertext stream during transit
        byte[] tamperedCiphertext = "TAMPERED secret ciphertext data".getBytes(StandardCharsets.UTF_8);

        MockMultipartFile file = new MockMultipartFile(
                "ciphertext",
                "ciphertext.enc",
                MediaType.APPLICATION_OCTET_STREAM_VALUE,
                tamperedCiphertext // altered bytes
        );

        mockMvc.perform(multipart("/files")
                        .file(file)
                        .param("originalName", "financial_report.xlsx")
                        .param("iv", "SampleIVBase64==")
                        .param("ecdsaSignature", validSignature)
                        .param("ciphertextHash", authenticHash) // declared hash of original
                        .param("ownerWrappedKey", "WrappedKeyBase64==")
                        .header("Authorization", "Bearer " + ownerToken))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.containsString("integrity mismatch")));

        // Verify no record was persisted in database
        assertThat(fileRepository.findByOwnerIdOrderByCreatedAtDesc(ownerId))
                .filteredOn(f -> f.getOriginalName().equals("financial_report.xlsx"))
                .isEmpty();
    }

    @Test
    @DisplayName("Forged ECDSA signature: rejected with 400 Bad Request before streaming")
    void testForgedSignatureRejected() throws Exception {
        byte[] ciphertext = "Important military document".getBytes(StandardCharsets.UTF_8);
        String ciphertextHash = computeSha256Hex(ciphertext);

        // Generate a separate forged key pair to simulate attacker signature
        KeyPairGenerator kpg = KeyPairGenerator.getInstance("EC");
        kpg.initialize(new ECGenParameterSpec("secp256r1"));
        KeyPair attackerKey = kpg.generateKeyPair();
        String forgedSignature = signWithEcdsa(attackerKey.getPrivate(), ciphertextHash);

        MockMultipartFile file = new MockMultipartFile(
                "ciphertext",
                "ciphertext.enc",
                MediaType.APPLICATION_OCTET_STREAM_VALUE,
                ciphertext
        );

        mockMvc.perform(multipart("/files")
                        .file(file)
                        .param("originalName", "top_secret.docx")
                        .param("iv", "SampleIVBase64==")
                        .param("ecdsaSignature", forgedSignature)
                        .param("ciphertextHash", ciphertextHash)
                        .param("ownerWrappedKey", "WrappedKeyBase64==")
                        .header("Authorization", "Bearer " + ownerToken))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.containsString("signature verification failed")));
    }

    @Test
    @DisplayName("Multi-recipient sharing: owner adds recipient wrapped key, recipient retrieves package")
    void testOwnerAddsRecipientKeyAndRecipientRetrievesPackage() throws Exception {
        // Step 1: Owner uploads file
        byte[] ciphertext = "Multi recipient shared dataset".getBytes(StandardCharsets.UTF_8);
        String ciphertextHash = computeSha256Hex(ciphertext);
        String ecdsaSignature = signWithEcdsa(ownerEcKeyPair.getPrivate(), ciphertextHash);
        String ownerWrappedKey = "OwnerWrappedAesKey==";

        MockMultipartFile file = new MockMultipartFile(
                "ciphertext",
                "ciphertext.enc",
                MediaType.APPLICATION_OCTET_STREAM_VALUE,
                ciphertext
        );

        MvcResult uploadResult = mockMvc.perform(multipart("/files")
                        .file(file)
                        .param("originalName", "dataset.csv")
                        .param("iv", "IVSample==")
                        .param("ecdsaSignature", ecdsaSignature)
                        .param("ciphertextHash", ciphertextHash)
                        .param("ownerWrappedKey", ownerWrappedKey)
                        .header("Authorization", "Bearer " + ownerToken))
                .andExpect(status().isCreated())
                .andReturn();

        JsonNode json = objectMapper.readTree(uploadResult.getResponse().getContentAsString());
        UUID fileId = UUID.fromString(json.path("data").path("id").asText());

        // Step 2: Recipient user details
        UUID recipientId = UUID.randomUUID();
        String recipientToken = generateTestJwt(recipientId, "recipient_user", "ROLE_SENDER_RECIPIENT");
        String recipientWrappedKey = "RecipientWrappedAesKeyWithRecipientRsaKey==";

        // Step 3: Owner adds recipient wrapped key via POST /files/{id}/keys
        AddFileKeyRequest addKeyRequest = new AddFileKeyRequest(recipientId, recipientWrappedKey);
        mockMvc.perform(post("/files/{id}/keys", fileId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(addKeyRequest))
                        .header("Authorization", "Bearer " + ownerToken))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.recipientId").value(recipientId.toString()))
                .andExpect(jsonPath("$.data.wrappedKey").value(recipientWrappedKey));

        // Step 4: Recipient retrieves the package via GET /files/{id}/package
        MvcResult recipientResult = mockMvc.perform(get("/files/{id}/package", fileId)
                        .header("Authorization", "Bearer " + recipientToken))
                .andExpect(status().isOk())
                .andExpect(header().string("X-File-Wrapped-Key", recipientWrappedKey))
                .andReturn();

        MvcResult dispatchedRecipient = mockMvc.perform(asyncDispatch(recipientResult))
                .andExpect(status().isOk())
                .andReturn();

        assertThat(dispatchedRecipient.getResponse().getContentAsByteArray()).isEqualTo(ciphertext);

        // Step 5: Unauthorized third-party user cannot retrieve package (403 Forbidden)
        UUID unauthorizedUserId = UUID.randomUUID();
        String unauthorizedToken = generateTestJwt(unauthorizedUserId, "attacker", "ROLE_SENDER_RECIPIENT");
        mockMvc.perform(get("/files/{id}/package", fileId)
                        .header("Authorization", "Bearer " + unauthorizedToken))
                .andExpect(status().isForbidden());

        // Step 6: Non-owner cannot add recipient keys (403 Forbidden)
        AddFileKeyRequest illegalKeyRequest = new AddFileKeyRequest(UUID.randomUUID(), "IllegalKey==");
        mockMvc.perform(post("/files/{id}/keys", fileId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(illegalKeyRequest))
                        .header("Authorization", "Bearer " + unauthorizedToken))
                .andExpect(status().isForbidden());
    }

    /**
     * Generates a signed JWT for testing authenticated endpoints.
     */
    private String generateTestJwt(UUID userId, String username, String role) {
        SecretKey key = Keys.hmacShaKeyFor(TEST_JWT_SECRET.getBytes(StandardCharsets.UTF_8));
        return Jwts.builder()
                .subject(userId.toString())
                .id(UUID.randomUUID().toString())
                .claim("username", username)
                .claim("role", role)
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + 3600000))
                .signWith(key)
                .compact();
    }

    /**
     * Computes the lowercase 64-character SHA-256 hex digest of a byte array.
     */
    private String computeSha256Hex(byte[] data) throws NoSuchAlgorithmException {
        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        return HexFormat.of().formatHex(digest.digest(data));
    }

    /**
     * Signs the ciphertext SHA-256 hash using the EC private key.
     */
    private String signWithEcdsa(PrivateKey privateKey, String hashHex) throws Exception {
        Signature signature = Signature.getInstance("SHA256withECDSA");
        signature.initSign(privateKey);
        signature.update(hashHex.getBytes(StandardCharsets.UTF_8));
        return Base64.getEncoder().encodeToString(signature.sign());
    }
}
