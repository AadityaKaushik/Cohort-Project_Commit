# Tech Stack

## Frontend

| What | Used for |
|---|---|
| React 19 + TypeScript | The whole UI |
| Vite | Dev server and build |
| Tailwind CSS 4 + shadcn/ui | Styling and base components |
| React Router 7 | Pages and routing |
| TanStack Query | Fetching and caching API data |
| Recharts | Analytics and federated learning charts |
| date-fns, lucide-react | Dates and icons |
| Vitest + React Testing Library | Frontend tests |

## Backend

| What | Used for |
|---|---|
| Java 21 + Spring Boot 3.5 | REST API (`api/`) |
| Spring Security | JWT login, Argon2id password hashing, Google / GitHub / LinkedIn sign-in (OAuth2) |
| Spring Data JPA (Hibernate) | Database access |
| PostgreSQL 16 | Database |
| Flyway | Database migrations |
| Resilience4j | Circuit breaker around calls to the FL service |
| Spring Mail | Sign-up and password reset codes (Gmail SMTP) |
| OpenPDF | PDF report and resume export |
| Ollama (local model), Gemini optional | AI features like resume match and prep packs |
| JUnit 5, MockMvc, Testcontainers | Backend tests |

## Federated learning

| What | Used for |
|---|---|
| Python 3.12 | The FL service (`fl-service/`) |
| Flower | FedAvg strategy and round logic |
| scikit-learn | The local model (logistic regression with `SGDClassifier`) |
| NumPy | Clipping, noise and averaging |
| FastAPI + Uvicorn | Small HTTP API the Java backend calls |
| pytest | FL tests |

## Running it

Everything runs in Docker with Docker Compose: `cohort-db`, `cohort-api`, `cohort-web`, and `cohort-fl-service` when `docker-compose.fl.yml` is added. GitHub Actions runs the tests on every push.
