# Architecture Design Specification: Zero-Knowledge Web Client (`web-app`)

- **Author:** Senior Developer & User
- **Date:** 2026-11-03
- **Status:** Approved (Ready for Implementation Planning)
- **Target Subsystem:** `web-app` (Frontend Single-Page Application)

---

## 1. Executive Summary & Intent

### 1.1 Objective
Provide full, secure browser access to the **SecureFileShare** platform at `https://<server-ip>:8443/` without requiring users to download or run the Java Swing desktop client.

### 1.2 Guiding Principles
1. **Zero-Knowledge Architecture:** The server must remain completely blind to plaintext files, unencrypted Data Encryption Keys (DEKs), and user private keys. All cryptographic operations occur exclusively client-side inside the user's browser.
2. **Minimal Resource Footprint (Ponytail / Cloud Coexistence):** To respect the 512MB RAM bounds of the Oracle Cloud host VM, the web frontend will not add an extra Node.js runtime container. Instead, it compiles to static assets served directly by the existing Caddy reverse proxy via its internal high-performance `file_server`.
3. **Parity with Desktop Client:** The web interface mirrors the desktop client's core workflows: User Authentication, Key Pair Generation & Registry, File Encryption & Chunked Upload, Recipient Sharing, Download & Decryption, and Cryptographic Audit Verification.

---

## 2. System Architecture & Tech Stack

### 2.1 Technology Stack
* **Framework:** React 18 / 19 with TypeScript.
* **Build Tooling:** Vite (fast ESM development and optimized production asset bundling).
* **Styling & Design System:** Scoped CSS variables and modern component tokens matching the desktop client palette (Slate base, Indigo primary accents, high-contrast dark/light card surfaces, FlatLaf-compatible spacing).
* **HTTP Client:** Native `fetch` API wrapped in typed service modules with automatic JWT Bearer header injection and unified error normalization.
* **Cryptographic Engine:** Browser Native **Web Crypto API** (`window.crypto.subtle`) for hardware-accelerated AES-GCM, RSA-OAEP, and SHA-256 operations.
* **Local Key Vault:** Browser **IndexedDB** for persistent, secure storage of non-extractable / encrypted private keys.

### 2.2 Directory Layout (`web-app/`)
```text
web-app/
├── index.html
├── package.json
├── vite.config.ts
├── tsconfig.json
└── src/
    ├── main.tsx
    ├── App.tsx
    ├── api/                     # Typed REST API integration
    │   ├── authApi.ts           # Login, registration, public key exchange
    │   ├── fileApi.ts           # Ciphertext upload, chunking, file metadata, download
    │   ├── shareApi.ts          # Share link generation, access verification, revocations
    │   └── auditApi.ts          # Immutable audit entries and hash chain verification
    ├── crypto/                  # Pure Web Crypto zero-knowledge service
    │   ├── keyService.ts        # RSA-OAEP key pair generation, SPKI export, IndexedDB storage
    │   ├── encryptionService.ts # AES-256-GCM file encryption, IV generation, DEK wrapping
    │   └── decryptionService.ts # DEK unwrapping, AES-256-GCM payload decryption, Blob download
    ├── components/              # Shared UI components
    │   ├── Layout.tsx           # App shell with responsive sidebar & user profile
    │   ├── Card.tsx             # Surface container matching desktop Card component
    │   ├── Button.tsx           # Primary, secondary, and danger button variants
    │   ├── Table.tsx            # Accessible data table with pagination controls
    │   ├── Modal.tsx            # Dialog overlay for shares, key export, and alerts
    │   ├── Toast.tsx            # Global notification system
    │   └── ProgressBar.tsx      # Multi-step encryption & upload status bar
    ├── pages/                   # Application view controllers
    │   ├── LoginPage.tsx        # Login form + missing key check
    │   ├── RegisterPage.tsx     # Registration + initial key generation wizard
    │   ├── UploadPage.tsx       # Drag-and-drop file upload with recipient key selection
    │   ├── FilesPage.tsx        # File listing, download & decrypt, share creation
    │   ├── ShareAccessPage.tsx  # Public share token decryption & download portal
    │   ├── AuditPage.tsx        # Audit log table + interactive hash chain verification
    │   └── KeysPage.tsx         # Fingerprint inspection, private key backup & import
    └── state/
        └── authContext.tsx      # User session, JWT lifecycle, and auth state provider
```

