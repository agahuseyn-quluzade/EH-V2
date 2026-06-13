# e-health-insurance-notification

## Status: DONE
## Port: 8086
## Database: ehi_notification

## What's Done
- [x] build.gradle
- [x] Entity: Notification, UserContact (local read-model)
- [x] Repository: NotificationRepository, UserContactRepository
- [x] Service: NotificationService, RetryScheduler
- [x] Controller: NotificationController
- [x] Kafka consumer: all topics (user.registered, policy.created, payment.completed, payment.failed, claim.submitted, claim.decision, fraud.detected)
- [x] GlobalExceptionHandler
- [x] application.yml (ddl-auto: none, AWS config)
- [x] Liquibase changelogs (001-notifications, 002-user_contacts, 003-index)
- [x] Dockerfile

## Entities

### Notification
| Field | Type | Notes |
|---|---|---|
| id | UUID | PK, generated |
| userId | UUID | recipient user |
| type | NotificationType (infra enum) | WELCOME, POLICY_ACTIVATED, POLICY_PENDING, CLAIM_SUBMITTED, CLAIM_APPROVED, CLAIM_REJECTED, PAYMENT_SUCCESS, PAYMENT_FAILED, FRAUD_ALERT |
| channel | NotificationChannel (infra enum) | EMAIL, SMS |
| recipient | String | real email or phone (see channel routing below) |
| subject | String | |
| body | String (TEXT) | |
| status | NotificationStatus (local enum) | PENDING, SENT, FAILED |
| retryCount | int | incremented by RetryScheduler |
| createdAt | Instant | |
| updatedAt | Instant | |

### UserContact (`user_contacts`) — local read-model

| Field | Type | Notes |
|---|---|---|
| id | UUID | PK, generated |
| userId | UUID | unique — one record per user |
| email | String | not null — from UserRegisteredEvent |
| phone | String | nullable — from UserRegisteredEvent |

Populated by `UserRegisteredEventConsumer` on first event; upserted on re-delivery.

## Endpoints
| Method | Path | Description | Auth |
|---|---|---|---|
| GET | /api/v1/notifications/me | My notifications | CUSTOMER |
| GET | /api/v1/notifications | All notifications (paginated) | ADMIN |

## Kafka

- Produces: (none)
- Consumes: user.registered, policy.created, payment.completed, payment.failed, claim.submitted, claim.decision, fraud.detected

Channel routing (all consumers look up `UserContact` by `userId`; skip with warn if not found):

| Consumer | Type | Channel | Recipient |
| --- | --- | --- | --- |
| `UserRegisteredEventConsumer` | WELCOME | EMAIL | event.email (direct, no lookup needed) |
| `PolicyCreatedEventConsumer` | POLICY_PENDING | EMAIL | contact.email |
| `PaymentCompletedEventConsumer` | POLICY_ACTIVATED / PAYMENT_SUCCESS | SMS (EMAIL fallback) | phone if present, else email |
| `PaymentFailedEventConsumer` | PAYMENT_FAILED | SMS (EMAIL fallback) | phone if present, else email |
| `ClaimSubmittedEventConsumer` | CLAIM_SUBMITTED | EMAIL | contact.email |
| `ClaimDecisionEventConsumer` | CLAIM_APPROVED / CLAIM_REJECTED | SMS (EMAIL fallback) | phone if present, else email |
| `FraudDetectedEventConsumer` | FRAUD_ALERT (riskScore ≥ 70 only) | EMAIL | contact.email |

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

## AWS SNS + SES İnteqrasiyası (DONE)

### Arxitektura

`NotificationSender` interfeysinə çevrildi; iki implementasiya:

- **`LogNotificationSender`** (`@ConditionalOnProperty(aws.enabled=false, matchIfMissing=true)`) — lokal/test mühiti üçün default stub.
- **`AwsNotificationSender`** (`@ConditionalOnProperty(aws.enabled=true)`) — `SMS` → SNS direct publish, `EMAIL` → SES transactional email.

### Konfiqurasiya

```yaml
aws:
  enabled: ${AWS_ENABLED:false}
  region: ${AWS_REGION:eu-central-1}
  ses:
    from: ${AWS_SES_FROM:no-reply@ehi.example.com}
```

Credentials: `DefaultCredentialsProvider` — environment variable (`AWS_ACCESS_KEY_ID`/`AWS_SECRET_ACCESS_KEY`) və ya IAM role vasitəsilə.

### İdempotency

`Notification` entity-ə `correlationId` (UUID, nullable) əlavə edildi. `NotificationService.send()` imzası `(UUID correlationId, UUID userId, ...)` şəklindədir. Göndərişdən əvvəl `existsByCorrelationIdAndType` yoxlanılır — Kafka redelivery halında dublikat SMS/email göndərilmir.

Hər consumer öz event ID-sini `correlationId` kimi ötürür:

- `UserRegisteredEvent` → `userId`
- `PolicyCreatedEvent` → `policyId`
- `ClaimSubmittedEvent`, `ClaimDecisionEvent`, `FraudDetectedEvent` → `claimId`
- `PaymentCompletedEvent`, `PaymentFailedEvent` → `paymentId`

### Test

