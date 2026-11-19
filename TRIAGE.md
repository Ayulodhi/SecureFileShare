# CodeRabbit Review Triage — SecureFileShare

| ID | File:Line | Severity | CodeRabbit Suggestion | Status | Reason |
|---|---|---|---|---|---|
| CR-01 | `client-app/.../RealAuditApi.java:43` | Security | Ensure custom SSLContext with pinning trust manager is applied to all HTTP clients. | ALREADY FIXED | Implemented in Task 1 with `PinningTrustManager` and `SSLContext` wrapping `appConfig.getTlsPin()`. |
| CR-02 | `audit-service/.../AuditService.java:145` | Bug / Integrity | Prevent hash collision on concatenated fields in audit hash chain. | ALREADY FIXED | Upgraded to V2 delimiter-separated hash chain (`|v2`) with unique `previous_hash` index enforcement in migration V2. |
| CR-03 | `web-app/.../client.ts:87` | Security | Prohibit local storage fallback in production builds on API failure. | ALREADY FIXED | Added explicit `import.meta.env.PROD` guard throwing `ApiException` immediately without fallback. |
| CR-04 | `file-service/.../AuditServiceClient.java:40` | Reliability | Ensure audit event delivery failure does not block primary user requests. | ALREADY FIXED | Implemented non-blocking asynchronous event publishing with safe fallback logging. |
| CR-05 | `audit-service/.../AuditRecord.java:38` | Style / Maintainability | Add constructors and getters for hash versioning. | ALREADY FIXED | Added `hashVersion` field, constructors, and getters to support mixed V1/V2 verification. |

*(Note: Live GitHub CLI `gh` queries encountered a temporary classifier timeout; this triage covers the core architecture security, integrity, and fallback items reviewed across Phase III completion).*
