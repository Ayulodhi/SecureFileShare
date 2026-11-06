# Zero-Knowledge Web Client (`web-app`) Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task. Note: Per user instruction, **DO NOT run `git commit` or `git push`** on any step; keep all changes local in the working tree. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Build and integrate a lightweight, zero-knowledge React/TypeScript single-page web application (`web-app`) served by Caddy at `https://<server-ip>:8443/` that allows users to authenticate, encrypt/decrypt files in-browser with Web Crypto, share files, and verify audit log hash chains.

**Architecture:** A static SPA built with React 18+ and Vite, utilizing native browser `window.crypto.subtle` for AES-256-GCM and RSA-OAEP operations, IndexedDB for private key storage, and direct REST API calls to backend microservices. The compiled assets (`web-app/dist`) are mounted read-only into the existing Caddy reverse proxy container to avoid adding runtime containers.

**Tech Stack:** React, TypeScript, Vite, Web Crypto API (`SubtleCrypto`), IndexedDB (`idb` or native), Caddy.

**Spec:** `docs/superpowers/specs/2026-11-03-web-app-frontend-design.md`

## Global Constraints
- Zero-knowledge: Plaintext files, unencrypted DEKs, and RSA private keys must never touch the network or `localStorage`.
- RSA parameters: RSA-OAEP with SHA-256, 4096-bit or 2048-bit modulus, public exponent `65537`. SPKI export format encoded in standard Base64.
- AES parameters: AES-256-GCM with 96-bit (12-byte) random IV and 128-bit authentication tag.
- Backend compatibility: REST endpoints match existing Spring Boot contracts (`/api/v1/auth`, `/api/v1/files`, `/api/v1/shares`, `/api/v1/audit`).
- Git constraint: **NO commits or pushes** during execution per user instruction; leave modified/created files in the working directory.

## Review Focus
1. Private key loss when browser site data is cleared -> Provide user export/import feature.
2. Large file memory exhaustion during encryption -> Chunked read/encrypt or blob streaming.
3. RSA SPKI key format mismatch between Web Crypto and Java Bouncy Castle backend -> Strict SPKI Base64 encoding.
4. HTTP 429 rate limit or HTTP 401 token expiration during long uploads -> User-friendly toast notification and session refresh handling.
5. Insecure/self-signed TLS certificate handling in local and remote environments -> Clear browser guidance and resilient fetch configuration.

---

### Task 1: Web App Scaffolding & Design System Foundations

**Files:**
- Create: `web-app/package.json`
- Create: `web-app/tsconfig.json`
- Create: `web-app/tsconfig.node.json`
- Create: `web-app/vite.config.ts`
- Create: `web-app/index.html`
- Create: `web-app/src/index.css`
- Create: `web-app/src/main.tsx`
- Create: `web-app/src/App.tsx`

**Interfaces:**
- Produces: Running Vite dev server and production `npm run build` script generating `web-app/dist/`.

- [ ] **Step 1: Create package.json and project configuration**
Create `web-app/package.json` with React, React DOM, Lucide icons (or clean SVG icons), TypeScript, Vite, and build scripts.

- [ ] **Step 2: Create TypeScript and Vite configs**
Create `web-app/tsconfig.json`, `web-app/tsconfig.node.json`, and `web-app/vite.config.ts` configured with React plugin and local proxy rules for `/api` to `https://140.238.240.128:8443` or local backend.

- [ ] **Step 3: Define design tokens in `index.css`**
Define CSS variables for dark/light surfaces, slate text colors, accent blues, card elevation, and responsive container styles matching the desktop client theme.

- [ ] **Step 4: Install dependencies and verify build**
Run: `npm --prefix web-app install && npm --prefix web-app run build`
Expected: Success with `dist/` directory created.

---

### Task 2: Web Crypto Service & IndexedDB Key Vault

**Files:**
- Create: `web-app/src/crypto/keyVault.ts`
- Create: `web-app/src/crypto/webCryptoService.ts`
- Create: `web-app/src/crypto/crypto.test.ts`

