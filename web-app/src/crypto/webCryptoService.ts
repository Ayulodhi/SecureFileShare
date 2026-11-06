/**
 * Web Crypto Service for SecureFileShare
 * WHAT: Implements client-side zero-knowledge cryptographic primitives using the native browser Web Crypto API.
 * WHY: Strict compliance with architecture standards:
 *      - RSA-4096 (RSA-OAEP with SHA-256) for session key wrapping
 *      - ECDSA P-256 (SHA-256) for file payload integrity signing
 *      - AES-256-GCM (12-byte IV, 128-bit tag) for payload encryption
 *      - PBKDF2 (100,000 iterations) for passphrase-based private key backup protection
 */

import { StoredUserKeys, saveUserKeys } from './keyVault';

// Utilities for array buffer to Base64 / Hex
export function arrayBufferToBase64(buffer: ArrayBuffer): string {
  const bytes = new Uint8Array(buffer);
  let binary = '';
  for (let i = 0; i < bytes.byteLength; i++) {
    binary += String.fromCharCode(bytes[i]);
  }
  return globalThis.btoa(binary);
}

export function base64ToArrayBuffer(base64: string): ArrayBuffer {
  // Clean whitespace/newlines
  const cleanBase64 = base64.replace(/[\r\n\s]/g, '');
  const binaryString = globalThis.atob(cleanBase64);
  const len = binaryString.length;
  const bytes = new Uint8Array(len);
  for (let i = 0; i < len; i++) {
    bytes[i] = binaryString.charCodeAt(i);
  }
  return bytes.buffer;
}

export function arrayBufferToHex(buffer: ArrayBuffer): string {
  const bytes = new Uint8Array(buffer);
  return Array.from(bytes)
    .map((b) => b.toString(16).padStart(2, '0'))
    .join('');
}

export function spkiToPem(spkiBuffer: ArrayBuffer): string {
  const base64 = arrayBufferToBase64(spkiBuffer);
  // Break into 64-char lines
  const lines = base64.match(/.{1,64}/g) || [];
  return `-----BEGIN PUBLIC KEY-----\n${lines.join('\n')}\n-----END PUBLIC KEY-----`;
}

export function pemToSpkiBuffer(pem: string): ArrayBuffer {
  const cleanBase64 = pem
    .replace(/-----BEGIN [A-Z ]+-----/g, '')
    .replace(/-----END [A-Z ]+-----/g, '')
    .replace(/[\r\n\s]/g, '');
  return base64ToArrayBuffer(cleanBase64);
}

/**
 * Generates an RSA-4096 key pair and an ECDSA P-256 key pair,
 * computes the SHA-256 public key fingerprint, and persists the bundle in IndexedDB.
 */
export async function generateUserKeyPairBundle(username: string): Promise<StoredUserKeys> {
  // Step 1: Generate RSA-4096 Key Pair for Key Wrapping
  const rsaKeyPair = await globalThis.crypto.subtle.generateKey(
    {
      name: 'RSA-OAEP',
      modulusLength: 4096,
      publicExponent: new Uint8Array([1, 0, 1]), // 65537
      hash: 'SHA-256',
    },
    true, // extractable for backup export
    ['wrapKey', 'unwrapKey', 'encrypt', 'decrypt']
  );

  // Step 2: Generate ECDSA P-256 Key Pair for Integrity Signing
  const ecKeyPair = await globalThis.crypto.subtle.generateKey(
    {
      name: 'ECDSA',
      namedCurve: 'P-256',
    },
    true,
    ['sign', 'verify']
  );

  // Step 3: Export Public Keys to PEM format
  const rsaSpki = await globalThis.crypto.subtle.exportKey('spki', rsaKeyPair.publicKey);
  const ecSpki = await globalThis.crypto.subtle.exportKey('spki', ecKeyPair.publicKey);

  const rsaPublicKeyPem = spkiToPem(rsaSpki);
  const ecPublicKeyPem = spkiToPem(ecSpki);

  // Step 4: Compute SHA-256 Fingerprint over RSA SPKI DER
  const fingerprintBuffer = await globalThis.crypto.subtle.digest('SHA-256', rsaSpki);
  const fingerprint = arrayBufferToHex(fingerprintBuffer);

  const keyBundle: StoredUserKeys = {
    username,
    rsaPrivateKey: rsaKeyPair.privateKey,
    ecPrivateKey: ecKeyPair.privateKey,
    rsaPublicKeyPem,
    ecPublicKeyPem,
    fingerprint,
    createdAt: new Date().toISOString(),
  };

  await saveUserKeys(keyBundle);
  return keyBundle;
}

