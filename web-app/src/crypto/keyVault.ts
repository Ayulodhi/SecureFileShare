/**
 * IndexedDB Key Vault for SecureFileShare
 * WHAT: Manages browser-local persistent storage for user cryptographic private keys.
 * WHY: Zero-knowledge security guarantee: private keys must NEVER leave the client or be saved in localStorage.
 */

const DB_NAME = 'sfs_vault';
const DB_VERSION = 1;
const STORE_NAME = 'user_keys';

export interface StoredUserKeys {
  username: string;
  rsaPrivateKey: CryptoKey;
  ecPrivateKey: CryptoKey;
  rsaPublicKeyPem: string;
  ecPublicKeyPem: string;
  fingerprint: string;
  createdAt: string;
}

// In-memory fallback for non-browser / test environments
const memoryStore = new Map<string, StoredUserKeys>();

function openDatabase(): Promise<IDBDatabase> {
  return new Promise((resolve, reject) => {
    if (typeof indexedDB === 'undefined') {
      reject(new Error('IndexedDB is not supported in this environment'));
      return;
    }
    const request = indexedDB.open(DB_NAME, DB_VERSION);

    request.onupgradeneeded = (event) => {
      const db = (event.target as IDBOpenDBRequest).result;
      if (!db.objectStoreNames.contains(STORE_NAME)) {
        db.createObjectStore(STORE_NAME, { keyPath: 'username' });
      }
    };

    request.onsuccess = () => resolve(request.result);
    request.onerror = () => reject(request.error);
  });
}

export async function saveUserKeys(keys: StoredUserKeys): Promise<void> {
  if (typeof indexedDB === 'undefined') {
    memoryStore.set(keys.username, keys);
    return;
  }
  const db = await openDatabase();
  return new Promise((resolve, reject) => {
    const tx = db.transaction(STORE_NAME, 'readwrite');
    const store = tx.objectStore(STORE_NAME);
    const req = store.put(keys);
    req.onsuccess = () => resolve();
    req.onerror = () => reject(req.error);
  });
}

export async function getUserKeys(username: string): Promise<StoredUserKeys | null> {
  if (typeof indexedDB === 'undefined') {
    return memoryStore.get(username) || null;
  }
  const db = await openDatabase();
  return new Promise((resolve, reject) => {
    const tx = db.transaction(STORE_NAME, 'readonly');
    const store = tx.objectStore(STORE_NAME);
    const req = store.get(username);
    req.onsuccess = () => resolve(req.result || null);
    req.onerror = () => reject(req.error);
  });
}

export async function hasUserKeys(username: string): Promise<boolean> {
  const keys = await getUserKeys(username);
  return keys !== null;
}

export async function deleteUserKeys(username: string): Promise<void> {
  if (typeof indexedDB === 'undefined') {
    memoryStore.delete(username);
    return;
  }
  const db = await openDatabase();
  return new Promise((resolve, reject) => {
    const tx = db.transaction(STORE_NAME, 'readwrite');
    const store = tx.objectStore(STORE_NAME);
    const req = store.delete(username);
    req.onsuccess = () => resolve();
    req.onerror = () => reject(req.error);
  });
}
