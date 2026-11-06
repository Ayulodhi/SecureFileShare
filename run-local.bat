@echo off
REM ==============================================================================
REM SecureFileShare - Local Development Startup Script
REM Starts Docker backing containers (Postgres, MinIO, Redis) and launches
REM all 4 Spring Boot microservices in separate background windows.
REM ==============================================================================

echo [1/3] Starting Docker infrastructure containers (Postgres, MinIO, Redis)...
docker compose up -d
if %ERRORLEVEL% NEQ 0 (
    echo [ERROR] Docker compose failed to start. Ensure Docker Desktop is running.
    exit /b %ERRORLEVEL%
)

echo [2/3] Waiting for containers to become healthy...
timeout /t 5 /nobreak >nul

echo [3/3] Launching backend microservices in separate windows...
start "SFS - Auth Service (Port 8081)" cmd /k "mvnw.cmd -pl auth-service spring-boot:run"
start "SFS - File Service (Port 8082)" cmd /k "mvnw.cmd -pl file-service spring-boot:run"
start "SFS - Share Service (Port 8083)" cmd /k "mvnw.cmd -pl share-service spring-boot:run"
start "SFS - Audit Service (Port 8084)" cmd /k "mvnw.cmd -pl audit-service spring-boot:run"

echo.
echo All microservices launched in individual windows.
echo Verification endpoints:
echo   - Auth Service:  http://localhost:8081/api/v1/health
echo   - File Service:  http://localhost:8082/api/v1/health
echo   - Share Service: http://localhost:8083/api/v1/health
echo   - Audit Service: http://localhost:8084/api/v1/health
echo.
echo To run the desktop client GUI, execute:
echo   mvnw.cmd -pl client-app exec:java
