# fl-service

Python side of the federated-learning slice (Flower + scikit-learn). Spring Boot owns the rest of the
application (see `docs/architecture.md`); this directory holds only the
client/server training code and a small FastAPI wrapper.

## Layout

- `app/common/` feature schema v1 (24 features) and the shared model parameter shape (24 coefficients + intercept).
- `app/client/` feature extraction, local `SGDClassifier` training, L2 clipping and Gaussian noise.
- `app/server/` FedAvg strategy over deltas, minimum-cohort refusal, poisoning filter, round orchestration, append-only model registry.
- `app/control/` FastAPI endpoints the Java API calls (`app.main:app`).
- `spike/` a standalone two-client FedAvg experiment that checks Flower's aggregation against scikit-learn models (see `docs/recommendations-and-fl.md`). Not on the production path.

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

## Demo round with synthetic clients

A round needs at least 3 participants, so a demo with one or two real users never runs one. `app.demo.simulate`
fills the cohort with synthetic clients and runs real rounds through the normal path. It only runs when started by hand.

- Synthetic clients are named `synthetic-client-01`, `synthetic-client-02`, and so on. Their 24 features come from a
  seeded generator (`app/demo/synthetic.py`) that stays inside the schema bounds. They exist only in fl-service's
  in-memory feature store, are never written to any user table, and are removed when the command ends.
- Clipping norm, noise, the minimum cohort and the poisoning filter are unchanged.
- Each round calls `POST /api/v1/dev/fl/round` on the API. That endpoint exists only under the `dev` profile. It runs
  the normal feature export for opted-in users and then the normal round job, so the API saves the round and one
  Privacy Ledger entry per real opted-in user. Entries for synthetic clients are dropped by the API.

Before running it, log in as `seed.demo.user@example.com` and opt in on the Privacy page. Then, with the stack up:

```bash
docker compose -f docker-compose.yml -f docker-compose.fl.yml run --rm \
  -e COHORT_DEMO_PASSWORD='SeedDemo123!' fl-service \
  python -m app.demo.simulate --clients 8 --rounds 2 --seed 7
```

Without Docker, add `--api-url http://localhost:8080 --fl-url http://localhost:8000`.

Afterwards, open the Privacy page as the demo user. The ledger shows one entry per round with the clip norms, the
cohort size (real users plus synthetic clients) and the vector that was sent. If nobody is opted in, no round runs.
With `--clients 1` the round is refused for being below the minimum cohort, which shows the refusal path.