/**
 * Encrypts a plaintext File using AES-256-GCM, signs the ciphertext with ECDSA,
 * and wraps the AES Data Encryption Key (DEK) with the recipient's RSA public key.
 */
export async function encryptFilePayload(
  file: File,
  recipientRsaPublicKeyPem: string,
  signerEcPrivateKey: CryptoKey
): Promise<{
  ciphertextBlob: Blob;
  ivBase64: string;
  ecdsaSignatureBase64: string;
  ciphertextHashHex: string;
  ownerWrappedKeyBase64: string;
}> {
  // Step 1: Read plaintext file bytes
  const fileBytes = await file.arrayBuffer();

  // Step 2: Generate random 256-bit AES DEK
  const dek = await globalThis.crypto.subtle.generateKey(
    {
      name: 'AES-GCM',
      length: 256,
    },
    true, // extractable for wrapping
    ['encrypt', 'decrypt']
  );

  // Step 3: Generate 96-bit (12-byte) initialization vector (IV)
  const iv = globalThis.crypto.getRandomValues(new Uint8Array(12));

  // Step 4: Encrypt plaintext bytes using AES-256-GCM (128-bit tag appended automatically)
  const ciphertextBuffer = await globalThis.crypto.subtle.encrypt(
    {
      name: 'AES-GCM',
      iv,
      tagLength: 128,
    },
    dek,
    fileBytes
  );

  // Step 5: Compute SHA-256 hash of the ciphertext
  const ciphertextHashBuffer = await globalThis.crypto.subtle.digest('SHA-256', ciphertextBuffer);
  const ciphertextHashHex = arrayBufferToHex(ciphertextHashBuffer);

  // Step 6: Sign ciphertext SHA-256 digest with ECDSA P-256 private key
  const signatureBuffer = await globalThis.crypto.subtle.sign(
    {
      name: 'ECDSA',
      hash: { name: 'SHA-256' },
    },
    signerEcPrivateKey,
    ciphertextHashBuffer
  );

  // Step 7: Import recipient's RSA-4096 public key from PEM
  const recipientSpkiBuffer = pemToSpkiBuffer(recipientRsaPublicKeyPem);
  const recipientRsaPublicKey = await globalThis.crypto.subtle.importKey(
    'spki',
    recipientSpkiBuffer,
    {
      name: 'RSA-OAEP',
      hash: 'SHA-256',
    },
    false,
    ['wrapKey', 'encrypt']
  );

  // Step 8: Wrap the AES DEK using recipient's RSA-OAEP public key
  const wrappedKeyBuffer = await globalThis.crypto.subtle.wrapKey(
    'raw',
    dek,
    recipientRsaPublicKey,
    {
      name: 'RSA-OAEP',
    }
  );

  return {
    ciphertextBlob: new Blob([ciphertextBuffer], { type: 'application/octet-stream' }),
    ivBase64: arrayBufferToBase64(iv.buffer),
    ecdsaSignatureBase64: arrayBufferToBase64(signatureBuffer),
    ciphertextHashHex,
    ownerWrappedKeyBase64: arrayBufferToBase64(wrappedKeyBuffer),
  };
}

/**
 * Decrypts an encrypted file payload: unwraps the AES DEK with user's private RSA key,
 * then decrypts the ciphertext using AES-256-GCM.
 */
export async function decryptFilePayload(
  ciphertextBuffer: ArrayBuffer,
  ivBase64: string,
  wrappedDekBase64: string,
  rsaPrivateKey: CryptoKey
): Promise<ArrayBuffer> {
  // Step 1: Decode parameters
  const ivBuffer = base64ToArrayBuffer(ivBase64);
  const wrappedDekBuffer = base64ToArrayBuffer(wrappedDekBase64);

  // Step 2: Unwrap AES DEK using RSA-OAEP private key
  const dek = await globalThis.crypto.subtle.unwrapKey(
    'raw',
    wrappedDekBuffer,
    rsaPrivateKey,
    {
      name: 'RSA-OAEP',
    },
    {
      name: 'AES-GCM',
      length: 256,
    },
    false,
    ['decrypt']
  );

  // Step 3: Decrypt ciphertext using unwrapped AES DEK
  const plaintextBuffer = await globalThis.crypto.subtle.decrypt(
    {
      name: 'AES-GCM',
      iv: new Uint8Array(ivBuffer),
      tagLength: 128,
    },
    dek,
    ciphertextBuffer
  );

  return plaintextBuffer;
}

