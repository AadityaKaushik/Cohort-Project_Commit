# Cohort

Cohort is a job-search campaign manager: applications, resumes, interview debriefs, and
preparation tracking in one place, with a **Federated Learning** layer that lets the app
learn from users' preparation patterns without centralising anyone's data.

Plain-language overview: [`docs/plan/PROJECT_OVERVIEW.md`](docs/plan/PROJECT_OVERVIEW.md).
Requirements, backlog and design: [`docs/plan/PROJECT_PLAN.md`](docs/plan/PROJECT_PLAN.md)
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
  plan/         Requirements, architecture and build phases
  adr/          Architecture decision records
  company-directory.md   Company directory: reference data and sourcing rules
  data-quality-rules.md  Input checks for user-entered text
scripts/        CI helper scripts
```

## Status

The plan has nine sprints and 110 phases (`docs/plan/BUILD_PHASES_JAVA.md`), one commit per phase.
`docs/implementation-overview.md` summarises the build and `docs/poster.html` is the project poster.

| Sprint | Goal | Phases | Built | Still to do |
|---|---|---|---|---|
| 1 | Foundation | P01-P15 | All | None |
| 2 | Resumes and interview capture | P16-P26 | All | Time the debrief (must be under 3 minutes) |
| 3 | Preparation and analytics | P27-P38 | All | None |
| 4 | Recommendations and federated learning | P39-P58 | All | Check a Privacy Ledger entry end to end with the FL profile running |
| 5 | Notifications, reports and account | P59-P68 | All | None |
| 6 | Experience and core gaps | P69-P81 | All | Review screenshots at 390 px and 1440 px in both themes |
| 7 | Job-search workflow | P82-P88 | All | None |
| 8 | AI and intelligence | P89-P104 | P89, P94, P95, P98, P99, P103 | Nothing; P90-P93, P96, P97, P100-P102 and P104 are deferred by the plan |
| 9 | Hardening and delivery | P105-P110 | All | Two full demo rehearsals, the 375 px check, axe-core with zero critical issues in both themes |

## Running locally

With Docker or Podman Compose:

```
cp .env.example .env
docker compose up --build
```

The API is on `localhost:8080`, the frontend on `localhost:5173` and Postgres on host port 5433
(`POSTGRES_HOST_PORT`). The default `dev` profile loads a seed dataset; sign in with
`seed.demo.user@example.com` / `SeedDemo123!`. To add the FL service:

```
docker compose -f docker-compose.yml -f docker-compose.fl.yml up --build
```

Without containers for the app itself, start Postgres, then:

```
cd api && mvn spring-boot:run -Dspring-boot.run.profiles=dev
cd frontend && npm install && npm run dev
```

Tests: `cd api && mvn test` (needs a container runtime for Testcontainers),
`cd frontend && npm test`, `cd fl-service && pytest`.

## Contributing

See [`CONTRIBUTING.md`](CONTRIBUTING.md) for branch naming, commit conventions, and the review
process.

## License

See [`LICENSE`](LICENSE).
