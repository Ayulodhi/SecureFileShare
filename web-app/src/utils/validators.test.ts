import test from 'node:test';
import assert from 'node:assert';
import {
  validateUsername,
  validateEmail,
  validatePassword,
  validatePasswordConfirmation,
} from './validators.ts';

// ============================================================================
// Equivalence Partitioning & Output Testing (EPOT): validateUsername
// ============================================================================
test('EPOT validateUsername: Equivalence Partitions and Boundary Outputs', () => {
  // Partition 1: Empty or whitespace
  assert.strictEqual(validateUsername(''), 'Username is required');
  assert.strictEqual(validateUsername('   '), 'Username is required');

  // Partition 2: Too short boundary (< 3 chars)
  assert.strictEqual(validateUsername('ab'), 'Username must be at least 3 characters');

  // Partition 3: Disallowed special characters
  assert.strictEqual(validateUsername('alice test'), 'Username can only contain letters, numbers, and underscores');
  assert.strictEqual(validateUsername('alice@123'), 'Username can only contain letters, numbers, and underscores');

  // Partition 4: Valid usernames (3 chars boundary, alphanumeric, underscore)
  assert.strictEqual(validateUsername('ali'), null, 'Exact 3-char boundary should be valid');
  assert.strictEqual(validateUsername('alice_sec_2026'), null, 'Standard valid username');
  assert.strictEqual(validateUsername('admin'), null, 'Standard valid username');
});

// ============================================================================
// Equivalence Partitioning & Output Testing (EPOT): validateEmail
// ============================================================================
test('EPOT validateEmail: Equivalence Partitions for RFC Email Ingress', () => {
  // Partition 1: Empty string
  assert.strictEqual(validateEmail(''), 'Email address is required');
  assert.strictEqual(validateEmail('   '), 'Email address is required');

  // Partition 2: Malformed email structures
  assert.strictEqual(validateEmail('plainaddress'), 'Invalid email address format');
  assert.strictEqual(validateEmail('@missinguser.com'), 'Invalid email address format');
  assert.strictEqual(validateEmail('user@.com'), 'Invalid email address format');
  assert.strictEqual(validateEmail('user@domain'), 'Invalid email address format');

  // Partition 3: Valid emails
  assert.strictEqual(validateEmail('alice@example.com'), null);
  assert.strictEqual(validateEmail('admin.user+tag@sub.domain.org'), null);
});

// ============================================================================
// Equivalence Partitioning & Output Testing (EPOT): validatePassword
// ============================================================================
test('EPOT validatePassword: Password Strength Boundary Partitions', () => {
  // Partition 1: Empty
  assert.strictEqual(validatePassword(''), 'Password is required');

  // Partition 2: Boundary < 8 characters
  assert.strictEqual(validatePassword('1234567'), 'Password must be at least 8 characters long');

  // Partition 3: Boundary >= 8 characters
  assert.strictEqual(validatePassword('12345678'), null, 'Exact 8-char boundary should be valid');
  assert.strictEqual(validatePassword('SuperSecurePassphrase2026!'), null);
});

// ============================================================================
// Equivalence Partitioning & Output Testing (EPOT): validatePasswordConfirmation
// ============================================================================
test('EPOT validatePasswordConfirmation: Equality Partitions', () => {
  assert.strictEqual(
    validatePasswordConfirmation('password123', 'password123'),
    null,
    'Matching passwords should pass'
  );
  assert.strictEqual(
    validatePasswordConfirmation('password123', 'differentPassword'),
    'Passwords do not match',
    'Mismatched passwords must return error'
  );
  assert.strictEqual(
    validatePasswordConfirmation('password123', ''),
    'Please confirm your password',
    'Empty confirmation must return prompt'
  );
});
