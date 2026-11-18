# FEATURE_PLAN.md: Per-User Private Space & Friends/Invite System

**Target:** MCA AJP-III-2026-T022  
**Author:** Lead Engineer, SecureFileShare  
**Date:** 2026-11-17  
**Status:** Awaiting User Approval  

---

## 1. Investigation & Current Codebase Audit

- **List Files Implementation Today**: `FileController.listFiles()` in `file-service` calls `FileService.listFiles(callerId)`, which queries `fileRepository.findByOwnerIdOrderByCreatedAtDesc(callerId)`. This currently isolates files by owner correctly. However, `share-service` and link resolution (`/shares/token/{token}`) allow accessing files without validating if the current caller is an authorized direct recipient beyond possessing the share token URL.
- **Owner ID Derivation**: `owner_id` is derived strictly from the authenticated JWT principal (`authentication.getPrincipal()` yielding the caller's `UUID`), never from the request body, preventing IDOR during uploads.
- **IDOR Risks Audit**:
  - `GET /files/{id}/package`: Checks `isOwner` or `hasValidShare` (placeholder currently returns true). **Risk**: Must be tightened to strict database grant verification.
  - `GET /shares/token/{token}`: Publicly resolves token without session check. **Mitigation**: Validates token active status and expiry.
  - `DELETE /files/{id}`: Needs owner verification.
- **Key Distribution & Public Key Registry**: Senders query `AuthServiceClient.getActiveKeys(userId)` in `auth-service` to retrieve registered RSA-4096 and EC P-256 public keys. Recipients store wrapped AES keys in `file_keys`.
- **User Identification**: Users are identified by internal UUIDs (`UUID`) and usernames (`String`).

---

## 2. Access Model (Private Space & Direct Grants)

- **Rules Matrix**:
  - **Owner**: Full access (List, View Metadata, Download, Delete, Share, Revoke).
  - **Direct Recipient (Friend)**: Metadata, Download (via stored `file_keys`), cannot Delete/Share/Revoke.
  - **Link Holder**: Download/Metadata (via valid, unexpired, unrevoked share token).
  - **Everyone Else**: `404 Not Found` (never `403 Forbidden` to prevent enumeration).
- **Central Authorization**: Implemented in a shared authorization component or service check (`FileAccessService`) validating ownership or active recipient grants across service boundaries.
- **Backfill Migration**: Flyway migration ensuring all existing files without an explicit owner default to a system admin UUID or remain associated with their original `owner_id`.

---

## 3. Friends and Invites System

- **Data Models**:
  - `friend_requests`: `id` (UUID), `requester_id` (UUID), `addressee_id` (UUID), `status` (`PENDING`, `ACCEPTED`, `DECLINED`, `CANCELED`, `BLOCKED`), `created_at`, `updated_at`.
  - `friendships`: Canonical ordering (`user_id_1`, `user_id_2` where `user_id_1 < user_id_2`), unique constraint.
  - `invites`: `id` (UUID), `user_id` (UUID), `invite_code` (VARCHAR, unique, unguessable), `expires_at`, `max_uses`, `use_count`, `revoked`.
- **Public Handle / Friend Code**: Unique 8-character alphanumeric handle per user (e.g., `SFS-7X9K`), stored in `users` table.
- **Privacy & Rate Limiting**: User search restricted to exact handle match only, rate-limited via Redis.

---

## 4. API Contract Summary

- `GET /api/v1/users/search?handle=...`: Find user by handle.
- `POST /api/v1/friends/requests`: Send friend request.
- `POST /api/v1/friends/requests/{id}/accept`: Accept request.
- `GET /api/v1/friends`: List friends.
- `POST /api/v1/invites`: Create invite link/code.
- `GET /api/v1/files/shared-with-me`: List files shared directly by friends.

---

## 5. Security & Abuse Mitigation

- **IDOR Protection**: All file operations return `404` for unauthorized resources.
- **Invite Spam**: Rate-limited pending requests (max 10 active pending requests per user).
- **Unfriend Behavior**: Unfriending retains existing shared files unless explicitly revoked by the owner.
- **Audit Logging**: Emit `FRIEND_REQUEST_SENT`, `FRIEND_ACCEPTED`, `SHARE_CREATED`, and `ACCESS_DENIED` events via the asynchronous outbox audit client.

---

## 6. Test Plan (TDD)

- **Unit Tests**: Access rules matrix, friend state machine, invite code generation.
- **Integration Tests (Testcontainers)**: User isolation, friend sharing round-trip, unfriend lifecycle.
- **Security Tests**: Guessed file ID returns 404, tampered links rejected.

---

## 7. Open Questions for Approval

1. Should "unfriend" automatically revoke all active file shares between the two users?
2. Should invite links be single-use by default or support multi-use with expiration?
3. Should friend codes be auto-generated upon registration for all existing users?
4. Is maximum pending friend requests capped at 20?
5. Should blocked users receive generic errors or silent drops?
6. Should file metadata search be case-insensitive?
7. Should direct sharing to friends require pre-wrapping the AES key in the client immediately?
8. Should admin user creation bypass friend requirements?

---

## Summary (Under 20 Lines)
This plan introduces per-user file isolation returning `404` for unauthorized accesses, a secure friend request and invite-code system, centralized authorization grants, and robust audit logging. All features will be built using strict TDD, Spring Boot conventions, and zero-knowledge cryptographic separation.
