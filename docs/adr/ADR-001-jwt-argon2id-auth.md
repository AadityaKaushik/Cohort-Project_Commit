# ADR-001: JWT access tokens + Argon2id password hashing

Status: Accepted  
Date: Sprint 1  
Source: `PROJECT_PLAN.md` §15 (Technology Stack), carried into the Java stack unchanged (`ARCHITECTURE_REVISION_JAVA.md` §2)

## Context

The system needs to authenticate users statelessly (NFR-23: the backend must be horizontally replicable with no server-side session affinity) while still allowing a session to be revoked on logout (FR-06). A purely stateless JWT scheme cannot do this, because a signed token stays valid until it expires.

Separately, passwords must be stored in a way that resists offline cracking even if the database is exfiltrated (FR-04, NFR-01), which rules out fast general-purpose hashes (SHA-256, even salted) and leaves a slow, memory-hard KDF.

## Decision

- Access tokens are short-lived, self-issued, signed JWTs (HS256, via Spring Security's Nimbus-based `JwtEncoder`/`JwtDecoder`), TTL ≤ 15 minutes (NFR-02). They carry the user's ID as `sub` and nothing sensitive. The resource server validates the signature and expiry on every request, with no database round-trip.
- Refresh tokens are opaque, database-backed, and individually revocable. Each is stored as a hash (never the raw token) in `refresh_tokens`, with an `expires_at` and a nullable `revoked_at`. Logout (FR-06) sets `revoked_at`; a subsequent refresh attempt with that token is rejected. This makes logout take effect immediately instead of after up to 15 minutes.
- Passwords are hashed with Argon2id (`org.springframework.security.crypto.argon2.Argon2PasswordEncoder`, backed by Bouncy Castle because Spring Security does not bundle an Argon2 implementation), memory cost ≥ 64 MiB, time cost ≥ 3, a unique salt per password (NFR-01). Argon2id was chosen over bcrypt because it is memory-hard. bcrypt's small, fixed memory use makes it cheaper to attack with GPU/ASIC hardware, and OWASP recommends Argon2id for new systems.

## Consequences

- Two token types means two pieces of client-side state to manage and a refresh interceptor to write (done in P08). This is more complex than one long-lived token, but it gives both statelessness and revocation.
- A compromised access token is valid until it naturally expires (≤ 15 minutes), an individual access token cannot be revoked, only refresh tokens. This trade-off is accepted because of the short TTL.
- Argon2id is slow by design, so login and registration pay a fixed CPU and memory cost per request. `AuthService` also runs a dummy Argon2 verification for unknown emails so failed-login timing does not reveal whether an email exists (FR-02). Every login attempt therefore pays this cost.
