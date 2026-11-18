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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@AutoConfigureMockMvc
public class TaskOneAuthorizationTddTest extends BaseIntegrationTest {

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
    @DisplayName("Task 1 RED: DELETE file by stranger must return 404 matching non-existent control response")
    void testStrangerDeleteReturns404MatchingMissing() throws Exception {
        com.securefileshare.file.entity.FileRecord aliceFile = new com.securefileshare.file.entity.FileRecord(
                aliceId,
                "alice_file.pdf",
                1024L,
                "a1b2c3d4e5f60718293a4b5c6d7e8f901234567890abcdef1234567890abcdef",
                "dGVzdF9pdg==",
                "dGVzdF9zaWc=",
                aliceId + "/obj-1"
        );
        com.securefileshare.file.entity.FileRecord saved = fileRepository.save(aliceFile);
        UUID missingId = UUID.randomUUID();

        // 1. Bob deletes missing ID -> 404
        MvcResult missingRes = mockMvc.perform(delete("/files/" + missingId)
                        .header("Authorization", "Bearer " + bobToken))
                .andExpect(status().isNotFound())
                .andReturn();

        // 2. Bob deletes Alice's file ID -> should be 404
        MvcResult strangerRes = mockMvc.perform(delete("/files/" + saved.getId())
                        .header("Authorization", "Bearer " + bobToken))
                .andExpect(status().isNotFound())
                .andReturn();

        String missingNormalized = TestErrorUtil.normalizeErrorJson(missingRes.getResponse().getContentAsString(), missingId);
        String strangerNormalized = TestErrorUtil.normalizeErrorJson(strangerRes.getResponse().getContentAsString(), saved.getId());

        assertThat(strangerRes.getResponse().getStatus()).isEqualTo(missingRes.getResponse().getStatus());
        assertThat(strangerRes.getResponse().getContentType()).isEqualTo(missingRes.getResponse().getContentType());
        assertThat(strangerNormalized).isEqualTo(missingNormalized);
    }
}
