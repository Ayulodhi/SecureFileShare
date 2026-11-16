# Structured Implementation Plan: SecureFileShare Phase-III Completion

**Target:** MCA AJP-III-2026-T022  
**Author:** Lead Engineer, SecureFileShare  
**Date:** 2026-11-16  
**Status:** Approved for planning; implementation pending user authorization.

---

## Zero-Knowledge Architectural Constraint
> **CRITICAL MANDATE**: The server (all backend services) must **NEVER** hold a plaintext file, a symmetric AES key, or a private user key (RSA or EC). All cryptographic operations happen exclusively on client devices (web or desktop). Servers act strictly as untrusted ciphertext storage, signature verifiers, and access-control gateways.

---

## 1. Share Service (Port 8083)

### Goal and Scope
- **In Scope**: Implement persistent recipient-bound, expiring, and revocable share links with JWT access control in `share-service`. Provide API endpoints matching the web and desktop clients (`/api/v1/shares`, `/api/v1/shares/{id}`, `/api/v1/shares/token/{token}`).
- **Out Scope**: Direct file decryption or key re-encryption on the server.

### API Contract
- `GET /api/v1/shares`: Returns list of shares created by authenticated user (`ShareRecord[]`).
- `POST /api/v1/shares`: Creates a share link. Accepts `CreateShareRequest` (`fileId`, `fileName`, `recipientUsername`, `expiryOption`). Returns `ShareRecord`.
- `DELETE /api/v1/shares/{shareId}`: Revokes an active share. Returns `204 No Content`.
- `GET /api/v1/shares/token/{token}`: Resolves public share token (used by recipient landing page). Returns `ShareRecord`.

### Data Model
- **Table**: `shares` (Flyway migration: `V1__initial_share_schema.sql` or `V2__share_links.sql`)
  - `id` (VARCHAR / UUID, PK)
  - `file_id` (UUID, FK to file record)
  - `owner_id` (UUID)
  - `recipient_username` (VARCHAR)
  - `share_token` (VARCHAR, UNIQUE)
  - `expires_at` (TIMESTAMP)
  - `status` (VARCHAR: `ACTIVE`, `REVOKED`, `EXPIRED`)
  - `created_at` (TIMESTAMP)
- **Indexes**: `idx_shares_token`, `idx_shares_owner`, `idx_shares_file`.

### Security Considerations
- Token brute-force defense: High-entropy crypto-random tokens (`sh_` + random base36).
- Authorization check: Only the file owner can create or revoke shares. Expiration is enforced on lookup.

### Task Breakdown (< 0.5 days per task)
1. **Task 1.1**: Create Flyway migration for `shares` table and JPA Entity.
2. **Task 1.2**: Implement `ShareRepository` and business service (`ShareService`) with expiry and revocation logic.
3. **Task 1.3**: Implement `ShareController` exposing `/api/v1/shares` endpoints matching client API contracts.
4. **Task 1.4**: Integrate JWT authentication filter and owner authorization checks.

---

## 2. Audit Service (Port 8084)

### Goal and Scope
- **In Scope**: Append-only, hash-chained audit log in `audit-service` (each entry stores SHA-256 hash of `prev_hash + entry_data`), admin verification endpoint (`GET /api/v1/audit/verify`), paginated entry retrieval (`GET /api/v1/audit/entries`), and event ingestion from Auth, File, and Share services.
- **Out Scope**: Real-time streaming websocket notification of audit events.

### API Contract
- `GET /api/v1/audit/entries?page=0&size=50&actor=&action=`: Returns paginated `AuditEntry[]`. Requires ROLE_ADMIN.
- `GET /api/v1/audit/verify`: Verifies the entire hash chain from entry 1 to N. Returns `AuditVerifyResult` (`valid`, `checkedCount`, `brokenEntryId`, `message`). Requires ROLE_ADMIN.
- `POST /api/v1/audit/events`: Internal microservice ingestion endpoint for logging actions (`LOGIN`, `LOGOUT`, `UPLOAD`, `DOWNLOAD`, `SHARE_CREATED`, `SHARE_REVOKED`).

### Data Model
- **Table**: `audit_entries` (Flyway migration: `V1__initial_audit_schema.sql`)
  - `id` (BIGSERIAL, PK)
  - `timestamp` (TIMESTAMP)
  - `actor` (VARCHAR)
  - `action` (VARCHAR)
  - `file_name` (VARCHAR)
  - `hash` (VARCHAR(64), SHA-256)
  - `previous_hash` (VARCHAR(64), SHA-256)
- **Indexes**: `idx_audit_timestamp`, `idx_audit_action`, `idx_audit_actor`.

### Security Considerations
- Tamper-evidence: Any modification to past database rows breaks the cryptographic hash chain.
- Role restriction: Audit log inspection and chain verification are restricted to `ROLE_ADMIN`.

