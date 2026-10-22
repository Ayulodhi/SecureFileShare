-- ==============================================================================
-- SecureFileShare - PostgreSQL Multi-Database Initialization Script
-- WHAT: Creates individual isolated databases for each Spring Boot microservice.
-- WHY: Enforces strict database-per-service pattern so services cannot directly
--      cross-query each other's tables, preserving microservice isolation boundaries.
-- ==============================================================================

SELECT 'CREATE DATABASE sfs_auth'
WHERE NOT EXISTS (SELECT FROM pg_database WHERE datname = 'sfs_auth')\gexec

SELECT 'CREATE DATABASE sfs_file'
WHERE NOT EXISTS (SELECT FROM pg_database WHERE datname = 'sfs_file')\gexec

SELECT 'CREATE DATABASE sfs_share'
WHERE NOT EXISTS (SELECT FROM pg_database WHERE datname = 'sfs_share')\gexec

SELECT 'CREATE DATABASE sfs_audit'
WHERE NOT EXISTS (SELECT FROM pg_database WHERE datname = 'sfs_audit')\gexec
