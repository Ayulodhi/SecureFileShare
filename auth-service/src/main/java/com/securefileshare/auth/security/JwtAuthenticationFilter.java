package com.securefileshare.auth.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.securefileshare.common.constant.SecurityConstants;
import com.securefileshare.common.dto.ApiError;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.lang.NonNull;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Collections;
import java.util.UUID;

/**
 * Spring Security filter executed once per incoming HTTP request.
 * WHAT: Intercepts HTTP requests, parses the Bearer JWT, validates signature, checks Redis blacklist,
 *      and populates the SecurityContextHolder.
 * WHY: Enforces authentication across protected endpoints and honors instant token revocations (logout).
 */
@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtService jwtService;
    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper;

    public JwtAuthenticationFilter(JwtService jwtService, StringRedisTemplate redisTemplate, ObjectMapper objectMapper) {
        this.jwtService = jwtService;
        this.redisTemplate = redisTemplate;
        this.objectMapper = objectMapper;
    }

    @Override
    // Added Spring's @NonNull annotations to parameter types to conform with OncePerRequestFilter method signature
    protected void doFilterInternal(
            @NonNull HttpServletRequest request,
            @NonNull HttpServletResponse response,
            @NonNull FilterChain filterChain)
            throws ServletException, IOException {

        String authHeader = request.getHeader(SecurityConstants.AUTH_HEADER);

        // If no Authorization header or not Bearer prefix, proceed down the chain (anonymous request)
        if (authHeader == null || !authHeader.startsWith(SecurityConstants.BEARER_PREFIX)) {
            filterChain.doFilter(request, response);
            return;
        }

        // Extract token payload string
        String token = authHeader.substring(SecurityConstants.BEARER_PREFIX.length()).trim();

        // Step 1: Validate cryptographic signature and expiration timestamp
        if (!jwtService.validateToken(token)) {
            sendUnauthorizedError(response, request.getRequestURI(), "Invalid or expired JWT token");
            return;
        }

        // Step 2: Extract unique JTI and verify against Redis revocation blacklist
        // Why: Enables immediate logout without waiting for the JWT's natural expiration
        String jti = jwtService.extractJti(token);
        String blacklistKey = SecurityConstants.REDIS_JWT_BLACKLIST_PREFIX + jti;
        Boolean isBlacklisted = redisTemplate.hasKey(blacklistKey);

        if (Boolean.TRUE.equals(isBlacklisted)) {
            sendUnauthorizedError(response, request.getRequestURI(), "Token has been revoked. Please log in again.");
            return;
        }

        // Step 3: Populate Spring SecurityContext with authenticated user details
        UUID userId = jwtService.extractUserId(token);
        // Removed unused 'username' extraction to eliminate IDE warning; userId is the principal required by the security context
        String role = jwtService.extractRole(token);

        // Wrap role in standard Spring Security GrantedAuthority
        SimpleGrantedAuthority authority = new SimpleGrantedAuthority(
                role.startsWith("ROLE_") ? role : "ROLE_" + role
        );

        UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(
                userId, // Principal is the User UUID
                token,  // Credentials stored as token for subsequent logout extraction
                Collections.singletonList(authority)
        );

        SecurityContextHolder.getContext().setAuthentication(authentication);

        filterChain.doFilter(request, response);
    }

    /**
     * Writes a standardized JSON ApiError response directly to the HTTP response stream.
     *
     * @param response HttpServletResponse instance
     * @param uri      Request URI
     * @param message  Sanitized error explanation
     */
    private void sendUnauthorizedError(HttpServletResponse response, String uri, String message) throws IOException {
        response.setStatus(HttpStatus.UNAUTHORIZED.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);

        ApiError error = new ApiError(
                HttpStatus.UNAUTHORIZED.value(),
                HttpStatus.UNAUTHORIZED.getReasonPhrase(),
                message,
                uri
        );

        response.getWriter().write(objectMapper.writeValueAsString(error));
    }
}
