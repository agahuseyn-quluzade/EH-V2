# e-health-insurance-notification

## Status: DONE
## Port: 8086
## Database: ehi_notification

## What's Done
- [x] build.gradle
- [x] Entity: Notification
- [x] Repository: NotificationRepository
- [x] Service: NotificationService, RetryScheduler
- [x] Controller: NotificationController
- [x] Kafka consumer: all topics (user.registered, policy.created, payment.completed, payment.failed, claim.submitted, claim.decision, fraud.detected)
- [x] GlobalExceptionHandler
- [x] application.yml
- [x] Dockerfile

## Entities

### Notification
| Field | Type | Notes |
|---|---|---|
| id | UUID | PK, generated |
| userId | UUID | recipient user |
| type | NotificationType (infra enum) | WELCOME, POLICY_ACTIVATED, POLICY_PENDING, CLAIM_SUBMITTED, CLAIM_APPROVED, CLAIM_REJECTED, PAYMENT_SUCCESS, PAYMENT_FAILED, FRAUD_ALERT |
| channel | NotificationChannel (infra enum) | EMAIL, SMS |
| recipient | String | email (from UserRegisteredEvent) or userId.toString() placeholder for other events |
| subject | String | |
| body | String (TEXT) | |
| status | NotificationStatus (local enum) | PENDING, SENT, FAILED |
| retryCount | int | incremented by RetryScheduler |
| createdAt | Instant | |
| updatedAt | Instant | |

## Endpoints
| Method | Path | Description | Auth |
|---|---|---|---|
| GET | /api/v1/notifications/me | My notifications | CUSTOMER |
| GET | /api/v1/notifications | All notifications (paginated) | ADMIN |

## Kafka
- Produces: (none)
- Consumes: user.registered, policy.created, payment.completed, payment.failed, claim.submitted, claim.decision, fraud.detected
  - `UserRegisteredEventConsumer` → WELCOME (recipient = event email)
  - `PolicyCreatedEventConsumer` → POLICY_PENDING
  - `PaymentCompletedEventConsumer` → POLICY_ACTIVATED (referenceType=POLICY_PREMIUM) or PAYMENT_SUCCESS (referenceType=CLAIM_PAYOUT)
  - `PaymentFailedEventConsumer` → PAYMENT_FAILED
  - `ClaimSubmittedEventConsumer` → CLAIM_SUBMITTED
  - `ClaimDecisionEventConsumer` → CLAIM_APPROVED (decision=APPROVED) or CLAIM_REJECTED (decision=REJECTED); ignored for other decisions
  - `FraudDetectedEventConsumer` → FRAUD_ALERT, only if riskScore >= 70

## Decisions & Notes
- MVP: no real email/SMS — `NotificationSender` (service/impl) always "sends" successfully via `log.info`, then saves the notification with status SENT.
- Retry: `RetryScheduler` (scheduler package) runs every 5 minutes (`@Scheduled(fixedRate = 300000)`), retries notifications with status FAILED and retryCount < 3 (MAX_RETRIES), incrementing retryCount on each attempt.
- `NotificationStatus` (PENDING/SENT/FAILED) kept local to this service (not infra) — no other service consumes it.
- Endpoint paths corrected from doc's `/api/notifications/...` to actual `/api/v1/notifications/...` (consistent with all other services' `/api/v1/` prefix).
- `recipient` field: for `user.registered` we use the real email from `UserRegisteredEvent`; for all other events (policy/payment/claim/fraud) we have no email/phone available in this service, so `recipient` is set to `userId.toString()` as an MVP placeholder.
- Infra's `NotificationEvent` record and `KafkaTopics.NOTIFICATION_SEND` topic exist but are not used — this service consumes the 7 domain events directly per the original design, not a generic notification-send topic.
- JWT/security setup identical to claim/payment/ai services (JwtProperties, JwtProvider, JwtAuthenticationFilter, SecurityConfig).
- `./gradlew build` passes (BUILD SUCCESSFUL).
- Dockerfile (multi-stage, same pattern as claim/payment/ai): `infra-build` stage publishes `e-health-insurance-infra` (via Compose's `additional_contexts: infra`) to `/root/.m2`, `build` stage compiles `bootJar`, runtime stage `eclipse-temurin:17-jre-jammy`. `gradle.properties` removed before building. `.dockerignore` excludes `.gradle/`, `build/`, `out/`.
- `application-docker.yml` overrides `spring.datasource.url` → `postgres:5432/ehi_notification` and `spring.kafka.bootstrap-servers` → `kafka:29092`, activated via `SPRING_PROFILES_ACTIVE=docker`.
