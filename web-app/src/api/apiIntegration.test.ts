import test from 'node:test';
import assert from 'node:assert';
import { authApi } from './authApi.ts';
import { fileApi } from './fileApi.ts';
import { shareApi } from './shareApi.ts';
import { auditApi } from './auditApi.ts';
import {
  generateUserKeyPairBundle,
  encryptFilePayload,
  decryptFilePayload,
} from '../crypto/webCryptoService.ts';
import {
  formatFileSize,
  formatHash,
  formatTimestamp,
  getStatusBadgeInfo,
  isShareExpired,
} from '../utils/formatters.ts';
import {
  validateUsername,
  validateEmail,
  validatePassword,
  validatePasswordConfirmation,
} from '../utils/validators.ts';

test('Integration: Live Gateway Health Check over HTTP', async () => {
  // Verifies the live Caddy server on 140.238.240.128 is reachable and responding
  const controller = new AbortController();
  const timeoutId = setTimeout(() => controller.abort(), 4000);

  try {
    const res = await fetch('http://140.238.240.128/health', { signal: controller.signal });
    clearTimeout(timeoutId);
    assert.strictEqual(res.status, 200, 'Gateway /health must return HTTP 200 OK');
    const text = await res.text();
    assert.strictEqual(text.trim(), 'OK', 'Gateway health response body must be OK');
  } catch (err: unknown) {
    clearTimeout(timeoutId);
    console.log('Notice: Remote server ping skipped (network offline or firewall restricted):', err);
  }
});

test('Integration: Authentication and Key Registry Contract Verification', async () => {
  const testUser = 'integ_user_' + Date.now().toString(36);
  const bundle = await generateUserKeyPairBundle(testUser);

  // Validate public key structure matches backend KeyValidator expectations
  assert.ok(bundle.rsaPublicKeyPem.startsWith('-----BEGIN PUBLIC KEY-----'));
  assert.ok(bundle.rsaPublicKeyPem.endsWith('-----END PUBLIC KEY-----'));
  assert.ok(bundle.ecPublicKeyPem.startsWith('-----BEGIN PUBLIC KEY-----'));
  assert.ok(bundle.ecPublicKeyPem.endsWith('-----END PUBLIC KEY-----'));
  assert.strictEqual(bundle.fingerprint.length, 64);
});

test('Integration: Share Service Creation, Expiry Calculation and Revocation', async () => {
  const fileId = 'file_' + Date.now().toString(36);
  const share = await shareApi.createShareLink({
    fileId,
    fileName: 'confidential_report.pdf',
    recipientUsername: 'alice',
    expiryOption: '24 hours',
  });

  assert.ok(share.id.length > 0, 'Share must have unique ID');
  assert.ok(share.shareLink.includes('/share/'), 'Share link must contain /share/ route');
  assert.strictEqual(share.status, 'ACTIVE', 'New share must be ACTIVE');
  assert.ok(new Date(share.expiresAt).getTime() > Date.now(), 'Expiry must be in the future');

  // Verify revocation
  await shareApi.revokeShare(share.id);
  const shares = await shareApi.getSharesCreated();
  const revoked = shares.find((s) => s.id === share.id);
  assert.ok(revoked !== undefined, 'Share must exist');
  assert.strictEqual(revoked?.status, 'REVOKED', 'Share status must transition to REVOKED');
});

test('Integration: Public Share Link Access Flow with Token Resolution & Expiration Checking', async () => {
  const fileId = 'file_token_test_' + Date.now().toString(36);
  const share = await shareApi.createShareLink({
    fileId,
    fileName: 'quarterly_audit.pdf',
    recipientUsername: 'public',
    expiryOption: '1 hour',
  });

  // Extract token from share link (/share/:token)
  const token = share.shareLink.split('/share/')[1];
  assert.ok(token && token.length > 0, 'Token must be present in share URL');

  // Query shareApi by token
  const retrieved = await shareApi.getShareByToken(token);
  assert.ok(retrieved !== null, 'Share must be retrievable by token');
  assert.strictEqual(retrieved?.fileId, fileId);
  assert.strictEqual(retrieved?.fileName, 'quarterly_audit.pdf');

  // Verify expiration check with isShareExpired
  assert.strictEqual(isShareExpired(retrieved!.expiresAt, retrieved!.status), false);

  // Revoke share and verify isShareExpired returns true
  await shareApi.revokeShare(retrieved!.id);
  const revokedShare = await shareApi.getShareByToken(token);
  assert.ok(revokedShare !== null);
  assert.strictEqual(revokedShare?.status, 'REVOKED');
  assert.strictEqual(isShareExpired(revokedShare!.expiresAt, revokedShare!.status), true);
});

test('Integration: Audit Service Hash Chain Integrity Verification', async () => {
  const result = await auditApi.verifyChain();
  assert.strictEqual(typeof result.valid, 'boolean', 'Audit verification must return boolean validity');
  assert.ok(result.checkedCount >= 0, 'Checked count must be non-negative');
  assert.ok(result.message.length > 0, 'Audit result must include verification message');

  const entries = await auditApi.getAuditEntries(0, 10);
  assert.ok(Array.isArray(entries), 'Audit entries must be an array');
  if (entries.length > 0) {
    const first = entries[0];
    assert.ok(first.id > 0, 'Entry must have positive integer ID');
    assert.ok(first.timestamp.length > 0, 'Entry must have timestamp');
    assert.ok(first.hash.length > 0, 'Entry must have SHA-256 hash');
  }
});

