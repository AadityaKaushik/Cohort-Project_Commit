# Cohort — Concept Poster

*Smart Job Application Tracker with Federated Interview Prep*

## The problem

A student running a job-search campaign applies to 40–150 roles a season. That campaign's state lives across a spreadsheet, a notes app, and memory. Three specific failures follow:

1. **Pipeline amnesia** — applications stall silently; nobody notices because nothing surfaces it.
2. **Post-interview evaporation** — the most valuable artefact a candidate produces (what was actually asked, how it went) decays within 48 hours and is almost never captured structurally.
3. **Blind preparation** — students grind a generic question list instead of targeting the topics that actually cost them interviews.

## The solution

One system that closes the loop: **track → interview → capture what happened → convert that into targeted preparation → measure whether it worked**. Federated Learning then identifies which preparation patterns are associated with better outcomes across a peer cohort, without any user's raw data leaving their own data partition.

## Design decisions

| # | Idea | How it is implemented |
|---|---|---|
| D1 | **FL on separated data** | Each FL client is a separate process with its own isolated Postgres schema; the Flower server holds zero credentials to any user-content table. Stopping the server container and inspecting its filesystem shows no user data. |
| D2 | **Privacy Ledger** | Every federated round a user's client joined is logged: exact feature schema, pre/post-clip norm, noise multiplier, cohort size, and the actual transmitted vector — inspectable, not just promised. |
| D3 | **Evidence-tiered recommendations** | Every suggestion carries a tier (A: your data / B: thin data / C: federated / D: heuristic) and its language is enforced non-causal by a failing unit test (`test_no_causal_language`) if a banned phrase slips in. |
| D4 | **Topic Mastery Graph with decay** | Prep topics form a prerequisite DAG; mastery per topic decays over time and root-causes backward (a Shortest Path failure traces to weak BFS, not "practice more graphs"). |
| D5 | **Stall detection** | Flags applications that have exceeded *the user's own* historical median dwell time for that stage, with the sample size shown. |
| D6 | **Federated Lift viewer** | A live "Local only" vs "Local + Federated" toggle on recommendations, showing exactly what the federated model changed and by how much. |
| D7 | **Privacy budget as a UI object** | Epsilon spent, rounds remaining, and what happens at zero are all visible, not buried in a config file. |

## Architecture at a glance

```
React + TS SPA  →  Spring Boot 3 / Java 21 API  →  PostgreSQL 16
                          │
                          └── (Sprint 4) FederatedLearningGateway → fl-service (Python: Flower + scikit-learn)
                                          no user-content DB credentials, ever
```

Design patterns (each one addresses a specific coupling problem): **State** (status transitions), **Repository** (`userId`-required-by-signature isolation), **DTO + Mapper**, and — arriving through the rest of the build — Specification, Chain of Responsibility, Strategy, Observer, Template Method, Adapter, Facade, Builder, Decorator, Circuit Breaker. Full rationale in `docs/architecture.md` and `ARCHITECTURE_REVISION_JAVA.md` §4.

## Where the build stands (Sprint 1 — "Foundation")

- Auth (register/login/logout/refresh) — JWT + Argon2id, rate-limited, no account-enumeration timing leak.
- Applications — full CRUD, a State-pattern-enforced status machine (`Saved → … → Offer → Accepted/Declined`, terminal exits from any non-terminal stage), an immutable status-history timeline.
- Frontend — protected routes, a token-refresh interceptor, and a dense sortable applications table (the Kanban board is a second view; see `PROJECT_PLAN.md` §2.3).
- 238 backend tests, ~87% line coverage (JaCoCo-gated in CI), including a mandatory cross-user isolation check on every owned-data endpoint.

## What's next

Sprint 2 (resumes, interview capture, seed data) → Sprint 3 (prep graph, analytics, the Flower spike) → Sprint 4 (the FL boundary itself, circuit breaker, Privacy Ledger) → Sprint 5 (notifications, reports, hardening, delivery). Full phase-by-phase plan: `docs/plan/BUILD_PHASES_JAVA.md`.

---

*Cohort works on three levels: the peer cohort of job seekers, the FL client cohort, and the minimum-cohort-size privacy guarantee.*