### Task Breakdown (< 0.5 days per task)
1. **Task 2.1**: Create Flyway migration for `audit_entries` table and JPA Entity.
2. **Task 2.2**: Implement hash-chain generator (calculating `SHA-256(prevHash + timestamp + actor + action + file)`).
3. **Task 2.3**: Implement `AuditService` with append-only integrity enforcement and verification loop.
4. **Task 2.4**: Implement `AuditController` (`/api/v1/audit/entries`, `/api/v1/audit/verify`) with ADMIN role security.
5. **Task 2.5**: Wire inter-service event publishing (Auth, File, Share -> Audit service).

---

## 3. Desktop Client Crypto Engine and Keystore (Java Swing)

### Goal and Scope
- **In Scope**: Port web client crypto (AES-256-GCM, RSA-4096 OAEP wrap, ECDSA, SHA-256) to Java using Bouncy Castle. Replace `client-app` stub services (`StubEncryptionService`, `MockFileApi`, `MockShareApi`, `StubMalwareScanner`) with real HTTP API calls against backend microservices.
- **Out Scope**: Redesigning Swing UI layout.

### Technical Approach
- Use Bouncy Castle provider (`org.bouncycastle:bcprov-jdk18on`) for secp256r1 ECDSA signing and RSA-4096-OAEP wrapping.
- Implement `RealFileApi`, `RealShareApi`, `RealAuthApi`, `RealAuditApi` using `java.net.http.HttpClient`.

### Task Breakdown (< 0.5 days per task)
1. **Task 3.1**: Implement Bouncy Castle crypto utility for AES-256-GCM encryption/decryption, RSA key generation/wrapping, and ECDSA signing.
2. **Task 3.2**: Implement `RealAuthApi` and `RealFileApi` in `client-app`.
3. **Task 3.3**: Replace `UploadController` and `ReceiveController` stubs with real API communication and streaming file upload/download.
4. **Task 3.4**: Integrate local keystore persistence using encrypted file storage in user home directory.

---

## 4. Remaining Phase 7 Security Tests

### Goal and Scope
- **In Scope**: Implement integration/security tests for:
  - Revoked share access attempt (`403 Forbidden`).
  - Modified ciphertext payload upload rejection (`400 Bad Request` + MinIO purge).
  - Tampered audit row detection (Java / JS test asserting chain break).
- **Out Scope**: Manual penetration testing.

### Task Breakdown (< 0.5 days per task)
1. **Task 4.1**: Add revoked share test in `ShareServiceIntegrationTest.java`.
2. **Task 4.2**: Add modified ciphertext upload test in `FileServiceIntegrationTest.java`.
3. **Task 4.3**: Add hash-chain tampering test in `AuditServiceIntegrationTest.java` / `AuditVerifyTest.java`.

---

## 5. Phase 6 Security Hardening

### Goal and Scope
- **In Scope**:
  - Remove insecure `TrustAllManager` from `RealAuditApi.java` and configure proper TLS certificate pinning or standard truststore.
  - Disable web client local fallback in production builds (`NODE_ENV === 'production'`).
  - Audit rate limiting across all public ingress points.
  - Comprehensive security headers review in Caddyfile and Spring Security configs.
- **Out Scope**: External commercial Web Application Firewall (WAF) deployment.

### Task Breakdown (< 0.5 days per task)
1. **Task 5.1**: Replace insecure SSL context in `RealAuditApi.java` with standard JVM truststore validation.
2. **Task 5.2**: Update `apiClient.ts` to disable local fallback storage when `import.meta.env.PROD` is true.
3. **Task 5.3**: Review and tighten CORS, CSRF, and CSP security headers.

---

## 6. Monitoring (Prometheus, Grafana, Loki within 512 MB RAM limit)

### Goal and Scope
- **In Scope / Feasibility Analysis**: Running Prometheus + Grafana + Loki inside a 512 MB RAM VPS is **resource-prohibitive** (Prometheus & Grafana alone consume ~400–600 MB idle).
- **Proposed Alternative**: Lightweight resource-efficient monitoring:
  - Spring Boot Actuator `/actuator/prometheus` metrics endpoint exposed internally.
  - Lightweight log aggregation via Docker container log rotation and local log files (`/var/log/securefileshare/`).
  - Health check alerts via lightweight shell cron monitor script checking microservice HTTP `/health` endpoints every 60 seconds.

---

## 7. Optional: Chunked Upload (Lifting 100 MB Cap)

### Goal and Scope
- **In Scope**: Multipart chunked upload protocol (initialize upload -> upload 5MB chunks -> complete upload with combined SHA-256 and ECDSA signature).
- **Out Scope**: Resumable multi-day uploads.

---

