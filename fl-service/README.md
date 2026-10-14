# fl-service

Python side of the federated-learning slice (Flower + scikit-learn). Spring Boot owns the rest of the
application (see `docs/adr/ADR-010-java-spring-boot-main-api.md`); this directory holds only the
client/server training code and a small FastAPI wrapper.

## Layout

- `app/common/` feature schema v1 (24 features) and the shared model parameter shape (24 coefficients + intercept).
- `app/client/` feature extraction, local `SGDClassifier` training, L2 clipping and Gaussian noise.
- `app/server/` FedAvg strategy over deltas, minimum-cohort refusal, poisoning filter, round orchestration, append-only model registry.
- `app/control/` FastAPI endpoints the Java API calls (`app.main:app`).
- `spike/` the Sprint 3 FedAvg spike; findings are in `docs/adr/ADR-011-fl-spike-findings.md`. Not on the production path.

The service holds no database credentials. Rounds and the model ledger are persisted by the Java API.

## Setup and tests

```bash
cd fl-service
python3 -m venv .venv
source .venv/bin/activate
pip install -e ".[dev]"
pytest
```

## Run

```bash
uvicorn app.main:app --port 8000
python -m spike.run
```

Or through Docker: `docker compose -f docker-compose.yml -f docker-compose.fl.yml up -d --build`.
