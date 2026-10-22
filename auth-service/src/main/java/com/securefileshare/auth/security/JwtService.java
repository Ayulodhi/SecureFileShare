package com.securefileshare.auth.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;
import java.util.UUID;

/**
 * Service responsible for cryptographic JSON Web Token (JWT) creation and validation.
 * WHAT: Issues cryptographically signed HMAC-SHA256 tokens and parses claim payloads.
 * WHY: Provides stateless user authentication across services while embedding user roles and unique token IDs (jti).
 */
@Service
public class JwtService {

    private final SecretKey signingKey;
    private final long expirationMs;

    /**
     * Initializes the JWT service with secret key and lifetime from configuration.
     *
     * @param secret       HMAC secret string (at least 256 bits)
     * @param expirationMs Token validity lifetime in milliseconds
     */
    public JwtService(
            @Value("${security.jwt.secret}") String secret,
            @Value("${security.jwt.expiration-ms:900000}") long expirationMs) {
        // Enforce presence of JWT secret key from environment configuration
        // Why: Prevents running authentication services without a configured cryptographic secret
        if (secret == null || secret.isBlank()) {
            throw new IllegalArgumentException("Missing required JWT signing secret. Provide JWT_SECRET environment variable.");
        }

        // Enforce strong signing key of at least 32 bytes (256 bits)
        // Why: Prevents brute-force dictionary attacks against HMAC-SHA256 signatures
        byte[] keyBytes = secret.getBytes(StandardCharsets.UTF_8);
        if (keyBytes.length < 32) {
            throw new IllegalArgumentException("JWT secret key must be at least 32 characters (256 bits) long. Current length: " + keyBytes.length);
        }
        this.signingKey = Keys.hmacShaKeyFor(keyBytes);
        this.expirationMs = expirationMs;
    }

    /**
     * Generates a signed JWT containing user identity, role, and a unique JTI.
     *
     * @param userId   User UUID
     * @param username Username string
     * @param role     User role name (e.g., ROLE_SENDER_RECIPIENT)
     * @return Signed compact JWT string
     */
    public String generateToken(UUID userId, String username, String role) {
        Instant now = Instant.now();
        Instant expiry = now.plusMillis(expirationMs);

        // Security Decision: Generate a unique JTI (JWT ID) for every issued token
        // Why: The JTI allows selective token revocation in Redis (logout blacklist) without revoking all user sessions
        String jti = UUID.randomUUID().toString();

        return Jwts.builder()
                .subject(userId.toString())
                .id(jti)
                .claim("username", username)
                .claim("role", role)
                .issuedAt(Date.from(now))
                .expiration(Date.from(expiry))
                .signWith(signingKey)
                .compact();
    }

    /**
     * Validates the cryptographic signature and expiration of a JWT string.
     *
     * @param token Compact JWT string
     * @return True if signature is valid and token is not expired
     */
    public boolean validateToken(String token) {
        try {
            // Parser enforces HMAC signature verification and checks exp claim automatically
            Jwts.parser()
                    .verifyWith(signingKey)
                    .build()
                    .parseSignedClaims(token);
            return true;
        } catch (JwtException | IllegalArgumentException ex) {
            // Why: Catches signature tampering, expired tokens, or malformed claims without throwing out of the filter
            return false;
        }
    }

    /**
     * Extracts all claims from a validated token.
     *
     * @param token Compact JWT string
     * @return Claims payload
     */
    public Claims extractAllClaims(String token) {
        return Jwts.parser()
                .verifyWith(signingKey)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    /**
     * Extracts the user UUID from the token subject claim.
     *
     * @param token Compact JWT string
     * @return User UUID
     */
    public UUID extractUserId(String token) {
        return UUID.fromString(extractAllClaims(token).getSubject());
    }

    /**
     * Extracts the username from the token claims.
     *
     * @param token Compact JWT string
     * @return Username string
     */
    public String extractUsername(String token) {
        return extractAllClaims(token).get("username", String.class);
    }

    /**
     * Extracts the role claim from the token.
     *
     * @param token Compact JWT string
     * @return Role string
     */
    public String extractRole(String token) {
        return extractAllClaims(token).get("role", String.class);
    }

    /**
     * Extracts the unique token identifier (JTI).
     *
     * @param token Compact JWT string
     * @return Unique JTI string
     */
    public String extractJti(String token) {
        return extractAllClaims(token).getId();
    }

    /**
     * Extracts the token expiration instant.
     *
     * @param token Compact JWT string
     * @return Expiration Instant
     */
    public Instant extractExpiration(String token) {
        return extractAllClaims(token).getExpiration().toInstant();
    }

    public long getExpirationMs() {
        return expirationMs;
    }
}
