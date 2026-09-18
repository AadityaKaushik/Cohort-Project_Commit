# ADR-001: JWT access tokens + Argon2id password hashing

**Status:** Accepted
**Date:** Sprint 1
**Source:** `PROJECT_PLAN.md` §15 (Technology Stack), carried into the Java stack unchanged (`ARCHITECTURE_REVISION_JAVA.md` §2)

## Context

The system needs to authenticate users statelessly (NFR-23: the backend must be horizontally replicable with no server-side session affinity) while still allowing a session to be genuinely revoked on logout (FR-06) — a pure stateless JWT scheme can't do the latter, since a signed token remains valid until it expires no matter what the server does.

Separately, passwords must be stored in a way that resists offline cracking even if the database is exfiltrated (FR-04, NFR-01), which rules out fast general-purpose hashes (SHA-256, even salted) and narrows the field to a deliberately slow, memory-hard KDF.

## Decision

- **Access tokens are short-lived, self-issued, signed JWTs** (HS256, via Spring Security's Nimbus-based `JwtEncoder`/`JwtDecoder`), TTL ≤ 15 minutes (NFR-02). They carry the user's ID as `sub` and nothing sensitive — the resource server validates the signature and expiry on every request, with no database round-trip.
- **Refresh tokens are opaque, database-backed, and individually revocable.** Each is stored as a hash (never the raw token) in `refresh_tokens`, with an `expires_at` and a nullable `revoked_at`. Logout (FR-06) sets `revoked_at`; a subsequent refresh attempt with that token is rejected. This is what makes "log out" a real guarantee rather than "wait up to 15 minutes."
- **Passwords are hashed with Argon2id** (`org.springframework.security.crypto.argon2.Argon2PasswordEncoder`, backed by Bouncy Castle since Spring Security doesn't bundle an Argon2 implementation itself), memory cost ≥ 64 MiB, time cost ≥ 3, a unique salt per password (NFR-01). Argon2id was chosen over bcrypt specifically because it is memory-hard: bcrypt's fixed, small memory footprint makes it comparatively cheap to attack with GPU/ASIC hardware, and Argon2id is the current OWASP-recommended default for new systems.

## Consequences

- Two token types means two pieces of client-side state to manage and a refresh interceptor to write (done in P08) — more moving parts than a single long-lived token, but it's what buys both statelessness *and* real revocation.
- A compromised access token is valid until it naturally expires (≤ 15 minutes) — there is no server-side kill switch for an individual access token, only for refresh tokens. This is an accepted trade-off given the short TTL.
- Argon2id's deliberate slowness (by design) means login and registration pay a fixed CPU/memory cost per request; `AuthService` also runs a dummy Argon2 verification against a non-existent user so failed-login timing doesn't leak whether an email exists (FR-02), which means *every* login attempt — valid or not — pays this cost by design.
