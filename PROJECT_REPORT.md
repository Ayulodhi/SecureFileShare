# PROJECT REPORT ON
# SecureFileShare – Zero-Knowledge Encrypted File Sharing & Vault Platform

Submitted to the Department of Computer Applications in partial fulfillment for the award of the degree of  
**MASTER OF COMPUTER APPLICATIONS (MCA)**  
Batch (2025–2027)  

**Submitted by:**  
Ayu-lodhi  

**Under the Guidance of:**  
Project Supervisor / Faculty Guide  

Department of Computer Applications  
Graphic Era (Deemed to be University)  
Dehradun, Uttarakhand  
November 2026  

---

## TABLE OF CONTENTS
1. **Chapter 1: Introduction**
   - 1.1 Introduction
   - 1.2 Motivation
   - 1.3 Objectives & Scope
   - 1.4 Hardware & Software Requirements
   - 1.5 Tools & Technologies Used
   - 1.6 Organization of Report
2. **Chapter 2: System Analysis & SRS**
   - 2.1 Identification of Need
   - 2.2 Preliminary Investigation
   - 2.3 Feasibility Study
   - 2.4 Functional & Non-Functional Requirements
   - 2.5 Use Case & Activity Diagrams
3. **Chapter 3: System Design & Architecture**
   - 3.1 Microservice Architecture
   - 3.2 Database Design & Entity-Relationship Model
   - 3.3 Zero-Knowledge Cryptographic Vault & Key Management
   - 3.4 API Design & Security
4. **Chapter 4: Project Management & Implementation**
   - 4.1 Project Planning & Development Phases
   - 4.2 Microservice Decomposition (`auth-service`, `file-service`, `share-service`, `audit-service`)
   - 4.3 Containerization & Docker Orchestration
   - 4.4 Continuous Integration & Deployment (CI/CD)
5. **Chapter 5: Testing & Quality Assurance**
   - 5.1 Unit & Integration Testing
   - 5.2 Security & Cryptographic Verification
6. **Chapter 6: Conclusion & Future Enhancements**

---

## CHAPTER 1: INTRODUCTION

### 1.1 Introduction
In the modern digital era, the secure transfer and storage of sensitive files over cloud infrastructure is a critical requirement for individuals and enterprises. Traditional cloud storage providers (such as standard Google Drive or Dropbox) operate on a trusted-server model, where files are stored in plaintext or decrypted on the server side. This creates severe privacy vulnerabilities, exposing user data to insider threats, unauthorized surveillance, and cloud provider breaches.

**SecureFileShare** is an enterprise-grade, zero-knowledge encrypted file-sharing and vault platform. It guarantees that the server and cloud storage administrators **never** have access to plaintext files, encryption keys, or user private credentials. All cryptographic operations—including AES-256-GCM authenticated encryption, RSA-4096-OAEP key wrapping, and NIST P-256 ECDSA digital signatures—are performed strictly on the client side before any payload touches the network or storage layer.

### 1.2 Motivation
The primary motivation behind SecureFileShare stems from increasing data privacy regulations (such as GDPR and HIPAA) and the frequent occurrence of cloud data breaches. Standard file-sharing solutions fail to provide mathematical guarantees of confidentiality. By implementing a decentralized zero-knowledge architecture backed by microservices, Spring Boot 3, React, MinIO S3 object storage, and PostgreSQL, SecureFileShare ensures absolute data sovereignty for its users.

### 1.3 Objectives & Scope
The key objectives of the project are:
1. **Zero-Knowledge Architecture:** Ensure the server never decrypts data or holds user private keys or symmetric AES keys.
2. **Client-Side Cryptography:** Perform AES-256-GCM encryption, RSA key wrapping, and ECDSA signature generation directly within the browser/client application.
3. **Microservice Decomposition:** Separate responsibilities cleanly into independent Spring Boot microservices (`auth-service`, `file-service`, `share-service`, `audit-service`).
4. **Tamper-Evident Audit Logging:** Implement an immutable, cryptographically hashed audit chain to track compliance and access events.
5. **Secure Object Storage:** Utilize S3-compatible MinIO storage for storing opaque encrypted ciphertexts.

### 1.4 Hardware & Software Requirements
- **Development OS:** Windows 11 / Ubuntu 24.04 LTS
- **Runtime Environment:** Java 21 LTS (Temurin JDK), Node.js 22 LTS
- **Database & Storage:** PostgreSQL 16, Redis 7, MinIO Object Storage
- **Containerization:** Docker & Docker Compose
- **Build Tools:** Maven 3.9+, npm / Vite

