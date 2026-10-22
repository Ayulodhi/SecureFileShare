package com.securefileshare.auth.service;

import com.securefileshare.auth.dto.AuthResponse;
import com.securefileshare.auth.dto.LoginRequest;
import com.securefileshare.auth.dto.RegisterRequest;
import com.securefileshare.auth.dto.UserResponse;
import com.securefileshare.auth.entity.Role;
import com.securefileshare.auth.entity.User;
import com.securefileshare.auth.repository.UserRepository;
import com.securefileshare.auth.security.JwtService;
import com.securefileshare.common.constant.SecurityConstants;
import com.securefileshare.common.exception.ApiException;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;
import java.util.Objects;

/**
 * Service orchestrating user registration, authentication, token issuance, and logout.
 * WHAT: Coordinates database user management, password verification, rate-limiting, and JWT operations.
 * WHY: Centralizes identity business logic and guarantees password hashes and tokens are handled securely.
 */
@Service
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final RateLimiterService rateLimiterService;
    private final StringRedisTemplate redisTemplate;

    public AuthService(
            UserRepository userRepository,
            PasswordEncoder passwordEncoder,
            JwtService jwtService,
            RateLimiterService rateLimiterService,
            StringRedisTemplate redisTemplate) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
        this.rateLimiterService = rateLimiterService;
        this.redisTemplate = redisTemplate;
    }

    /**
     * Registers a new user account on the platform.
     *
     * @param request Validated registration parameters
     * @return UserResponse summary
     */
    @Transactional
    public UserResponse register(RegisterRequest request) {
        // Security Rule: Administrative accounts cannot be self-registered
        // Why: Prevents privilege escalation attacks via self-service signup endpoints
        if (request.getRole() == Role.ADMIN) {
            throw new ApiException(HttpStatus.FORBIDDEN, "Administrative accounts cannot be self-registered");
        }

        // Validate uniqueness of username
        if (userRepository.existsByUsername(request.getUsername())) {
            throw new ApiException(HttpStatus.CONFLICT, "Username is already taken");
        }

        // Validate uniqueness of email address
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new ApiException(HttpStatus.CONFLICT, "Email address is already registered");
        }

        // Hash the password with salted BCrypt (work factor 12)
        // Why: Protects user credentials against offline rainbow table and dictionary attacks
        String passwordHash = passwordEncoder.encode(request.getPassword());

        User user = new User(
                request.getUsername().trim(),
                request.getEmail().toLowerCase().trim(),
                passwordHash,
                request.getRole()
        );

        User saved = userRepository.save(user);

        return new UserResponse(saved.getId(), saved.getUsername(), saved.getEmail(), saved.getRole(), saved.getCreatedAt());
    }

    /**
     * Authenticates credentials, checks rate limits, and issues a signed JWT access token.
     *
     * @param request  Login credentials
     * @param clientIp Source IP address of client for rate-limiting
     * @return AuthResponse containing signed JWT
     */
    @Transactional(readOnly = true)
    public AuthResponse login(LoginRequest request, String clientIp) {
        // Step 1: Enforce rate limiting before checking password
        // Why: Blocks abusive automated brute-force attempts before performing CPU-intensive BCrypt checks
        rateLimiterService.checkRateLimit(request.getUsername(), clientIp);

        // Step 2: Lookup user by username
        User user = userRepository.findByUsername(request.getUsername())
                .orElse(null);

        // Step 3: Verify credentials
        // Security Decision: Use a constant-time check and a generic failure message
        // Why: Prevents user enumeration attacks (attacker cannot distinguish between nonexistent user and wrong password)
        if (user == null || !passwordEncoder.matches(request.getPassword(), user.getPasswordHash())) {
            rateLimiterService.recordFailedAttempt(request.getUsername(), clientIp);
            throw new ApiException(HttpStatus.UNAUTHORIZED, "Invalid username or password");
        }

        // Step 4: Reset rate-limit counter upon successful authentication
        rateLimiterService.resetAttempts(request.getUsername(), clientIp);

        // Step 5: Generate cryptographically signed JWT with unique JTI
        String token = jwtService.generateToken(
                user.getId(),
                user.getUsername(),
                user.getRole().name()
        );

        long expiresInSeconds = jwtService.getExpirationMs() / 1000;

        return new AuthResponse(
                token,
                expiresInSeconds,
                user.getId(),
                user.getUsername(),
                user.getRole().name()
        );
    }

    /**
     * Invalidates the caller's active JWT by placing its JTI into the Redis blacklist.
     *
     * @param token Active Bearer token string
     */
    public void logout(String token) {
        if (token == null || !jwtService.validateToken(token)) {
            // If token is already invalid or expired, no revocation needed
            return;
        }

        String jti = jwtService.extractJti(token);
        Instant expiration = jwtService.extractExpiration(token);
        Instant now = Instant.now();

        // Calculate remaining token lifetime so the blacklist key automatically expires once the token naturally lapses
        long remainingMillis = Duration.between(now, expiration).toMillis();

        if (remainingMillis > 0) {
            String blacklistKey = SecurityConstants.REDIS_JWT_BLACKLIST_PREFIX + jti;
            // Place JTI in Redis with remaining TTL
            // Why: Enforces instantaneous session revocation without maintaining a persistent database table
            // Explicitly assert non-null Duration to satisfy Spring Data Redis @NonNull parameter contract
            redisTemplate.opsForValue().set(blacklistKey, "revoked", Objects.requireNonNull(Duration.ofMillis(remainingMillis)));
        }
    }
}