---

## 3. Cryptographic & Security Architecture

### 3.1 Key Management Lifecycle
1. **Key Generation:**
   * Uses `crypto.subtle.generateKey` with algorithm `RSA-OAEP`, modulus length `4096` (or `2048`), public exponent `65537` (`[1, 0, 1]`), and `SHA-256` digest.
2. **Public Key Registration:**
   * Exported as `spki` binary, encoded to Base64, and published to the backend via `POST /api/v1/auth/keys`.
   * Stored in the backend `key_registry` table with its SHA-256 fingerprint for public recipient lookup.
3. **Private Key Storage:**
   * Stored in browser `IndexedDB` (`database: sfs_vault`, `store: private_keys`), keyed by username.
   * Never exposed to local storage or transmitted across HTTP boundaries.
4. **Key Backup & Portability:**
   * A dedicated "Backup Key" feature allows users to export an encrypted JSON/PEM file protected by a user passphrase (derived via PBKDF2), preventing data lockout if browser storage is cleared.

### 3.2 File Encryption & Upload Pipeline
```text
[Plaintext File]
       │
       ▼
1. Generate ephemeral 256-bit AES-GCM Data Encryption Key (DEK)
2. Generate random 96-bit (12-byte) IV
       │
       ├─────────────────────────────────────────┐
       ▼                                         ▼
3. Encrypt file payload via AES-256-GCM    4. Encrypt DEK with Recipient's
   (produces Ciphertext + 128-bit Tag)        RSA-OAEP Public Key (Wrapped DEK)
       │                                         │
5. Compute SHA-256 integrity hash                 │
       │                                         │
       └─────────────────┬───────────────────────┘
                         ▼
             POST /api/v1/files/upload
             (Payload: Ciphertext, Wrapped DEK, IV, SHA-256, Metadata)
```

### 3.3 File Download & Decryption Pipeline
```text
           GET /api/v1/files/{id}/download
                         │
                         ▼
             (Ciphertext, Wrapped DEK, IV)
                         │
       ┌─────────────────┴───────────────────────┐
       ▼                                         ▼
1. Fetch User Private Key                 2. Buffer Ciphertext & IV
   from browser IndexedDB                        │
       │                                         │
3. Decrypt Wrapped DEK via RSA-OAEP               │
   (recovers plaintext 256-bit DEK)              │
       │                                         │
       └─────────────────┬───────────────────────┘
                         ▼
4. Decrypt Ciphertext via AES-256-GCM (DEK + IV)
                         │
                         ▼
5. Generate in-memory Blob and trigger browser file save
```

---

## 4. UI Pages & Workflow Specifications

### 4.1 Authentication & Onboarding
* **`/login`:** Accepts username/password, submits to `POST /api/v1/auth/login`. On success, checks if the local IndexedDB contains the corresponding private key. If absent (e.g. new browser), displays a non-blocking prompt offering to import a backup key or register a new key pair.
* **`/register`:** Submits new account credentials to `POST /api/v1/auth/register`. Automatically triggers the in-browser Web Crypto key generator and uploads the public key before redirecting to the dashboard.

### 4.2 File Upload & Encryption (`/app/upload`)
* Interactive dropzone supporting drag-and-drop or file browsing.
* Recipient selection dropdown populated dynamically from `GET /api/v1/auth/keys`.
* Live progress indicator detailing:
  1. "Generating AES-256 session key..."
  2. "Encrypting ciphertext in browser..."
  3. "Calculating SHA-256 checksum..."
  4. "Uploading encrypted payload to secure vault..."

