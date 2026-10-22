# SecureFileShare - Backend

Zero-Knowledge End-to-End Encrypted File Sharing Platform (Backend Services).

## Architecture Stack
- **Language**: Java 21 (LTS)
- **Framework**: Spring Boot 3.3.4
- **Database**: PostgreSQL 16 (isolated DB per service)
- **Object Storage**: MinIO (S3-compatible)
- **Cache & Rate Limiting**: Redis 7
- **Database Migrations**: Flyway
- **Testing**: JUnit 5, AssertJ, Mockito

## Microservices & Assigned Ports
- `common`: Shared DTOs, security constants, and base exception handlers.
- `auth-service`: Port `8081` (Identity, RSA-4096 & EC P-256 Public Key Registry, JWT, Redis Rate Limiting)
- `file-service`: Port `8082` (Zero-knowledge MinIO ciphertext streaming, integrity verification, wrapped keys)
- `share-service`: Port `8083` (HMAC share links, recipient validation, instantaneous revocation)
- `audit-service`: Port `8084` (Tamper-evident audit log events)

---

## Getting Started

### 1. Prerequisites
- Java 21 JDK installed (`java -version`)
- Maven 3.9+ installed (`mvn -version`)
- Docker & Docker Compose installed and running

### 2. Start Infrastructure Containers
Start PostgreSQL, MinIO, and Redis:
```bash
docker compose up -d
```
Verify container health:
```bash
docker compose ps
```

### 3. Build the Project
From the repository root:
```bash
mvn clean install
```

### 4. Run the Services
Each service can be launched independently:
```bash
# In separate terminal tabs:
mvn -pl auth-service spring-boot:run
mvn -pl file-service spring-boot:run
mvn -pl share-service spring-boot:run
mvn -pl audit-service spring-boot:run
```

### 5. Verify /health Endpoints
- `auth-service`: `curl http://localhost:8081/api/v1/health`
- `file-service`: `curl http://localhost:8082/api/v1/health`
- `share-service`: `curl http://localhost:8083/api/v1/health`
- `audit-service`: `curl http://localhost:8084/api/v1/health`
# SecureFileShare
# SecureFileShare
