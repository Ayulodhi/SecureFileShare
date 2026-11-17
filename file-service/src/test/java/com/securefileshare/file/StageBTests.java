package com.securefileshare.file;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.securefileshare.file.client.AuthServiceClient;
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
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@AutoConfigureMockMvc
public class StageBTests {

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
    private FileRepository fileRepository;

    @Autowired
    private FileKeyRepository fileKeyRepository;

    @MockBean
    private AuthServiceClient authServiceClient;

    private UUID aliceId;
    private UUID bobId;
    private String bobToken;

    @BeforeEach
    void setUp() {
        fileKeyRepository.deleteAll();
        fileRepository.deleteAll();

        aliceId = UUID.randomUUID();
        bobId = UUID.randomUUID();

        SecretKey key = Keys.hmacShaKeyFor(TEST_JWT_SECRET.getBytes(StandardCharsets.UTF_8));
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
    @DisplayName("Stage B - Task 1 & 2: Central authorization and shared-with-me verification")
    void testCentralAuthAndSharedWithMe() throws Exception {
        com.securefileshare.file.entity.FileRecord aliceFile = new com.securefileshare.file.entity.FileRecord(
                aliceId, "shared_doc.txt", 512L,
                "a1b2c3d4e5f60718293a4b5c6d7e8f901234567890abcdef1234567890abcdef",
                "iv==", "sig==", aliceId + "/obj-stageb"
        );
        com.securefileshare.file.entity.FileRecord saved = fileRepository.save(aliceFile);

        // Bob tries to access Alice's package without grant -> 404 Not Found
        mockMvc.perform(get("/files/" + saved.getId() + "/package")
                        .header("Authorization", "Bearer " + bobToken))
                .andExpect(status().isNotFound());

        // Grant access directly via file_keys
        fileKeyRepository.save(new com.securefileshare.file.entity.FileKey(saved, bobId, "wrapped_key"));

        // Bob checks shared-with-me -> contains file
        MvcResult sharedRes = mockMvc.perform(get("/files/shared-with-me")
                        .header("Authorization", "Bearer " + bobToken))
                .andExpect(status().isOk())
                .andReturn();
        assertThat(sharedRes.getResponse().getContentAsString()).contains("shared_doc.txt");
    }
}
