package com.securefileshare.auth.service;

import com.securefileshare.common.constant.SecurityConstants;
import com.securefileshare.common.exception.ApiException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.Objects;

/**
 * Service managing Redis-backed rate limiting for authentication endpoints.
 * WHAT: Tracks failed login attempts per username and IP address in Redis with a rolling expiry window.
 * WHY: CRITICAL DEFENSE: Thwarts brute-force dictionary attacks, password spraying, and credential stuffing
 *      by locking out repeated failed attempts after exceeding configured thresholds.
 */
@Service
public class RateLimiterService {

    private final StringRedisTemplate redisTemplate;
    private final int maxAttempts;
    private final int windowMinutes;

    public RateLimiterService(
            StringRedisTemplate redisTemplate,
            @Value("${security.rate-limit.max-attempts:5}") int maxAttempts,
            @Value("${security.rate-limit.window-minutes:15}") int windowMinutes) {
        this.redisTemplate = redisTemplate;
        this.maxAttempts = maxAttempts;
        this.windowMinutes = windowMinutes;
    }

    /**
     * Checks if the username or client IP address has exceeded the failed attempt threshold.
     *
     * @param username Attempted username
     * @param clientIp Source IP address of the client request
     * @throws ApiException with 429 TOO_MANY_REQUESTS if threshold is breached
     */
    public void checkRateLimit(String username, String clientIp) {
        String userKey = SecurityConstants.REDIS_RATE_LIMIT_PREFIX + "user:" + username.toLowerCase().trim();
        String ipKey = SecurityConstants.REDIS_RATE_LIMIT_PREFIX + "ip:" + clientIp.trim();

        // Check user-targeted rate limit bucket
        // Why: Protects targeted user accounts from brute-force password guessing
        String userAttempts = redisTemplate.opsForValue().get(userKey);
        if (userAttempts != null && Integer.parseInt(userAttempts) >= maxAttempts) {
            throw new ApiException(HttpStatus.TOO_MANY_REQUESTS,
                    "Account temporarily locked due to excessive failed login attempts. Try again in " + windowMinutes + " minutes.");
        }

        // Check IP-targeted rate limit bucket
        // Why: Protects against credential spraying where an attacker tries one password across thousands of usernames
        String ipAttempts = redisTemplate.opsForValue().get(ipKey);
        if (ipAttempts != null && Integer.parseInt(ipAttempts) >= (maxAttempts * 3)) {
            throw new ApiException(HttpStatus.TOO_MANY_REQUESTS,
                    "Too many login attempts from your IP address. Try again in " + windowMinutes + " minutes.");
        }
    }

    /**
     * Increments the failure count for both username and client IP upon an invalid login attempt.
     *
     * @param username Attempted username
     * @param clientIp Source IP address
     */
    public void recordFailedAttempt(String username, String clientIp) {
        String userKey = SecurityConstants.REDIS_RATE_LIMIT_PREFIX + "user:" + username.toLowerCase().trim();
        String ipKey = SecurityConstants.REDIS_RATE_LIMIT_PREFIX + "ip:" + clientIp.trim();

        // Explicitly assert non-null Duration to satisfy Spring Data Redis @NonNull parameter contract
        Duration window = Objects.requireNonNull(Duration.ofMinutes(windowMinutes));

        // Increment username failure counter and set TTL if new key
        Long count = redisTemplate.opsForValue().increment(userKey);
        if (count != null && count == 1) {
            redisTemplate.expire(userKey, window);
        }

        // Increment IP failure counter and set TTL if new key
        Long ipCount = redisTemplate.opsForValue().increment(ipKey);
        if (ipCount != null && ipCount == 1) {
            redisTemplate.expire(ipKey, window);
        }
    }

    /**
     * Resets the failed attempt counter upon a successful login.
     *
     * @param username Successfully authenticated username
     * @param clientIp Source IP address
     */
    public void resetAttempts(String username, String clientIp) {
        String userKey = SecurityConstants.REDIS_RATE_LIMIT_PREFIX + "user:" + username.toLowerCase().trim();
        // Delete user counter so subsequent logins are not blocked
        redisTemplate.delete(userKey);
    }
}
