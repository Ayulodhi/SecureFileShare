package com.securefileshare.auth.service;

import com.securefileshare.auth.client.AuditServiceClient;
import com.securefileshare.auth.dto.RegisterRequest;
import com.securefileshare.auth.entity.Role;
import com.securefileshare.auth.entity.User;
import com.securefileshare.auth.repository.UserRepository;
import com.securefileshare.auth.security.JwtService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit tests for AuthService.
 * WHAT: Verifies user registration and email verification triggering.
 * WHY: Ensures new user registration successfully invokes EmailService.
 */
class AuthServiceTest {

    private UserRepository userRepository;
    private PasswordEncoder passwordEncoder;
    private JwtService jwtService;
    private RateLimiterService rateLimiterService;
    private StringRedisTemplate redisTemplate;
    private AuditServiceClient auditServiceClient;
    private EmailService emailService;
    private AuthService authService;

    @BeforeEach
    void setUp() {
        userRepository = Mockito.mock(UserRepository.class);
        passwordEncoder = Mockito.mock(PasswordEncoder.class);
        jwtService = Mockito.mock(JwtService.class);
        rateLimiterService = Mockito.mock(RateLimiterService.class);
        redisTemplate = Mockito.mock(StringRedisTemplate.class);
        auditServiceClient = Mockito.mock(AuditServiceClient.class);
        emailService = Mockito.mock(EmailService.class);

        authService = new AuthService(
                userRepository,
                passwordEncoder,
                jwtService,
                rateLimiterService,
                redisTemplate,
                auditServiceClient,
                emailService
        );
    }

    @Test
    @DisplayName("Should send verification email upon successful user registration")
    void testRegisterTriggersEmailVerification() {
        RegisterRequest request = new RegisterRequest();
        request.setUsername("alice");
        request.setEmail("alice@example.com");
        request.setPassword("Password123!");
        request.setRole(Role.SENDER_RECIPIENT);

        when(userRepository.existsByUsername(anyString())).thenReturn(false);
        when(userRepository.existsByEmail(anyString())).thenReturn(false);
        when(passwordEncoder.encode(anyString())).thenReturn("hashed_pass");
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> {
            User u = invocation.getArgument(0);
            return u;
        });

        var response = authService.register(request);

        assertThat(response).isNotNull();
        assertThat(response.getEmail()).isEqualTo("alice@example.com");
        verify(emailService).sendVerificationEmail(anyString(), anyString());
    }

    @Test
    @DisplayName("Should send document share email when recipient has account")
    void testSendShareNotificationToRegisteredUser() {
        String recipientEmail = "bob@example.com";
        User bob = new User("bob", recipientEmail, "hash", Role.SENDER_RECIPIENT);

        when(userRepository.findByEmail(recipientEmail)).thenReturn(java.util.Optional.of(bob));

        authService.sendShareNotification(recipientEmail, "alice", "document.pdf");

        verify(emailService).sendDocumentShareEmail(recipientEmail, "alice", "document.pdf");
    }

    @Test
    @DisplayName("Should send document share invite email when recipient has no account")
    void testSendShareNotificationToUnregisteredUser() {
        String unknownEmail = "unknown@example.com";

        when(userRepository.findByEmail(unknownEmail)).thenReturn(java.util.Optional.empty());
        when(userRepository.findByUsername(unknownEmail)).thenReturn(java.util.Optional.empty());

        authService.sendShareNotification(unknownEmail, "alice", "document.pdf");

        verify(emailService).sendDocumentShareInviteEmail(unknownEmail, "alice", "document.pdf");
    }
}
