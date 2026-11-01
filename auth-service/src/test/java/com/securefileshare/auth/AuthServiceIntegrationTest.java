package com.securefileshare.auth;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.securefileshare.auth.dto.KeyRegistrationRequest;
import com.securefileshare.auth.dto.LoginRequest;
import com.securefileshare.auth.dto.RegisterRequest;
import com.securefileshare.auth.entity.Role;
import com.securefileshare.auth.repository.KeyRegistryRepository;
import com.securefileshare.auth.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.http.MediaType;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.SecureRandom;
import java.security.spec.ECGenParameterSpec;
import java.util.Base64;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * End-to-end integration tests for auth-service against containerized PostgreSQL.
 * WHAT: Verifies user registration, login, JWT token issuance, RSA-4096 and EC P-256 public key registry,
 *       key retrieval, and security enforcement for weak keys.
 * WHY: Validates critical authentication and zero-knowledge cryptographic identity distribution contracts.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@AutoConfigureMockMvc
public class AuthServiceIntegrationTest {

    private static final String TEST_JWT_SECRET = "super_secret_test_jwt_key_auth_service_2026_at_least_32_chars!";

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        String host = System.getenv().getOrDefault("POSTGRES_HOST", "localhost");
        String port = System.getenv().getOrDefault("POSTGRES_PORT", "5432");
        String user = System.getenv().getOrDefault("POSTGRES_USER", "sfs_admin");
        String pass = System.getenv().getOrDefault("POSTGRES_PASSWORD", "sfs_secure_pass_2026");

        registry.add("spring.datasource.url", () -> "jdbc:postgresql://" + host + ":" + port + "/sfs_auth");
        registry.add("spring.datasource.username", () -> user);
        registry.add("spring.datasource.password", () -> pass);

