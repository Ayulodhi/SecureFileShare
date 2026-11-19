# -*- coding: utf-8 -*-
"""
sections_ch1_ch2.py
Chapter 1: Introduction
Chapter 2: System Analysis & SRS
"""

def get_ch1_ch2_html(img_usecase_b64, img_storage_b64, img_sequence_b64):
    template = """
<!-- ==================== CHAPTER 1: INTRODUCTION ==================== -->
<div class="page-break">
    <div style="text-align: right; font-size: 10pt; font-style: italic;">1</div>
    <h1 class="chap-header">Chapter 1</h1>
    <h1 class="chap-title">INTRODUCTION</h1>

    <h2 class="sec-title">1.1 Introduction</h2>
    <p>File storage and transmission are foundational services of modern computing infrastructure across academic institutions, commercial enterprises, healthcare systems, and governmental bodies. Every day, terabytes of confidential electronic documents&mdash;including academic research records, examination question banks, financial audits, proprietary intellectual property, medical evaluations, and legal contracts&mdash;are transferred across public networks and stored in cloud repositories.</p>

    <p>However, mainstream public cloud storage providers (such as Google Drive, Microsoft OneDrive, and Dropbox) operate almost exclusively on a &ldquo;trusted-server&rdquo; architectural model. In this traditional model, users upload files in plaintext or over transport-layer encrypted channels (TLS/HTTPS). Once received by the cloud service provider, the incoming payloads are decrypted on the application servers before being written to persistent block or object storage. Even when providers claim &ldquo;encryption at rest,&rdquo; the cryptographic keys are managed, retained, and accessible by the provider&rsquo;s internal infrastructure.</p>

    <p>This centralized custody model creates critical vulnerabilities:</p>
    <ul>
        <li><strong>Insider Threats:</strong> Malicious or compromised system administrators, database engineers, or third-party infrastructure contractors can inspect, copy, or exfiltrate plaintext user files directly.</li>
        <li><strong>Subpoena and Interception Risks:</strong> Centralized storage providers can be legally compelled to decrypt and surrender private files without the data owner&rsquo;s knowledge or active cryptographic consent.</li>
        <li><strong>Server-Side Exploits:</strong> If backend application servers experience Remote Code Execution (RCE), memory dump vulnerabilities, or container boundary escalations, unencrypted files in active memory or local caches are immediately exposed.</li>
    </ul>

    <p>To overcome these fundamental security flaws, <strong>SecureFileShare</strong> introduces an enterprise-grade, <strong>Zero-Knowledge End-to-End Encrypted File Sharing &amp; Vault Platform</strong>. Zero-knowledge means that the backend infrastructure&mdash;including application gateways, microservices, databases, and MinIO S3 object storage&mdash;has <strong>mathematically zero knowledge</strong> of the plaintext content of any user file, the symmetric encryption keys (AES-256) used to encrypt files, and the private asymmetric cryptographic keys (RSA-4096 and NIST P-256) belonging to users.</p>

    <p>All cryptographic operations are executed strictly on the client side (within the user&rsquo;s web browser via the W3C Web Crypto API, or within the native desktop client via Java Bouncy Castle). The server functions solely as an untrusted ciphertext repository, public key directory, access-control gatekeeper, and cryptographic audit verifier.</p>

    <h2 class="sec-title">1.2 Motivation</h2>
    <p>The motivation behind developing SecureFileShare stems from pressing real-world data privacy mandates and the academic pursuit of applying modern distributed systems and advanced cryptography to solve cloud security vulnerabilities.</p>

    <p>In recent years, stringent data protection regulations such as the European Union&rsquo;s General Data Protection Regulation (GDPR), the Health Insurance Portability and Accountability Act (HIPAA), and India&rsquo;s Digital Personal Data Protection (DPDP) Act have mandated rigorous technical safeguards for personal and enterprise data. Standard file-sharing tools fall short of providing verifiable cryptographic sovereignty.</p>

    <p>Furthermore, academic institutions and enterprises frequently need to distribute highly confidential materials&mdash;such as proprietary software artifacts, patent disclosures, legal contracts, research datasets, and student evaluation records&mdash;across public networks without risking unintended disclosure.</p>

    <p>Developing SecureFileShare provided the opportunity to address these pressing challenges by building an end-to-end distributed system utilizing:</p>
    <ul>
        <li><strong>Java 21 (LTS)</strong> and <strong>Spring Boot 3.3.4</strong> to engineer a microservice ecosystem (<code>auth-service</code>, <code>file-service</code>, <code>share-service</code>, <code>audit-service</code>, and <code>common</code>).</li>
        <li><strong>PostgreSQL 16</strong> with independent schemas and <strong>Flyway</strong> automated database migrations for relational metadata.</li>
        <li><strong>MinIO S3</strong> distributed object storage for hosting opaque, streaming ciphertexts.</li>
        <li><strong>Client-Side Cryptographic Vaults</strong> utilizing AES-256-GCM authenticated encryption, RSA-4096-OAEP envelope key wrapping, and NIST P-256 ECDSA digital signatures for non-repudiation.</li>
        <li><strong>Tamper-Evident Audit Logging</strong> implementing mathematical SHA-256 hash chaining to provide verifiable, append-only compliance monitoring.</li>
        <li><strong>Docker &amp; Container Orchestration</strong> fronted by a <strong>Caddy</strong> reverse proxy enforcing automatic TLS termination, Strict Transport Security (HSTS), and a rigid Content Security Policy (CSP).</li>
    </ul>

    <h2 class="sec-title">1.3 Objectives &amp; Scope</h2>
    <h3 class="subsec-title">Specific Objectives</h3>
    <ol>
        <li><strong>Zero-Knowledge Confidentiality:</strong> Ensure plaintext data and symmetric keys never reach the backend network, server memory, or persistent storage.</li>
        <li><strong>Client-Side Cryptographic Pipeline:</strong> Implement client-side AES-256-GCM authenticated encryption, ensuring cryptographic confidentiality and tamper detection.</li>
        <li><strong>Public-Key Cryptographic Vault:</strong> Generate and register client-side RSA-4096 key pairs for asymmetric key wrapping (using OAEP padding) and NIST P-256 elliptic curve key pairs for digital signatures.</li>
        <li><strong>Authenticity and Non-Repudiation:</strong> Verify client-generated ECDSA digital signatures on the backend before any encrypted payload is accepted into persistent storage.</li>
        <li><strong>Multi-Recipient File Sharing:</strong> Enable users to securely share files by wrapping the file&rsquo;s symmetric AES key with each recipient&rsquo;s registered RSA public key (<code>POST /api/v1/files/{id}/keys</code>).</li>
        <li><strong>Expiring and Revocable Share Links:</strong> Provide cryptographic share tokens supporting configurable time-to-live (TTL) expiration and instantaneous owner revocation.</li>
        <li><strong>Tamper-Evident Audit Logging:</strong> Construct an immutable audit service where every administrative and file lifecycle event is linked via a cryptographic SHA-256 hash chain.</li>
        <li><strong>Microservice Scalability &amp; Isolation:</strong> Decompose backend logic into decoupled Spring Boot microservices with dedicated PostgreSQL databases.</li>
        <li><strong>Dual Client Support:</strong> Provide an interactive React 18 / TypeScript / Tailwind CSS web application and a native Java 21 / Swing / FlatLaf desktop client.</li>
        <li><strong>Comprehensive Quality Assurance:</strong> Ensure full automated test coverage via JUnit 5, Mockito, AssertJ, and Vitest, validating all builds cleanly and verifying resilience under security attack scenarios.</li>
    </ol>

    <h3 class="subsec-title">Scope of the Project</h3>
    <p>The scope of SecureFileShare encompasses the end-to-end lifecycle of confidential file handling: user registration, public key directory management, zero-knowledge file upload with streaming ciphertexts, multi-recipient access granting, expiring share links, immutable audit trail verification, and containerized deployment with automated CI/CD pipelines.</p>

    <h2 class="sec-title">1.4 Hardware &amp; Software Requirements</h2>
    <h3 class="subsec-title">Hardware Requirements</h3>
    <p>The system is architected as a lightweight, containerized distributed system capable of running efficiently in both modern developer environments and resource-constrained cloud servers:</p>
    <ul>
        <li><strong>Development Workstation:</strong> Intel Core i5 / AMD Ryzen 5 or higher (64-bit multi-core), 8 GB RAM minimum (16 GB recommended), 256 GB SSD storage.</li>
        <li><strong>Production Server:</strong> ARM64 / x86_64 Cloud VM (e.g., Oracle Cloud Infrastructure Ampere A1), 2 vCPUs, 2 GB to 4 GB RAM, 50 GB NVMe / SSD persistent block storage.</li>
        <li><strong>Client Devices:</strong> Modern desktop, laptop, or smartphone running Chrome, Edge, Safari, or Firefox with Web Crypto API support.</li>
    </ul>

    <h3 class="subsec-title">Software Requirements</h3>
    <ul>
        <li><strong>Operating System:</strong> Windows 10/11, Ubuntu Linux 22.04/24.04 LTS, or macOS Sonoma.</li>
        <li><strong>Java Runtime &amp; SDK:</strong> Eclipse Temurin OpenJDK 21 LTS.</li>
        <li><strong>Build Automation:</strong> Apache Maven 3.9.6+, Maven Wrapper (<code>./mvnw</code>).</li>
        <li><strong>Backend Framework:</strong> Spring Boot 3.3.4, Spring Security 6, Spring Data JPA, Hibernate 6.</li>
        <li><strong>Database Engine:</strong> PostgreSQL 16.2 with Flyway 10.x migrations.</li>
        <li><strong>Cache &amp; Rate Limiting:</strong> Redis 7.2 (Alpine).</li>
        <li><strong>Object Storage:</strong> MinIO S3-Compatible Server (RELEASE.2024-03).</li>
        <li><strong>Frontend Stack:</strong> Node.js 22 LTS, React 18/19, TypeScript 5.4, Vite 5.2, Tailwind CSS 3.4.</li>
        <li><strong>Desktop Framework:</strong> Java 21 Swing, FlatLaf 3.4 Look &amp; Feel, Bouncy Castle Provider (<code>bcprov-jdk18on</code>).</li>
        <li><strong>Edge Reverse Proxy:</strong> Caddy v2.7+ with automated TLS termination and security headers.</li>
    </ul>

    <h2 class="sec-title">1.5 Tools &amp; Technologies Used</h2>
    <p>The system leverages a cohesive modern technology stack:</p>
    <ul>
        <li><strong>Java 21 LTS &amp; Spring Boot 3.3.4:</strong> Provides high-performance non-blocking I/O, declarative security filters, and microservice decoupling.</li>
        <li><strong>PostgreSQL 16 &amp; Flyway:</strong> Delivers strict ACID compliance, relational integrity, and automated schema migration tracking across separate microservice databases.</li>
        <li><strong>MinIO Object Storage:</strong> High-performance, S3-compliant storage for hosting opaque, streaming ciphertexts without exhausting JVM heap memory.</li>
        <li><strong>Redis 7:</strong> In-memory storage utilized for the JWT logout revocation blacklist and token-bucket API rate limiting.</li>
        <li><strong>W3C Web Crypto API &amp; Bouncy Castle:</strong> Hardware-accelerated client-side cryptographic primitives (AES-256-GCM, RSA-4096-OAEP, NIST P-256 ECDSA).</li>
        <li><strong>React 18 &amp; TypeScript:</strong> Modern component-driven Single Page Application (SPA) offering type safety, responsive design, and local key vault storage via IndexedDB.</li>
        <li><strong>Caddy Reverse Proxy:</strong> Automatic Let&rsquo;s Encrypt / ZeroSSL TLS certificate provisioning, strict CSP, HSTS, and unified reverse proxy routing.</li>
    </ul>

    <h2 class="sec-title">1.6 Organization of Report</h2>
    <p>This report is systematically organized into eight detailed chapters following academic standards:</p>
    <ul>
        <li><strong>Chapter 1: Introduction:</strong> Establishes project background, motivation, zero-knowledge threat modeling, objectives, hardware/software specifications, and technology stack.</li>
        <li><strong>Chapter 2: System Analysis &amp; SRS:</strong> Details preliminary investigations, technical/economic/operational feasibility studies, functional and non-functional requirements, Use Case diagrams, DFDs, Sequence diagrams, and Activity diagrams.</li>
        <li><strong>Chapter 3: System Design:</strong> Explains microservice system architecture, ER diagrams, database schemas, modularization details, cryptographic algorithms, user interface wireframes, and engineering paradigms.</li>
        <li><strong>Chapter 4: Project Management:</strong> Details WBS, timeline, milestones, deliverables, risk management matrix, cost estimation, PERT network analysis, Gantt scheduling, and CI/CD automation.</li>
        <li><strong>Chapter 5: Input Design:</strong> Covers registration, client key generation, encrypted file upload, recipient wrapping, and key backup forms.</li>
        <li><strong>Chapter 6: Output Design:</strong> Presents encrypted file vault dashboards and immutable audit trail verification interfaces.</li>
        <li><strong>Chapter 7: Testing:</strong> Details testing strategies, automated unit and integration tests (validating green builds across all modules), test cases TC-01 to TC-12, and security hardening.</li>
        <li><strong>Chapter 8: Summary &amp; Future Scope:</strong> Synthesizes project achievements and outlines future technical enhancements.</li>
    </ul>
</div>

<!-- ==================== CHAPTER 2: SYSTEM ANALYSIS & SRS ==================== -->
<div class="page-break">
    <div style="text-align: right; font-size: 10pt; font-style: italic;">12</div>
    <h1 class="chap-header">Chapter 2</h1>
    <h1 class="chap-title">SYSTEM ANALYSIS &amp; REQUIREMENTS SPECIFICATIONS</h1>

    <h2 class="sec-title">2.1 Identification of Need</h2>
    <p>In modern collaborative environments, data transmission and cloud storage represent the largest surface area for security exploits. Sensitive enterprise files&mdash;including proprietary algorithms, intellectual property, confidential financial statements, student assessment records, and legal agreements&mdash;are routinely exposed to third-party cloud infrastructure.</p>

    <p>Standard cloud storage providers present systemic risks:</p>
    <ol>
        <li><strong>Server-Side Decryption Vulnerability:</strong> Cloud servers receive and decrypt files before storage. A breach of the server exposes every customer&rsquo;s plaintext data.</li>
        <li><strong>Centralized Key Custody:</strong> Encryption keys are held by the hosting company, allowing unauthorized administrative access or unilateral third-party compliance without user notification.</li>
        <li><strong>Lack of Non-Repudiation:</strong> Standard file sharing lacks cryptographic signatures linking uploaded content to the author&rsquo;s identity.</li>
        <li><strong>Mutable Audit Logs:</strong> Standard database log entries can be altered or erased by compromised administrators to conceal exfiltration.</li>
    </ol>
    <p>These limitations establish the urgent need for a verified <strong>Zero-Knowledge Encrypted File Sharing Platform</strong> where the server operates strictly on opaque ciphertexts and mathematically cannot inspect or surrender user data.</p>

    <h2 class="sec-title">2.2 Preliminary Investigation</h2>
    <p>A rigorous preliminary investigation evaluated three prevailing file-sharing paradigms:</p>
    <ul>
        <li><strong>Standard Cloud Storage (Google Drive, Dropbox, OneDrive):</strong> Excellent usability and scalability, but zero cryptographic privacy. The cloud provider retains full key access.</li>
        <li><strong>Proprietary Zero-Knowledge Clouds (Mega, Proton Drive):</strong> Strong client-side encryption, but closed ecosystems lacking flexible enterprise microservice integration and verifiable mathematical audit chains.</li>
        <li><strong>Decentralized PGP/GPG Email Transfers:</strong> Strong cryptographic privacy, but severely impaired by poor usability, manual public key discovery, lack of centralized link expiration, and zero revocation capabilities.</li>
    </ul>
    <p>The investigation demonstrated the need for a solution combining client-side cryptographic isolation with the streamlined user experience of microservices, cloud object storage, and automated public key directories.</p>

    <h2 class="sec-title">2.3 Feasibility Study</h2>
    <h3 class="subsec-title">2.3.1 Technical Feasibility</h3>
    <p>The system is technically feasible. The W3C Web Crypto API natively provides hardware-accelerated AES-256-GCM, RSA-OAEP, and ECDSA signing inside all modern web browsers. Spring Boot 3.3.4 and Java 21 provide enterprise microservice resilience. MinIO provides horizontally scalable S3-compatible storage, while PostgreSQL 16 guarantees ACID metadata management.</p>

    <h3 class="subsec-title">2.3.2 Economic Feasibility</h3>
    <p>The project uses 100% open-source software (Java, Spring Boot, PostgreSQL, Redis, MinIO, React, Caddy, Docker). Cloud deployment is optimized for zero licensing expense, capable of operating within Oracle Cloud Always-Free tier instances.</p>

    <h3 class="subsec-title">2.3.3 Operational Feasibility</h3>
    <p>Operationally, cryptographic complexities (key generation, key wrapping, signature generation) occur transparently in the background. Users interact through standard web or desktop interfaces without requiring specialized cryptographic training.</p>

    <h2 class="sec-title">2.4 Functional Requirements</h2>
    <div class="table-caption">Table 2.1: Functional Requirements of SecureFileShare</div>
    <table class="report-table">
        <thead>
            <tr>
                <th style="width: 12%;">Req ID</th>
                <th style="width: 28%;">Requirement Title</th>
                <th style="width: 60%; text-align: left;">Detailed Description</th>
            </tr>
        </thead>
        <tbody>
            <tr><td style="text-align: center;">FR-01</td><td>User Registration &amp; Auth</td><td>Register users with unique username/email, hash passwords via BCrypt, issue signed JWTs upon authentication.</td></tr>
            <tr><td style="text-align: center;">FR-02</td><td>Public Key Registry</td><td>Store and serve user public keys (RSA-4096 and EC P-256) with SHA-256 fingerprints; support key rotation.</td></tr>
            <tr><td style="text-align: center;">FR-03</td><td>Client-Side File Encryption</td><td>Generate single-use random 256-bit AES key per file; encrypt payload locally via AES-256-GCM.</td></tr>
            <tr><td style="text-align: center;">FR-04</td><td>RSA Key Wrapping</td><td>Wrap symmetric AES key using recipient/owner RSA-4096 public key with OAEP padding before uploading.</td></tr>
            <tr><td style="text-align: center;">FR-05</td><td>Digital Signature Generation</td><td>Compute SHA-256 hash of ciphertext; sign hash using user NIST P-256 private key.</td></tr>
            <tr><td style="text-align: center;">FR-06</td><td>Signature Verification</td><td>Backend <code>file-service</code> validates SHA-256 checksum and ECDSA signature against registered public key.</td></tr>
            <tr><td style="text-align: center;">FR-07</td><td>Object Storage Streaming</td><td>Stream encrypted ciphertext directly to MinIO S3 object storage without holding plaintext in server RAM.</td></tr>
            <tr><td style="text-align: center;">FR-08</td><td>Multi-Recipient Sharing</td><td>Allow file owners to wrap symmetric AES keys for multiple recipients (<code>POST /api/v1/files/{id}/keys</code>).</td></tr>
            <tr><td style="text-align: center;">FR-09</td><td>Expiring &amp; Revocable Links</td><td>Generate unique cryptographic share tokens with configurable TTL expiration and immediate revocation capability.</td></tr>
            <tr><td style="text-align: center;">FR-10</td><td>Tamper-Evident Audit Log</td><td>Record all lifecycle and security events into an append-only, SHA-256 hash-chained log (<code>audit-service</code>).</td></tr>
            <tr><td style="text-align: center;">FR-11</td><td>Audit Chain Verification</td><td>Provide administrative endpoint (<code>GET /api/v1/audit/verify</code>) to mathematically verify audit chain integrity.</td></tr>
            <tr><td style="text-align: center;">FR-12</td><td>Key Backup &amp; Recovery</td><td>Support exporting/importing encrypted cryptographic key pairs protected by user-defined passphrases.</td></tr>
        </tbody>
    </table>

    <h2 class="sec-title">2.5 Non-Functional Requirements</h2>
    <div class="table-caption">Table 2.2: Non-Functional Requirements of SecureFileShare</div>
    <table class="report-table">
        <thead>
            <tr>
                <th style="width: 10%;">S. No.</th>
                <th style="width: 25%;">Quality Attribute</th>
                <th style="width: 65%; text-align: left;">Technical Requirement &amp; Specification</th>
            </tr>
        </thead>
        <tbody>
            <tr><td style="text-align: center;">1</td><td>Confidentiality</td><td>Zero-knowledge design: Plaintext files and symmetric AES keys must never be accessible to backend servers.</td></tr>
            <tr><td style="text-align: center;">2</td><td>Integrity</td><td>End-to-end cryptographic verification via AES-GCM authentication tags, SHA-256 checksums, and ECDSA signatures.</td></tr>
            <tr><td style="text-align: center;">3</td><td>Non-Repudiation</td><td>Every uploaded package carries a verifiable ECDSA signature proving ownership and authorship.</td></tr>
            <tr><td style="text-align: center;">4</td><td>Performance</td><td>Streaming uploads must support payloads up to 100 MB with p95 API response times under 500ms.</td></tr>
            <tr><td style="text-align: center;">5</td><td>Scalability</td><td>Decoupled microservice architecture enabling independent horizontal scaling of storage and authentication nodes.</td></tr>
            <tr><td style="text-align: center;">6</td><td>Availability</td><td>Stateless service layer with container health probes, automated restart policies, and rolling updates.</td></tr>
            <tr><td style="text-align: center;">7</td><td>Auditability</td><td>Tampering with database logs is mathematically detectable via broken SHA-256 hash chains.</td></tr>
            <tr><td style="text-align: center;">8</td><td>Security Hardening</td><td>Enforced TLS 1.3, strict Content Security Policy (CSP), Redis rate limiting, and JWT revocation blacklists.</td></tr>
        </tbody>
    </table>

    <h2 class="sec-title">2.6 Use Case Diagram</h2>
    <p>The Use Case Diagram represents the interactions between users and the core functional capabilities of the SecureFileShare platform.</p>

    <div class="img-container">
        <img src="IMG_USECASE_TAG" alt="Figure 2.1: Use Case Diagram of SecureFileShare Platform" style="max-width: 55%; height: auto; display: block; margin: 0 auto; border: 1px solid #ccc; border-radius: 4px;" />
    </div>
    <div class="figure-caption">Figure 2.1: Use Case Diagram of SecureFileShare Platform</div>

    <div class="img-explanation">
        <strong>Detailed Technical Explanation of Figure 2.1:</strong><br>
        Figure 2.1 illustrates the comprehensive use case model of the SecureFileShare platform:
        <ul>
            <li><strong>Primary Actor:</strong> The <em>User</em> represents authenticated users operating through either the React Single Page Application (SPA) or the native Java Swing desktop client.</li>
            <li><strong>Platform Boundary:</strong> Encapsulates seven core use cases:
                <ol>
                    <li><strong>Register / Login:</strong> Captures user credentials, verifies BCrypt password hashes, issues stateless JWT tokens, and manages session lifecycles.</li>
                    <li><strong>Manage Keys (RSA/EC):</strong> Coordinates the generation of client-side RSA-4096 key pairs (for asymmetric key wrapping) and NIST P-256 EC key pairs (for ECDSA signing), registering public keys with <code>auth-service</code>.</li>
                    <li><strong>Upload Encrypted File:</strong> Orchestrates client-side AES-256-GCM symmetric encryption, RSA-4096-OAEP key wrapping, SHA-256 hashing, ECDSA signing, and multipart transmission to <code>file-service</code>.</li>
                    <li><strong>Download Decrypted File:</strong> Handles package retrieval, RSA key unwrapping, and in-memory AES-GCM decryption back to original plaintext.</li>
                    <li><strong>Create Share Link (HMAC):</strong> Generates high-entropy cryptographic share tokens with configurable TTL expiration windows and wraps keys for designated recipients.</li>
                    <li><strong>Revoke Share Link:</strong> Enables instantaneous cancellation of active shares, setting status to <code>REVOKED</code> in <code>share-service</code>.</li>
                    <li><strong>View Audit Log:</strong> Provides authorized users and administrators access to the tamper-evident audit trail and hash-chain verification endpoints.</li>
                </ol>
            </li>
            <li><strong>Implementation Status:</strong> All seven illustrated use cases are fully implemented and verified in the codebase. Advanced multi-device key synchronization and automated group vaults represent planned architectural extensions.</li>
        </ul>
    </div>

    <h2 class="sec-title">2.7 Data Flow Diagram</h2>
    <p>The Data Flow Diagram (DFD) models the flow of data through external entities, microservices, and databases.</p>

    <h3 class="subsec-title">2.7.1 DFD Level 0 (Context Diagram)</h3>
    <p>In the Context Diagram, external entities (File Owner, Recipient, Administrator) interact with the centralized SecureFileShare system:</p>
    <ul>
        <li>The <em>File Owner</em> provides registration credentials, uploads public keys, and transmits encrypted packages (ciphertext, wrapped key, ECDSA signature).</li>
        <li>The <em>Recipient User</em> searches for authorized files, submits share tokens, retrieves wrapped keys, and downloads ciphertexts for local decryption.</li>
        <li>The <em>System Administrator</em> triggers audit verification checks and monitors platform health.</li>
        <li>The system stores opaque ciphertexts in <em>MinIO S3 Storage</em> and metadata in isolated <em>PostgreSQL</em> databases.</li>
    </ul>

    <h3 class="subsec-title">2.7.2 DFD Level 1 (Decomposition Diagram)</h3>
    <p>DFD Level 1 decomposes the system into four autonomous microservice processes:</p>
    <ul>
        <li><strong>Process 1.0 (Auth &amp; Key Management - Port 8081):</strong> Handles credentials, validates JWTs, verifies public keys via <code>KeyValidator</code>, and manages the <code>users</code> and <code>key_registry</code> tables.</li>
        <li><strong>Process 2.0 (Zero-Knowledge Ingestion - Port 8082):</strong> Validates incoming packages via <code>FileService</code>, verifies ECDSA signatures, streams ciphertexts to MinIO S3, and updates <code>files</code> and <code>file_keys</code>.</li>
        <li><strong>Process 3.0 (Share &amp; Access Control - Port 8083):</strong> Manages share link creation, expiry enforcement, and revocation in the <code>shares</code> table.</li>
        <li><strong>Process 4.0 (Tamper-Evident Audit - Port 8084):</strong> Receives inter-service events via <code>AuditService</code>, calculates sequential SHA-256 hash chains, and appends to <code>audit_entries</code>.</li>
    </ul>

    <h2 class="sec-title">2.8 Sequence Diagram</h2>
    <p>The Sequence Diagram illustrates the exact time-ordered flow of cryptographic operations, inter-service API calls, and storage events during zero-knowledge file sharing.</p>

    <div class="img-container">
        <img src="IMG_SEQUENCE_TAG" alt="Figure 2.2: Sequence Diagram of Recipient Ciphertext Upload and Key Sharing Pipeline" style="max-width: 90%; height: auto; display: block; margin: 0 auto; border: 1px solid #ccc; border-radius: 4px;" />
    </div>
    <div class="figure-caption">Figure 2.2: Sequence Diagram of Recipient Ciphertext Upload and Key Sharing Pipeline</div>

    <div class="img-explanation">
        <strong>Detailed Technical Explanation of Figure 2.2:</strong><br>
        Figure 2.2 details the 11-step end-to-end cryptographic and network sequence:
        <ol>
            <li><strong>Step 1 (Selects File):</strong> The user selects a local file through the client interface.</li>
            <li><strong>Step 2 (Generate Random Symmetric Key):</strong> The client crypto engine creates a cryptographically secure 256-bit symmetric AES key.</li>
            <li><strong>Step 3 (Encrypt File):</strong> The client encrypts the file locally using AES-256-GCM, producing ciphertext and a 12-byte IV.</li>
            <li><strong>Step 4 &amp; 5 (Request &amp; Return Public Key):</strong> The client queries <code>auth-service (:8081)</code> for the recipient&rsquo;s public keys; <code>auth-service</code> returns the registered RSA-4096 and EC P-256 public keys.</li>
            <li><strong>Step 6 (Wrap AES Key):</strong> The client wraps the symmetric AES key using the recipient&rsquo;s RSA public key with OAEP padding.</li>
            <li><strong>Step 7 (Upload Ciphertext + Wrapped Key):</strong> The client computes the SHA-256 hash of the ciphertext, signs it using its NIST P-256 EC private key, and transmits the package to <code>file-service (:8082)</code>.</li>
            <li><strong>Step 8 (Verify Signatures):</strong> <code>file-service</code> validates the SHA-256 checksum and verifies the ECDSA signature against the sender&rsquo;s public key.</li>
            <li><strong>Step 9 (Stream Ciphertext):</strong> <code>file-service</code> streams the opaque ciphertext directly into MinIO S3 object storage.</li>
            <li><strong>Step 10 (Store Metadata + Wrapped Key):</strong> <code>file-service</code> stores file metadata in the <code>files</code> table and the wrapped AES key in the <code>file_keys</code> table in PostgreSQL.</li>
            <li><strong>Step 11 (Upload Successful):</strong> <code>file-service</code> returns HTTP 201 Created to the client.</li>
        </ol>
        <p><strong>Crucial Architectural Invariant:</strong> The highlighted banner affirms that the server never sees plaintext files or unwrapped keys at any point during transmission or storage.</p>
    </div>

    <h2 class="sec-title">2.9 Activity Diagram</h2>
    <p>The Activity Diagram models the operational logic and decision flows governing both encrypted file uploads and downloads.</p>

    <div class="img-container">
        <img src="IMG_STORAGE_TAG" alt="Figure 2.3: Activity Diagram: Encrypted File Storage and Retrieval Flow" style="max-width: 50%; height: auto; display: block; margin: 0 auto; border: 1px solid #ccc; border-radius: 4px;" />
    </div>
    <div class="figure-caption">Figure 2.3: Activity Diagram: Encrypted File Storage and Retrieval Flow</div>

    <div class="img-explanation">
        <strong>Detailed Technical Explanation of Figure 2.3:</strong><br>
        Figure 2.3 depicts the decision branches governing storage and retrieval:
        <ul>
            <li><strong>Decision Node (Action?):</strong> The workflow branches depending on whether the caller initiates an <em>Upload</em> or <em>Download</em> operation.</li>
            <li><strong>Upload Branch:</strong>
                <ol>
                    <li><em>Client Encrypts File:</em> Local AES-256-GCM authenticated encryption.</li>
                    <li><em>Client Wraps Key:</em> RSA-4096-OAEP asymmetric key wrapping.</li>
                    <li><em>POST /api/v1/files:</em> Multipart submission of ciphertext stream and metadata.</li>
                    <li><em>Decision (Verify Signature):</em> <code>file-service</code> validates the ECDSA signature. If <strong>Invalid</strong>, the upload is rejected with an HTTP 400 error. If <strong>Valid</strong>, the ciphertext is streamed to MinIO, metadata is saved to PostgreSQL, and HTTP 201 Created is returned.</li>
                </ol>
            </li>
            <li><strong>Download Branch:</strong>
                <ol>
                    <li><em>GET /api/v1/files/{id}:</em> Client requests file package.</li>
                    <li><em>Check DB Metadata:</em> Backend verifies access authorization in PostgreSQL.</li>
                    <li><em>Stream Ciphertext from MinIO:</em> Opaque ciphertext is streamed from S3 storage.</li>
                    <li><em>Return Ciphertext + Wrapped Key:</em> Backend delivers package to client.</li>
                    <li><em>Client Unwraps Key:</em> Client unwraps AES key using its private RSA key.</li>
                    <li><em>Client Decrypts File:</em> Client executes AES-GCM decryption in local memory, restoring original plaintext.</li>
                </ol>
            </li>
        </ul>
    </div>
</div>
"""
    return template.replace("IMG_USECASE_TAG", img_usecase_b64).replace("IMG_STORAGE_TAG", img_storage_b64).replace("IMG_SEQUENCE_TAG", img_sequence_b64)
