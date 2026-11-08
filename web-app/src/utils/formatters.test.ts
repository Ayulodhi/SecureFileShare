import test from 'node:test';
import assert from 'node:assert';
import {
  formatFileSize,
  formatHash,
  formatTimestamp,
  getStatusBadgeInfo,
  isShareExpired,
} from './formatters.ts';

// ============================================================================
// Equivalence Partitioning & Output Testing (EPOT): formatFileSize
// ============================================================================
test('EPOT formatFileSize: Equivalence Partitions and Boundary Output Verification', () => {
  // Partition 1: Zero and negative inputs
  assert.strictEqual(formatFileSize(0), '0 B', 'Zero bytes partition');
  assert.strictEqual(formatFileSize(-100), '0 B', 'Negative boundary partition');
  assert.strictEqual(formatFileSize(NaN), '0 B', 'Invalid NaN partition');

  // Partition 2: Small byte values (< 1024 B)
  assert.strictEqual(formatFileSize(1), '1 B', 'Lower boundary partition');
  assert.strictEqual(formatFileSize(512), '512 B', 'Mid-byte partition');
  assert.strictEqual(formatFileSize(1023), '1023 B', 'Upper byte boundary partition');

  // Partition 3: Kilobyte range [1024 B, 1 MB)
  assert.strictEqual(formatFileSize(1024), '1.0 KB', 'Exact 1 KB boundary');
  assert.strictEqual(formatFileSize(1536), '1.5 KB', 'Mid KB partition');
  assert.strictEqual(formatFileSize(1048575), '1024.0 KB', 'Upper KB boundary');

  // Partition 4: Megabyte range [1 MB, 1 GB)
  assert.strictEqual(formatFileSize(1048576), '1.0 MB', 'Exact 1 MB boundary');
  assert.strictEqual(formatFileSize(5242880), '5.0 MB', '5 MB partition');

  // Partition 5: Gigabyte range (>= 1 GB)
  assert.strictEqual(formatFileSize(1073741824), '1.0 GB', 'Exact 1 GB boundary');
  assert.strictEqual(formatFileSize(2684354560), '2.5 GB', '2.5 GB partition');
});

// ============================================================================
// Equivalence Partitioning & Output Testing (EPOT): formatHash
// ============================================================================
test('EPOT formatHash: Equivalence Partitions and Output Truncation', () => {
  const fullSha256 = 'e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855';

  // Partition 1: Standard 64-char hash with default prefix (16 chars)
  assert.strictEqual(
    formatHash(fullSha256),
    'e3b0c44298fc1c14...',
    'Default 16-char truncation partition'
  );

  // Partition 2: Custom length parameter
  assert.strictEqual(
    formatHash(fullSha256, 8),
    'e3b0c442...',
    'Custom 8-char truncation partition'
  );

  // Partition 3: Short hash (length <= requested truncation)
  assert.strictEqual(formatHash('abc123', 16), 'abc123', 'Short hash should not append ellipsis');

  // Partition 4: Empty or non-string inputs
  assert.strictEqual(formatHash(''), '—', 'Empty hash partition');
  assert.strictEqual(formatHash(null as unknown as string), '—', 'Null hash partition');
  assert.strictEqual(formatHash(undefined as unknown as string), '—', 'Undefined hash partition');
});

// ============================================================================
// Equivalence Partitioning & Output Testing (EPOT): formatTimestamp
// ============================================================================
test('EPOT formatTimestamp: ISO and Epoch Partition Output Verification', () => {
  // Partition 1: Valid ISO date string
  const iso = '2026-11-08T14:30:45.000Z';
  const formatted = formatTimestamp(iso);
  assert.ok(formatted.includes('2026-11-08'), 'Must include correct date');
  assert.ok(formatted.includes(':'), 'Must include formatted time');

  // Partition 2: Valid numeric Epoch timestamp
  const epoch = 1794148245000;
  const formattedEpoch = formatTimestamp(epoch);
  assert.ok(formattedEpoch.length >= 16, 'Formatted timestamp must have standard length');

  // Partition 3: Invalid timestamp inputs
  assert.strictEqual(formatTimestamp('invalid-date'), '—', 'Invalid date string partition');
  assert.strictEqual(formatTimestamp(''), '—', 'Empty date partition');
  assert.strictEqual(formatTimestamp(null as unknown as string), '—', 'Null date partition');
});

// ============================================================================
// Equivalence Partitioning & Output Testing (EPOT): getStatusBadgeInfo
// ============================================================================
test('EPOT getStatusBadgeInfo: Status Mapping Equivalence Partitions', () => {
  // Partition 1: Clean / Active (Success states)
  const clean = getStatusBadgeInfo('CLEAN');
  assert.strictEqual(clean.variant, 'success');
  assert.strictEqual(clean.label, 'Clean');

  const active = getStatusBadgeInfo('ACTIVE');
  assert.strictEqual(active.variant, 'success');
  assert.strictEqual(active.label, 'Active');

  // Partition 2: Pending states (Warning states)
  const pendingScan = getStatusBadgeInfo('PENDING_SCAN');
  assert.strictEqual(pendingScan.variant, 'warning');
  assert.strictEqual(pendingScan.label, 'Scanning');

  const pending = getStatusBadgeInfo('PENDING');
  assert.strictEqual(pending.variant, 'warning');
  assert.strictEqual(pending.label, 'Pending');

  // Partition 3: Threat / Revoked states (Danger states)
  const infected = getStatusBadgeInfo('INFECTED');
  assert.strictEqual(infected.variant, 'danger');
  assert.strictEqual(infected.label, 'Infected');

  const revoked = getStatusBadgeInfo('REVOKED');
  assert.strictEqual(revoked.variant, 'danger');
  assert.strictEqual(revoked.label, 'Revoked');

  // Partition 4: Unknown / Empty status
  const unknown = getStatusBadgeInfo('CUSTOM_STATUS');
  assert.strictEqual(unknown.variant, 'neutral');
  assert.strictEqual(unknown.label, 'CUSTOM_STATUS');

  const empty = getStatusBadgeInfo('');
  assert.strictEqual(empty.variant, 'neutral');
  assert.strictEqual(empty.label, 'Unknown');
});

// ============================================================================
// Equivalence Partitioning & Output Testing (EPOT): isShareExpired
// ============================================================================
test('EPOT isShareExpired: Expiration & Revocation Equivalence Partitions', () => {
  const oneHourAgo = new Date(Date.now() - 3600 * 1000).toISOString();
  const oneHourFuture = new Date(Date.now() + 3600 * 1000).toISOString();

  // Partition 1: Past timestamp -> Expired
  assert.strictEqual(isShareExpired(oneHourAgo, 'ACTIVE'), true, 'Past date partition must be expired');

  // Partition 2: Future timestamp and ACTIVE -> Not Expired
  assert.strictEqual(isShareExpired(oneHourFuture, 'ACTIVE'), false, 'Future active partition must not be expired');

  // Partition 3: Future timestamp but REVOKED -> Treated as Expired/Inactive
  assert.strictEqual(isShareExpired(oneHourFuture, 'REVOKED'), true, 'Revoked partition must be expired');

  // Partition 4: Malformed or missing date
  assert.strictEqual(isShareExpired('invalid', 'ACTIVE'), true, 'Invalid date must default to expired');
});
