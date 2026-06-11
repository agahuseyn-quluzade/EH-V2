# Redis — configuration, keyspaces & TTLs

Single password-protected Redis instance, used for **production** purposes by
two services: IAM (token store) and the Gateway (rate limiting). Payment uses
it for idempotency.

## Configuration

| Aspect | Value | Notes |
|--------|-------|-------|
| Image | `redis:7.4-alpine` | |
| Auth | `--requirepass $REDIS_PASSWORD` | required; `REDIS_PASSWORD` has no default |
| Persistence | `--appendonly yes` (AOF) | survives restart; volume `redis-data` |
| Exposure | dev: `127.0.0.1:6379`; **prod: not published** | internal network only |
| Healthcheck | `redis-cli -a … ping → PONG` | |
| Metrics | `redis-exporter` → Prometheus | |

Clients connect via `REDIS_HOST` / `REDIS_PORT` / `REDIS_PASSWORD`.

## Keyspaces & TTLs

| Service | Key pattern | Value | TTL | Env |
|---------|-------------|-------|-----|-----|
| IAM | `iam:refresh:{sha256}` | userId | = refresh-token expiry (**7d**) | `JWT_REFRESH_EXPIRATION_MS` |
| IAM | `iam:refresh:user:{userId}` | set of token hashes | = refresh expiry | — |
| IAM | `iam:password-reset:{sha256}` | userId | **15m** | `PASSWORD_RESET_TOKEN_TTL` |
| IAM | `iam:email-verify:{sha256}` | userId | **24h** | `EMAIL_VERIFICATION_TOKEN_TTL` |
| Gateway | `request_rate_limiter.{route}.{key}.*` | token-bucket counters | seconds (bucket refill) | `RATE_LIMIT_*` |
| Payment | idempotency key | payment result | **24h** | `PAYMENT_IDEMPOTENCY_TTL_SECONDS` (86400) |
| Payment | idempotency lock | in-flight lock | **5m** | `PAYMENT_IDEMPOTENCY_LOCK_TTL_SECONDS` (300) |

Notes:
- Tokens are stored **hashed** (SHA-256), never in plaintext. TTL equals the
  token's own lifetime, so expired entries self-evict — no cleanup job needed.
- One-time tokens (reset / verify) are consumed atomically with `GETDEL`, so a
  token cannot be replayed.
- Refresh-token rotation deletes the old hash on use; password reset calls
  `revokeAllForUser` to drop the whole `iam:refresh:user:{id}` set.

## Failure behaviour

- **Gateway rate limiting → fail-open.** If Redis is unreachable the limiter
  allows the request (rate limiting is defense-in-depth, not authorization;
  fail-closed would take the whole platform down on a Redis blip). See
  `RateLimitConfig` javadoc.
- **IAM token store → hard dependency.** Redis is in the IAM readiness group,
  so a Redis outage marks IAM not-ready (login/refresh cannot be served safely).

## Sizing & ops

- Memory is small (hashed tokens + counters). Set `maxmemory` with
  `allkeys-lru`/`volatile-ttl` if you co-locate other caches.
- AOF gives durability across restarts; for HA use Redis Sentinel or a managed
  Redis (ElastiCache / MemoryStore) and point `REDIS_HOST` at it.
- Rotate `REDIS_PASSWORD` via the secret manager; restart dependents to pick up.