## Test-Driven Development Approach (Red-Green-Refactor)

### 1. Unit Tests
- **Framework**: JUnit 5, Mockito, AssertJ (backend); Vitest (web-app); JUnit 5 (client-app).
- **Scope**: Pure logic (JWT validation, expiry rules, revocation checks, hash-chain math, crypto round-trip).
- **Coverage Goal**: Minimum **85% line coverage** enforced via JaCoCo plugin in Maven.

### 2. Integration Tests
- **Framework**: Spring Boot Test with Testcontainers (PostgreSQL 16, Redis, MinIO).
- **Scope**: Flyway migrations, JWT auth filters, service-to-service calls (File -> Audit, Share -> Audit), cross-client interop (encrypt in web, download/decrypt in desktop).

### 3. Security Tests (Phase 7)
- Wrong token, expired token, revoked token, revoked share, wrong recipient, modified ciphertext, tampered audit row, rate-limit abuse.

### 4. Performance Tests
- **Tool**: **k6** (lightweight Go binary, excellent JSON metrics output, minimal RAM footprint).
- **Scenarios**:
  - Login burst (50 concurrent logins).
  - File upload/download of 1 MB, 10 MB, and 100 MB.
  - Audit log append throughput (100 req/sec).
  - Audit chain verification time as log grows (1k, 10k, 100k entries).
- **Thresholds**: p95 latency < 500ms, error rate < 0.1%, RAM usage stable under 450 MB. Run locally via Docker Compose before deployment.

### 5. CI Integration
- **Every Push**: Unit tests + Gitleaks secret scan.
- **Pull Requests**: Integration tests + OWASP Dependency-Check.
- **Nightly / Manual**: Performance (k6) & security test suites.

---

## Phased Timeline & Milestones

| Phase | Milestone | Duration | DoD (Definition of Done) |
|---|---|---|---|
| **Phase 1** | Share Service Implementation | 2 days | All share tests pass, contracts match web/desktop clients |
| **Phase 2** | Audit Service & Hash Chain | 2 days | Append-only log, hash verification, Actuator integration pass |
| **Phase 3** | Desktop Crypto & Real APIs | 3 days | Java Bouncy Castle crypto + real backend calls working |
| **Phase 4** | Security Tests & Hardening | 2 days | Phase 7 tests green, TLS insecure skip removed, fallback disabled in prod |
| **Phase 5** | Performance & CI Finalization | 1 day | k6 performance benchmarks pass, CI pipeline green |

---

## Test Pyramid Summary Table

| Test Type | Count Planned | Tools | Execution Environment |
|---|---|---|---|
| Unit Tests | 80+ | JUnit 5, Vitest, AssertJ | Local / CI (Every Push) |
| Integration Tests | 25+ | Spring Boot Test, Testcontainers | Local / CI (PR) |
| Security Tests | 15+ | MockMvc, Testcontainers | Local / CI (PR) |
| Performance Tests | 4 scenarios | k6 | Isolated Docker Compose |

---

## Risk Register (Top 5 Risks)

1. **Memory Exhaustion on 512 MB VPS**: Mitigated by selecting lightweight monitoring alternatives (Actuator + shell health checks) instead of heavy Prometheus/Grafana stacks.
2. **Client Crypto Incompatibility**: Mitigated by shared test vectors between TypeScript Web Crypto and Java Bouncy Castle.
3. **Hash Chain Drift in Audit Service**: Mitigated by database-level table locks (`SELECT ... FOR UPDATE`) on append.
4. **Token Expiry Sync Issues**: Mitigated by NTP time synchronization in Docker containers.
5. **CORS / Preflight Failures**: Mitigated by centralized Spring Security CORS configuration in gateway.

---

## Traceability Table

| Requirement / Deliverable | Tasks | Test Suites |
|---|---|---|
| C.4 Share Links (Expiring / Revocable) | Tasks 1.1–1.4 | `ShareServiceIntegrationTest`, Web API tests |
| C.5 Hash-Chained Audit Log | Tasks 2.1–2.5 | `AuditServiceIntegrationTest`, `AuditVerifyTest` |
| F.28 Desktop Client Real API & Crypto | Tasks 3.1–3.4 | `client-app` integration tests |
| Phase 7 Security Tests | Tasks 4.1–4.3 | `Phase7SecurityTest`, `FileServiceIntegrationTest` |
| Phase 6 Security Hardening | Tasks 5.1–5.3 | Security regression tests |

---

## Git Branching & PR Strategy
- `main`: Production-ready release branch (protected, requires PR + CI green).
- `develop`: Integration branch for ongoing feature development.
- Feature branches (`feat/share-service`, `feat/audit-service`, `feat/desktop-crypto`): Merged into `develop` via PR after passing Unit and Integration tests.
