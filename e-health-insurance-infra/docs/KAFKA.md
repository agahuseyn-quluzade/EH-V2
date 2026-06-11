# Kafka — topics, retry, DLQ & consumer groups

Event backbone for the platform. One single-broker KRaft cluster locally; a
managed multi-broker cluster in production (`KAFKA_TOPIC_REPLICATION_FACTOR=3`,
`KAFKA_TOPIC_PARTITIONS>=6`). Topics are **not** auto-created
(`KAFKA_AUTO_CREATE_TOPICS_ENABLE=false`); the `kafka-init` one-shot job
provisions them after the broker is healthy.

## Topics

All topic names are env-driven (see `.env.example`). Defaults:

| Domain | Topic | Produced by | Consumed by |
|--------|-------|-------------|-------------|
| Payment | `payment.initiated` | payment | policy |
| Payment | `payment.succeeded` | payment | policy |
| Payment | `payment.failed` | payment | policy |
| Payment | `payment.refunded` | payment | — |
| Policy | `policy.purchase.requested` | policy | — |
| Policy | `policy.activated` | policy | — |
| Policy | `policy.cancelled` | policy | — |
| Claim | `claim.submitted` | claim | ai |
| Claim | `claim.ai.scored` | ai | claim |
| Claim | `claim.review.required` | claim | — |
| Claim | `claim.approved` | claim | — |
| Claim | `claim.rejected` | claim | — |
| Notification | `notification.email.requested` | claim, iam | notification |
| Health record | `health-record.created` | health-record | — |
| Health record | `health-record.updated` | health-record | — |
| Audit | `audit.event.created` | health-record | — |

Every topic has a matching **`.dlq`** topic (e.g. `claim.submitted.dlq`).

## Consumer groups

| Group | Service | Subscribes to |
|-------|---------|---------------|
| `notification-service-email` | notification | `notification.email.requested` |
| `policy-service-payment-events` | policy | `payment.{initiated,succeeded,failed}` |
| `claim-service-ai-score` | claim | `claim.ai.scored` |
| `ai-service-claim-submitted` | ai | `claim.submitted` |

Group ids are overridable via `*_CONSUMER_GROUP` env vars. Keep them **stable**
across deploys — changing a group id re-reads the topic from its configured
offset reset and can reprocess history.

## Producing — transactional outbox

Producers (payment, policy, claim, ai, health-record) never write to Kafka
inline. They persist the event to an **outbox** table in the same DB
transaction as the business change, and a publisher polls and ships it. This
gives exactly-once-ish semantics (at-least-once delivery + idempotent
consumers).

Publisher config (per service, env-driven):

| Setting | Env | Default |
|---------|-----|---------|
| Enabled | `OUTBOX_PUBLISHER_ENABLED` | `true` |
| Batch size | `OUTBOX_BATCH_SIZE` | `50` |
| Poll delay | `OUTBOX_PUBLISHER_FIXED_DELAY_MS` | `5000` |
| Max attempts | `OUTBOX_MAX_ATTEMPTS` | `10` |
| Retry delay | `OUTBOX_RETRY_DELAY_MS` | `30000` |
| DLQ suffix | `OUTBOX_DLQ_SUFFIX` | `.dlq` |

Producer reliability is enforced at the client: `acks=all`,
`enable.idempotence=true`, `max.in.flight.requests.per.connection=5`,
`retries=10`, `delivery.timeout.ms=120000`.

After `OUTBOX_MAX_ATTEMPTS` failed publish attempts the record is routed to
`<topic><OUTBOX_DLQ_SUFFIX>` and marked failed.

## Consuming — retry & DLQ

Consumers are idempotent (dedupe on `eventId` via a processed-events table).
On a processing failure the message is retried; once the per-service retry
budget is exhausted it is published to the topic's DLQ instead of blocking the
partition.

Notification (the busiest consumer) is the reference implementation:

| Setting | Env | Default |
|---------|-----|---------|
| Event retry interval | `NOTIFICATION_EVENT_RETRY_INTERVAL_MS` | `1000` |
| Event max retries | `NOTIFICATION_EVENT_MAX_RETRY_ATTEMPTS` | `3` |
| Delivery max attempts | `NOTIFICATION_DELIVERY_MAX_ATTEMPTS` | `3` |
| Delivery retry delay | `NOTIFICATION_DELIVERY_RETRY_DELAY_MS` | `120000` |
| DLQ enabled | `NOTIFICATION_DELIVERY_DLQ_ENABLED` | `true` |
| DLQ topic | `NOTIFICATION_EMAIL_REQUESTED_DLQ_TOPIC` | `notification.email.requested.dlq` |

## Operating the DLQs

- **Monitor**: `kafka-exporter` exposes per-topic lag/size to Prometheus. Alert
  on any `*.dlq` topic with `messages > 0` and on consumer-group lag growth.
- **Inspect** (dev): Kafka UI at `:8089` (`--profile dev-tools`).
- **Replay**: after fixing the root cause, re-publish DLQ records back onto the
  source topic (console producer or a small admin job). Because consumers
  dedupe on `eventId`, replaying an already-processed event is a no-op.