### 1.5 Tools & Technologies Used
- **Backend Framework:** Spring Boot 3.3.4 (Spring Web, Spring Security, Spring Data JPA)
- **Frontend Framework:** React 19, TypeScript, Vite, Tailwind CSS
- **Database & Migration:** PostgreSQL, Flyway Database Migrations
- **Object Storage:** MinIO S3 SDK
- **Security & Cryptography:** JJWT (Java JWT), Web Crypto API (SubtleCrypto), BouncyCastle
- **DevOps & CI/CD:** GitHub Actions CI/CD Pipelines, Docker Buildx, Caddy Reverse Proxy

---

## CHAPTER 2: SYSTEM ANALYSIS & SRS

### 2.1 Identification of Need
Enterprises and privacy-conscious users require a file transfer mechanism that prevents cloud providers or compromised servers from inspecting confidential documents. SecureFileShare addresses this by ensuring zero knowledge at every trust boundary.

### 2.2 Functional & Non-Functional Requirements
- **Functional Requirements:**
  - User registration, login, and JWT-based stateless authentication.
  - Public key registry for storing RSA-4096 and EC P-256 public keys.
  - Zero-knowledge file upload with streaming ciphertexts and ECDSA signature verification.
  - Secure file sharing and multi-recipient key wrapping (`POST /files/{id}/keys`).
  - Tamper-evident audit logging with cryptographic hash chaining.
- **Non-Functional Requirements:**
  - High availability and scalability via independent microservices.
  - Strict confidentiality and integrity through authenticated encryption.
  - Low latency streaming uploads directly to MinIO object storage.

---

## CHAPTER 3: SYSTEM DESIGN & ARCHITECTURE

### 3.1 Microservice Architecture
SecureFileShare is structured as a multi-module Maven reactor backend interacting with a zero-knowledge React frontend:
- **`common`**: Shared DTOs, exception handling, and API response envelopes (`ApiResponse`).
- **`auth-service`**: User authentication, JWT issuance, friend connections, and cryptographic key registry.
- **`file-service`**: Zero-knowledge encrypted file ingestion, MinIO S3 streaming, signature validation, and metadata management.
- **`share-service`**: Secure file sharing permissions, access grants, and recipient management.
- **`audit-service`**: Immutable compliance audit log ingestion and hash-chain verification.

### 3.2 Zero-Knowledge Cryptographic Vault Model
1. **Symmetric Encryption:** The client generates a random, single-use AES-256-GCM key to encrypt the file locally.
2. **Key Wrapping:** The AES key is wrapped with the owner's (and recipient's) RSA-4096 public key using OAEP padding.
3. **Digital Signatures:** The client computes the SHA-256 hash of the ciphertext and signs it using their NIST P-256 EC private key.
4. **Server Ingestion:** The server verifies the ECDSA signature against the registered public key and streams the opaque ciphertext directly into MinIO S3 storage without ever seeing the plaintext or keys.

---

## CHAPTER 4: PROJECT MANAGEMENT & IMPLEMENTATION

### 4.1 Development Phases
- **Phase 1:** Core Microservice Setup & Common DTO Architecture.
- **Phase 2:** Zero-Knowledge File Service & MinIO Object Storage Integration.
- **Phase 3:** Share Service & Multi-Recipient Key Management.
- **Phase 4:** Audit Service & Cryptographic Hash Chain Verification.
- **Phase 5:** Production Containerization, Caddy Gateway, and CI/CD Pipeline Automation.

---

## CHAPTER 5: TESTING & QUALITY ASSURANCE
- **Unit Testing:** Verified core cryptographic engines (`CryptoEngine`, `JwtService`, `SignatureVerificationService`) using JUnit 5 and Mockito.
- **Integration Testing:** Verified database persistence and microservice interactions against containerized PostgreSQL, Redis, and MinIO instances.

---

## CHAPTER 6: CONCLUSION & FUTURE ENHANCEMENTS
SecureFileShare successfully demonstrates an enterprise-ready, zero-knowledge cloud file sharing platform. By strictly separating cryptography to the client side and maintaining modular Spring Boot microservices, the platform achieves absolute data confidentiality and operational resilience. Future enhancements may include end-to-end encrypted group workspaces and decentralized peer-to-peer key exchange.
