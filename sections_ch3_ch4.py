# -*- coding: utf-8 -*-
"""
sections_ch3_ch4.py
Chapter 3: System Design
Chapter 4: Project Management
"""

def get_ch3_ch4_html(img_client_arch_b64, img_topology_b64, img_er_b64, img_cicd_b64):
    template = """
<!-- ==================== CHAPTER 3: SYSTEM DESIGN ==================== -->
<div class="page-break">
    <div style="text-align: right; font-size: 10pt; font-style: italic;">29</div>
    <h1 class="chap-header">Chapter 3</h1>
    <h1 class="chap-title">SYSTEM DESIGN</h1>

    <h2 class="sec-title">3.1 System Architecture</h2>
    <p>SecureFileShare is architected as a distributed microservice ecosystem designed to enforce zero-knowledge cryptographic invariants across all boundaries. The system decouples frontend clients, edge routing, domain microservices, and storage subsystems into distinct architectural layers.</p>

    <div class="img-container">
        <img src="IMG_CLIENT_ARCH_TAG" alt="Figure 3.1: Client-Service Monorepo Architecture of SecureFileShare" style="max-width: 78%; height: auto; display: block; margin: 0 auto; border: 1px solid #ccc; border-radius: 4px;" />
    </div>
    <div class="figure-caption">Figure 3.1: Client-Service Monorepo Architecture of SecureFileShare</div>

    <div class="img-explanation">
        <strong>Detailed Technical Explanation of Figure 3.1:</strong><br>
        Figure 3.1 depicts the structural decomposition of the SecureFileShare Maven monorepo:
        <ul>
            <li><strong>Frontend Clients:</strong>
                <ul>
                    <li><code>client-app</code>: A native Java 21 desktop application built using FlatLaf dark look-and-feel. It executes client-side cryptographic routines via Bouncy Castle and interacts with backend microservices via HTTP APIs.</li>
                    <li><code>web-app</code>: A modern Single Page Application (SPA) built with React 18, TypeScript, and Vite. It utilizes the browser-native W3C Web Crypto API for hardware-accelerated in-memory cryptography.</li>
                </ul>
            </li>
            <li><strong>Application Microservices Layer:</strong>
                <ul>
                    <li><code>audit-service</code> (Port 8084): Maintains an append-only compliance audit trail linked by cryptographic SHA-256 hash chains.</li>
                    <li><code>auth-service</code> (Port 8081): Manages user identity, password hashing (BCrypt), JWT token issuance, the public key registry (RSA-4096 / EC P-256), and Redis-backed rate limiting.</li>
                    <li><code>file-service</code> (Port 8082): Ingests encrypted multipart packages, verifies SHA-256 checksums, validates ECDSA digital signatures, streams opaque ciphertexts to MinIO, and stores per-recipient wrapped keys.</li>
                    <li><code>share-service</code> (Port 8083): Governs expiring share links, cryptographic share tokens, and instantaneous link revocation.</li>
                </ul>
            </li>
            <li><strong>Shared Module:</strong>
                <ul>
                    <li><code>common</code>: A shared Java library providing standard domain DTOs (e.g., <code>ApiResponse</code>, <code>ErrorResponse</code>, <code>KeyDetailsDto</code>), security constants, and global exception handlers shared across all four backend services.</li>
                </ul>
            </li>
        </ul>
    </div>

    <p>To examine the internal execution topology, component delegation, and isolated data stores, Figure 3.2 illustrates the end-to-end component interaction model.</p>

    <div class="img-container">
        <img src="IMG_TOPOLOGY_TAG" alt="Figure 3.2: Detailed Microservice Component &amp; Storage Topology" style="max-width: 82%; height: auto; display: block; margin: 0 auto; border: 1px solid #ccc; border-radius: 4px;" />
    </div>
    <div class="figure-caption">Figure 3.2: Detailed Microservice Component &amp; Storage Topology</div>

    <div class="img-explanation">
        <strong>Detailed Technical Explanation of Figure 3.2:</strong><br>
        Figure 3.2 provides an in-depth view of internal service components and data stores:
        <ul>
            <li><strong>User Clients:</strong>
                <ul>
                    <li><em>Web App Router (React SPA):</em> Routes user actions to local encryption components powered by <code>Web Crypto API</code>.</li>
                    <li><em>Desktop Client (Java):</em> Routes actions to the client-side <code>CryptoEngine</code> powered by Bouncy Castle.</li>
                </ul>
            </li>
            <li><strong>Encrypted Files (<code>file-service :8082</code>):</strong>
                <ul>
                    <li>File endpoints delegate requests to <code>FileService.java</code>.</li>
                    <li>The <em>Ciphertext Storage</em> component streams opaque binary objects directly to <strong>MinIO (Object Storage)</strong>.</li>
                    <li>The <em>Signature Checks</em> component verifies ECDSA digital signatures against sender public keys.</li>
                    <li>The <em>File Metadata</em> component persists file records and wrapped keys into isolated PostgreSQL databases.</li>
                </ul>
            </li>
            <li><strong>Audit Records (<code>audit-service :8084</code>):</strong>
                <ul>
                    <li>Audit endpoints delegate to <code>AuditService.java</code>, which computes sequential SHA-256 hash chains across all security events and stores them in <strong>PostgreSQL</strong>.</li>
                </ul>
            </li>
            <li><strong>Share Links (<code>share-service :8083</code>):</strong>
                <ul>
                    <li>Share endpoints delegate to <code>ShareService.java</code>, managing share tokens, expiry, and revocation in PostgreSQL.</li>
                </ul>
            </li>
            <li><strong>Identity &amp; Keys (<code>auth-service :8081</code>):</strong>
                <ul>
                    <li>The <em>Public Key Registry</em> validates keys via <code>KeyValidator.java</code> (RSA-4096, EC P-256) and stores public keys.</li>
                    <li>The <em>Account Authentication</em> component (<code>AuthService.java</code>) issues JWT tokens via <code>JwtService.java</code>, reads/writes user records in PostgreSQL, and enforces token-bucket rate limiting via <strong>Redis 7</strong>.</li>
                </ul>
            </li>
        </ul>
    </div>

    <div class="table-caption">Table 3.1: Communication Flow Between System Layers of SecureFileShare</div>
    <table class="report-table">
        <thead>
            <tr>
                <th style="width: 10%;">Step</th>
                <th style="width: 25%;">From</th>
                <th style="width: 25%;">To</th>
                <th style="width: 40%; text-align: left;">Description of Communication</th>
            </tr>
        </thead>
        <tbody>
            <tr><td style="text-align: center;">1</td><td>User Client (Web/Desktop)</td><td>Caddy Reverse Proxy</td><td>Client transmits HTTPS request with JWT authorization header.</td></tr>
            <tr><td style="text-align: center;">2</td><td>Caddy Gateway</td><td>Target Microservice</td><td>Caddy routes request to port 8081, 8082, 8083, or 8084.</td></tr>
            <tr><td style="text-align: center;">3</td><td><code>file-service</code></td><td><code>auth-service</code></td><td>Resolves recipient public keys for key wrapping validation.</td></tr>
            <tr><td style="text-align: center;">4</td><td><code>file-service</code></td><td>MinIO S3 Storage</td><td>Streams opaque encrypted ciphertext directly to S3 bucket.</td></tr>
            <tr><td style="text-align: center;">5</td><td>Microservices</td><td>PostgreSQL Databases</td><td>Executes ACID relational transactions via Spring Data JPA.</td></tr>
            <tr><td style="text-align: center;">6</td><td><code>auth-service</code></td><td>Redis Cache</td><td>Checks rate limits and verifies token is not in revocation blacklist.</td></tr>
            <tr><td style="text-align: center;">7</td><td>Microservices</td><td><code>audit-service</code></td><td>Asynchronously dispatches security and data lifecycle events.</td></tr>
            <tr><td style="text-align: center;">8</td><td><code>audit-service</code></td><td>PostgreSQL (<code>audit_db</code>)</td><td>Appends event, updates SHA-256 hash chain under table lock.</td></tr>
        </tbody>
    </table>

    <h2 class="sec-title">3.2 Entity Relationship (ER) Model</h2>
    <p>The Entity Relationship (ER) Model defines the relational data structures and foreign key associations across the platform&rsquo;s isolated databases.</p>

    <div class="img-container">
        <img src="IMG_ER_TAG" alt="Figure 3.3: Database Entity Relationship (ER) Diagram" style="max-width: 65%; height: auto; display: block; margin: 0 auto; border: 1px solid #ccc; border-radius: 4px;" />
    </div>
    <div class="figure-caption">Figure 3.3: Database Entity Relationship (ER) Diagram</div>

    <div class="img-explanation">
        <strong>Detailed Technical Explanation of Figure 3.3:</strong><br>
        Figure 3.3 displays the database schema entities and their cardinalities:
        <ul>
            <li><strong>USERS:</strong> Stores user identity and public cryptographic credentials. Attributes: <code>id</code> (UUID, PK), <code>username</code> (string, UK), <code>email</code> (string, UK), <code>password_hash</code> (string), <code>public_key_rsa</code> (string), <code>public_key_ec</code> (string), <code>created_at</code> (timestamp).</li>
            <li><strong>FILE_RECORDS:</strong> Stores metadata for encrypted file packages. Attributes: <code>id</code> (UUID, PK), <code>owner_id</code> (UUID, FK referencing USERS), <code>file_name</code> (string), <code>ciphertext_path</code> (string), <code>wrapped_key</code> (string), <code>signature</code> (string), <code>size</code> (BIGINT), <code>uploaded_at</code> (timestamp). Cardinality: One user <em>owns</em> many file records (1:N).</li>
            <li><strong>SHARE_RECORDS:</strong> Stores access grants and share links. Attributes: <code>id</code> (UUID, PK), <code>file_id</code> (UUID, FK referencing FILE_RECORDS), <code>recipient_id</code> (UUID, FK referencing USERS), <code>share_token</code> (string), <code>expires_at</code> (timestamp), <code>is_revoked</code> (boolean). Cardinality: One file record is <em>shared via</em> many share records (1:N).</li>
            <li><strong>AUDIT_RECORDS:</strong> Stores append-only compliance logs. Attributes: <code>id</code> (UUID/BIGSERIAL, PK), <code>actor_id</code> (UUID), <code>action</code> (string), <code>resource_id</code> (string), <code>prev_hash</code> (string), <code>current_hash</code> (string), <code>created_at</code> (timestamp). Each entry stores <code>prev_hash</code>, forming a tamper-evident cryptographic chain.</li>
        </ul>
    </div>

    <div class="table-caption">Table 3.2: Entities and Attributes of SecureFileShare System</div>
    <table class="report-table">
        <thead>
            <tr>
                <th style="width: 18%;">Entity Name</th>
                <th style="width: 47%;">Key Attributes</th>
                <th style="width: 35%; text-align: left;">Description</th>
            </tr>
        </thead>
        <tbody>
            <tr><td><code>USERS</code></td><td><code>id (PK)</code>, <code>username</code>, <code>email</code>, <code>password_hash</code>, <code>public_key_rsa</code>, <code>public_key_ec</code></td><td>Stores user credentials and registered public keys.</td></tr>
            <tr><td><code>KEY_REGISTRY</code></td><td><code>id (PK)</code>, <code>user_id (FK)</code>, <code>rsa_public_key</code>, <code>ec_public_key</code>, <code>rsa_fingerprint</code>, <code>revoked</code></td><td>Maintains active and historical rotated public keys.</td></tr>
            <tr><td><code>FILES</code></td><td><code>id (PK)</code>, <code>owner_id</code>, <code>original_name</code>, <code>ciphertext_sha256</code>, <code>iv</code>, <code>ecdsa_signature</code>, <code>object_key</code></td><td>Tracks encrypted file packages and MinIO S3 object paths.</td></tr>
            <tr><td><code>FILE_KEYS</code></td><td><code>id (PK)</code>, <code>file_id (FK)</code>, <code>recipient_id</code>, <code>wrapped_key</code></td><td>Stores AES symmetric keys wrapped with recipient RSA keys.</td></tr>
            <tr><td><code>SHARES</code></td><td><code>id (PK)</code>, <code>file_id</code>, <code>owner_id</code>, <code>recipient_username</code>, <code>share_token</code>, <code>expires_at</code>, <code>status</code></td><td>Governs expiring and revocable file share links.</td></tr>
            <tr><td><code>AUDIT_ENTRIES</code></td><td><code>id (PK)</code>, <code>timestamp</code>, <code>actor</code>, <code>action</code>, <code>file_name</code>, <code>hash</code>, <code>previous_hash</code></td><td>Append-only, SHA-256 hash-chained compliance audit log.</td></tr>
        </tbody>
    </table>

    <h2 class="sec-title">3.3 Database Design</h2>
    <p>The database architecture enforces microservice isolation through separate PostgreSQL database instances (<code>auth_db</code>, <code>file_db</code>, <code>share_db</code>, <code>audit_db</code>), each managed via automated Flyway migrations.</p>

    <div class="table-caption">Table 3.3: Database Tables and Structure Specifications</div>
    <table class="report-table">
        <thead>
            <tr>
                <th style="width: 20%;">Database &amp; Table</th>
                <th style="width: 45%;">Field Specifications</th>
                <th style="width: 35%; text-align: left;">Constraints &amp; Indexes</th>
            </tr>
        </thead>
        <tbody>
            <tr>
                <td><code>auth_db.users</code></td>
                <td><code>id UUID</code>, <code>username VARCHAR(50)</code>, <code>email VARCHAR(255)</code>, <code>password_hash VARCHAR(255)</code>, <code>role VARCHAR(30)</code></td>
                <td><code>PK(id)</code>, <code>UK(username)</code>, <code>UK(email)</code>, <code>INDEX(username)</code></td>
            </tr>
            <tr>
                <td><code>auth_db.key_registry</code></td>
                <td><code>id UUID</code>, <code>user_id UUID</code>, <code>rsa_public_key TEXT</code>, <code>ec_public_key TEXT</code>, <code>rsa_fingerprint VARCHAR(64)</code>, <code>revoked BOOLEAN</code></td>
                <td><code>PK(id)</code>, <code>FK(user_id -> users.id)</code>, <code>INDEX(user_id, revoked)</code></td>
            </tr>
            <tr>
                <td><code>file_db.files</code></td>
                <td><code>id UUID</code>, <code>owner_id UUID</code>, <code>original_name VARCHAR(255)</code>, <code>size_bytes BIGINT</code>, <code>ciphertext_sha256 VARCHAR(64)</code>, <code>iv VARCHAR(255)</code>, <code>object_key VARCHAR(255)</code></td>
                <td><code>PK(id)</code>, <code>UK(object_key)</code>, <code>INDEX(owner_id)</code>, <code>INDEX(status)</code></td>
            </tr>
            <tr>
                <td><code>file_db.file_keys</code></td>
                <td><code>id UUID</code>, <code>file_id UUID</code>, <code>recipient_id UUID</code>, <code>wrapped_key TEXT</code></td>
                <td><code>PK(id)</code>, <code>FK(file_id -> files.id)</code>, <code>UK(file_id, recipient_id)</code></td>
            </tr>
            <tr>
                <td><code>share_db.shares</code></td>
                <td><code>id VARCHAR(64)</code>, <code>file_id UUID</code>, <code>owner_id UUID</code>, <code>share_token VARCHAR(255)</code>, <code>expires_at TIMESTAMPTZ</code>, <code>status VARCHAR(32)</code></td>
                <td><code>PK(id)</code>, <code>UK(share_token)</code>, <code>INDEX(share_token)</code>, <code>INDEX(owner_id)</code></td>
            </tr>
            <tr>
                <td><code>audit_db.audit_entries</code></td>
                <td><code>id BIGSERIAL</code>, <code>timestamp TIMESTAMPTZ</code>, <code>actor VARCHAR(255)</code>, <code>action VARCHAR(64)</code>, <code>hash VARCHAR(64)</code>, <code>previous_hash VARCHAR(64)</code></td>
                <td><code>PK(id)</code>, <code>INDEX(timestamp DESC)</code>, <code>INDEX(action)</code></td>
            </tr>
        </tbody>
    </table>

    <h2 class="sec-title">3.4 Modularization Details</h2>
    <p>The system is cleanly divided into functional modules to maximize maintainability, facilitate unit testing, and isolate operational concerns:</p>
    <ul>
        <li><strong>Authentication Module (<code>auth-service</code>):</strong> Enforces user registration, BCrypt credential hashing, stateless JWT issuance, and Redis-backed rate limiting.</li>
        <li><strong>Key Registry Module:</strong> Validates and stores PEM-encoded RSA-4096 and NIST P-256 public keys and computes SHA-256 key fingerprints.</li>
        <li><strong>File Ingestion Module (<code>file-service</code>):</strong> Streams encrypted multipart payloads into MinIO S3 object storage without holding plaintext in JVM heap memory.</li>
        <li><strong>Signature &amp; Integrity Verification Module:</strong> Computes SHA-256 checksums across incoming streams and verifies NIST P-256 ECDSA digital signatures.</li>
        <li><strong>Sharing &amp; Access Control Module (<code>share-service</code>):</strong> Generates cryptographic share tokens, enforces TTL expiration, and supports instantaneous owner revocation.</li>
        <li><strong>Tamper-Evident Audit Module (<code>audit-service</code>):</strong> Maintains an append-only log with SHA-256 hash chaining and provides full-chain integrity verification.</li>
        <li><strong>Client Cryptography Modules:</strong> Implements AES-256-GCM encryption, RSA-4096-OAEP key wrapping, and ECDSA signing via Web Crypto in the web client and Bouncy Castle in the desktop client.</li>
    </ul>

    <h2 class="sec-title">3.5 Algorithm Design</h2>
    <h3 class="subsec-title">Algorithm 1: Client-Side AES-256-GCM File Encryption</h3>
    <pre class="code-block">
Input: Plaintext file bytes P, User RSA Public Key K_pub_rsa, User EC Private Key K_priv_ec
Output: Ciphertext C, IV, Wrapped Key K_wrapped, Signature Sig, Checksum H

1: Generate single-use 256-bit symmetric AES key: K_aes &lt;- SecureRandom(32 bytes)
2: Generate 96-bit Initialization Vector: IV &lt;- SecureRandom(12 bytes)
3: Encrypt plaintext using AES-256 in Galois/Counter Mode:
      (C, Tag) &lt;- AES_256_GCM_Encrypt(Key = K_aes, IV = IV, Plaintext = P)
4: Compute SHA-256 integrity hash:
      H &lt;- SHA_256(C)
5: Sign hash using NIST P-256 private key:
      Sig &lt;- ECDSA_Sign(Key = K_priv_ec, Data = H)
6: Wrap symmetric AES key with RSA-4096 public key using OAEP padding:
      K_wrapped &lt;- RSA_OAEP_Encrypt(Key = K_pub_rsa, Plaintext = K_aes)
7: Return payload package with Ciphertext, IV, WrappedKey, Signature, and Checksum.
    </pre>

    <h3 class="subsec-title">Algorithm 2: Server-Side Ingestion &amp; Signature Verification</h3>
    <pre class="code-block">
Input: Incoming multipart package, Sender User ID, Authorization Header (JWT)
Output: HTTP 201 Created or HTTP 400 Bad Request

1: Validate JWT signature and expiration; extract Sender ID.
2: Retrieve sender's active NIST P-256 public key K_pub_ec from key_registry.
3: Compute SHA-256 hash of incoming ciphertext stream: H_computed &lt;- SHA_256(Stream(C)).
4: If H_computed != H_provided: Abort with HTTP 400 (Checksum Mismatch).
5: Verify digital signature: Valid &lt;- ECDSA_Verify(Key = K_pub_ec, Data = H_computed, Sig).
6: If Valid == False: Abort with HTTP 400 (Invalid Cryptographic Signature).
7: Stream opaque ciphertext directly into MinIO S3 bucket under UUID object key.
8: Persist metadata in files table and wrapped key in file_keys table.
9: Disseminate asynchronous UPLOAD event to audit-service.
10: Return HTTP 201 Created with File UUID.
    </pre>

    <h3 class="subsec-title">Algorithm 3: Append-Only Audit Hash Chain Verification</h3>
    <pre class="code-block">
Input: All records in audit_entries table ordered by id ASC
Output: AuditVerifyResult [ Valid: Boolean, CheckedCount: Integer, BrokenId: Long ]

1: Set ExpectedPrevHash &lt;- GenesisHash ("0000000000000000000000000000000000000000000000000000000000000000")
2: For each record R in audit_entries:
      If R.previous_hash != ExpectedPrevHash:
          Return [ Valid: False, CheckedCount: Checked, BrokenId: R.id, Reason: "Chain link mismatch" ]
      RecomputedHash &lt;- SHA_256(R.previous_hash + "|" + R.timestamp + "|" + R.actor + "|" + R.action + "|" + R.file_name)
      If RecomputedHash != R.hash:
          Return [ Valid: False, CheckedCount: Checked, BrokenId: R.id, Reason: "Row content altered" ]
      ExpectedPrevHash &lt;- R.hash
      Checked &lt;- Checked + 1
3: Return [ Valid: True, CheckedCount: Checked, BrokenId: null, Reason: "Chain integrity verified" ]
    </pre>

    <h2 class="sec-title">3.6 User Interface Design</h2>
    <div class="table-caption">Table 3.4: User Interface Components and Description</div>
    <table class="report-table">
        <thead>
            <tr>
                <th style="width: 25%;">Component Screen</th>
                <th style="width: 35%;">Client Framework</th>
                <th style="width: 40%; text-align: left;">Primary Operational Function</th>
            </tr>
        </thead>
        <tbody>
            <tr><td><code>LoginPage</code> / <code>LoginPanel</code></td><td>React SPA / Java Swing</td><td>Authenticates credentials, retrieves JWT, and initializes cryptographic session.</td></tr>
            <tr><td><code>RegisterPage</code> / <code>RegisterPanel</code></td><td>React SPA / Java Swing</td><td>Captures user details and generates RSA-4096 / NIST P-256 key pairs locally.</td></tr>
            <tr><td><code>FilesPage</code> / <code>MyFilesPanel</code></td><td>React SPA / Java Swing</td><td>Displays encrypted personal vault with file metadata, download, and share actions.</td></tr>
            <tr><td><code>UploadPage</code> / <code>UploadDialog</code></td><td>React SPA / Java Swing</td><td>Orchestrates client-side AES encryption, RSA key wrapping, and streaming upload.</td></tr>
            <tr><td><code>SharesPage</code> / <code>SharesPanel</code></td><td>React SPA / Java Swing</td><td>Manages outgoing and incoming share links with real-time revocation controls.</td></tr>
            <tr><td><code>KeysPage</code> / <code>MyKeysPanel</code></td><td>React SPA / Java Swing</td><td>Displays active key fingerprints, public keys, and passphrase-protected backup tools.</td></tr>
            <tr><td><code>AuditPage</code> / <code>AuditPanel</code></td><td>React SPA / Java Swing</td><td>Renders the compliance event log and executes full hash-chain integrity verification.</td></tr>
        </tbody>
    </table>

    <h2 class="sec-title">3.7 Data Integrity &amp; Constraints</h2>
    <p>Data integrity is enforced across four distinct tiers:</p>
    <ul>
        <li><strong>Entity Integrity:</strong> Enforced through primary keys utilizing randomly generated UUIDs (<code>gen_random_uuid()</code>) across all entities, preventing enumeration exploits.</li>
        <li><strong>Referential Integrity:</strong> Enforced through relational foreign key constraints with cascading deletions (e.g., deleting a file cascades to purge its wrapped keys from <code>file_keys</code>).</li>
        <li><strong>Domain Integrity:</strong> Strictly typed columns restricting statuses to approved enumeration values (<code>CLEAN</code>, <code>PENDING_SCAN</code>, <code>QUARANTINED</code>) and hashes to exact 64-character hexadecimal strings.</li>
        <li><strong>Cryptographic Integrity:</strong> AES-256-GCM authentication tags guarantee that altered ciphertexts fail decryption; ECDSA signatures guarantee non-repudiation; SHA-256 audit chaining ensures historical immutability.</li>
    </ul>

    <h2 class="sec-title">3.8 Software Engineering Paradigm</h2>
    <p>The project follows a <strong>Hybrid Software Engineering Methodology</strong> combining Waterfall planning with Iterative development:</p>
    <ul>
        <li><strong>Waterfall Phase:</strong> Requirements definition, cryptographic protocol modeling, and relational schema normalization were completed and frozen before coding.</li>
        <li><strong>Iterative Phase:</strong> Microservices were implemented across iterative sprints. Each sprint produced working code, automated JUnit 5 and Vitest test suites, and Docker container verification before proceeding to the next service.</li>
    </ul>
</div>

<!-- ==================== CHAPTER 4: PROJECT MANAGEMENT ==================== -->
<div class="page-break">
    <div style="text-align: right; font-size: 10pt; font-style: italic;">55</div>
    <h1 class="chap-header">Chapter 4</h1>
    <h1 class="chap-title">PROJECT MANAGEMENT</h1>

    <h2 class="sec-title">4.1 Project Planning &amp; Scheduling</h2>
    <p>Project management for SecureFileShare followed a structured 8-week lifecycle. A formal Work Breakdown Structure (WBS) partitioned engineering activities across eight distinct phases.</p>

    <div class="table-caption">Table 4.1: Work Breakdown Structure (WBS)</div>
    <table class="report-table">
        <thead>
            <tr>
                <th style="width: 15%;">Phase</th>
                <th style="width: 35%;">Task Name</th>
                <th style="width: 50%; text-align: left;">Detailed Description &amp; Engineering Focus</th>
            </tr>
        </thead>
        <tbody>
            <tr><td style="text-align: center;">Phase 1</td><td>Requirements &amp; Threat Modeling</td><td>Zero-knowledge constraint definition, cryptographic algorithm selection.</td></tr>
            <tr><td style="text-align: center;">Phase 2</td><td>Architecture &amp; Database Design</td><td>Maven multi-module scaffolding, Flyway migrations, PostgreSQL schema design.</td></tr>
            <tr><td style="text-align: center;">Phase 3</td><td>Auth Service &amp; Key Registry</td><td>JWT authentication filters, BCrypt hashing, public key registry, Redis rate limiting.</td></tr>
            <tr><td style="text-align: center;">Phase 4</td><td>File Service &amp; MinIO Streaming</td><td>Multipart upload pipelines, SHA-256 verification, ECDSA signature validation.</td></tr>
            <tr><td style="text-align: center;">Phase 5</td><td>Share Service &amp; Access Control</td><td>Expiring share tokens, recipient binding, owner revocation workflows.</td></tr>
            <tr><td style="text-align: center;">Phase 6</td><td>Audit Service &amp; Hash Chaining</td><td>Append-only event ingestion, SHA-256 hash chaining, admin verification API.</td></tr>
            <tr><td style="text-align: center;">Phase 7</td><td>Dual Client Development</td><td>React 18 Web Crypto SPA and Java 21 Swing Bouncy Castle desktop application.</td></tr>
            <tr><td style="text-align: center;">Phase 8</td><td>CI/CD, Security Hardening &amp; Deploy</td><td>GitHub Actions automation, Gitleaks, OWASP scanning, Caddy TLS deployment.</td></tr>
        </tbody>
    </table>

    <div class="table-caption">Table 4.2: Project Timeline (8-Week Lifecycle)</div>
    <table class="report-table">
        <thead>
            <tr>
                <th style="width: 15%;">Week</th>
                <th style="width: 45%;">Major Development Activity</th>
                <th style="width: 40%; text-align: left;">Expected Output Milestone</th>
            </tr>
        </thead>
        <tbody>
            <tr><td style="text-align: center;">Week 1</td><td>Requirement Analysis &amp; Crypto Modeling</td><td>Approved SRS, cryptographic protocol specification.</td></tr>
            <tr><td style="text-align: center;">Week 2</td><td>System Architecture &amp; Database Design</td><td>Parent POM, Flyway schemas, database migrations verified.</td></tr>
            <tr><td style="text-align: center;">Week 3</td><td>Auth Service &amp; Key Registry Implementation</td><td>Working auth-service, JWT issuance, public key storage.</td></tr>
            <tr><td style="text-align: center;">Week 4</td><td>File Service &amp; MinIO Object Streaming</td><td>Working file-service, MinIO upload/download, signature check.</td></tr>
            <tr><td style="text-align: center;">Week 5</td><td>Share Service &amp; Audit Hash Chain</td><td>Working share-service and append-only audit-service.</td></tr>
            <tr><td style="text-align: center;">Week 6</td><td>Frontend Web Application &amp; Web Crypto</td><td>React SPA with in-browser AES-GCM and IndexedDB key vault.</td></tr>
            <tr><td style="text-align: center;">Week 7</td><td>Desktop Client &amp; Integration Testing</td><td>Java Swing client with Bouncy Castle; integration test passes.</td></tr>
            <tr><td style="text-align: center;">Week 8</td><td>CI/CD Pipelines, Security Audit &amp; Deployment</td><td>Live production deployment behind Caddy TLS proxy.</td></tr>
        </tbody>
    </table>

    <h2 class="sec-title">4.2 Milestones &amp; Deliverables</h2>
    <div class="table-caption">Table 4.3: Project Milestones &amp; Target Deliverables</div>
    <table class="report-table">
        <thead>
            <tr>
                <th style="width: 15%;">Milestone</th>
                <th style="width: 40%;">Description</th>
                <th style="width: 45%; text-align: left;">Verification Output</th>
            </tr>
        </thead>
        <tbody>
            <tr><td style="text-align: center;">M1</td><td>Cryptographic Specification</td><td>Zero-knowledge mathematical flow approved.</td></tr>
            <tr><td style="text-align: center;">M2</td><td>Database &amp; Schema Scaffolding</td><td>Flyway migrations apply cleanly to 4 databases.</td></tr>
            <tr><td style="text-align: center;">M3</td><td>Authentication &amp; Key Registry</td><td>Users register, authenticate, store RSA/EC public keys.</td></tr>
            <tr><td style="text-align: center;">M4</td><td>Zero-Knowledge File Ingestion</td><td>Ciphertexts stream to MinIO; ECDSA signatures verified.</td></tr>
            <tr><td style="text-align: center;">M5</td><td>Share Links &amp; Hash Chain</td><td>Share links expire and revoke; audit chain verifies intact.</td></tr>
            <tr><td style="text-align: center;">M6</td><td>Dual-Client Crypto Operation</td><td>Web and desktop clients interoperate seamlessly.</td></tr>
            <tr><td style="text-align: center;">M7</td><td>Automated CI/CD Deployment</td><td>Gitleaks, Maven verify, and Docker push execute cleanly.</td></tr>
        </tbody>
    </table>

    <h2 class="sec-title">4.3 Risk Management</h2>
    <div class="table-caption">Table 4.5: Risk Analysis &amp; Impact Matrix</div>
    <table class="report-table">
        <thead>
            <tr>
                <th style="width: 20%;">Identified Risk</th>
                <th style="width: 15%;">Impact</th>
                <th style="width: 15%;">Probability</th>
                <th style="width: 50%; text-align: left;">Mitigation Strategy Enforced</th>
            </tr>
        </thead>
        <tbody>
            <tr><td>Plaintext Leakage to Server</td><td>Critical</td><td>Low</td><td>Strict DTO validation rejecting unencrypted fields; client-only keys.</td></tr>
            <tr><td>Cross-Client Crypto Drift</td><td>High</td><td>Medium</td><td>Shared test vectors validating Web Crypto against Bouncy Castle.</td></tr>
            <tr><td>Audit Hash Chain Drift</td><td>High</td><td>Medium</td><td>Pessimistic locking (<code>SELECT ... FOR UPDATE</code>) on append.</td></tr>
            <tr><td>Upload Memory Exhaustion</td><td>High</td><td>Medium</td><td>Direct reactive streaming to MinIO S3 without JVM heap buffering.</td></tr>
            <tr><td>Brute-Force Token Guessing</td><td>High</td><td>Low</td><td>128-bit high-entropy cryptographic random share tokens.</td></tr>
            <tr><td>Credential Leakage in Git</td><td>High</td><td>Low</td><td>Automated Gitleaks secret scanning on every git push via CI.</td></tr>
        </tbody>
    </table>

    <h2 class="sec-title">4.4 Cost Estimation</h2>
    <div class="table-caption">Table 4.6: Cost Estimation of the Project</div>
    <table class="report-table">
        <thead>
            <tr>
                <th style="width: 30%;">Component Category</th>
                <th style="width: 45%;">Selected Technology &amp; Tooling</th>
                <th style="width: 25%; text-align: center;">Estimated Cost</th>
            </tr>
        </thead>
        <tbody>
            <tr><td>Hardware &amp; Workstation</td><td>Existing Development PC / Laptop</td><td style="text-align: center;">₹0 (Owned)</td></tr>
            <tr><td>Backend Framework</td><td>Java 21 LTS &amp; Spring Boot 3.3.4 (Open-Source)</td><td style="text-align: center;">₹0</td></tr>
            <tr><td>Database &amp; Caching</td><td>PostgreSQL 16 &amp; Redis 7 (Open-Source)</td><td style="text-align: center;">₹0</td></tr>
            <tr><td>Object Storage</td><td>MinIO S3 Compatible Community Edition</td><td style="text-align: center;">₹0</td></tr>
            <tr><td>Frontend &amp; Desktop</td><td>React 18, Vite, Tailwind CSS, Java Swing FlatLaf</td><td style="text-align: center;">₹0</td></tr>
            <tr><td>Cloud Infrastructure</td><td>Oracle Cloud Infrastructure Always-Free Tier</td><td style="text-align: center;">₹0</td></tr>
            <tr><td>SSL/TLS Certificates</td><td>Automated Let&rsquo;s Encrypt via Caddy</td><td style="text-align: center;">₹0</td></tr>
            <tr><td>CI/CD Automation</td><td>GitHub Actions Public Repository Runners</td><td style="text-align: center;">₹0</td></tr>
            <tr style="font-weight: bold; background-color: #f0f4f8;"><td>TOTAL PROJECT EXPENSE</td><td>Full-Stack Zero-Knowledge Platform</td><td style="text-align: center;">₹0 (Zero Cost)</td></tr>
        </tbody>
    </table>

    <h2 class="sec-title">4.5 PERT Chart &amp; Critical Path</h2>
    <p>The Program Evaluation and Review Technique (PERT) sequence identifies the critical path: <code>Requirements (1W) -> System Design (1W) -> Database Migrations (1W) -> Backend File Service (1.5W) -> Client Cryptography &amp; Web App (2W) -> Testing (1W) -> Production Deployment (0.5W)</code>. Total project duration: <strong>8 Weeks</strong>.</p>

    <h2 class="sec-title">4.6 Gantt Chart</h2>
    <p>The Gantt schedule maps parallel activities: Auth and File microservices were engineered concurrently during Weeks 3 and 4; Web and Desktop client interfaces were built concurrently in Weeks 5 and 6, followed by integration testing and deployment.</p>

    <h2 class="sec-title">4.7 Continuous Integration &amp; Deployment Pipeline</h2>
    <p>The deployment pipeline automates validation, building, containerization, and cloud deployment via GitHub Actions.</p>

    <div class="img-container">
        <img src="IMG_CICD_TAG" alt="Figure 4.1: Continuous Integration and Continuous Deployment (CI/CD) Pipeline" style="max-width: 48%; height: auto; display: block; margin: 0 auto; border: 1px solid #ccc; border-radius: 4px;" />
    </div>
    <div class="figure-caption">Figure 4.1: Continuous Integration and Continuous Deployment (CI/CD) Pipeline</div>

    <div class="img-explanation">
        <strong>Detailed Technical Explanation of Figure 4.1:</strong><br>
        Figure 4.1 illustrates the two-stage CI/CD pipeline powering SecureFileShare:
        <ul>
            <li><strong>Continuous Integration (<code>ci.yml</code> - Top Box):</strong>
                <ol>
                    <li><em>Push/PR to main/develop:</em> Workflow triggers automatically on every code push.</li>
                    <li><em>Gitleaks Secret Scan:</em> Scans git history to detect hardcoded secrets or credentials.</li>
                    <li><em>Setup Java 21 + Maven Cache:</em> Configures Temurin JDK 21 and caches dependencies.</li>
                    <li><em>./mvnw -B verify:</em> Compiles all microservices and runs all JUnit 5 unit and integration tests.</li>
                    <li><em>OWASP Dependency Check:</em> Audits dependencies against the NVD database for vulnerabilities.</li>
                    <li><em>Archive Surefire &amp; OWASP Reports:</em> Saves test and security artifacts for auditing.</li>
                </ol>
            </li>
            <li><strong>Continuous Deployment (<code>deploy.yml</code> - Bottom Box):</strong>
                <ol>
                    <li><em>Merge to main:</em> Deployment triggers on verified merges to the release branch.</li>
                    <li><em>Build Multi-stage linux/arm64 Images:</em> Compiles minimal container images.</li>
                    <li><em>QEMU + Docker Buildx:</em> Cross-compiles images for ARM64 cloud architecture.</li>
                    <li><em>Push to GHCR SHA &amp; latest:</em> Publishes tagged container images to GitHub Container Registry.</li>
                    <li><em>SSH to Oracle Cloud VM:</em> Connects securely to the production host.</li>
                    <li><em>Copies docker-compose.prod.yml &amp; Caddyfile:</em> Synchronizes deployment configurations.</li>
                    <li><em>docker compose pull &amp; up -d:</em> Pulls fresh images and restarts containers with minimal downtime.</li>
                    <li><em>Health Check (:8081 - :8084):</em> Automated health probes verify all microservice endpoints. If <strong>Pass</strong>, marks <em>Deployment Success</em>. If <strong>Fail</strong>, automatically triggers <em>Auto-Rollback to previous Commit SHA</em>.</li>
                </ol>
            </li>
        </ul>
    </div>
</div>
"""
    return template.replace("IMG_CLIENT_ARCH_TAG", img_client_arch_b64).replace("IMG_TOPOLOGY_TAG", img_topology_b64).replace("IMG_ER_TAG", img_er_b64).replace("IMG_CICD_TAG", img_cicd_b64)
