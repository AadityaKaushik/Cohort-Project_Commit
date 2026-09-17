# Rollback scripts

Flyway's automatic `undo` migrations are a paid (Teams/Enterprise) feature; this project uses
open-source Flyway. To keep every migration genuinely reversible (NFR-27) without that
dependency, each versioned migration in `../migration/` has a corresponding hand-written
rollback script here, applied manually and in **reverse** version order:

```
psql "$DATABASE_URL" -f U2__drop_refresh_tokens_table.sql
psql "$DATABASE_URL" -f U1__drop_users_table.sql
```

Then delete the corresponding rows from `flyway_schema_history` (or drop and let Flyway
re-baseline) before re-running `V1`/`V2` forward again in a fresh environment.

These scripts are never executed automatically — Flyway does not scan this directory.
