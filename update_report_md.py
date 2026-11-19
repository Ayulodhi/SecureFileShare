# -*- coding: utf-8 -*-
import os

md_path = r"E:\project emp\pbl java\PROJECT_REPORT.md"
with open(md_path, "r", encoding="utf-8") as f:
    content = f.read()

# 1. Update List of Figures table
old_lof_start = content.find("## LIST OF FIGURES")
old_lot_start = content.find("## LIST OF TABLES")
if old_lof_start != -1 and old_lot_start != -1:
    new_lof = """## LIST OF FIGURES

| S. No. | Figure No. | Title | Page No. |
| :---: | :---: | :--- | :---: |
| 1 | Figure 2.1 | Use Case Diagram of SecureFileShare Platform | 22 |
| 2 | Figure 2.2 | Sequence Diagram of Recipient Ciphertext Upload and Key Sharing Pipeline | 26 |
| 3 | Figure 2.3 | Activity Diagram: Encrypted File Storage and Retrieval Flow | 28 |
| 4 | Figure 3.1 | Client-Service Monorepo Architecture of SecureFileShare | 32 |
| 5 | Figure 3.2 | Detailed Microservice Component & Storage Topology | 34 |
| 6 | Figure 3.3 | Database Entity Relationship (ER) Diagram | 37 |
| 7 | Figure 4.1 | Continuous Integration and Continuous Deployment (CI/CD) Pipeline | 70 |

<div style="page-break-after: always;"></div>

---

"""
    content = content[:old_lof_start] + new_lof + content[old_lot_start:]

# 2. Update Section 2.6 Use Case Diagram
uc_pattern_start = content.find("### 2.6 Use Case Diagram")
uc_pattern_end = content.find("### 2.7 Data Flow Diagram")
if uc_pattern_start != -1 and uc_pattern_end != -1:
    new_uc = """### 2.6 Use Case Diagram
The primary actors interacting with SecureFileShare are:
- **File Owner / Sender:** Registers, generates key pairs, encrypts files locally, uploads ciphertexts, manages sharing, and revokes access.
- **Recipient / Authorized User:** Browses shared files, fetches wrapped keys, decrypts files locally using their private RSA key, and verifies signatures.
- **System Administrator:** Monitors platform health, manages user states, and inspects the tamper-evident audit log and hash-chain integrity.

![Figure 2.1: Use Case Diagram of SecureFileShare Platform](images/fig1_use_case_overview.png)

<p align="center"><strong>Figure 2.1: Use Case Diagram of SecureFileShare Platform</strong></p>

> **Detailed Technical Explanation of Figure 2.1:**  
> Figure 2.1 models the core operations accessible to authenticated users within the SecureFileShare platform boundary: Register/Login, Manage Keys (RSA/EC), Upload Encrypted File, Download Decrypted File, Create Share Link (HMAC), Revoke Share Link, and View Audit Log. Core workflows are fully implemented; automated group vault delegations represent planned architectural extensions.

"""
    content = content[:uc_pattern_start] + new_uc + content[uc_pattern_end:]

# 3. Update Section 2.8 Sequence Diagram
seq_start = content.find("### 2.8 Sequence Diagram")
seq_end = content.find("### 2.9 Activity Diagram")
if seq_start != -1 and seq_end != -1:
    new_seq = """### 2.8 Sequence Diagram
The Sequence Diagram details the exact cryptographic and API call sequence during file upload and recipient key sharing:

![Figure 2.2: Sequence Diagram of Recipient Ciphertext Upload and Key Sharing Pipeline](images/fig3_sequence_upload_pipeline.png)

<p align="center"><strong>Figure 2.2: Sequence Diagram of Recipient Ciphertext Upload and Key Sharing Pipeline</strong></p>

> **Detailed Technical Explanation of Figure 2.2:**  
> Figure 2.2 illustrates the 11-step cryptographic sequence: File selection, random AES key generation, AES-256-GCM local encryption, recipient public key lookup, RSA-4096-OAEP key wrapping, SHA-256 hashing and ECDSA signing, multipart upload to file-service, signature verification, MinIO S3 streaming, and PostgreSQL metadata storage. Crucially, the server never sees plaintext or unwrapped keys.

"""
    content = content[:seq_start] + new_seq + content[seq_end:]

# 4. Update Section 2.9 Activity Diagram
act_start = content.find("### 2.9 Activity Diagram")
act_end = content.find("# CHAPTER 3: SYSTEM DESIGN")
if act_start != -1 and act_end != -1:
    new_act = """### 2.9 Activity Diagram
The Activity Diagram models decision branches governing uploads and downloads across the platform:

![Figure 2.3: Activity Diagram: Encrypted File Storage and Retrieval Flow](images/fig2_storage_workflow_activity.png)

<p align="center"><strong>Figure 2.3: Activity Diagram: Encrypted File Storage and Retrieval Flow</strong></p>

> **Detailed Technical Explanation of Figure 2.3:**  
> Figure 2.3 models the decision workflows: The upload branch executes local encryption, key wrapping, POST /api/v1/files, and signature verification (rejecting invalid signatures with HTTP 400). The download branch queries metadata, streams ciphertext from MinIO, returns the package, and unwraps/decrypts the file locally in browser memory.

<div style="page-break-after: always;"></div>

---

"""
    content = content[:act_start] + new_act + content[act_end:]

