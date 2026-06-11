# PostgreSQL — backup & restore strategy

One Postgres instance hosts **one database per service** (created on first boot
by `initdb/01-create-databases.sql`):

`ehi_iam`, `ehi_policy`, `ehi_claim`, `ehi_ai`, `ehi_notification`,
`ehi_payment`, `ehi_health_record`.

Schema is owned by each service via Liquibase migrations — restores must be
followed by the app starting (it validates/applies the changelog).

## Backup policy

| Tier | Method | Frequency | Retention |
|------|--------|-----------|-----------|
| Logical (per-DB) | `pg_dump -Fc` each database | every 6h | 7 days |
| Logical (cluster) | `pg_dumpall --globals-only` (roles) | daily | 30 days |
| Physical / PITR | base backup + WAL archiving | continuous | 14 days |

For managed Postgres (RDS / Cloud SQL) use the provider's automated snapshots +
PITR and treat the scripts below as the portable/self-hosted fallback.

### Logical backup (self-hosted)

```bash
# Per-database custom-format dumps (parallel-restorable, compressed)
for db in ehi_iam ehi_policy ehi_claim ehi_ai ehi_notification ehi_payment ehi_health_record; do
  docker compose exec -T postgres \
    pg_dump -U "$POSTGRES_USER" -Fc "$db" \
    > "backups/$(date +%F_%H%M)_${db}.dump"
done

# Cluster roles (run once per cycle)
docker compose exec -T postgres \
  pg_dumpall -U "$POSTGRES_USER" --globals-only \
  > "backups/$(date +%F)_globals.sql"
```

Encrypt at rest and ship off-host (e.g. `… | age -r <key>` → object storage
with versioning + lifecycle rules). Never store backups on the same volume as
`postgres-data`.

### Continuous archiving (PITR)

Enable WAL archiving on the server (`postgresql.conf`):

```
wal_level = replica
archive_mode = on
archive_command = 'age -r <key> < %p | aws s3 cp - s3://ehi-pg-wal/%f'
```

Take a weekly base backup with `pg_basebackup -Ft -z` and keep the WAL stream;
this allows restore to any point within the retention window.

## Restore

```bash
# 1) Recreate the database (or restore into a fresh instance)
docker compose exec -T postgres createdb -U "$POSTGRES_USER" ehi_claim

# 2) Restore the custom-format dump (parallel)
docker compose exec -T postgres \
  pg_restore -U "$POSTGRES_USER" -d ehi_claim --clean --if-exists -j4 \
  < backups/2026-06-11_0000_ehi_claim.dump

# 3) Start the owning service — Liquibase validates the schema on boot
docker compose up -d claim
```

PITR restore: restore the latest base backup, then replay WAL with a
`recovery_target_time` in `recovery.signal` / `postgresql.auto.conf`.

## Verification & DR

- **Test restores monthly** into a throwaway instance — an untested backup is
  not a backup. Automate a smoke restore of one DB in CI.
- Track RPO/RTO targets: with 6h logical + continuous WAL, **RPO ≈ minutes**
  (PITR) / 6h (logical-only), **RTO** = time to provision + restore + app boot.
- Alert if no successful backup completed within the expected window.
