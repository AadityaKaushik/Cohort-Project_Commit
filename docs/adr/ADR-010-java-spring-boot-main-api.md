# ADR-010: Java/Spring Boot for the main API, Python retained only for Flower

**Status:** Accepted
**Date:** Sprint 1
**Source:** `ARCHITECTURE_REVISION_JAVA.md` (whole document); this ADR records the top-level decision it implements

## Context

`PROJECT_PLAN.md` originally specified a Python/FastAPI backend end to end, with Flower and scikit-learn handling the federated learning slice. Partway into planning, the team decided to build the main application in Java/Spring Boot instead, keeping Python only where a library genuinely leaves no alternative — Flower's client/server framework and scikit-learn's training code, both Python-only in practice.

The two live options were:

1. **All-Python** (the original plan): one language throughout, simplest mental model, but the whole team commits to FastAPI/SQLAlchemy/Alembic/pytest idioms with no separation between "the application" and "the ML experiment."
2. **Java for everything except Flower** (this decision): the application layer (auth, applications, resumes, interviews, prep, analytics, recommendations, notifications, reports) moves to Spring Boot; only the Flower client/server processes and their scikit-learn training code stay in Python, behind a network boundary.

## Decision

**Spring Boot 3 (Java 21) owns the entire user-facing application.** The `fl-service` Python process is deliberately kept small — Flower client/server plus scikit-learn training, clipping, and noise — and talks to the Java API only through a thin, explicit contract: a sanitised numeric feature export in, round metadata and model parameters out (never raw user data in either direction). `FederatedLearningGateway` / `FlowerServiceAdapter` (Adapter pattern) is the one class in the Java codebase that knows this contract exists; everything else in the API is unaware Python is involved at all.

This was the team's stated stack preference (mature DI container, first-class support for the design-pattern layer in `ARCHITECTURE_REVISION_JAVA.md` §4 — State, Repository, Specification, Template Method, and friends all have idiomatic, well-supported homes in Spring/Spring Data), and it has a secondary benefit the original plan didn't get for free: the privacy boundary in D1 is now enforced by a *language and process* boundary, not just a network one within the same runtime. Java is the only thing with database credentials; Python physically cannot have them without someone deliberately wiring a new dependency into a small, auditable service.

## Consequences

- **New failure mode, deliberately mitigated.** The FL "server" is no longer an in-process call — it's a network hop to a different process in a different language, which can be slow, down, or mid-deploy. This is why Sprint 4 adds a Resilience4j circuit breaker around `FlowerServiceAdapter` with a local-only fallback (§3 of `ARCHITECTURE_REVISION_JAVA.md`) — a piece of architecture the all-Python plan would not have needed, and now does.
- **Two toolchains to run**, not one: Maven + JUnit + Testcontainers for the API, pip + pytest for `fl-service`. `docker-compose.yml` and CI (P13) each need a job per language.
- **Team Java depth risk** (R15 in `ARCHITECTURE_REVISION_JAVA.md` §9): mitigated by front-loading Spring Boot fundamentals in Sprint 1 and keeping `fl-service` intentionally thin, so the Python surface area needing deep expertise stays small.
- The database schema, API contract, and frontend are **unchanged** by this decision (`ARCHITECTURE_REVISION_JAVA.md` §1) — a frontend developer, or an evaluator comparing this repo to the original plan's endpoint list, sees no difference there. The change is entirely in how the backend is implemented, not what it does.
