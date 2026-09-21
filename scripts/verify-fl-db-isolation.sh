#!/usr/bin/env bash
# P50 (US-61: "the server holds no user-schema database credentials,
# verified in CI"). Run against a live docker-compose stack, after
#   docker compose -f docker-compose.yml -f docker-compose.fl.yml up -d --build db api fl-service
# (the `api` container's own Flyway run applies V19, which creates the `fl`
# schema, its two bookkeeping tables, and the `cohort_fl` role). Exits
# non-zero on the first violation found; wired into .github/workflows/ci.yml
# as the `fl-db-isolation` job.
#
# See also: api/src/test/java/com/cohort/api/FlSchemaIsolationIntegrationTest.java,
# which proves the same grants against a Testcontainers Postgres and can run
# in this sandbox even without a live fl-service container.
set -euo pipefail

COMPOSE=(docker compose -f docker-compose.yml -f docker-compose.fl.yml)
DB_USER="${POSTGRES_USER:-cohort_app}"
DB_NAME="${POSTGRES_DB:-cohort}"

echo "== Check 1: fl-service container has no main-app DB credentials =="
if "${COMPOSE[@]}" exec -T fl-service env | grep -E '^(POSTGRES_USER|POSTGRES_PASSWORD|SPRING_DATASOURCE_URL)='; then
  echo "FAIL: fl-service container has main application DB credentials set." >&2
  exit 1
fi
echo "OK: no main-app DB credential env vars found in fl-service."

echo "== Check 2: cohort_fl role has zero privileges on any public-schema (user-content) table =="
leaked=$("${COMPOSE[@]}" exec -T db psql -U "$DB_USER" -d "$DB_NAME" -t -A -c "
  WITH public_tables AS MATERIALIZED (
      SELECT table_name FROM information_schema.tables
      WHERE table_schema = 'public' AND table_type = 'BASE TABLE'
  )
  SELECT table_name FROM public_tables
  WHERE has_table_privilege('cohort_fl', 'public.' || quote_ident(table_name), 'SELECT')
     OR has_table_privilege('cohort_fl', 'public.' || quote_ident(table_name), 'INSERT')
     OR has_table_privilege('cohort_fl', 'public.' || quote_ident(table_name), 'UPDATE')
     OR has_table_privilege('cohort_fl', 'public.' || quote_ident(table_name), 'DELETE');
")
if [ -n "$leaked" ]; then
  echo "FAIL: cohort_fl has privileges on user-content table(s): $leaked" >&2
  exit 1
fi
echo "OK: cohort_fl has no privileges on any public-schema table."

echo "== Check 3: cohort_fl is granted exactly the fl schema's two bookkeeping tables =="
granted=$("${COMPOSE[@]}" exec -T db psql -U "$DB_USER" -d "$DB_NAME" -t -A -c "
  SELECT DISTINCT table_schema || '.' || table_name FROM information_schema.role_table_grants
  WHERE grantee = 'cohort_fl' ORDER BY 1;
" | tr -d '\r')
expected=$'fl.fl_rounds\nfl.privacy_ledger_entries'
if [ "$(echo "$granted" | sort -u)" != "$(echo "$expected" | sort -u)" ]; then
  echo "FAIL: cohort_fl's granted tables are not exactly fl.fl_rounds + fl.privacy_ledger_entries. Got: [$granted]" >&2
  exit 1
fi
echo "OK: cohort_fl is granted exactly fl.fl_rounds and fl.privacy_ledger_entries."

echo "All fl-service DB isolation checks passed."
