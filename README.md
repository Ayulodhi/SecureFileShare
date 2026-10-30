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

---

## Production CI/CD Pipeline & Deployment

### Pipeline Architecture Overview
The automated deployment pipeline consists of two coordinated GitHub Actions workflows:

1. **Continuous Integration (`.github/workflows/ci.yml`)**:
   - Triggers on every push and pull request to `main` and `develop`.
   - Executes **Gitleaks** secret scanning across full commit history.
   - Sets up **Java 21 (Temurin)** with Maven caching.
   - Executes `./mvnw -B verify` across all modules (compilation + JUnit unit & integration tests).
   - Runs **OWASP Dependency-Check** against the National Vulnerability Database (CVSS threshold >= 8).
   - Archives Surefire test reports and OWASP vulnerability reports as downloadable artifacts.

2. **Continuous Deployment (`.github/workflows/deploy.yml`)**:
   - Triggers automatically upon merges or direct pushes to `main`.
   - Cross-compiles multi-stage `linux/arm64` container images using **QEMU** and **Docker Buildx**.
   - Publishes tagged container images (`${COMMIT_SHA}` and `latest`) to **GitHub Container Registry (GHCR)**.
   - Connects securely to the Oracle Cloud ARM VM via SSH (`sfsdeploy` user).
   - Copies `docker-compose.prod.yml`, `Caddyfile`, and initialization scripts to `/opt/securefileshare`.
   - Generates production `.env` with atomic file permissions (`chmod 600`).
   - Executes `docker compose pull` and rolling update `docker compose up -d`.
   - Probes microservice `/api/v1/health` endpoints; if any probe fails or times out within 90 seconds, it automatically triggers a rollback to the previous running commit tag.

---

## Manual Rollback Runbook

If an emergency manual rollback is required (e.g. unexpected runtime regression or data incompatibility):

### Step 1: Connect to Production Server
```bash
ssh sfsdeploy@<SERVER_HOST>
cd /opt/securefileshare
```

### Step 2: Identify Previous Working Image Tag or Commit SHA
Inspect the current container status and previous releases:
```bash
# View running container versions
docker compose -f docker-compose.prod.yml ps

# Inspect git commit history or GHCR package tags to select target SHA:
# https://github.com/ayu-lodhi/securefileshare/pkgs/container/securefileshare%2Fauth-service
```

### Step 3: Update IMAGE_TAG in `.env`
Edit the `.env` file to set `IMAGE_TAG` to the desired known-good commit SHA (e.g., `962fa64`):
```bash
# Update IMAGE_TAG to the known good commit SHA
sed -i 's/^IMAGE_TAG=.*/IMAGE_TAG=962fa6443c22ad39ecfc875691090332822a188f/' .env

# Verify change
grep '^IMAGE_TAG=' .env
```

### Step 4: Pull Target Images and Restart Containers
```bash
# Pull the target images from GHCR
docker compose -f docker-compose.prod.yml pull

# Recreate containers with zero downtime
docker compose -f docker-compose.prod.yml up -d --remove-orphans
```

### Step 5: Verify System Health
```bash
# Verify container health status
docker compose -f docker-compose.prod.yml ps

# Confirm health endpoints respond 200 OK
curl -i https://<SERVER_DOMAIN>/api/v1/auth/health
```
