/**
 * Form and Input Validation Utilities for SecureFileShare UI
 * Implemented with TDD & Equivalence Partitioning & Output Testing (EPOT).
 */

const USERNAME_REGEX = /^[a-zA-Z0-9_]+$/;
const EMAIL_REGEX = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;

/**
 * Validates username against length and character requirements.
 * Returns null if valid, or an error string if invalid.
 */
export function validateUsername(username: string): string | null {
  if (!username || !username.trim()) {
    return 'Username is required';
  }

  const trimmed = username.trim();
  if (trimmed.length < 3) {
    return 'Username must be at least 3 characters';
  }

  if (!USERNAME_REGEX.test(trimmed)) {
    return 'Username can only contain letters, numbers, and underscores';
  }

  return null;
}

/**
 * Validates email format.
 * Returns null if valid, or an error string if invalid.
 */
export function validateEmail(email: string): string | null {
  if (!email || !email.trim()) {
    return 'Email address is required';
  }

  const trimmed = email.trim();
  if (!EMAIL_REGEX.test(trimmed)) {
    return 'Invalid email address format';
  }

  return null;
}

/**
 * Validates password meets minimum security threshold.
 * Returns null if valid, or an error string if invalid.
 */
export function validatePassword(password: string): string | null {
  if (!password) {
    return 'Password is required';
  }

  if (password.length < 8) {
    return 'Password must be at least 8 characters long';
  }

  return null;
}

/**
 * Validates that confirmed password matches the original password.
 * Returns null if matching, or an error string if mismatched.
 */
export function validatePasswordConfirmation(
  password: string,
  confirmPassword: string
): string | null {
  if (!confirmPassword) {
    return 'Please confirm your password';
  }

  if (password !== confirmPassword) {
    return 'Passwords do not match';
  }

  return null;
}
