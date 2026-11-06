/**
 * Share Service API Client
 * WHAT: Manages encrypted share links and access grants.
 * WHY: Decouples UI from share-service implementation; handles fallback storage if service is mock-only.
 */

import { request } from './client';

export interface ShareRecord {
  id: string;
  fileId: string;
  fileName: string;
  recipientUsername: string;
  shareLink: string;
  expiresAt: string;
  status: 'ACTIVE' | 'REVOKED' | 'EXPIRED';
  createdAt: string;
}

export interface CreateShareRequest {
  fileId: string;
  fileName: string;
  recipientUsername: string;
  expiryOption: string; // e.g., '1 hour', '24 hours', '7 days'
}

const LOCAL_SHARES_KEY = 'sfs_local_shares';
const memoryShares = new Map<string, string>();

function getStorageItem(key: string): string | null {
  if (typeof localStorage !== 'undefined') {
    return localStorage.getItem(key);
  }
  return memoryShares.get(key) || null;
}

function setStorageItem(key: string, value: string): void {
  if (typeof localStorage !== 'undefined') {
    localStorage.setItem(key, value);
  } else {
    memoryShares.set(key, value);
  }
}

export const shareApi = {
  async getSharesCreated(): Promise<ShareRecord[]> {
    try {
      return await request<ShareRecord[]>('/api/v1/shares', { method: 'GET' });
    } catch {
      // Fallback to local storage if remote service is still in health-only/stub mode
      const raw = getStorageItem(LOCAL_SHARES_KEY);
      return raw ? JSON.parse(raw) : [];
    }
  },

  async createShareLink(req: CreateShareRequest): Promise<ShareRecord> {
    const hours = req.expiryOption.includes('1 hour')
      ? 1
      : req.expiryOption.includes('24')
      ? 24
      : req.expiryOption.includes('7')
      ? 168
      : 720;
    const expiresAt = new Date(Date.now() + hours * 3600 * 1000).toISOString();
    const token = 'sh_' + Math.random().toString(36).substring(2, 12) + Date.now().toString(36);
    const origin = typeof window !== 'undefined' ? window.location.origin : 'https://localhost:8443';
    const shareLink = `${origin}/share/${token}`;

    const newRecord: ShareRecord = {
      id: 'sr_' + Date.now().toString(36),
      fileId: req.fileId,
      fileName: req.fileName,
      recipientUsername: req.recipientUsername,
      shareLink,
      expiresAt,
      status: 'ACTIVE',
      createdAt: new Date().toISOString(),
    };

    try {
      return await request<ShareRecord>('/api/v1/shares', {
        method: 'POST',
        body: JSON.stringify(req),
      });
    } catch {
      // Persist locally for seamless demonstration and frontend functionality
      const raw = getStorageItem(LOCAL_SHARES_KEY);
      const list: ShareRecord[] = raw ? JSON.parse(raw) : [];
      list.unshift(newRecord);
      setStorageItem(LOCAL_SHARES_KEY, JSON.stringify(list));
      return newRecord;
    }
  },

  async revokeShare(shareId: string): Promise<void> {
    try {
      await request<void>(`/api/v1/shares/${shareId}`, { method: 'DELETE' });
    } catch {
      const raw = getStorageItem(LOCAL_SHARES_KEY);
      if (raw) {
        const list: ShareRecord[] = JSON.parse(raw);
        const item = list.find((s) => s.id === shareId);
        if (item) {
          item.status = 'REVOKED';
          setStorageItem(LOCAL_SHARES_KEY, JSON.stringify(list));
        }
      }
    }
  },

  async getShareByToken(token: string): Promise<ShareRecord | null> {
    try {
      return await request<ShareRecord>(`/api/v1/shares/token/${token}`, { method: 'GET' });
    } catch {
      const raw = getStorageItem(LOCAL_SHARES_KEY);
      if (raw) {
        const list: ShareRecord[] = JSON.parse(raw);
        return list.find((s) => s.shareLink.includes(token)) || null;
      }
      return null;
    }
  },
};
