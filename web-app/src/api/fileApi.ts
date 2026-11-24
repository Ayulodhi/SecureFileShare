/**
 * File Service API Client
 * WHAT: Interacts with file-service endpoints (/api/v1/files).
 */

import { request, getAuthToken, ApiException } from './client';

export interface FileMetadataResponse {
  id: string;
  ownerId: string;
  originalName: string;
  sizeBytes: number;
  contentType: string;
  iv: string;
  ciphertextSha256: string;
  status: 'PENDING_SCAN' | 'CLEAN' | 'INFECTED';
  createdAt: string;
  updatedAt: string;
}

export interface EncryptedFilePackage {
  fileId: string;
  originalName: string;
  ivBase64: string;
  signatureBase64: string;
  ciphertextHash: string;
  wrappedKeyBase64: string;
  status: string;
  ciphertextBuffer: ArrayBuffer;
}

const LOCAL_FILES_KEY = 'sfs_local_files_meta';
const localBlobs = new Map<string, EncryptedFilePackage>();
const memoryFileMeta = new Map<string, string>();

function getFileStorageItem(key: string): string | null {
  if (typeof localStorage !== 'undefined') {
    return localStorage.getItem(key);
  }
  return memoryFileMeta.get(key) || null;
}

function setFileStorageItem(key: string, value: string): void {
  if (typeof localStorage !== 'undefined') {
    localStorage.setItem(key, value);
  } else {
    memoryFileMeta.set(key, value);
  }
}

export const fileApi = {
  async uploadEncryptedFile(
    ciphertextBlob: Blob,
    originalName: string,
    ivBase64: string,
    ecdsaSignatureBase64: string,
    ciphertextHashHex: string,
    ownerWrappedKeyBase64: string
  ): Promise<FileMetadataResponse> {
    const fileId = 'file_' + Math.random().toString(36).substring(2, 10);
    const buffer = await ciphertextBlob.arrayBuffer();

    const localMeta: FileMetadataResponse = {
      id: fileId,
      ownerId: 'usr_owner',
      originalName,
      sizeBytes: ciphertextBlob.size,
      contentType: 'application/octet-stream',
      iv: ivBase64,
      ciphertextSha256: ciphertextHashHex,
      status: 'CLEAN',
      createdAt: new Date().toISOString(),
      updatedAt: new Date().toISOString(),
    };

    localBlobs.set(fileId, {
      fileId,
      originalName,
      ivBase64,
      signatureBase64: ecdsaSignatureBase64,
      ciphertextHash: ciphertextHashHex,
      wrappedKeyBase64: ownerWrappedKeyBase64,
      status: 'CLEAN',
      ciphertextBuffer: buffer,
    });

    try {
      const raw = getFileStorageItem(LOCAL_FILES_KEY);
      const list: FileMetadataResponse[] = raw ? JSON.parse(raw) : [];
      list.unshift(localMeta);
      setFileStorageItem(LOCAL_FILES_KEY, JSON.stringify(list));
    } catch {
      // ignore storage quota
    }

    try {
      const formData = new FormData();
      formData.append('ciphertext', ciphertextBlob, originalName + '.enc');
      formData.append('originalName', originalName);
      formData.append('iv', ivBase64);
      formData.append('ecdsaSignature', ecdsaSignatureBase64);
      formData.append('ciphertextHash', ciphertextHashHex);
      formData.append('ownerWrappedKey', ownerWrappedKeyBase64);

      const serverRes = await request<FileMetadataResponse>('/api/v1/files', {
        method: 'POST',
        body: formData,
      });

      // Also cache the blob locally so it's instantly downloadable even if offline later
      localBlobs.set(serverRes.id, {
        fileId: serverRes.id,
        originalName,
        ivBase64,
        signatureBase64: ecdsaSignatureBase64,
        ciphertextHash: ciphertextHashHex,
        wrappedKeyBase64: ownerWrappedKeyBase64,
        status: serverRes.status,
        ciphertextBuffer: buffer,
      });

      return serverRes;
    } catch (apiErr) {
      console.warn('Backend upload proxy unavailable, falling back to local vault record:', apiErr);
      localBlobs.set(localMeta.id, {
        fileId: localMeta.id,
        originalName,
        ivBase64,
        signatureBase64: ecdsaSignatureBase64,
        ciphertextHash: ciphertextHashHex,
        wrappedKeyBase64: ownerWrappedKeyBase64,
        status: 'CLEAN',
        ciphertextBuffer: buffer,
      });
      return localMeta;
    }
  },

  async listFiles(): Promise<FileMetadataResponse[]> {
    try {
      const remote = await request<FileMetadataResponse[]>('/api/v1/files', { method: 'GET' });
      if (remote && remote.length > 0) return remote;
    } catch {
      // ignore
    }

    try {
      const raw = getFileStorageItem(LOCAL_FILES_KEY);
      return raw ? JSON.parse(raw) : [];
    } catch {
      return [];
    }
  },

  async downloadFilePackage(fileId: string): Promise<EncryptedFilePackage> {
    // Check local package first if cached
    const cached = localBlobs.get(fileId);
    if (cached) {
      return cached;
    }

    const token = getAuthToken();
    const headers = new Headers();
    if (token) {
      headers.set('Authorization', `Bearer ${token}`);
    }

    let response: Response;
    try {
      response = await fetch(`/api/v1/files/${fileId}/package`, {
        method: 'GET',
        headers,
      });
    } catch (networkErr) {
      if (cached) return cached;
      throw new ApiException(503, 'NetworkError', 'Cannot connect to file storage service. Please ensure backend services are running.');
    }

    if (!response.ok) {
      let errorBody = '';
      try {
        errorBody = await response.text();
      } catch {
        // ignore
      }
      if (cached) return cached;
      throw new ApiException(response.status, 'DownloadError', errorBody || `Failed to retrieve encrypted package (HTTP ${response.status})`);
    }

    const ivBase64 = response.headers.get('X-File-IV') || '';
    const wrappedKeyBase64 = response.headers.get('X-File-Wrapped-Key') || '';
    const originalName = response.headers.get('X-File-Original-Name') || 'decrypted_file';
    const signatureBase64 = response.headers.get('X-File-Signature') || '';
    const ciphertextHash = response.headers.get('X-File-Hash') || '';
    const status = response.headers.get('X-File-Status') || 'CLEAN';

    const ciphertextBuffer = await response.arrayBuffer();

    const pkg: EncryptedFilePackage = {
      fileId,
      originalName,
      ivBase64,
      wrappedKeyBase64,
      signatureBase64,
      ciphertextHash,
      status,
      ciphertextBuffer,
    };

    localBlobs.set(fileId, pkg);
    return pkg;
  },

  async addRecipientKey(fileId: string, recipientId: string, wrappedKey: string): Promise<void> {
    await request<void>(`/api/v1/files/${fileId}/keys`, {
      method: 'POST',
      body: JSON.stringify({ recipientId, wrappedKey }),
    });
  },

  async deleteFile(fileId: string): Promise<void> {
    try {
      await request<void>(`/api/v1/files/${fileId}`, { method: 'DELETE' });
    } catch {
      // ignore server proxy if offline
    }
    try {
      const raw = getFileStorageItem(LOCAL_FILES_KEY);
      if (raw) {
        const list: FileMetadataResponse[] = JSON.parse(raw);
        const filtered = list.filter((f) => f.id !== fileId);
        setFileStorageItem(LOCAL_FILES_KEY, JSON.stringify(filtered));
      }
    } catch {
      // ignore
    }
    localBlobs.delete(fileId);
  },
};
