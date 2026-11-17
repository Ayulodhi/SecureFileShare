package com.securefileshare.file;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.securefileshare.file.client.AuthServiceClient;
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
import java.util.Base64;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@AutoConfigureMockMvc
public class FileIsolationTddTest {

    private static final String TEST_JWT_SECRET = "super_secret_test_jwt_key_file_service_2026_at_least_32_chars!";

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        String host = System.getenv().getOrDefault("POSTGRES_HOST", "localhost");
        String port = System.getenv().getOrDefault("POSTGRES_PORT", "5432");
        registry.add("spring.datasource.url", () -> "jdbc:postgresql://" + host + ":" + port + "/sfs_file");
        registry.add("spring.datasource.username", () -> "sfs_admin");
        registry.add("spring.datasource.password", () -> "sfs_secure_pass_2026");
        registry.add("security.jwt.secret", () -> TEST_JWT_SECRET);
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

    private UUID aliceId;
    private UUID bobId;
    private String aliceToken;
    private String bobToken;

    @BeforeEach
    void setUp() {
        fileKeyRepository.deleteAll();
        fileRepository.deleteAll();

        aliceId = UUID.randomUUID();
        bobId = UUID.randomUUID();

        SecretKey key = Keys.hmacShaKeyFor(TEST_JWT_SECRET.getBytes(StandardCharsets.UTF_8));
        aliceToken = Jwts.builder()
                .subject(aliceId.toString())
                .claim("username", "alice")
                .claim("role", "ROLE_SENDER_RECIPIENT")
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + 3600000))
                .signWith(key)
                .compact();

        bobToken = Jwts.builder()
                .subject(bobId.toString())
                .claim("username", "bob")
                .claim("role", "ROLE_SENDER_RECIPIENT")
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + 3600000))
                .signWith(key)
                .compact();
    }

    @Test
    @DisplayName("TDD Isolation: Unauthorized user accessing another user's file package receives 404 Not Found")
    void testUnauthorizedUserFileAccessReturns404() throws Exception {
        // Alice uploads a file (mocked or direct repository insert)
        // For TDD Red phase, let's insert a file owned by Alice directly
        com.securefileshare.file.entity.FileRecord aliceFile = new com.securefileshare.file.entity.FileRecord(
                aliceId,
                "alice_secret.pdf",
                1024L,
                "a1b2c3d4e5f60718293a4b5c6d7e8f901234567890abcdef1234567890abcdef",
                "dGVzdF9pdg==",
                "dGVzdF9zaWc=",
                aliceId + "/test-object-key"
        );
        com.securefileshare.file.entity.FileRecord saved = fileRepository.save(aliceFile);

        // Bob attempts to download Alice's file package -> must return 404 Not Found (not 403) to prevent enumeration
        mockMvc.perform(get("/files/" + saved.getId() + "/package")
                        .header("Authorization", "Bearer " + bobToken))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("TDD Isolation: User can list only their own files")
    void testUserCanOnlyListOwnFiles() throws Exception {
        com.securefileshare.file.entity.FileRecord aliceFile = new com.securefileshare.file.entity.FileRecord(
                aliceId, "alice_file.txt", 500L,
                "0123456789abcdef0123456789abcdef0123456789abcdef0123456789abcdef",
                "iv==", "sig==", aliceId + "/alice-key"
        );
        fileRepository.save(aliceFile);

        // Bob lists files -> should return empty list
        MvcResult result = mockMvc.perform(get("/files")
                        .header("Authorization", "Bearer " + bobToken))
                .andExpect(status().isOk())
                .andReturn();

        String json = result.getResponse().getContentAsString();
        assertThat(json).doesNotContain("alice_file.txt");
    }
}
