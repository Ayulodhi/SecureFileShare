/**
 * Formatting and Status Utilities for SecureFileShare UI
 * Implemented with TDD & Equivalence Partitioning & Output Testing (EPOT).
 */

export interface StatusBadgeInfo {
  variant: 'success' | 'warning' | 'danger' | 'neutral';
  label: string;
}

/**
 * Formats a numeric byte count into an appropriate human-readable string.
 * Equivalence partitions: 0/negative -> '0 B', <1024 B -> 'X B', <1 MB -> 'X.X KB', <1 GB -> 'X.X MB', >=1 GB -> 'X.X GB'
 */
export function formatFileSize(bytes: number): string {
  if (typeof bytes !== 'number' || isNaN(bytes) || bytes <= 0) {
    return '0 B';
  }

  if (bytes < 1024) {
    return `${Math.floor(bytes)} B`;
  }

  const kb = bytes / 1024;
  if (kb < 1024) {
    return `${kb.toFixed(1)} KB`;
  }

  const mb = kb / 1024;
  if (mb < 1024) {
    return `${mb.toFixed(1)} MB`;
  }

  const gb = mb / 1024;
  return `${gb.toFixed(1)} GB`;
}

/**
 * Truncates a hex hash with ellipsis for clean UI display.
 */
export function formatHash(hash: string, length = 16): string {
  if (!hash || typeof hash !== 'string') {
    return '—';
  }

  if (hash.length <= length) {
    return hash;
  }

  return `${hash.substring(0, length)}...`;
}

/**
 * Formats an ISO string, timestamp number, or Date into a standardized readable format.
 */
export function formatTimestamp(isoOrDate: string | number | Date): string {
  if (!isoOrDate) {
    return '—';
  }

  const date = new Date(isoOrDate);
  if (isNaN(date.getTime())) {
    return '—';
  }

  return date.toISOString().replace('T', ' ').substring(0, 19);
}

/**
 * Maps a file or share status string to a visual badge configuration.
 */
export function getStatusBadgeInfo(status: string): StatusBadgeInfo {
  if (!status || typeof status !== 'string') {
    return { variant: 'neutral', label: 'Unknown' };
  }

  const normalized = status.toUpperCase().trim();

  switch (normalized) {
    case 'CLEAN':
      return { variant: 'success', label: 'Clean' };
    case 'ACTIVE':
      return { variant: 'success', label: 'Active' };
    case 'PENDING_SCAN':
      return { variant: 'warning', label: 'Scanning' };
    case 'PENDING':
      return { variant: 'warning', label: 'Pending' };
    case 'INFECTED':
      return { variant: 'danger', label: 'Infected' };
    case 'REVOKED':
      return { variant: 'danger', label: 'Revoked' };
    default:
      return { variant: 'neutral', label: status };
  }
}

/**
 * Checks whether an authorization share link has expired or been revoked.
 */
export function isShareExpired(expiresAt: string | number | Date, status?: string): boolean {
  if (status && status.toUpperCase() === 'REVOKED') {
    return true;
  }

  if (!expiresAt) {
    return true;
  }

  const date = new Date(expiresAt);
  if (isNaN(date.getTime())) {
    return true;
  }

  return date.getTime() < Date.now();
}
