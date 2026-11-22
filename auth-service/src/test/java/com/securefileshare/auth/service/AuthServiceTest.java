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
}