- **`AwsNotificationSenderTest`** (4 unit test): SNS publish, SNS xəta → false, SES sendEmail, SES xəta → false.
- IT testlər (`NotificationFlowIT`) Docker tələb edir; `./gradlew integrationTest` ilə ayrıca işlədilir (default `test` task-ından exclude).

### Pre-deploy

1. `AWS_ENABLED=true`, `AWS_REGION`, `AWS_SES_FROM` env dəyişənlərini set et.
2. SES-də göndərici email-i verify et (sandbox → production mode keçidi).
3. SNS SMS üçün IAM permission: `sns:Publish`.
4. `recipient` field is now real (email/phone from `user_contacts` read-model). No further action needed.

## Logging (SLF4J) — DONE

> All 7 consumers, `RetryScheduler`, and `NotificationSender` already log. The only gap is the
> persisted FAILED outcome in the service layer. Add `@Slf4j` only to `NotificationServiceImpl`,
> only the line below (avoid duplicating what `NotificationSender` already logs).

- **`NotificationServiceImpl`** (`@Slf4j`):
  - `send`: `warn` when the notification is saved with status FAILED —
    `"Notification FAILED: type={}, userId={}, recipient={}"`. Successful sends are already logged by
    `NotificationSender`, so no extra `info` is needed here.

## Review Findings (see root `check.md` for full detail)

- ✅ **`recipient` real-recipient fix done.** `user_contacts` read-model (projected from `user.registered`) resolves real email/phone for all consumers. SMS for financial events (payment, claim decision), EMAIL for informational (policy, claim submitted, fraud). Phone-null → EMAIL fallback.
- 🟢 **Retry scheduler isn't multi-instance safe.** With >1 notification replica, two `RetryScheduler`s
  would double-send (no locking/`ShedLock`). Note for scaling.

## Testing (DONE — 34 tests, all green)

Stack: JUnit 5 + Mockito + AssertJ (unit); `@SpringBootTest` + `@EmbeddedKafka` + Postgres
(`ehi_notification_test`, `create-drop`) for the flow IT. `ext['testcontainers.version'] = '1.20.6'`
override applied for Docker 29.x compatibility (testcontainers/spring-kafka-test added even though
the IT ultimately uses `@EmbeddedKafka` rather than a Testcontainers Kafka broker).

### Implemented test classes (all passing, 34 tests total)
- **`NotificationServiceImplTest`** (5): `send()` persists PENDING then updates to SENT when
  `NotificationSender` returns true (and FAILED when it returns false), passing the correctly
  built `Notification` to the sender and returning the mapped DTO; `getMyNotifications` scoped by
  `userId`; `getAllNotifications` pagination/mapping via `PagedResponse`.
- **Consumer mapping tests** (21 across 7 classes): each consumer test mocks `UserContactRepository`
  and verifies the correct channel/recipient resolution.
  `UserRegisteredEventConsumerTest` (WELCOME, recipient = event email),
  `PolicyCreatedEventConsumerTest` (POLICY_PENDING via EMAIL, no-contact skip),
  `PaymentCompletedEventConsumerTest` (POLICY_ACTIVATED / PAYMENT_SUCCESS via SMS, no-contact skip),
  `PaymentFailedEventConsumerTest` (PAYMENT_FAILED SMS / EMAIL fallback when no phone, no-contact skip),
  `ClaimSubmittedEventConsumerTest` (CLAIM_SUBMITTED EMAIL, no-contact skip),
  `ClaimDecisionEventConsumerTest` (CLAIM_APPROVED / CLAIM_REJECTED via SMS, no-send for UNDER_REVIEW),
  `FraudDetectedEventConsumerTest` (FRAUD_ALERT EMAIL only when `riskScore >= 70`; no-send below threshold or `null`).
- **`RetrySchedulerTest`** (3): queries `findByStatusAndRetryCountLessThan(FAILED, 3)`; increments
  `retryCount` and sets SENT/FAILED based on `NotificationSender` result.
- **`NotificationControllerTest`** (5, `@WebMvcTest`): `GET /me` CUSTOMER-only, `GET ` (paginated)
  ADMIN-only, forbidden for other roles, unauthenticated → 4xx.
- **`NotificationFlowIT`** (9, `@EmbeddedKafka`): publishes each of the 7 domain events (2 cases for
  `payment.completed` and `claim.decision`) and asserts the persisted `Notification` row has the
  expected `type`/`subject`/`status`.

### Implementation decisions / deviations
- `NotificationServiceImplTest`: `ArgumentCaptor` can't be used to compare the PENDING vs SENT
  saves because `send()` mutates and re-saves the *same* `Notification` instance — both captured
  values reflect the final state. Instead, the mock `save()` answer records `getStatus()` at call
  time into a list, asserting `[PENDING, SENT]` (or `[PENDING, FAILED]`) in order.
- `NotificationFlowIT`: `awaitNotification` polls `findByUserId` filtering on
  `type == expectedType && status != PENDING` (not just "row exists") — otherwise the poll can
  observe the row between the two `save()` calls inside `send()` (status still PENDING), pass the
  `isPresent()` check, and fail the subsequent `status == SENT` assertion. This was flaky only
  under the full-suite run, not in isolation.