### 4.3 File Management & Sharing (`/app/files`)
* Table showing file metadata (filename, original size, uploaded timestamp, encryption algorithm).
* Single-click **Download & Decrypt**: Streams ciphertext, decrypts locally, and triggers browser file download.
* **Share Modal:** Generates an HMAC-protected share token via `POST /api/v1/shares` with configurable expiration and maximum access count. Displays a copyable link: `https://<server-ip>:8443/share/<token>`.
* **Revoke Share:** Invalidates an active share link via `DELETE /api/v1/shares/{id}`.

### 4.4 Public Share Access (`/share/:token`)
* Accessible by recipients without logging into the main dashboard.
* Verifies share validity with `GET /api/v1/shares/{token}`.
* Decrypts file payload using recipient's key or the embedded share secret and triggers browser download.

### 4.5 Audit Trail & Hash Chain Verification (`/app/audit`)
* Displays security audit events (`UPLOAD`, `DOWNLOAD`, `SHARE_CREATED`, `SHARE_REVOKED`, `KEY_REGISTERED`).
* Features an interactive **"Verify Audit Hash Chain"** button calling `GET /api/v1/audit/verify`. Displays a verified badge confirming the immutable append-only ledger has not been tampered with.

### 4.6 Key Vault Management (`/app/keys`)
* Displays the active public key algorithm, bit strength, and SHA-256 fingerprint.
* Provides **"Export Encrypted Key Backup"** and **"Import Key Backup"** actions.

---

## 5. Server Deployment & Caddy Integration

### 5.1 Static Asset Serving
Caddy serves the compiled single-page application directly from the host filesystem:
```text
Production Build:
web-app/ (source) ──[ npm run build ]──> web-app/dist/ ──[ Docker Volume Mount ]──> Caddy /var/www/html
```

### 5.2 `docker-compose.prod.yml` Updates
Mount the built `web-app/dist` folder into the existing `sfs_prod_caddy` container:
```yaml
services:
  caddy:
    # ...
    volumes:
      - ./Caddyfile:/etc/caddy/Caddyfile:ro
      - ./web-app/dist:/var/www/html:ro
      - caddy_data:/data
      - caddy_config:/config
```

### 5.3 `Caddyfile` Routing & SPA History Fallback
```caddy
:443 {
    tls internal

    # Mount static assets
    root * /var/www/html

    # Security Headers supporting modern SPA and Web Crypto downloads
    header {
        Strict-Transport-Security "max-age=31536000; includeSubDomains; preload"
        X-Content-Type-Options "nosniff"
        X-Frame-Options "DENY"
        Referrer-Policy "strict-origin-when-cross-origin"
        Content-Security-Policy "default-src 'self'; script-src 'self'; style-src 'self' 'unsafe-inline'; connect-src 'self'; img-src 'self' data: blob:; worker-src 'self' blob:;"
        -Server
    }

    # API Gateway Reverse Proxy (unchanged)
    handle /api/v1/auth* {
        reverse_proxy auth-service:8081 { ... }
    }
    handle /api/v1/files* {
        reverse_proxy file-service:8082 { ... }
    }
    handle /api/v1/shares* {
        reverse_proxy share-service:8083 { ... }
    }
    handle /api/v1/audit* {
        reverse_proxy audit-service:8084 { ... }
    }

    # SPA Client Routing: serve static file or fallback to index.html
    handle {
        try_files {path} /index.html
        file_server
    }
}
```

---

## 6. Verification & Quality Assurance Strategy

1. **Cryptographic Compatibility Verification:**
   * Verify that AES-256-GCM ciphertexts and RSA-OAEP wrapped keys generated by the web client can be decrypted seamlessly by the Java desktop client (and vice versa).
2. **Build Verification:**
   * Ensure `npm run build` succeeds cleanly with zero TypeScript errors or broken imports.
3. **End-to-End Functional Verification:**
   * Register a user via the web interface.
   * Upload an encrypted file in the web interface and verify successful storage in MinIO.
   * Download and decrypt the file in the browser, confirming identical SHA-256 hash to original file.
   * Confirm the event appears in the Audit Trail and the hash chain validates successfully.