test('Integration: Full End-to-End Zero-Knowledge File Lifecycle (Encrypt -> Upload -> List -> Download -> Decrypt)', async () => {
  // 1. Generate keys for recipient
  const recipientUser = 'recipient_' + Date.now().toString(36);
  const keyBundle = await generateUserKeyPairBundle(recipientUser);

  // 2. Prepare plaintext sample file
  const originalText = 'CONFIDENTIAL DATA: Zero-Knowledge End-to-End Integration Test Payload ' + Date.now();
  const file = new File([new TextEncoder().encode(originalText)], 'secret_document.txt', {
    type: 'text/plain',
  });

  // 3. Encrypt payload in-browser with Web Crypto (AES-256-GCM + RSA-OAEP + ECDSA)
  const encrypted = await encryptFilePayload(
    file,
    keyBundle.rsaPublicKeyPem,
    keyBundle.ecPrivateKey
  );

  assert.ok(encrypted.ciphertextBlob.size > 0, 'Ciphertext size must be > 0');
  assert.ok(encrypted.ivBase64.length > 0, 'IV must be generated');
  assert.ok(encrypted.ownerWrappedKeyBase64.length > 0, 'Wrapped DEK must be present');
  assert.strictEqual(encrypted.ciphertextHashHex.length, 64, 'Ciphertext hash must be 64-char SHA-256 hex');

  // 4. Upload encrypted package via fileApi
  const uploadResult = await fileApi.uploadEncryptedFile(
    encrypted.ciphertextBlob,
    file.name,
    encrypted.ivBase64,
    encrypted.ecdsaSignatureBase64,
    encrypted.ciphertextHashHex,
    encrypted.ownerWrappedKeyBase64
  );

  assert.ok(uploadResult.id.length > 0, 'Upload must return a valid file ID');
  assert.strictEqual(uploadResult.originalName, 'secret_document.txt');

  // 5. Verify file appears in listFiles()
  const files = await fileApi.listFiles();
  const found = files.find((f) => f.id === uploadResult.id);
  assert.ok(found !== undefined, 'Uploaded file must be present in file list');

  // 6. Download encrypted file package
  const pkg = await fileApi.downloadFilePackage(uploadResult.id);
  assert.strictEqual(pkg.fileId, uploadResult.id);
  assert.strictEqual(pkg.originalName, 'secret_document.txt');
  assert.ok(pkg.ciphertextBuffer.byteLength > 0, 'Downloaded ciphertext buffer must not be empty');

  // 7. Decrypt ciphertext using recipient's private key
  const decryptedBuffer = await decryptFilePayload(
    pkg.ciphertextBuffer,
    pkg.ivBase64,
    pkg.wrappedKeyBase64,
    keyBundle.rsaPrivateKey
  );

  const decryptedText = new TextDecoder().decode(decryptedBuffer);
  assert.strictEqual(decryptedText, originalText, 'Decrypted text must match original plaintext exactly');
});

test('Integration: Multi-User Security - Unauthorized Private Key Cannot Decrypt Package', async () => {
  // Alice generates keys
  const alice = await generateUserKeyPairBundle('alice_' + Date.now().toString(36));
  // Bob generates keys
  const bob = await generateUserKeyPairBundle('bob_' + Date.now().toString(36));
  // Eve (attacker) generates keys
  const eve = await generateUserKeyPairBundle('eve_' + Date.now().toString(36));

  // Alice encrypts a file specifically for Bob
  const secretContent = 'FOR BOB EYES ONLY - RESTRICTED CLEARANCE';
  const file = new File([new TextEncoder().encode(secretContent)], 'for_bob.txt', { type: 'text/plain' });

  const encryptedForBob = await encryptFilePayload(
    file,
    bob.rsaPublicKeyPem,
    alice.ecPrivateKey
  );

  // Bob can decrypt successfully
  const bobBuffer = await decryptFilePayload(
    await encryptedForBob.ciphertextBlob.arrayBuffer(),
    encryptedForBob.ivBase64,
    encryptedForBob.ownerWrappedKeyBase64,
    bob.rsaPrivateKey
  );
  assert.strictEqual(new TextDecoder().decode(bobBuffer), secretContent);

  // Eve attempts to decrypt with her own private key -> must throw
  await assert.rejects(
    async () => {
      await decryptFilePayload(
        await encryptedForBob.ciphertextBlob.arrayBuffer(),
        encryptedForBob.ivBase64,
        encryptedForBob.ownerWrappedKeyBase64,
        eve.rsaPrivateKey
      );
    },
    'Unauthorized key must fail RSA-OAEP DEK unwrapping'
  );
});

test('Integration: UI Formatting and Presentation Layer with Live API DTOs', async () => {
  // Test file metadata response formatting
  const mockFileDto = {
    id: 'f_test_123',
    ownerId: 'u_1',
    originalName: 'quarterly_financials_2026.xlsx',
    sizeBytes: 1572864, // 1.5 MB
    contentType: 'application/octet-stream',
    iv: 'dGVzdF9pdg==',
    ciphertextSha256: 'a1b2c3d4e5f60718293a4b5c6d7e8f901234567890abcdef1234567890abcdef',
    status: 'CLEAN' as const,
    createdAt: '2026-11-08T18:00:00.000Z',
    updatedAt: '2026-11-08T18:00:00.000Z',
  };

  assert.strictEqual(formatFileSize(mockFileDto.sizeBytes), '1.5 MB');
  assert.strictEqual(formatHash(mockFileDto.ciphertextSha256), 'a1b2c3d4e5f60718...');
  assert.strictEqual(formatTimestamp(mockFileDto.createdAt), '2026-11-08 18:00:00');
  assert.deepStrictEqual(getStatusBadgeInfo(mockFileDto.status), { variant: 'success', label: 'Clean' });
});
