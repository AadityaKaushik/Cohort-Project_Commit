# Contributing

## Branching

Branch names follow `feature/US-XX-short-description` (or `chore/`, `fix/` for non-story work),
referencing the user story or phase ID from `docs/plan/BUILD_PHASES_JAVA.md`.

## Commit conventions

Conventional commits: `type(scope): description (US-XX)`.

Types: `feat`, `fix`, `test`, `docs`, `chore`, `refactor`.

Examples:
- `feat(auth): Spring Security + JWT + Argon2id (US-01,02,03,04)`
- `fix(applications): reject backward status transition without note (US-21)`

## Design patterns

Any commit that adds or changes a pattern-bearing class (see
`docs/plan/ARCHITECTURE_REVISION_JAVA.md` §4) must say, in one sentence in the PR description,
which pattern it implements or extends. This is a Definition-of-Done requirement, not optional
polish — it's how a reviewer tells deliberate design from accidental complexity.

## Tests

- Every phase that adds backend logic ships tests in the **same commit**: JUnit 5 for units/slices,
  MockMvc for controller tests, Testcontainers for anything touching real Postgres.
- Every phase touching user-owned data includes the cross-user isolation test for any new
  endpoint (expect 404, never 403, on foreign access — see FR-95).
- Run `mvn test` (in `api/`) or `pytest` (in `fl-service/`) before every commit. Never commit a
  red suite.

## Process

- One phase from the build-phases document per commit — phases are never merged together, even
  when a phase looks small.
- No phase starts before its listed dependency has landed.
