package com.securefileshare.auth.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.securefileshare.common.dto.ApiError;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

/**
 * Spring Security configuration for auth-service.
 * WHAT: Configures BCrypt password encoding, stateless JWT session policy, public/private route permissions,
 *      and installs the JwtAuthenticationFilter.
 * WHY: Enforces defense-in-depth across authentication endpoints and guarantees unauthenticated requests are rejected.
 */
@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthenticationFilter;
    private final ObjectMapper objectMapper;

    public SecurityConfig(JwtAuthenticationFilter jwtAuthenticationFilter, ObjectMapper objectMapper) {
        this.jwtAuthenticationFilter = jwtAuthenticationFilter;
        this.objectMapper = objectMapper;
    }

    /**
     * Password encoder bean using BCrypt with work factor (strength) 12.
     * WHAT: Generates salted, iterative hashes for user passwords.
     * WHY: Work factor 12 provides high cryptographic resistance against GPU-accelerated offline cracking.
     *
     * @return BCryptPasswordEncoder
     */
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder(12);
    }

    /**
     * Configures the HTTP security filter chain and authorization rules.
     *
     * @param http HttpSecurity configuration builder
     * @return Built SecurityFilterChain
     */
    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                // Disable CSRF because authentication is stateless and uses Bearer tokens stored in client headers (no cookies)
                // Replaced method reference with lambda to resolve IDE null type safety warning without changing behavior
                .csrf(csrf -> csrf.disable())

                // Enforce stateless session policy: no HTTP sessions created or persisted
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))

                // Configure endpoint authorization rules
                .authorizeHttpRequests(auth -> auth
                        // Health checks and actuator metrics are public
                        .requestMatchers("/health", "/api/v1/health", "/actuator/**").permitAll()
                        // Public authentication endpoints: registration and login
                        .requestMatchers(HttpMethod.POST, "/auth/register", "/api/v1/auth/register").permitAll()
                        .requestMatchers(HttpMethod.POST, "/auth/login", "/api/v1/auth/login").permitAll()
                        // Public key lookup is public so any participant can fetch recipient public keys and fingerprints out-of-band
                        .requestMatchers(HttpMethod.GET, "/auth/keys/*", "/api/v1/auth/keys/*").permitAll()
                        // All other endpoints require an authenticated JWT Bearer token
                        .anyRequest().authenticated()
                )

                // Custom JSON error response for unauthenticated access attempts
                .exceptionHandling(ex -> ex.authenticationEntryPoint((request, response, authException) -> {
                    response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                    response.setContentType(MediaType.APPLICATION_JSON_VALUE);

                    ApiError error = new ApiError(
                            HttpStatus.UNAUTHORIZED.value(),
                            HttpStatus.UNAUTHORIZED.getReasonPhrase(),
                            "Full authentication is required to access this resource",
                            request.getRequestURI()
                    );

                    response.getWriter().write(objectMapper.writeValueAsString(error));
                }))

                // Install JWT authentication filter before the standard username/password filter
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }
}