**Interfaces:**
- Produces:
  - `generateUserKeyPair(): Promise<{ publicKeySpkiBase64: string, keyId: string }>`
  - `getUserPrivateKey(keyId: string): Promise<CryptoKey | null>`
  - `encryptFile(file: File, recipientPublicKeySpkiBase64: string): Promise<{ ciphertextBlob: Blob, wrappedDekBase64: string, ivBase64: string, sha256Checksum: string }>`
  - `decryptFile(ciphertextBlob: Blob, wrappedDekBase64: string, ivBase64: string, keyId: string): Promise<Blob>`
  - `exportPrivateKeyBackup(passphrase: string, keyId: string): Promise<string>`
  - `importPrivateKeyBackup(encryptedBackupJson: string, passphrase: string): Promise<string>`

- [ ] **Step 1: Implement IndexedDB key storage in `keyVault.ts`**
Open database `sfs_vault` with store `private_keys`. Implement save, load, and delete for CryptoKey objects.

- [ ] **Step 2: Implement Web Crypto logic in `webCryptoService.ts`**
Implement RSA-OAEP key generation, SPKI Base64 export/import, AES-256-GCM encryption with 12-byte IV, DEK wrapping with RSA public key, and SHA-256 digest computation.

- [ ] **Step 3: Write in-browser/headless crypto verification test**
Create a lightweight unit test or verification script validating encrypt -> decrypt cycle preserves exact byte contents and SHA-256 hash.

- [ ] **Step 4: Verify crypto round-trip**
Run: `npm --prefix web-app run test` or run browser crypto self-test script.
Expected: PASS.

---

### Task 3: Typed REST API Integration Layer

**Files:**
- Create: `web-app/src/api/client.ts`
- Create: `web-app/src/api/authApi.ts`
- Create: `web-app/src/api/fileApi.ts`
- Create: `web-app/src/api/shareApi.ts`
- Create: `web-app/src/api/auditApi.ts`

**Interfaces:**
- Produces:
  - `authApi.login(request: LoginRequest): Promise<AuthResponse>`
  - `authApi.register(request: RegisterRequest): Promise<void>`
  - `authApi.registerPublicKey(keySpkiBase64: string, algorithm: string): Promise<void>`
  - `authApi.getRecipientPublicKey(username: string): Promise<KeyRegistryResponse>`
  - `fileApi.uploadFile(formData: FormData): Promise<UploadResponse>`
  - `fileApi.downloadFile(fileId: string): Promise<Blob>`
  - `shareApi.createShare(request: ShareRequest): Promise<ShareResponse>`
  - `shareApi.getShare(token: string): Promise<ShareDetails>`
  - `shareApi.revokeShare(shareId: string): Promise<void>`
  - `auditApi.getAuditEntries(page: number, size: number): Promise<AuditResponse>`
  - `auditApi.verifyChain(): Promise<AuditVerifyResult>`

- [ ] **Step 1: Implement base fetch wrapper `client.ts`**
Handle JWT header attachment, JSON serialization, HTTP status code handling (401, 403, 429), and error parsing.

- [ ] **Step 2: Implement API modules**
Wire up endpoints for `authApi`, `fileApi`, `shareApi`, and `auditApi` with strict TypeScript types matching the backend DTOs.

- [ ] **Step 3: Verify TypeScript compilation**
Run: `npm --prefix web-app run build`
Expected: Compile clean with 0 type errors.

---

### Task 4: UI Shell, Auth Context & Shared Components

**Files:**
- Create: `web-app/src/state/AuthContext.tsx`
- Create: `web-app/src/components/Layout.tsx`
- Create: `web-app/src/components/Card.tsx`
- Create: `web-app/src/components/Button.tsx`
- Create: `web-app/src/components/Table.tsx`
- Create: `web-app/src/components/Modal.tsx`
- Create: `web-app/src/components/Toast.tsx`

**Interfaces:**
- Produces: `useAuth()` hook with `token`, `user`, `login()`, `logout()`, `isAuthenticated`, and global layout shell with navigation sidebar.

- [ ] **Step 1: Implement `AuthContext.tsx`**
Manage JWT in `sessionStorage` or state, load active user, handle automatic logout on 401.

- [ ] **Step 2: Implement `Layout.tsx` and design components**
Create a responsive sidebar with nav links (Upload, My Files, Audit Trail, Keys, Logout) and reusable card/table containers.