# 5. Update Section 3.1 System Architecture
arch_start = content.find("### 3.1 System Architecture")
er_start = content.find("### 3.2 ER Diagram & Data Modeling")
if arch_start != -1 and er_start != -1:
    new_arch = """### 3.1 System Architecture
SecureFileShare is architected as a decoupled microservice platform across independent operational layers:

![Figure 3.1: Client-Service Monorepo Architecture of SecureFileShare](images/fig4_client_service_arch.png)

<p align="center"><strong>Figure 3.1: Client-Service Monorepo Architecture of SecureFileShare</strong></p>

> **Detailed Technical Explanation of Figure 3.1:**  
> Figure 3.1 depicts the structural decomposition of the Maven monorepo: `client-app` (Desktop Client) and `web-app` (React SPA) communicate over HTTP APIs with `audit-service` (8084), `auth-service` (8081), `file-service` (8082), and `share-service` (8083), which all depend on the `common` shared DTO library.

The internal service component delegations and storage topology are illustrated below:

![Figure 3.2: Detailed Microservice Component & Storage Topology](images/fig5_component_storage_topology.png)

<p align="center"><strong>Figure 3.2: Detailed Microservice Component & Storage Topology</strong></p>

> **Detailed Technical Explanation of Figure 3.2:**  
> Figure 3.2 details the internal mechanics of each microservice: Web App Router routes to Web Crypto API; Desktop Client routes to CryptoEngine; `file-service` delegates to `FileService.java` for MinIO streaming and signature checks; `audit-service` computes SHA-256 hash chains; `auth-service` delegates to `KeyValidator` and `AuthService` with Redis rate limiting.

#### Layer Interaction Flow

| Step | From | To | Description |
| :---: | :--- | :--- | :--- |
| **1** | Client (Browser/Desktop) | Caddy Edge Gateway | Client initiates authenticated HTTPS request carrying JWT header. |
| **2** | Caddy Gateway | Target Microservice | Caddy inspects URL path and proxies request to appropriate microservice port. |
| **3** | `file-service` | `auth-service` | Resolves recipient user ID and retrieves registered public keys. |
| **4** | `file-service` | MinIO Storage | Streams encrypted multipart binary directly into isolated S3 storage buckets. |
| **5** | Microservices | Dedicated PostgreSQL | Executes queries and transactions via Spring Data JPA and Hibernate. |
| **6** | `auth-service` | Redis Cache | Checks rate-limit counters and verifies token is not in the revocation blacklist. |
| **7** | Microservices | `audit-service` | Dispatches security events (`UPLOAD`, `SHARE`, `REVOKE`) to append-only audit log. |
| **8** | `audit-service` | `audit_db` | Locks tail record (`FOR UPDATE`), calculates next SHA-256 hash, and inserts entry. |

"""
    content = content[:arch_start] + new_arch + content[er_start:]

# 6. Update Section 3.2 ER Diagram
er_sec_start = content.find("### 3.2 ER Diagram & Data Modeling")
schema_start = content.find("### 3.3 Database Design")
if er_sec_start != -1 and schema_start != -1:
    new_er = """### 3.2 ER Diagram & Data Modeling
The database architecture enforces strict microservice isolation. Each domain entity is decoupled to ensure that databases can scale independently:

![Figure 3.3: Database Entity Relationship (ER) Diagram](images/fig6_database_er_diagram.png)

<p align="center"><strong>Figure 3.3: Database Entity Relationship (ER) Diagram</strong></p>

> **Detailed Technical Explanation of Figure 3.3:**  
> Figure 3.3 shows the relational schema: `USERS` (`id` PK, `username` UK, `email` UK, `public_key_rsa`, `public_key_ec`) owns (1:N) `FILE_RECORDS`. `FILE_RECORDS` (`id` PK, `owner_id` FK, `ciphertext_path`, `wrapped_key`, `signature`) is shared via (1:N) `SHARE_RECORDS`. `AUDIT_RECORDS` stores append-only compliance logs linked by `prev_hash` and `current_hash`.

"""
    content = content[:er_sec_start] + new_er + content[schema_start:]

# 7. Add Section 4.7 Continuous Integration & Deployment Pipeline before Chapter 5
ch5_start = content.find("# CHAPTER 5: INPUT DESIGN")
if ch5_start != -1 and "### 4.7 Continuous Integration & Deployment Pipeline" not in content:
    cicd_sec = """### 4.7 Continuous Integration & Deployment Pipeline
The automated release pipeline executes continuous testing, vulnerability analysis, and cloud deployment:

![Figure 4.1: Continuous Integration and Continuous Deployment (CI/CD) Pipeline](images/fig7_cicd_release_pipeline.png)

<p align="center"><strong>Figure 4.1: Continuous Integration and Continuous Deployment (CI/CD) Pipeline</strong></p>

> **Detailed Technical Explanation of Figure 4.1:**  
> Figure 4.1 details the two-stage GitHub Actions pipeline: Continuous Integration (`ci.yml`) executes Gitleaks secret scanning, sets up Java 21 Temurin with Maven caching, runs `./mvnw -B verify` across all modules, runs OWASP Dependency-Check, and archives test reports. Continuous Deployment (`deploy.yml`) builds ARM64 multi-stage images, pushes to GHCR, connects via SSH to Oracle Cloud VM, synchronizes compose files, restarts containers, and executes health probes on ports 8081-8084 (automatically rolling back if health checks fail).

<div style="page-break-after: always;"></div>

---

"""
    content = content[:ch5_start] + cicd_sec + content[ch5_start:]

with open(md_path, "w", encoding="utf-8") as f:
    f.write(content)

print(f"Successfully updated {md_path}! New size: {len(content)} characters.")