        // Required application security & seed properties
        registry.add("security.jwt.secret", () -> TEST_JWT_SECRET);
        registry.add("security.jwt.expiration-ms", () -> 3600000L);
        registry.add("security.rate-limit.max-attempts", () -> 5);
        registry.add("security.rate-limit.window-minutes", () -> 15);
        registry.add("security.admin.initial-username", () -> "test_admin");
        registry.add("security.admin.initial-email", () -> "test_admin@securefileshare.com");
        registry.add("security.admin.initial-password", () -> "Admin_Secure_Pass_2026!");
    }

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private KeyRegistryRepository keyRegistryRepository;

    @MockBean
    private StringRedisTemplate redisTemplate;

    @Mock
    private ValueOperations<String, String> valueOperations;

    @BeforeEach
    void setupRedisMock() {
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(redisTemplate.hasKey(anyString())).thenReturn(false);
    }

    @Test
    @DisplayName("User lifecycle: Register -> Login -> Register Cryptographic Public Keys -> Retrieve Public Keys")
    void testCompleteUserAndKeyRegistryLifecycle() throws Exception {
        String uniqueSuffix = UUID.randomUUID().toString().substring(0, 8);
        String username = "alice_" + uniqueSuffix;
        String email = "alice_" + uniqueSuffix + "@example.com";
        String password = "Password123!";

        // 1. Register new user
        RegisterRequest registerRequest = new RegisterRequest(username, email, password, Role.SENDER_RECIPIENT);
        MvcResult registerResult = mockMvc.perform(post("/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(registerRequest)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.username").value(username))
                .andExpect(jsonPath("$.data.email").value(email))
                .andExpect(jsonPath("$.data.id").isNotEmpty())
                .andReturn();

        JsonNode regNode = objectMapper.readTree(registerResult.getResponse().getContentAsString());
        UUID registeredUserId = UUID.fromString(regNode.path("data").path("id").asText());

        // 2. Login with registered credentials
        LoginRequest loginRequest = new LoginRequest(username, password);
        MvcResult loginResult = mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.token").isNotEmpty())
                .andExpect(jsonPath("$.data.userId").value(registeredUserId.toString()))
                .andReturn();

        JsonNode loginNode = objectMapper.readTree(loginResult.getResponse().getContentAsString());
        String jwtToken = loginNode.path("data").path("token").asText();

        // 3. Generate valid RSA-4096 and EC P-256 public keys
        KeyPairGenerator rsaGen = KeyPairGenerator.getInstance("RSA");
        rsaGen.initialize(4096, new SecureRandom());
        KeyPair rsaPair = rsaGen.generateKeyPair();
        String rsaPem = toPem("PUBLIC KEY", rsaPair.getPublic().getEncoded());

        KeyPairGenerator ecGen = KeyPairGenerator.getInstance("EC");
        ecGen.initialize(new ECGenParameterSpec("secp256r1"), new SecureRandom());
        KeyPair ecPair = ecGen.generateKeyPair();
        String ecPem = toPem("PUBLIC KEY", ecPair.getPublic().getEncoded());

        // 4. Register public keys using the Bearer JWT token
        KeyRegistrationRequest keyRegRequest = new KeyRegistrationRequest(rsaPem, ecPem);
        MvcResult keyResult = mockMvc.perform(post("/auth/keys")
                        .header("Authorization", "Bearer " + jwtToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(keyRegRequest)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.rsaFingerprint").isNotEmpty())
                .andExpect(jsonPath("$.data.ecFingerprint").isNotEmpty())
                .andExpect(jsonPath("$.data.userId").value(registeredUserId.toString()))
                .andReturn();

        JsonNode keyNode = objectMapper.readTree(keyResult.getResponse().getContentAsString());
        String rsaFingerprint = keyNode.path("data").path("rsaFingerprint").asText();
        String ecFingerprint = keyNode.path("data").path("ecFingerprint").asText();
        assertThat(rsaFingerprint).hasSize(64);
        assertThat(ecFingerprint).hasSize(64);

        // 5. Retrieve public keys by user ID (unauthenticated public key discovery for senders)
        mockMvc.perform(get("/auth/keys/" + registeredUserId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.userId").value(registeredUserId.toString()))
                .andExpect(jsonPath("$.data.rsaFingerprint").value(rsaFingerprint))
                .andExpect(jsonPath("$.data.ecFingerprint").value(ecFingerprint))
                .andExpect(jsonPath("$.data.revoked").value(false));
    }

    @Test
    @DisplayName("Registering duplicate username returns 409 Conflict")
    void testDuplicateUsernameFails() throws Exception {
        String uniqueSuffix = UUID.randomUUID().toString().substring(0, 8);
        String username = "dupuser_" + uniqueSuffix;
        String email1 = "dup1_" + uniqueSuffix + "@example.com";
        String email2 = "dup2_" + uniqueSuffix + "@example.com";

        RegisterRequest first = new RegisterRequest(username, email1, "Pass1234!", Role.SENDER_RECIPIENT);
        mockMvc.perform(post("/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(first)))
                .andExpect(status().isCreated());

        RegisterRequest second = new RegisterRequest(username, email2, "Pass1234!", Role.SENDER_RECIPIENT);
        mockMvc.perform(post("/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(second)))
                .andExpect(status().isConflict());
    }

    @Test
    @DisplayName("Login with incorrect password returns 401 Unauthorized")
    void testInvalidPasswordFails() throws Exception {
        String uniqueSuffix = UUID.randomUUID().toString().substring(0, 8);
        String username = "bob_" + uniqueSuffix;
        String email = "bob_" + uniqueSuffix + "@example.com";

        RegisterRequest reg = new RegisterRequest(username, email, "CorrectPassword123!", Role.SENDER_RECIPIENT);
        mockMvc.perform(post("/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(reg)))
                .andExpect(status().isCreated());

        LoginRequest badLogin = new LoginRequest(username, "WrongPassword!");
        mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(badLogin)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("Registering weak 2048-bit RSA key returns 400 Bad Request")
    void testRejectWeakRsaKey() throws Exception {
        String uniqueSuffix = UUID.randomUUID().toString().substring(0, 8);
        String username = "weak_user_" + uniqueSuffix;
        String email = "weak_" + uniqueSuffix + "@example.com";

        RegisterRequest reg = new RegisterRequest(username, email, "Password123!", Role.SENDER_RECIPIENT);
        mockMvc.perform(post("/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(reg)))
                .andExpect(status().isCreated());

        LoginRequest login = new LoginRequest(username, "Password123!");
        MvcResult loginResult = mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(login)))
                .andExpect(status().isOk())
                .andReturn();

        String jwtToken = objectMapper.readTree(loginResult.getResponse().getContentAsString())
                .path("data").path("token").asText();

        // Generate weak 2048-bit RSA key
        KeyPairGenerator rsaGen = KeyPairGenerator.getInstance("RSA");
        rsaGen.initialize(2048, new SecureRandom());
        KeyPair weakRsa = rsaGen.generateKeyPair();
        String weakRsaPem = toPem("PUBLIC KEY", weakRsa.getPublic().getEncoded());

        // Valid EC P-256 key
        KeyPairGenerator ecGen = KeyPairGenerator.getInstance("EC");
        ecGen.initialize(new ECGenParameterSpec("secp256r1"), new SecureRandom());
        KeyPair ecPair = ecGen.generateKeyPair();
        String ecPem = toPem("PUBLIC KEY", ecPair.getPublic().getEncoded());

        KeyRegistrationRequest keyReg = new KeyRegistrationRequest(weakRsaPem, ecPem);
        mockMvc.perform(post("/auth/keys")
                        .header("Authorization", "Bearer " + jwtToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(keyReg)))
                .andExpect(status().isBadRequest());
    }

    private String toPem(String type, byte[] derBytes) {
        String base64 = Base64.getEncoder().encodeToString(derBytes);
        return "-----BEGIN " + type + "-----\n" + base64 + "\n-----END " + type + "-----\n";
    }
}
