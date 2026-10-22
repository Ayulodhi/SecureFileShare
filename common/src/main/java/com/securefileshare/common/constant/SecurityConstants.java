package com.securefileshare.common.constant;

/**
 * Centrally defined security roles, HTTP headers, and cryptographic constants.
 * WHAT: Provides shared string and numeric constants used across microservices.
 * WHY: Eliminates hardcoded magic strings and ensures consistent role naming and token prefixes.
 */
public final class SecurityConstants {

    // Private constructor prevents instantiation of pure utility constant class
    private SecurityConstants() {}

    /**
     * Standard role for regular platform participants who send and receive encrypted packages.
     */
    public static final String ROLE_SENDER_RECIPIENT = "ROLE_SENDER_RECIPIENT";

    /**
     * Administrative role with elevated management privileges (cannot self-register).
     */
    public static final String ROLE_ADMIN = "ROLE_ADMIN";

    /**
     * Standard HTTP Authorization header key.
     */
    public static final String AUTH_HEADER = "Authorization";

    /**
     * Bearer token prefix convention in HTTP Authorization headers.
     */
    public static final String BEARER_PREFIX = "Bearer ";

    /**
     * Redis key prefix used for token revocation (blacklist) tracking.
     */
    public static final String REDIS_JWT_BLACKLIST_PREFIX = "jwt:blacklist:";

    /**
     * Redis key prefix used for login attempt rate-limiting buckets.
     */
    public static final String REDIS_RATE_LIMIT_PREFIX = "ratelimit:login:";

    /**
     * Required RSA key modulus bit length for key wrapping.
     * Why: 4096-bit RSA ensures robust long-term protection against factorization attacks.
     */
    public static final int REQUIRED_RSA_KEY_SIZE_BITS = 4096;

    /**
     * Standard elliptic curve name required for digital signatures.
     * Why: NIST P-256 (secp256r1) ensures standard ECDSA signature interoperability and resistance to weak-curve attacks.
     */
    public static final String REQUIRED_EC_CURVE_NAME = "secp256r1";
}