/**
 * Exports user keys to a passphrase-protected encrypted JSON string.
 */
export async function exportEncryptedKeyBackup(
  keys: StoredUserKeys,
  passphrase: string
): Promise<string> {
  // Export private keys to PKCS#8 DER
  const rsaPkcs8 = await globalThis.crypto.subtle.exportKey('pkcs8', keys.rsaPrivateKey);
  const ecPkcs8 = await globalThis.crypto.subtle.exportKey('pkcs8', keys.ecPrivateKey);

  const payload = JSON.stringify({
    username: keys.username,
    rsaPkcs8Base64: arrayBufferToBase64(rsaPkcs8),
    ecPkcs8Base64: arrayBufferToBase64(ecPkcs8),
    rsaPublicKeyPem: keys.rsaPublicKeyPem,
    ecPublicKeyPem: keys.ecPublicKeyPem,
    fingerprint: keys.fingerprint,
    createdAt: keys.createdAt,
  });

  // Derive encryption key from passphrase using PBKDF2
  const enc = new TextEncoder();
  const salt = globalThis.crypto.getRandomValues(new Uint8Array(16));
  const baseKey = await globalThis.crypto.subtle.importKey(
    'raw',
    enc.encode(passphrase),
    { name: 'PBKDF2' },
    false,
    ['deriveKey']
  );

  const backupKey = await globalThis.crypto.subtle.deriveKey(
    {
      name: 'PBKDF2',
      salt,
      iterations: 100000,
      hash: 'SHA-256',
    },
    baseKey,
    { name: 'AES-GCM', length: 256 },
    false,
    ['encrypt']
  );

  const iv = globalThis.crypto.getRandomValues(new Uint8Array(12));
  const encryptedPayload = await globalThis.crypto.subtle.encrypt(
    { name: 'AES-GCM', iv },
    backupKey,
    enc.encode(payload)
  );

  return JSON.stringify({
    version: 1,
    saltBase64: arrayBufferToBase64(salt.buffer),
    ivBase64: arrayBufferToBase64(iv.buffer),
    cipherBase64: arrayBufferToBase64(encryptedPayload),
  });
}

/**
 * Imports and decrypts a passphrase-protected key backup and saves it to IndexedDB.
 */
export async function importEncryptedKeyBackup(
  backupJson: string,
  passphrase: string
): Promise<StoredUserKeys> {
  const parsed = JSON.parse(backupJson);
  const salt = base64ToArrayBuffer(parsed.saltBase64);
  const iv = base64ToArrayBuffer(parsed.ivBase64);
  const cipher = base64ToArrayBuffer(parsed.cipherBase64);

  const enc = new TextEncoder();
  const baseKey = await globalThis.crypto.subtle.importKey(
    'raw',
    enc.encode(passphrase),
    { name: 'PBKDF2' },
    false,
    ['deriveKey']
  );

  const backupKey = await globalThis.crypto.subtle.deriveKey(
    {
      name: 'PBKDF2',
      salt: new Uint8Array(salt),
      iterations: 100000,
      hash: 'SHA-256',
    },
    baseKey,
    { name: 'AES-GCM', length: 256 },
    false,
    ['decrypt']
  );

  const decryptedBytes = await globalThis.crypto.subtle.decrypt(
    { name: 'AES-GCM', iv: new Uint8Array(iv) },
    backupKey,
    cipher
  );

  const dec = new TextDecoder();
  const payload = JSON.parse(dec.decode(decryptedBytes));

  // Re-import CryptoKey instances
  const rsaPrivateKey = await globalThis.crypto.subtle.importKey(
    'pkcs8',
    base64ToArrayBuffer(payload.rsaPkcs8Base64),
    { name: 'RSA-OAEP', hash: 'SHA-256' },
    true,
    ['unwrapKey', 'decrypt']
  );

  const ecPrivateKey = await globalThis.crypto.subtle.importKey(
    'pkcs8',
    base64ToArrayBuffer(payload.ecPkcs8Base64),
    { name: 'ECDSA', namedCurve: 'P-256' },
    true,
    ['sign']
  );

  const keys: StoredUserKeys = {
    username: payload.username,
    rsaPrivateKey,
    ecPrivateKey,
    rsaPublicKeyPem: payload.rsaPublicKeyPem,
    ecPublicKeyPem: payload.ecPublicKeyPem,
    fingerprint: payload.fingerprint,
    createdAt: payload.createdAt,
  };

  await saveUserKeys(keys);
  return keys;
}
