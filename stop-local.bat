@echo off
REM ==============================================================================
REM SecureFileShare - Local Development Teardown Script
REM Stops all running Spring Boot microservice processes and Docker containers.
REM ==============================================================================

echo Stopping Docker infrastructure containers...
docker compose down

echo Stopping any running Java Spring Boot services...
for /f "tokens=5" %%a in ('netstat -aon ^| findstr "LISTENING" ^| findstr ":8081 :8082 :8083 :8084"') do (
    taskkill /F /PID %%a 2>nul
)

echo Services stopped.
