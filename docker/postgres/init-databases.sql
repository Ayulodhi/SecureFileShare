-- ==============================================================================
-- SecureFileShare - PostgreSQL Multi-Database Initialization Script
-- WHAT: Creates individual isolated databases for each Spring Boot microservice.
-- WHY: Enforces strict database-per-service pattern so services cannot directly
--      cross-query each other's tables, preserving microservice isolation boundaries.
-- ==============================================================================

CREATE DATABASE sfs_auth;
CREATE DATABASE sfs_file;
CREATE DATABASE sfs_share;
CREATE DATABASE sfs_audit;
