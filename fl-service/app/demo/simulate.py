from __future__ import annotations

import argparse
import json
import os
import sys
import urllib.request
from typing import Callable, Protocol

from app.common.schema import FEATURE_NAMES, FeatureVector
from app.demo.synthetic import generate_synthetic_clients

DEFAULT_FL_URL = "http://cohort-fl-service:8000"
DEFAULT_API_URL = "http://cohort-api:8080"
DEFAULT_EMAIL = "seed.demo.user@example.com"
PASSWORD_ENV = "COHORT_DEMO_PASSWORD"

class Http(Protocol):
    def __call__(self, method: str, url: str, body: dict | None = None, token: str | None = None) -> dict | None: ...

def urllib_http(method: str, url: str, body: dict | None = None, token: str | None = None) -> dict | None:
    data = json.dumps(body).encode("utf-8") if body is not None else None
    request = urllib.request.Request(url, data=data, method=method)
    if data is not None:
        request.add_header("Content-Type", "application/json")
    if token is not None:
        request.add_header("Authorization", f"Bearer {token}")
    with urllib.request.urlopen(request, timeout=30) as response:
        raw = response.read()
    return json.loads(raw) if raw else None

def feature_list(vector: FeatureVector) -> list[float]:
    return [float(getattr(vector, name)) for name in FEATURE_NAMES]

def run_demo(
    http: Http,
    fl_url: str,
    api_url: str,
    email: str,
    password: str,
    clients: int,
    rounds: int,
    seed: int,
    log: Callable[[str], None] = print,
) -> list[dict]:
    synthetic = generate_synthetic_clients(clients, seed)
    login = http("POST", f"{api_url}/api/v1/auth/login", {"email": email, "password": password})
    token = login["access_token"]

    results: list[dict] = []
    try:
        for client_id, vector in synthetic.items():
            http("PUT", f"{fl_url}/fl/client-features/{client_id}", {"schema_version": 1, "features": feature_list(vector)})
        log(f"added {len(synthetic)} synthetic clients to fl-service")

        for _ in range(rounds):
            result = http("POST", f"{api_url}/api/v1/dev/fl/round", token=token)
            results.append(result)
            log(_describe(result))
    finally:
        for client_id in synthetic:
            http("DELETE", f"{fl_url}/fl/client-features/{client_id}")
        log(f"removed {len(synthetic)} synthetic clients from fl-service")
    return results

def _describe(result: dict) -> str:
    if not result.get("round_ran"):
        return "no round ran: no user is opted in to federated learning"
    status = "accepted" if result["accepted"] else f"refused ({result['reason']})"
    return (
        f"round {result['round_number']}: {status}, cohort size {result['cohort_size']}, "
        f"real users exported {result['exported_users']}"
    )

def main(argv: list[str] | None = None) -> int:
    parser = argparse.ArgumentParser(
        prog="python -m app.demo.simulate",
        description="Run FL rounds with synthetic clients next to any opted-in real users.",
    )
    parser.add_argument("--clients", type=int, default=8, help="number of synthetic clients")
    parser.add_argument("--rounds", type=int, default=1)
    parser.add_argument("--seed", type=int, default=7)
    parser.add_argument("--fl-url", default=DEFAULT_FL_URL)
    parser.add_argument("--api-url", default=DEFAULT_API_URL)
    parser.add_argument("--email", default=DEFAULT_EMAIL, help="account used to call the dev round trigger")
    args = parser.parse_args(argv)

    password = os.environ.get(PASSWORD_ENV)
    if not password:
        print(f"set {PASSWORD_ENV} to the password of {args.email}", file=sys.stderr)
        return 2
    if args.clients < 0 or args.rounds < 1:
        print("--clients must be 0 or more and --rounds at least 1", file=sys.stderr)
        return 2

    run_demo(urllib_http, args.fl_url, args.api_url, args.email, password, args.clients, args.rounds, args.seed)
    return 0

if __name__ == "__main__":
    sys.exit(main())
