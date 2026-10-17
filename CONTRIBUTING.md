# Contributing

## Branches

Each story or phase gets its own branch off `main`: `feature/US-XX-short-description`, or `fix/` and `chore/` for work that is not a story. Phases and their stories are listed in [`docs/sprints.md`](docs/sprints.md).

## Commits

Conventional commits with the story id: `type(scope): description (US-XX)`.

Types: `feat`, `fix`, `test`, `docs`, `chore`, `refactor`.

```
feat(auth): Spring Security + JWT + Argon2id (US-01,02,03,04)
fix(applications): reject a backward status change without a note (US-14)
```

One phase is one commit. Two phases never share a commit, even small ones, and a phase starts only after the phases it depends on are in.

## Pull requests

- Every pull request is reviewed and approved by someone other than its author before it is merged.
- A change to a class that carries a design pattern (State, Strategy, Decorator, Chain of Responsibility, Template Method, Observer, Builder, Facade, Adapter, Circuit Breaker) says in one sentence which pattern it implements or extends. The patterns and where they live are in [`docs/architecture.md`](docs/architecture.md).
- UI changes include screenshots at phone and desktop width.

## Tests

- Tests ship in the same commit as the code: JUnit 5 for units, MockMvc for controllers, Testcontainers for anything that touches Postgres, React Testing Library for components, pytest for fl-service.
- Every endpoint on user-owned data has a cross-user isolation test: another user's id returns 404, never 403.
- Run the suites before every commit (`mvn test` in `api/`, `npm test` in `frontend/`, `pytest` in `fl-service/`). A failing suite is never committed, and a test is never weakened to make it pass.

How the suites are organised and what each one proves is in [`docs/testing.md`](docs/testing.md).
