import test from 'node:test';
import assert from 'node:assert';
import {
  arrayBufferToBase64,
  base64ToArrayBuffer,
  arrayBufferToHex,
  spkiToPem,
  pemToSpkiBuffer,
  generateUserKeyPairBundle,
  encryptFilePayload,
  decryptFilePayload,
  exportEncryptedKeyBackup,
  importEncryptedKeyBackup,
} from './webCryptoService.ts';
import { getUserKeys } from './keyVault.ts';

test('TDD: Base64 and Hex utility round-trip', () => {
  const originalBytes = new Uint8Array([0xde, 0xad, 0xbe, 0xef, 0x01, 0x02, 0x03, 0xff]);
  const b64 = arrayBufferToBase64(originalBytes.buffer);
  const recovered = new Uint8Array(base64ToArrayBuffer(b64));
  assert.deepStrictEqual(recovered, originalBytes, 'Base64 round-trip must preserve bytes');

  const hex = arrayBufferToHex(originalBytes.buffer);
  assert.strictEqual(hex, 'deadbeef010203ff', 'Hex output must match exact lowercase representation');
});

test('TDD: SPKI PEM formatting round-trip', () => {
  const fakeDer = new Uint8Array([10, 20, 30, 40, 50, 60, 70, 80]);
  const pem = spkiToPem(fakeDer.buffer);
  assert.ok(pem.startsWith('-----BEGIN PUBLIC KEY-----'), 'PEM must start with header');
  assert.ok(pem.endsWith('-----END PUBLIC KEY-----'), 'PEM must end with footer');

  const recovered = new Uint8Array(pemToSpkiBuffer(pem));
  assert.deepStrictEqual(recovered, fakeDer, 'PEM decode must match original DER bytes');
});

test('TDD: Cryptographic Key Pair Generation and Vault Storage', async () => {
  const username = 'alice_test';
  const bundle = await generateUserKeyPairBundle(username);

  assert.strictEqual(bundle.username, username);
  assert.ok(bundle.rsaPublicKeyPem.includes('BEGIN PUBLIC KEY'));
  assert.ok(bundle.ecPublicKeyPem.includes('BEGIN PUBLIC KEY'));
  assert.strictEqual(bundle.fingerprint.length, 64, 'SHA-256 fingerprint must be 64 hex characters');

  // Verify stored in vault
  const retrieved = await getUserKeys(username);
  assert.ok(retrieved !== null, 'Keys must be retrieved from vault');
  assert.strictEqual(retrieved?.fingerprint, bundle.fingerprint);
});

test('TDD: Zero-Knowledge File Encryption and Decryption Round-Trip', async () => {
  const username = 'bob_sender';
  const bundle = await generateUserKeyPairBundle(username);

  // Create a sample plaintext File
  const originalContent = 'Top secret zero-knowledge payload for test verification 2026!';
  const originalBytes = new TextEncoder().encode(originalContent);
  const file = new File([originalBytes], 'test_document.txt', { type: 'text/plain' });

  // Encrypt file
  const encrypted = await encryptFilePayload(
    file,
    bundle.rsaPublicKeyPem,
    bundle.ecPrivateKey
  );

  assert.ok(encrypted.ciphertextBlob.size > 0, 'Ciphertext size must be greater than 0');
  assert.ok(encrypted.ivBase64.length > 0, 'IV must be present');
  assert.ok(encrypted.ownerWrappedKeyBase64.length > 0, 'Wrapped DEK must be present');
  assert.strictEqual(encrypted.ciphertextHashHex.length, 64, 'Ciphertext hash must be 64-hex SHA-256');

  // Decrypt file
  const cipherBytes = await encrypted.ciphertextBlob.arrayBuffer();
  const decryptedBytes = await decryptFilePayload(
    cipherBytes,
    encrypted.ivBase64,
    encrypted.ownerWrappedKeyBase64,
    bundle.rsaPrivateKey
  );

  const decryptedText = new TextDecoder().decode(decryptedBytes);
  assert.strictEqual(decryptedText, originalContent, 'Decrypted plaintext must match original byte-for-byte');
});

test('TDD: Tamper Detection - Modifying Ciphertext Fails Authentication Tag Verification', async () => {
  const username = 'charlie_tamper';
  const bundle = await generateUserKeyPairBundle(username);

  const originalContent = 'Sensitive financial transaction statement';
  const file = new File([new TextEncoder().encode(originalContent)], 'statement.pdf');

  const encrypted = await encryptFilePayload(file, bundle.rsaPublicKeyPem, bundle.ecPrivateKey);
  const cipherBytes = new Uint8Array(await encrypted.ciphertextBlob.arrayBuffer());

  // Tamper with one byte in the middle of the ciphertext
  cipherBytes[10] ^= 0x55;

  await assert.rejects(
    async () => {
      await decryptFilePayload(
        cipherBytes.buffer,
        encrypted.ivBase64,
        encrypted.ownerWrappedKeyBase64,
        bundle.rsaPrivateKey
      );
    },
    'Tampered ciphertext must cause AES-GCM MAC validation to throw'
  );
});

test('TDD: Passphrase-Protected Key Backup Export and Restoration', async () => {
  const username = 'dan_backup';
  const bundle = await generateUserKeyPairBundle(username);
  const passphrase = 'SuperSecurePassphrase2026!';

  // Export encrypted backup
  const backupJson = await exportEncryptedKeyBackup(bundle, passphrase);
  assert.ok(backupJson.includes('saltBase64'), 'Backup must contain PBKDF2 salt');
  assert.ok(backupJson.includes('cipherBase64'), 'Backup must contain AES-GCM ciphertext');

  // Verify wrong passphrase fails
  await assert.rejects(
    async () => {
      await importEncryptedKeyBackup(backupJson, 'WrongPassphrase!');
    },
    'Importing with incorrect passphrase must be rejected'
  );

  // Restore with correct passphrase
  const restored = await importEncryptedKeyBackup(backupJson, passphrase);
  assert.strictEqual(restored.username, username);
  assert.strictEqual(restored.fingerprint, bundle.fingerprint);
  assert.strictEqual(restored.rsaPublicKeyPem, bundle.rsaPublicKeyPem);
});
