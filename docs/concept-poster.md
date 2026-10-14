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
                          └── FederatedLearningGateway → fl-service (Python: Flower + scikit-learn)
                                          no user-content DB credentials, ever
```

Design patterns (each one addresses a specific coupling problem): State, Repository, DTO + Mapper, Strategy, Chain of Responsibility, Template Method, Observer, Adapter, Facade, Builder, Decorator and Circuit Breaker. Rationale in `docs/architecture.md` and `ARCHITECTURE_REVISION_JAVA.md` section 4.

## Where the build stands

- Auth: register, login, logout, refresh, password reset, email verification and optional social sign-in; JWT + Argon2id, rate-limited.
- Applications: CRUD, a State-pattern status machine and an immutable status-history timeline.
- Resumes, interview debriefs, preparation graph, analytics, recommendations, notifications, reports and the Privacy Ledger.
- Federated learning: the `fl-service` pipeline, the Java gateway with a circuit breaker, opt-in consent and the Federated Lift viewer.
- Every owned-data endpoint has a cross-user isolation test that expects 404.

The phase-by-phase plan (110 phases, nine sprints) is in `docs/plan/BUILD_PHASES_JAVA.md`.

---

*Cohort works on three levels: the peer cohort of job seekers, the FL client cohort, and the minimum-cohort-size privacy guarantee.*
