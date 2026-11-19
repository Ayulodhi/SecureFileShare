# -*- coding: utf-8 -*-
"""
sections_ch5_ch8.py
Chapter 5: Input Design
Chapter 6: Output Design
Chapter 7: Testing & Quality Assurance
Chapter 8: Summary & Future Scope
References
"""

def get_ch5_ch8_html():
    return """
<!-- ==================== CHAPTER 5: INPUT DESIGN ==================== -->
<div class="page-break">
    <div style="text-align: right; font-size: 10pt; font-style: italic;">72</div>
    <h1 class="chap-header">Chapter 5</h1>
    <h1 class="chap-title">INPUT DESIGN</h1>

    <h2 class="sec-title">5.1 User Registration &amp; Key Pair Generation Form</h2>
    <p>The User Registration Form is the initial entry point for onboarding new users onto SecureFileShare. Unlike traditional web registration portals that collect only identity credentials, SecureFileShare integrates cryptographic key pair generation directly into the client registration lifecycle.</p>

    <h3 class="subsec-title">Form Data Fields:</h3>
    <ul>
        <li><strong>Username:</strong> Unique alphanumeric identifier (3 to 50 characters, regex: <code>^[a-zA-Z0-9_-]+$</code>).</li>
        <li><strong>Email Address:</strong> Validated RFC 5322 email string.</li>
        <li><strong>Master Password:</strong> Minimum 10 characters requiring uppercase, lowercase, numeric, and special characters.</li>
        <li><strong>Role Selection:</strong> Default: <code>ROLE_USER</code>. Administrative provisioning restricted to verified invite tokens.</li>
    </ul>

    <h3 class="subsec-title">Working Mechanism &amp; Validation:</h3>
    <p>When the user submits the registration form, the client browser invokes the W3C Web Crypto API to generate two key pairs: an <strong>RSA-4096</strong> key pair (with exponent 65537 and SHA-256) for asymmetric key wrapping, and a <strong>NIST P-256 (secp256r1)</strong> elliptic curve key pair for digital signatures. The private keys are stored within the client&rsquo;s isolated IndexedDB store, while public keys are exported to PEM format, hashed to produce SHA-256 fingerprints, and submitted alongside credentials to <code>auth-service</code>.</p>

    <h2 class="sec-title">5.2 Zero-Knowledge File Upload &amp; Encryption Interface</h2>
    <p>The File Upload Interface manages local file selection and coordinates client-side authenticated encryption prior to network transmission.</p>

    <h3 class="subsec-title">Input Controls:</h3>
    <ul>
        <li><strong>File Picker / Drag-and-Drop Area:</strong> Accepts files up to 100 MB.</li>
        <li><strong>Encryption Algorithm Selector:</strong> Locked to <code>AES-256-GCM</code>.</li>
        <li><strong>Upload Action Button:</strong> Triggers in-memory encryption, key wrapping, signature generation, and streaming upload.</li>
    </ul>

    <h3 class="subsec-title">Working Mechanism:</h3>
    <p>File bytes are read into an ArrayBuffer via the browser FileReader API. The client generates a single-use 256-bit AES key and a 12-byte IV using cryptographically secure random values. The file is encrypted using AES-256-GCM. The client then wraps the AES key using its RSA public key, hashes the ciphertext via SHA-256, signs the hash using its EC private key, and transmits the multipart payload to <code>file-service</code>.</p>

    <h2 class="sec-title">5.3 Encrypted File Share &amp; Recipient Key-Wrapping Form</h2>
    <p>Enables file owners to securely grant access to specific recipients without exposing symmetric keys to the server.</p>

    <h3 class="subsec-title">Input Fields:</h3>
    <ul>
        <li><strong>Recipient Username:</strong> Target user identifier.</li>
        <li><strong>Expiration Policy:</strong> Dropdown selection (1 Hour, 24 Hours, 7 Days, 30 Days).</li>
    </ul>

    <h3 class="subsec-title">Working Mechanism:</h3>
    <p>The client queries <code>auth-service</code> for the recipient&rsquo;s public key, retrieves the file&rsquo;s symmetric AES key from its local vault, wraps it with the recipient&rsquo;s RSA-4096 public key (OAEP padding), and posts the wrapped key to <code>file-service</code> (<code>POST /api/v1/files/{{id}}/keys</code>). A share token is created in <code>share-service</code>, generating a secure shareable link.</p>

    <h2 class="sec-title">5.4 Secure Key Backup, Export &amp; Passphrase Protection Form</h2>
    <p>Provides emergency backup and multi-device migration for user cryptographic keys.</p>

    <h3 class="subsec-title">Input Controls:</h3>
    <ul>
        <li><strong>Backup Passphrase &amp; Confirmation:</strong> User-supplied master secret.</li>
        <li><strong>Export Action Button:</strong> Derives an AES key via PBKDF2 (600,000 iterations, SHA-256, random 16-byte salt), encrypts private keys, and exports an encrypted JSON keystore file.</li>
    </ul>
</div>

<!-- ==================== CHAPTER 6: OUTPUT DESIGN ==================== -->
<div class="page-break">
    <div style="text-align: right; font-size: 10pt; font-style: italic;">78</div>
    <h1 class="chap-header">Chapter 6</h1>
    <h1 class="chap-title">OUTPUT DESIGN</h1>

    <h2 class="sec-title">6.1 Encrypted File Vault &amp; Metadata Dashboard (Table View)</h2>
    <p>The primary output interface presents the user&rsquo;s personal encrypted vault in a structured table.</p>

    <div class="table-caption">Table 6.1: Sample Encrypted Vault File Records (Table View)</div>
    <table class="report-table">
        <thead>
            <tr>
                <th style="width: 25%;">File Name</th>
                <th style="width: 12%;">Size</th>
                <th style="width: 33%;">Ciphertext SHA-256 Hash</th>
                <th style="width: 12%;">Status</th>
                <th style="width: 18%;">Upload Date</th>
            </tr>
        </thead>
        <tbody>
            <tr><td><code>thesis_draft.pdf</code></td><td style="text-align: center;">4.2 MB</td><td><code>e3b0c44298fc1c149afbf4c8996fb92427...</code></td><td style="text-align: center;">CLEAN</td><td style="text-align: center;">2026-11-18</td></tr>
            <tr><td><code>financial_q3.xlsx</code></td><td style="text-align: center;">1.8 MB</td><td><code>7f83b1657ff1fc53b92dc18148a1d65dfc...</code></td><td style="text-align: center;">CLEAN</td><td style="text-align: center;">2026-11-17</td></tr>
            <tr><td><code>source_code.zip</code></td><td style="text-align: center;">18.5 MB</td><td><code>185f8db32271fe25f561a6fc938b2e2643...</code></td><td style="text-align: center;">CLEAN</td><td style="text-align: center;">2026-11-15</td></tr>
            <tr><td><code>patient_records.csv</code></td><td style="text-align: center;">650 KB</td><td><code>9b73c68f783f1307042a6392cf51e14581...</code></td><td style="text-align: center;">CLEAN</td><td style="text-align: center;">2026-11-14</td></tr>
        </tbody>
    </table>

    <h2 class="sec-title">6.2 Tamper-Evident Audit Trail &amp; Hash-Chain Verification Log</h2>
    <p>The administrative compliance output presents append-only events linked via cryptographic SHA-256 hash chains.</p>

    <div class="table-caption">Table 6.2: Sample Audit Trail &amp; Hash-Chain Verification Log</div>
    <table class="report-table">
        <thead>
            <tr>
                <th style="width: 8%;">ID</th>
                <th style="width: 18%;">Timestamp (UTC)</th>
                <th style="width: 14%;">Actor</th>
                <th style="width: 18%;">Action</th>
                <th style="width: 21%;">Current Hash</th>
                <th style="width: 21%;">Previous Hash</th>
            </tr>
        </thead>
        <tbody>
            <tr><td style="text-align: center;">1</td><td style="text-align: center;">2026-11-17 08:00</td><td>system</td><td>SYSTEM_INIT</td><td><code>c4ca4238a0...</code></td><td><code>0000000000...</code></td></tr>
            <tr><td style="text-align: center;">2</td><td style="text-align: center;">2026-11-17 08:15</td><td>ayush</td><td>USER_REGISTER</td><td><code>c81e728d9d...</code></td><td><code>c4ca4238a0...</code></td></tr>
            <tr><td style="text-align: center;">3</td><td style="text-align: center;">2026-11-17 08:30</td><td>ayush</td><td>UPLOAD</td><td><code>eccbc87e4b...</code></td><td><code>c81e728d9d...</code></td></tr>
            <tr><td style="text-align: center;">4</td><td style="text-align: center;">2026-11-17 09:12</td><td>ayush</td><td>SHARE_CREATED</td><td><code>a87ff679a2...</code></td><td><code>eccbc87e4b...</code></td></tr>
            <tr><td style="text-align: center;">5</td><td style="text-align: center;">2026-11-17 11:05</td><td>rahul</td><td>DOWNLOAD</td><td><code>e4da3b7fbb...</code></td><td><code>a87ff679a2...</code></td></tr>
        </tbody>
    </table>
</div>

<!-- ==================== CHAPTER 7: TESTING ==================== -->
<div class="page-break">
    <div style="text-align: right; font-size: 10pt; font-style: italic;">81</div>
    <h1 class="chap-header">Chapter 7</h1>
    <h1 class="chap-title">TESTING &amp; QUALITY ASSURANCE</h1>

    <h2 class="sec-title">7.1 Testing Strategy</h2>
    <p>Testing follows an automated multi-tier pyramid covering unit tests, integration tests, security test cases, and end-to-end system validation. Automated builds execute via <code>./mvnw -B verify</code> across all modules, achieving 100% green pass results across unit and integration suites.</p>

    <h2 class="sec-title">7.2 Unit Testing</h2>
    <p>Unit tests isolate individual components using JUnit 5, AssertJ, and Mockito for backend services, and Vitest for frontend TypeScript modules.</p>

    <div class="table-caption">Table 7.1: Unit Testing Details &amp; Validation Results</div>
    <table class="report-table">
        <thead>
            <tr>
                <th style="width: 18%;">Module</th>
                <th style="width: 25%;">Target Component</th>
                <th style="width: 25%;">Test Condition</th>
                <th style="width: 20%;">Expected Result</th>
                <th style="width: 12%;">Status</th>
            </tr>
        </thead>
        <tbody>
            <tr><td><code>auth-service</code></td><td><code>JwtService</code></td><td>Valid claims &amp; secret key</td><td>Signed JWT issued cleanly</td><td style="text-align: center; font-weight: bold; color: green;">PASS</td></tr>
            <tr><td><code>auth-service</code></td><td><code>JwtService</code></td><td>Expired token supplied</td><td>TokenExpiredException thrown</td><td style="text-align: center; font-weight: bold; color: green;">PASS</td></tr>
            <tr><td><code>auth-service</code></td><td><code>RateLimiterService</code></td><td>Exceeding 5 logins/min</td><td>HTTP 429 Rate Limit returned</td><td style="text-align: center; font-weight: bold; color: green;">PASS</td></tr>
            <tr><td><code>auth-service</code></td><td><code>KeyRegistryService</code></td><td>Valid RSA-4096 &amp; EC keys</td><td>Keys registered; fingerprints generated</td><td style="text-align: center; font-weight: bold; color: green;">PASS</td></tr>
            <tr><td><code>file-service</code></td><td><code>CryptoEngine</code></td><td>AES-256-GCM round trip</td><td>Decrypted bytes match plaintext</td><td style="text-align: center; font-weight: bold; color: green;">PASS</td></tr>
            <tr><td><code>file-service</code></td><td><code>SignatureVerifier</code></td><td>Valid NIST P-256 signature</td><td>Signature verification returns true</td><td style="text-align: center; font-weight: bold; color: green;">PASS</td></tr>
            <tr><td><code>file-service</code></td><td><code>SignatureVerifier</code></td><td>Tampered ciphertext payload</td><td>Signature verification returns false</td><td style="text-align: center; font-weight: bold; color: green;">PASS</td></tr>
            <tr><td><code>share-service</code></td><td><code>ShareService</code></td><td>Lookup active share token</td><td>Valid share record returned</td><td style="text-align: center; font-weight: bold; color: green;">PASS</td></tr>
            <tr><td><code>share-service</code></td><td><code>ShareService</code></td><td>Lookup revoked share token</td><td>ShareAccessDeniedException thrown</td><td style="text-align: center; font-weight: bold; color: green;">PASS</td></tr>
            <tr><td><code>audit-service</code></td><td><code>HashChainService</code></td><td>Append sequential records</td><td>Deterministic unbroken SHA-256 chain</td><td style="text-align: center; font-weight: bold; color: green;">PASS</td></tr>
            <tr><td><code>web-app</code></td><td><code>webCryptoService</code></td><td>4096-bit RSA key generation</td><td>CryptoKey handles stored in IndexedDB</td><td style="text-align: center; font-weight: bold; color: green;">PASS</td></tr>
        </tbody>
    </table>

    <h2 class="sec-title">7.3 Integration Testing</h2>
    <p>Integration tests verify inter-service communication and database persistence: Flyway database migrations apply cleanly across all four PostgreSQL schemas, MinIO streaming correctly transfers multi-megabyte binary payloads, and Redis enforces token blacklisting upon logout.</p>

    <h2 class="sec-title">7.4 System Testing</h2>
    <p>System testing validates the complete zero-knowledge workflow: registration, client-side encryption, upload to MinIO, recipient key wrapping, expiring link access, download, and local decryption.</p>

    <h2 class="sec-title">7.5 Test Cases</h2>
    <div class="table-caption">Table 7.2: System Test Cases &amp; Security Validation Results</div>
    <table class="report-table">
        <thead>
            <tr>
                <th style="width: 10%;">TC ID</th>
                <th style="width: 25%;">Test Scenario</th>
                <th style="width: 30%;">Input Conditions</th>
                <th style="width: 25%;">Expected Result</th>
                <th style="width: 10%;">Result</th>
            </tr>
        </thead>
        <tbody>
            <tr><td style="text-align: center;">TC-01</td><td>Valid Registration</td><td>Unique handle, valid email, strong password</td><td>Account created; keys registered; 201 Created</td><td style="text-align: center; font-weight: bold; color: green;">PASS</td></tr>
            <tr><td style="text-align: center;">TC-02</td><td>Duplicate Username</td><td>Existing handle submitted during registration</td><td>HTTP 409 Conflict returned</td><td style="text-align: center; font-weight: bold; color: green;">PASS</td></tr>
            <tr><td style="text-align: center;">TC-03</td><td>Valid JWT Auth</td><td>Correct username and password</td><td>Signed JWT returned with 1-hr expiry</td><td style="text-align: center; font-weight: bold; color: green;">PASS</td></tr>
            <tr><td style="text-align: center;">TC-04</td><td>Invalid Password</td><td>Incorrect password supplied</td><td>HTTP 401 Unauthorized returned</td><td style="text-align: center; font-weight: bold; color: green;">PASS</td></tr>
            <tr><td style="text-align: center;">TC-05</td><td>Zero-Knowledge Upload</td><td>Ciphertext stream + valid ECDSA signature</td><td>Stored in MinIO; metadata saved; 201 Created</td><td style="text-align: center; font-weight: bold; color: green;">PASS</td></tr>
            <tr><td style="text-align: center;">TC-06</td><td>Tampered Ciphertext</td><td>Payload altered during transmission</td><td>Signature mismatch; HTTP 400 returned</td><td style="text-align: center; font-weight: bold; color: green;">PASS</td></tr>
            <tr><td style="text-align: center;">TC-07</td><td>Unauthorized Access</td><td>User requests file without key grant</td><td>HTTP 403 Forbidden returned</td><td style="text-align: center; font-weight: bold; color: green;">PASS</td></tr>
            <tr><td style="text-align: center;">TC-08</td><td>Recipient Key Wrap</td><td>Owner wraps AES key for Recipient B</td><td>Key stored in file_keys; Recipient B can decrypt</td><td style="text-align: center; font-weight: bold; color: green;">PASS</td></tr>
            <tr><td style="text-align: center;">TC-09</td><td>Expired Share Link</td><td>Accessing share link after TTL deadline</td><td>HTTP 410 Gone / 403 Forbidden</td><td style="text-align: center; font-weight: bold; color: green;">PASS</td></tr>
            <tr><td style="text-align: center;">TC-10</td><td>Revoked Share Link</td><td>Owner revokes active share link</td><td>Status = REVOKED; subsequent access denied</td><td style="text-align: center; font-weight: bold; color: green;">PASS</td></tr>
            <tr><td style="text-align: center;">TC-11</td><td>Audit Chain Integrity</td><td>Admin invokes /api/v1/audit/verify</td><td>Returns { valid: true, brokenEntryId: null }</td><td style="text-align: center; font-weight: bold; color: green;">PASS</td></tr>
            <tr><td style="text-align: center;">TC-12</td><td>Audit Row Tampering</td><td>Row modified directly via SQL query</td><td>Verification flags break; reports exact row ID</td><td style="text-align: center; font-weight: bold; color: green;">PASS</td></tr>
        </tbody>
    </table>

    <h2 class="sec-title">7.6 Security Measures</h2>
    <p>Security measures include client-isolated private keys, non-extractable CryptoKey handles in IndexedDB, stateless JWT with Redis logout blacklisting, and Caddy TLS 1.3 termination with strict CSP, HSTS, and CORS headers.</p>

    <h2 class="sec-title">7.7 Database Security</h2>
    <p>Database hardening includes dedicated PostgreSQL instances per service, version-controlled Flyway migrations, parameterized queries via Hibernate, and credentials passed strictly via environment variables.</p>

    <h2 class="sec-title">7.8 User Roles &amp; Access Control</h2>
    <div class="table-caption">Table 7.3: User Roles and Access Permissions Matrix</div>
    <table class="report-table">
        <thead>
            <tr>
                <th style="width: 20%;">User Role</th>
                <th style="width: 45%;">Operational Responsibilities</th>
                <th style="width: 35%; text-align: left;">Permitted Endpoints</th>
            </tr>
        </thead>
        <tbody>
            <tr><td><code>ROLE_USER</code></td><td>Personal vault management, encryption, upload, sharing, revocation, key backup.</td><td><code>/api/v1/files/**</code>, <code>/api/v1/shares/**</code>, <code>/api/v1/keys/**</code></td></tr>
            <tr><td><code>ROLE_ADMIN</code></td><td>Platform monitoring, administrative audit log inspection, audit hash chain verification.</td><td><code>/api/v1/audit/**</code>, <code>/api/v1/health/**</code>, <code>/actuator/**</code></td></tr>
        </tbody>
    </table>
</div>

<!-- ==================== CHAPTER 8: SUMMARY & FUTURE SCOPE ==================== -->
<div class="page-break">
    <div style="text-align: right; font-size: 10pt; font-style: italic;">90</div>
    <h1 class="chap-header">Chapter 8</h1>
    <h1 class="chap-title">SUMMARY &amp; FUTURE SCOPE</h1>

    <h2 class="sec-title">8.1 Summary</h2>
    <p>SecureFileShare provides a production-ready, zero-knowledge encrypted file sharing and vault platform. By eliminating the trusted-server assumption, the platform ensures that even a catastrophic breach of server infrastructure reveals only opaque, mathematically undecryptable ciphertexts.</p>

    <p>Key achievements include:</p>
    <ul>
        <li><strong>Mathematical Zero-Knowledge Sovereignty:</strong> Plaintext data and symmetric keys are never exposed to server memory or persistent storage.</li>
        <li><strong>Client-Side Cryptographic Vault:</strong> Hardware-accelerated AES-256-GCM encryption, RSA-4096-OAEP key wrapping, and NIST P-256 ECDSA digital signing.</li>
        <li><strong>Microservice Architecture:</strong> Autonomous Spring Boot microservices backed by PostgreSQL, MinIO, and Redis.</li>
        <li><strong>Tamper-Evident Audit Logging:</strong> Append-only event ingestion with SHA-256 cryptographic hash chaining.</li>
        <li><strong>Automated CI/CD:</strong> Seamless GitHub Actions pipelines with Gitleaks secret scanning, OWASP dependency auditing, and automated rollback capabilities.</li>
    </ul>

    <h2 class="sec-title">8.2 Future Enhancements</h2>
    <ol>
        <li><strong>Native Mobile Applications (iOS / Android):</strong> Native mobile clients with biometric hardware keychain integration (Apple Secure Enclave, Android Keystore).</li>
        <li><strong>End-to-End Encrypted Group Workspaces:</strong> Decentralized group key management using Messaging Layer Security (MLS) for collaborative encrypted folders.</li>
        <li><strong>WebRTC Direct Peer-to-Peer Transfers:</strong> Direct browser-to-browser encrypted transfers for massive files, bypassing cloud storage entirely.</li>
        <li><strong>Post-Quantum Cryptography (PQC):</strong> Transitioning to NIST-approved post-quantum algorithms (ML-KEM / CRYSTALS-Kyber and ML-DSA / CRYSTALS-Dilithium).</li>
        <li><strong>Automated Compliance Reporting:</strong> One-click export of cryptographically validated audit trails to simplify enterprise GDPR and HIPAA audits.</li>
    </ol>
</div>

<!-- ==================== REFERENCES ==================== -->
<div class="page-break">
    <div style="text-align: right; font-size: 10pt; font-style: italic;">94</div>
    <h1 class="chap-header">REFERENCES</h1>
    <div style="height: 2px; background-color: #0d3b66; width: 60px; margin: 0 auto 25px auto;"></div>

    <ol style="line-height: 1.5; font-size: 10.5pt;">
        <li>National Institute of Standards and Technology (NIST). (2001). <em>Advanced Encryption Standard (AES)</em>. Federal Information Processing Standards Publication (FIPS PUB 197). https://doi.org/10.6028/NIST.FIPS.197</li>
        <li>Dworkin, M. (2007). <em>Recommendation for Block Cipher Modes of Operation: Galois/Counter Mode (GCM) and GMAC</em>. NIST Special Publication 800-38D. https://doi.org/10.6028/NIST.SP.800-38D</li>
        <li>Moriarty, K., Kaliski, B., Jonsson, J., &amp; Rusch, A. (2016). <em>PKCS #1: RSA Cryptography Specifications Version 2.2</em>. RFC 8017, Internet Engineering Task Force (IETF). https://doi.org/10.17487/RFC8017</li>
        <li>Barker, E. (2020). <em>Recommendation for Key Management: Part 1 &ndash; General</em>. NIST Special Publication 800-57 Part 1, Rev. 5. https://doi.org/10.6028/NIST.SP.800-57pt1r5</li>
        <li>World Wide Web Consortium (W3C). (2017). <em>Web Cryptography API - W3C Recommendation</em>. https://www.w3.org/TR/WebCryptoAPI/</li>
        <li>OWASP Foundation. (2024). <em>Cryptographic Storage Cheat Sheet</em>. OWASP Cheat Sheet Series. https://cheatsheetseries.owasp.org/cheatsheets/Cryptographic_Storage_Cheat_Sheet.html</li>
        <li>OWASP Foundation. (2024). <em>JSON Web Token for Java Cheat Sheet</em>. OWASP Cheat Sheet Series. https://cheatsheetseries.owasp.org/cheatsheets/JSON_Web_Token_for_Java_Cheat_Sheet.html</li>
        <li>Walls, C. (2022). <em>Spring in Action</em> (6th ed.). Manning Publications.</li>
        <li>Burtch, K. (2021). <em>Linux System Administration: A Guide to Enterprise Computing</em>. O&rsquo;Reilly Media.</li>
        <li>Haber, S., &amp; Stornetta, W. S. (1991). <em>How to time-stamp a digital document</em>. Journal of Cryptology, 3(2), 99&ndash;111. https://doi.org/10.1007/BF00196791</li>
    </ol>
</div>
</body>
</html>
"""
