# Deployment runbook — e-health-insurance

Polyrepo platform: 8 Spring Boot services + 2 shared Gradle libraries
(`ehi-common-security`, `ehi-common-events`) + a Vite frontend, wired together
by this infra repo's compose stack. Each service keeps its own repo/folder; the
only integration points are Docker Compose, the gateway routes, env config,
Gradle `includeBuild`, and the Kafka/HTTP contracts.

## Topology

```
            ┌────────── gateway :8080 (JWT, rate-limit, identity headers)
 client ───▶│
            └─▶ iam :8081  policy :8082  claim :8083  ai :8084
               notification :8085  health-record :8086  payment :8087
 infra: postgres  redis  kafka(+init)  mailhog/SMTP  jaeger  loki  prometheus  grafana
```

## Prerequisites

- Docker Engine + Compose v2, JDK 25 (for local Gradle builds).
- Secrets ready (secret manager or `.env.prod`): `JWT_SECRET`,
  `GATEWAY_HEADER_SECRET`, `POSTGRES_PASSWORD`, `REDIS_PASSWORD`,
  `GRAFANA_ADMIN_PASSWORD`, `OPENROUTER_API_KEY`, `SMTP_*`.
- DNS/TLS terminator in front of the gateway (the gateway itself is plain HTTP
  inside the network).

## Environments

| | Dev | Prod |
|---|---|---|
| Files | `docker-compose.yaml` + `.env` (from `.env.example`) | base + `docker-compose.prod.yaml` + `.env.prod` (from `.env.prod.example`) |
| DB creds | `ehi/ehi` | strong `POSTGRES_PASSWORD` |
| Email | MailHog `:8025` | real SMTP relay (`prod` profile) |
| Infra ports | published to host | internal only (gateway + dashboards excepted) |
| Tracing | 100% sample | `TRACING_SAMPLE_RATE=0.1` |
| Restarts/limits | none | `restart: unless-stopped` + CPU/mem limits |

## First-time / standard deploy (prod)

```bash
cd e-health-insurance-infra
cp .env.prod.example .env.prod          # fill REAL secrets (or inject from vault)

# 1) Bring up infra first so migrations/topics have something to talk to
docker compose -f docker-compose.yaml -f docker-compose.prod.yaml --env-file .env.prod \
  up -d postgres redis kafka kafka-init

# 2) Bring up the app services + observability
docker compose -f docker-compose.yaml -f docker-compose.prod.yaml --env-file .env.prod \
  up -d

# 3) Watch readiness
docker compose -f docker-compose.yaml -f docker-compose.prod.yaml ps
./scripts/smoke-test.sh                  # GATEWAY_URL defaults to http://localhost:8080
```

Startup ordering is automatic: `depends_on: condition: service_healthy` gates
each service on its dependencies' **readiness** probes; `kafka-init` must reach
`service_completed_successfully` before producers/consumers start.

## Database migrations

Liquibase runs **in-process on each service boot** (`spring.liquibase.enabled`,
`ddl-auto: validate`). No manual migration step. A failed changelog fails the
service start — check logs, fix the changeset, redeploy. Roll back with the
changeset's documented `rollback` block if needed.

## Rolling update of one service

```bash
# rebuild + restart just that service (others keep running)
docker compose -f docker-compose.yaml -f docker-compose.prod.yaml --env-file .env.prod \
  up -d --build --no-deps iam
docker compose ... ps iam        # wait until healthy
```

Because images are independent per repo, services deploy independently. API and
event contracts are backward-compatible (consumers ignore unknown fields,
events are additive) — deploy producers and consumers in any order.

## Health & verification

- Readiness: `curl -f http://<host>:<port>/actuator/health/readiness`
- Liveness: `…/actuator/health/liveness`
- End-to-end: `./scripts/smoke-test.sh` (health of all 8 + register→login→/me).
- Dashboards: Grafana `:3000`, Jaeger `:16686`, Prometheus `:9090`.

## Rollback

```bash
# pin to the previous image tag and redeploy the affected service
docker compose ... up -d --no-deps <service>
```

DB: if a release shipped a bad migration, restore per `POSTGRES_BACKUP.md`
(PITR to just before the deploy) and redeploy the prior image. Kafka events are
idempotent, so replays after rollback are safe.

## Common incidents

| Symptom | Check | Action |
|---------|-------|--------|
| Service stuck "unhealthy" | `docker logs`, `/actuator/health/readiness` | usually DB/Redis/Kafka dependency down — fix dep |
| Login/refresh failing | IAM readiness (Redis in group) | restore Redis; tokens are in Redis |
| Rate-limited unexpectedly | gateway logs, `RATE_LIMIT_*` | tune env limits; Redis down ⇒ fail-open (allows) |
| Events not flowing | `*.dlq` size, consumer lag (Grafana) | replay DLQ after root-cause fix (see KAFKA.md) |
| Emails not sent | notification logs, SMTP creds | verify `SMTP_*`; dev uses MailHog `:8025` |
| Image build fails | missing `ehi-common-*` COPY | Dockerfile must COPY every `includeBuild` module |

## Reference docs

- `KAFKA.md` — topics, retry, DLQ, consumer groups
- `REDIS.md` — keyspaces, TTLs, fail-open/closed
- `POSTGRES_BACKUP.md` — backup/restore/PITR
- `OBSERVABILITY.md` — metrics, logs, traces, masking
