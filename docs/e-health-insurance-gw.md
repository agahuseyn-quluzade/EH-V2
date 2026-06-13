# e-health-insurance-gw (API Gateway)

> The single entry point for all clients. Validates the JWT, optionally injects user headers, and
> routes `/api/v1/**` requests to the right backend service.

| | |
|---|---|
| Port | 8080 |
| Database | none |
| Kafka | none (no domain events — pure routing) |
| Built on | Spring Cloud Gateway (WebFlux, reactive) |

## Responsibilities
- Terminate all external traffic — the browser/clients talk only to the gateway.
- Validate the `Authorization: Bearer <JWT>` on protected routes; reject unauthenticated requests
  to non-public paths.
- Forward each request to the owning service based on the path prefix.

## Routes
Path prefix → target service. In Docker the targets are service names; in local dev they are
`localhost:<port>` (see `application.yml` vs `application-docker.yml`).

| Path | Service | Port |
|---|---|---|
| `/api/v1/auth/**` | iam | 8081 |
| `/api/v1/users/**` | iam | 8081 |
| `/api/v1/plans/**` | policy | 8082 |
| `/api/v1/policies/**` | policy | 8082 |
| `/api/v1/claims/**` | claim | 8083 |
| `/api/v1/payments/**` | payment | 8084 |
| `/api/v1/ai/**` | ai | 8085 |
| `/api/v1/notifications/**` | notification | 8086 |

## Security
- **JWT filter** validates the token signature/expiry on every protected route.
- **Public paths** (no token required):
  - `POST /api/v1/auth/**` (register, login, refresh)
  - `GET /api/v1/plans/**` (browse plans)
- Configured via `gateway.security.public-paths` / `public-get-paths` and the shared `JWT_SECRET`.

## Configuration
| Env var | Purpose |
|---|---|
| `JWT_SECRET` | Shared signing secret (must match the IAM service) |
| `SPRING_PROFILES_ACTIVE=docker` | Switches route targets to service names |

## Notes & limitations
- **Swagger is not exposed through the gateway** — Swagger paths (`/swagger-ui/**`, `/v3/api-docs/**`)
  are not under `/api/v1/`, so the gateway does not route them. Reach each service's Swagger UI on its
  own port. (Aggregating all docs at the gateway is possible but not implemented.)
- The gateway can inject `X-User-Id` / `X-User-Role` headers, but downstream services currently
  re-validate the JWT themselves rather than trusting those headers (known cross-cutting item).
