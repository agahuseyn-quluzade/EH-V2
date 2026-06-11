# Observability — metrics, logs, traces & data masking

Three pillars, all wired in the compose stack:

| Pillar | Stack | Where |
|--------|-------|-------|
| Metrics | Micrometer → Prometheus → Grafana | `:9090`, `:3000` |
| Logs | structured (ECS JSON) → Promtail → Loki → Grafana | `:3100` |
| Traces | Micrometer Tracing → OTLP → Jaeger | `:16686` |

## Metrics

Every service exposes `/actuator/prometheus`. Prometheus scrapes all eight app
services (`gateway, iam, policy, claim, ai, notification, health-record,
payment`) plus `kafka-exporter` and `redis-exporter`
(`observability/prometheus.yaml`).

Grafana auto-provisions the Prometheus/Loki/Jaeger datasources and a starter
**EHI – Platform Overview** dashboard (`observability/dashboards/`): service
up/down, HTTP request rate & p95 latency, JVM heap, and infra exporters. Extend
from there per service.

## Logs & sensitive-data masking

- Console logging is **structured ECS JSON** in containers
  (`LOG_FORMAT=ecs`), including `traceId`/`spanId` for log↔trace correlation
  (Grafana derived field → Jaeger).
- **Masking**: `SensitiveDataMasker` (notification service) redacts PII/secrets
  before anything reaches a log line or a DLQ payload:
  - emails → `j***@e***.com`, phones and long numbers (PAN/IBAN) → `****`
  - any key containing `password, secret, token, otp, pin, ssn, fin,
    nationalid, passport, card, cvv, iban, account, phone, email, amount,
    reason, diagnosis, medical, health, *id, idempotency` → masked
  - free-text scrubbed via regex (email / `secret=…` / 12–19 digit numbers)
- Covered by `SensitiveDataMaskerTest` (see notification `src/test`).
- **Do not** log request bodies, tokens, or `Authorization` headers. JWTs are
  never logged; the gateway strips client identity headers and re-signs them.
- Actuator exposure is limited to `health,info,metrics,prometheus`
  (+`gateway`/`circuitbreakers` where relevant); `/actuator/env` and `/heapdump`
  are **not** exposed, so config/secrets aren't reachable over HTTP.

## Health probes

Each service publishes Spring Boot probes:
- `/actuator/health/liveness` — process is alive (liveness).
- `/actuator/health/readiness` — dependencies are ready. Readiness groups
  include `db` (and `redis` for IAM and payment), so a service reports
  not-ready while a hard dependency is down.

Compose healthchecks `curl` the **readiness** endpoint; `depends_on:
condition: service_healthy` gates startup ordering on real readiness, not just
an open TCP port.

## Recommended alerts

- Service `up == 0` for >1m (per scrape target).
- HTTP 5xx ratio > 2% or p95 latency above SLO.
- Any `*.dlq` topic `messages > 0`; consumer-group lag rising.
- Redis/Postgres exporter down; Postgres connections near `max_connections`.
- No successful DB backup in the expected window.
