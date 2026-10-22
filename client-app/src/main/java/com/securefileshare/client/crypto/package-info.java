/**
 * Client-side cryptographic engine and local encrypted PKCS12 keystore.
 * WHAT: Implements RSA-4096 OAEP wrapping, ECDSA P-256 signing, and AES-256-GCM encryption.
 * WHY: Core Zero-Knowledge Rule: Cryptography executes on client; empty for Phase C0 shell setup.
 */
package com.securefileshare.client.crypto;
