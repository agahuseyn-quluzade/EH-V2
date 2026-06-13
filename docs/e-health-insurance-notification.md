# e-health-insurance-notification

> Consumer-only service. Reacts to domain events from across the platform and notifies the user by
> **email (real, SMTP)** and **SMS (mocked, logged)**. Keeps a local read-model of user contacts.

| | |
|---|---|
| Port | 8086 |
| Database | `ehi_notification` (PostgreSQL) |
| Swagger | http://localhost:8086/swagger-ui.html |
| Produces | — |
| Consumes | `user.registered`, `policy.created`, `payment.completed`, `payment.failed`, `claim.submitted`, `claim.decision`, `fraud.detected` |

## Responsibilities
- Turn domain events into user-facing notifications (subject + body, per type).
- Send email for real over SMTP; send SMS via a mock provider (logged) when a phone number is known.
- Persist every notification with its delivery status, and expose history to the user/admin.

## Domain model

### `Notification` (`notifications`)
| Field | Type | Notes |
|---|---|---|
| id | UUID | PK |
| userId | UUID | recipient user |
| correlationId | UUID | the source domain id (claimId/policyId/paymentId) — used for dedup |
| type | `NotificationType` | WELCOME, POLICY_*, CLAIM_*, PAYMENT_*, FRAUD_ALERT |
| channel | `NotificationChannel` | `EMAIL` / `SMS` |
| recipient | String | email address or phone number |
| subject / body | String | rendered message |
| status | `NotificationStatus` | `PENDING` → `SENT` / `FAILED` |
| retryCount | int | |
| createdAt / updatedAt | Instant | |

### `UserContact` (`user_contacts`)
Local read-model of how to reach a user, populated from `user.registered`.
| Field | Type | Notes |
|---|---|---|
| id | UUID | PK |
| userId | UUID | unique |
| email | String | not null |
| phone | String | nullable — SMS only sent when present |

## API — `/api/v1/notifications`
| Method | Path | Auth | Description |
|---|---|---|---|
| GET | `/me` | CUSTOMER | Own notifications (EMAIL+SMS rows for the same event collapsed into one) |
| GET | `/` | ADMIN | All notifications (paginated) |

`NotificationDto`: `{ id, userId, type, channel, recipient, subject, body, status, retryCount, createdAt }`.

## Events consumed → notification produced
| Event | Notification type | Channels |
|---|---|---|
| `user.registered` | WELCOME | EMAIL (+ SMS if phone) — also upserts `UserContact` |
| `policy.created` | POLICY_PENDING | EMAIL (+ SMS if phone) |
| `payment.completed` (POLICY_PREMIUM) | POLICY_ACTIVATED | EMAIL |
| `payment.completed` (CLAIM_PAYOUT) | PAYMENT_SUCCESS | EMAIL |
| `payment.failed` | PAYMENT_FAILED | EMAIL |
| `claim.submitted` | CLAIM_SUBMITTED | EMAIL |
| `claim.decision` (APPROVED/REJECTED) | CLAIM_APPROVED / CLAIM_REJECTED | EMAIL |
| `fraud.detected` (risky) | FRAUD_ALERT | EMAIL |

All consumers look up the `UserContact`; if none exists they skip (and log). `user.registered`
populates the contact, so it must arrive before downstream events for that user.

## Architecture: dispatcher + providers
- `NotificationSender` (interface) → `ChannelNotificationSender` dispatches by `channel`:
  - `EMAIL` → `EmailProvider`: `SmtpEmailProvider` (real SMTP, profile `!it`) or `MockEmailProvider`
    (logs, profile `it`/tests).
  - `SMS` → `SmsProvider`: `MockSmsProvider` (logs `SMS TO {phone} : {message}`).
- A send failure marks the notification `FAILED` (e.g. bad SMTP credentials) without throwing.

## Idempotency (dedup)
- Each notification carries a `correlationId` = the source event's domain id.
- Before sending, the service checks `existsByCorrelationIdAndTypeAndChannel` — a redelivered event
  doesn't create a duplicate notification, while still allowing parallel EMAIL + SMS rows.
- `GET /me` additionally **collapses** the EMAIL and SMS rows of the same event into a single entry
  (preferring EMAIL), so the UI shows one notification per event.

## Configuration
| Env var | Default | Purpose |
|---|---|---|
| `MAIL_HOST` | `smtp.gmail.com` | SMTP server |
| `MAIL_PORT` | `587` | SMTP port |
| `MAIL_USERNAME` / `MAIL_PASSWORD` | — | SMTP auth (Gmail app password) |
| `MAIL_FROM` | `no-reply@ehi.example.com` | From address |
| `JWT_SECRET` | — | Auth |

Without real SMTP credentials, email notifications are recorded as `FAILED` (expected); SMS still
logs. The `notification.email.from` is bound via `NotificationProperties`.

## Testing
Unit tests for the dispatcher (channel routing, provider-throws → false), the SMTP provider (message
building), and each consumer (contact-found → EMAIL, no-contact → skip, phone present → EMAIL+SMS);
integration tests publish each event and assert the persisted notification.

## Notes & limitations
- A `RetryScheduler` re-attempts FAILED notifications; in a multi-instance deployment it would need
  locking to avoid duplicate sends.
- Users who registered before this service existed won't have a `UserContact` until they re-register
  or it's backfilled.
