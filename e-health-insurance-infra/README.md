# e-health-insurance-infra

Shared infrastructure (PostgreSQL, MailHog) and the docker-compose stack that
wires together every microservice in the e-health-insurance platform.

## Services

| Service       | Port  | Notes                                  |
|---------------|-------|----------------------------------------|
| gateway       | 8080  | Spring Cloud Gateway (entry point)     |
| iam           | 8081  | Auth, users, JWT issuance              |
| policy        | 8082  | Plans and policies                     |
| claim         | 8083  | Claims, evidence                       |
| ai            | 8084  | AI: scoring, recommendations, chat     |
| notification  | 8085  | Email/SMS notifications                |
| health-record | 8086  | Member medical records and audit events |
| payment       | 8087  | Payment initiation, mock success/fail  |
| postgres      | 5432  | Shared (one DB per service)            |
| kafka          | 29092 | Local external listener, internal 9092 |
| kafka UI       | 8089  | Local UI, profile: dev-tools           |
| redis          | 6379  | Password-protected local Redis         |
| mailhog SMTP  | 1025  | Local SMTP                             |
| mailhog UI    | 8025  | Web UI for captured mail               |
| frontend      | 5173  | Vite dev server (profile: frontend)    |

## Usage

Create a `.env` file in this directory (start from `.env.example`):

```
JWT_SECRET=<base64-encoded HMAC key, 32+ bytes>
GATEWAY_HEADER_SECRET=<separate random signing secret>
OPENROUTER_API_KEY=<your OpenRouter API key>
LLM_MODEL=google/gemini-2.5-flash   # optional
REDIS_PASSWORD=<random Redis password>
```

Then:

```
docker compose up -d                         # backend + infra
docker compose --profile frontend up -d      # backend + frontend
docker compose --profile dev-tools up -d     # backend + Kafka UI
```

The `initdb/` scripts create one database per service on first boot of the
`postgres` volume.

## Kafka topics

The `kafka-init` one-shot service creates the required topics after the broker
is healthy. Topic names are configured in `.env` and default to:

```
payment.initiated
payment.succeeded
payment.failed
payment.refunded
policy.purchase.requested
policy.activated
policy.cancelled
claim.submitted
claim.review.required
claim.approved
claim.rejected
notification.email.requested
health-record.created
health-record.updated
audit.event.created
```

DLQ topics are created by the same `kafka-init` service using the default
`<topic>.dlq` naming convention, for example `payment.succeeded.dlq`.

For local clients outside Docker use `localhost:29092`. Services inside Docker
should use `kafka:9092`.

## Redis

Redis requires `REDIS_PASSWORD`. The port is bound to `127.0.0.1:6379` for
local development. Do not store medical, payment-card, or other long-lived
sensitive records in Redis.

## Notification email

The notification service runs with `NOTIFICATION_SPRING_PROFILE=local` in this
compose stack and sends mail through Mailhog (`MAIL_HOST=mailhog`,
`MAIL_PORT=1025`). Production deployments should use
`NOTIFICATION_SPRING_PROFILE=prod` and provide `SMTP_HOST`, `SMTP_PORT`,
`SMTP_USERNAME`, `SMTP_PASSWORD`, `SMTP_AUTH`, and `SMTP_STARTTLS`.
