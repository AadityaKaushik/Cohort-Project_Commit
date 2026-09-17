# Cohort

Cohort is a job-search campaign manager: applications, resumes, interview debriefs, and
preparation tracking in one place, with a **Federated Learning** layer that lets the app get
smarter across users' preparation patterns without ever centralising anyone's real data.

Full plain-language explanation: [`docs/plan/PROJECT_OVERVIEW.md`](docs/plan/PROJECT_OVERVIEW.md).
Requirements, backlog, and design detail: [`docs/plan/PROJECT_PLAN.md`](docs/plan/PROJECT_PLAN.md)
and [`docs/plan/ARCHITECTURE_REVISION_JAVA.md`](docs/plan/ARCHITECTURE_REVISION_JAVA.md).
Build sequencing: [`docs/plan/BUILD_PHASES_JAVA.md`](docs/plan/BUILD_PHASES_JAVA.md).

## Stack

| Layer | Technology |
|---|---|
| Frontend | React + TypeScript + Tailwind CSS + shadcn/ui + Recharts |
| Main backend | Java 21 + Spring Boot 3 (auth, applications, resumes, interviews, prep, analytics, recommendations, notifications, reports) |
| Database | PostgreSQL 16 + Spring Data JPA/Hibernate, versioned with Flyway |
| Federated Learning | Python + Flower + scikit-learn — isolated to `fl-service/`, the only Python in the system |
| Resilience | Resilience4j circuit breaker around the Java → FL boundary |
| Testing | JUnit 5, MockMvc, Testcontainers · Pytest · React Testing Library |

## Repository layout

```
api/            Spring Boot application (Java 21, Maven)
fl-service/     Flower client/server + scikit-learn, isolated Postgres schema
frontend/       React + TypeScript + Tailwind + shadcn/ui
docs/
  plan/         Requirements, architecture, and build-phase source documents
  adr/          Architecture Decision Records
```

## Status

This project is being built phase by phase, each phase landing as its own commit. See
`docs/plan/BUILD_PHASES_JAVA.md` for the full sequence and
[`git log`](../../commits/main) for what has actually landed so far.

## Getting started

Once the Docker Compose environment lands (Sprint 1), local setup will be:

```
cp .env.example .env
docker compose up
```

This section will be kept accurate as each phase adds a working piece — it will never describe
capability that isn't in the repository yet.

## Contributing

See [`CONTRIBUTING.md`](CONTRIBUTING.md) for branch naming, commit conventions, and the review
process.

## License

See [`LICENSE`](LICENSE).