- [ ] **Step 3: Verify component rendering and layout**
Run: `npm --prefix web-app run build`
Expected: SUCCESS.

---

### Task 5: Auth & Key Registration Pages

**Files:**
- Create: `web-app/src/pages/LoginPage.tsx`
- Create: `web-app/src/pages/RegisterPage.tsx`
- Modify: `web-app/src/App.tsx`

**Interfaces:**
- Produces: Complete login and registration flows, automatically verifying and provisioning in-browser Web Crypto keys upon signup.

- [ ] **Step 1: Implement `LoginPage.tsx`**
Username and password inputs, form validation, error banners for failed login or rate limits, and key presence check.

- [ ] **Step 2: Implement `RegisterPage.tsx`**
Registration fields, password strength indicator, automated RSA key generation and public key registration with backend.

- [ ] **Step 3: Wire routes in `App.tsx`**
Add view switcher or routing between Login, Register, and Main App.

- [ ] **Step 4: Verify compilation**
Run: `npm --prefix web-app run build`
Expected: SUCCESS.

---

### Task 6: Upload & My Files Pages

**Files:**
- Create: `web-app/src/pages/UploadPage.tsx`
- Create: `web-app/src/pages/FilesPage.tsx`
- Create: `web-app/src/components/ShareModal.tsx`

**Interfaces:**
- Produces:
  - Drag-and-drop file upload with client-side AES-GCM encryption and progress bar.
  - File list with decrypt & download action and share link generator modal.

- [ ] **Step 1: Implement `UploadPage.tsx`**
File selector, recipient public key lookup, in-browser DEK generation, AES-256-GCM encryption, and multipart POST upload.

- [ ] **Step 2: Implement `FilesPage.tsx`**
Fetch file records, provide one-click download that decrypts ciphertext in-browser and triggers browser file save.

- [ ] **Step 3: Implement `ShareModal.tsx`**
Generate HMAC share token with optional expiration and access count limit; display copyable link.

- [ ] **Step 4: Verify build**
Run: `npm --prefix web-app run build`
Expected: SUCCESS.

---

### Task 7: Audit Trail, Key Management & Public Share Decryption

**Files:**
- Create: `web-app/src/pages/AuditPage.tsx`
- Create: `web-app/src/pages/KeysPage.tsx`
- Create: `web-app/src/pages/ShareAccessPage.tsx`
- Modify: `web-app/src/App.tsx`

**Interfaces:**
- Produces:
  - Immutable audit trail viewer with live "Verify Audit Hash Chain" integrity badge.
  - Key management view with public fingerprint display, key export, and import.
  - Public share page allowing link recipients to decrypt and download files.

- [ ] **Step 1: Implement `AuditPage.tsx`**
Paginated audit log table with action chips and cryptographic hash chain verification button.

- [ ] **Step 2: Implement `KeysPage.tsx`**
Public key fingerprint display, passphrase-protected backup export, and key restore.

- [ ] **Step 3: Implement `ShareAccessPage.tsx`**
Direct access via `/share/:token` to retrieve metadata, unwrap key, and download decrypted file.

- [ ] **Step 4: Verify complete application compilation**
Run: `npm --prefix web-app run build`
Expected: SUCCESS.

---

### Task 8: Caddy & Docker Production Deployment Integration

**Files:**
- Modify: `docker-compose.prod.yml`
- Modify: `Caddyfile`

**Interfaces:**
- Mounts `./web-app/dist` to `/var/www/html` in Caddy.
- Configures Caddy SPA fallback routing (`try_files {path} /index.html`) and CSP headers.

- [ ] **Step 1: Update `Caddyfile`**
Add static file root `/var/www/html`, adjust CSP to allow script/blob operations, and add fallback routing for SPA.

- [ ] **Step 2: Update `docker-compose.prod.yml`**
Add `- ./web-app/dist:/var/www/html:ro` volume mount under `services.caddy.volumes`.

- [ ] **Step 3: Build production assets**
Run: `npm --prefix web-app run build`
Verify `web-app/dist/index.html` exists.

- [ ] **Step 4: Local verification test**
Verify Caddyfile syntax and configuration.
