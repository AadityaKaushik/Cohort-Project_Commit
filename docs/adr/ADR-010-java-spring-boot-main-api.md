# ADR-010: Java/Spring Boot for the main API, Python retained only for Flower

Status: Accepted  
Date: Sprint 1  
Source: `ARCHITECTURE_REVISION_JAVA.md` (whole document); this ADR records the top-level decision it implements

## Context

`PROJECT_PLAN.md` originally specified a Python/FastAPI backend end to end, with Flower and scikit-learn handling the federated learning slice. Partway into planning, the team decided to build the main application in Java/Spring Boot instead, keeping Python only where a library has no Java equivalent: Flower's client/server framework and scikit-learn's training code.

The two live options were:

1. All-Python (the original plan): one language throughout, but the whole team works in FastAPI/SQLAlchemy/Alembic/pytest with no separation between the application and the ML experiment.
2. Java for everything except Flower (this decision): the application layer (auth, applications, resumes, interviews, prep, analytics, recommendations, notifications, reports) moves to Spring Boot; only the Flower client/server processes and their scikit-learn training code stay in Python, behind a network boundary.

## Decision

Spring Boot 3 (Java 21) owns the entire user-facing application. The `fl-service` Python process is kept small (Flower client/server plus scikit-learn training, clipping and noise) and talks to the Java API only through a narrow contract: a sanitised numeric feature export in, round metadata and model parameters out. Raw user data never crosses it. `FederatedLearningGateway` / `FlowerServiceAdapter` (Adapter pattern) is the only class in the Java codebase that knows this contract.

This is the team's preferred stack (mature DI container, and the design patterns in `ARCHITECTURE_REVISION_JAVA.md` §4 such as State, Repository, Specification and Template Method fit Spring/Spring Data). It also moves the privacy boundary in D1 to a language and process boundary. Java is the only component with database credentials, and the Python service cannot get them without a change to a small, reviewable service.

## Consequences

- New failure mode. The FL server is a network call to a separate process, which can be slow, down or mid-deploy. Sprint 4 therefore adds a Resilience4j circuit breaker around `FlowerServiceAdapter` with a local-only fallback (§3 of `ARCHITECTURE_REVISION_JAVA.md`).
- Two toolchains to run, not one: Maven + JUnit + Testcontainers for the API, pip + pytest for `fl-service`. `docker-compose.yml` and CI (P13) each need a job per language.
- Team Java depth risk (R15 in `ARCHITECTURE_REVISION_JAVA.md` §9): mitigated by covering Spring Boot fundamentals in Sprint 1 and keeping `fl-service` thin.
- The database schema, API contract, and frontend are unchanged by this decision (`ARCHITECTURE_REVISION_JAVA.md` §1). Only the backend implementation changes.
