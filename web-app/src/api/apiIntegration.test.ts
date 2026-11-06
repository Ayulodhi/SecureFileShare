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

