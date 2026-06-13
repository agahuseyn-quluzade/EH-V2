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
| correlationId | UUID | event's own id (policyId/paymentId/claimId/userId); dedup key with type+channel |
| type | NotificationType (infra enum) | WELCOME, POLICY_ACTIVATED, POLICY_PENDING, CLAIM_SUBMITTED, CLAIM_APPROVED, CLAIM_REJECTED, PAYMENT_SUCCESS, PAYMENT_FAILED, FRAUD_ALERT |
| channel | NotificationChannel (infra enum) | EMAIL, SMS |
| recipient | String | resolved email/phone from `UserContact` (email from UserRegisteredEvent for the WELCOME row) |
| subject | String | |
| body | String (TEXT) | |
| status | NotificationStatus (local enum) | PENDING, SENT, FAILED |
| retryCount | int | incremented by RetryScheduler |
| createdAt | Instant | |
| updatedAt | Instant | |

### UserContact
| Field | Type | Notes |
|---|---|---|
| id | UUID | PK, generated |
| userId | UUID | unique, from `user.registered` |
| email | String | not null |
| phone | String | nullable — null when the user gave no phone |

## Endpoints
| Method | Path | Description | Auth |
|---|---|---|---|
| GET | /api/v1/notifications/me | My notifications | CUSTOMER |
| GET | /api/v1/notifications | All notifications (paginated) | ADMIN |

## Kafka
- Produces: (none)
- Consumes: user.registered, policy.created, payment.completed, payment.failed, claim.submitted, claim.decision, fraud.detected
  - `UserRegisteredEventConsumer` → upserts `UserContact` (email + phone from the event), then sends
    WELCOME EMAIL to `event.email()`, plus WELCOME SMS to `event.phone()` if present.
  - `PolicyCreatedEventConsumer` → POLICY_PENDING
  - `PaymentCompletedEventConsumer` → POLICY_ACTIVATED (referenceType=POLICY_PREMIUM) or PAYMENT_SUCCESS (referenceType=CLAIM_PAYOUT)
  - `PaymentFailedEventConsumer` → PAYMENT_FAILED
  - `ClaimSubmittedEventConsumer` → CLAIM_SUBMITTED
  - `ClaimDecisionEventConsumer` → CLAIM_APPROVED (decision=APPROVED) or CLAIM_REJECTED (decision=REJECTED); ignored for other decisions
  - `FraudDetectedEventConsumer` → FRAUD_ALERT, only if riskScore >= 70
  - The six non-registration consumers look up `UserContact` by `userId`; if absent they log a warn
    and skip (no `Notification` row). Otherwise they always send EMAIL to `contact.email`, plus SMS
    to `contact.phone` if non-null — same `type`, same `correlationId` (the event's own id), two
    independent `Notification` rows.

## Decisions & Notes
- **SMS (mock) + Email (real) — implemented** (see "IMPLEMENTATION PLAN" below for the full design).
  `NotificationSender` is now an interface; `ChannelNotificationSender` (service/impl) dispatches by
  `channel` to `SmsProvider` (`MockSmsProvider`, logs only) or `EmailProvider` (`SmtpEmailProvider`
  via `JavaMailSender`/SMTP, `@Profile("!it")`; `MockEmailProvider`, `@Profile("it")`, logs only).
- Retry: `RetryScheduler` (scheduler package) runs every 5 minutes (`@Scheduled(fixedRate = 300000)`), retries notifications with status FAILED and retryCount < 3 (MAX_RETRIES), incrementing retryCount on each attempt.
- `NotificationStatus` (PENDING/SENT/FAILED) kept local to this service (not infra) — no other service consumes it.
- Endpoint paths corrected from doc's `/api/notifications/...` to actual `/api/v1/notifications/...` (consistent with all other services' `/api/v1/` prefix).
- `recipient` field: now resolved from the local `user_contacts` table (`{userId → email, phone}`),
  populated by `UserRegisteredEventConsumer` from `UserRegisteredEvent.phone()` (infra Option A —
  `phone` threaded from `RegisterRequest` → `User` → `UserRegisteredEvent`). The old
  `userId.toString()` placeholder is gone.
- Infra's `NotificationEvent` record and `KafkaTopics.NOTIFICATION_SEND` topic exist but are not used — this service consumes the 7 domain events directly per the original design, not a generic notification-send topic.
- `correlationId` (the event's own id) + `existsByCorrelationIdAndTypeAndChannel` dedup means Kafka
  redelivery does not double-send, while the parallel EMAIL+SMS of one event remain independent rows
  (channel is part of the dedup key).
- JWT/security setup identical to claim/payment/ai services (JwtProperties, JwtProvider, JwtAuthenticationFilter, SecurityConfig).
- `./gradlew build` passes (BUILD SUCCESSFUL).
- Dockerfile (multi-stage, same pattern as claim/payment/ai): `infra-build` stage publishes `e-health-insurance-infra` (via Compose's `additional_contexts: infra`) to `/root/.m2`, `build` stage compiles `bootJar`, runtime stage `eclipse-temurin:21-jre-jammy`. `gradle.properties` removed before building. `.dockerignore` excludes `.gradle/`, `build/`, `out/`.
- `application-docker.yml` overrides `spring.datasource.url` → `postgres:5432/ehi_notification` and `spring.kafka.bootstrap-servers` → `kafka:29092`, activated via `SPRING_PROFILES_ACTIVE=docker`.
- Schema managed by Liquibase (`runtimeOnly 'org.liquibase:liquibase-core'`), aligned with the other persistence services: `application.yml` uses `ddl-auto: validate` + `show-sql: false` (hardened from the original `update`/`true`). Each changeset carries a precondition with `onFail: MARK_RAN`, so it applies on a fresh DB and cleanly skips (records as ran) on a DB where the object already exists. The IT profile (`application-it.yml`) keeps `ddl-auto: create-drop`, same as the other services.
  `db/changelog/db.changelog-master.yaml` now has 4 changesets:
  `001-create-notifications` (the `notifications` table), `002-create-user-contacts` (the
  `user_contacts` table), `003-add-correlation-id-column` (adds `correlation_id` to `notifications`),
  `004-add-correlation-index` (index on `notifications(correlation_id, type, channel)`).
  > Gotcha hit during implementation: with `ddl-auto: create-drop` + Liquibase, if the `it` test JVM
  > is killed mid-run, Liquibase's `DATABASECHANGELOG` can retain entries for changesets whose tables
  > were already dropped by `create-drop`'s shutdown hook — the next run then fails a later
  > changeset with "relation does not exist". Fix is to drop/recreate `ehi_notification_test` (a
  > disposable test DB) when this happens.

## Logging (SLF4J) — DONE

> All 7 consumers, `RetryScheduler`, and `NotificationSender` already log. The only gap is the
> persisted FAILED outcome in the service layer. Add `@Slf4j` only to `NotificationServiceImpl`,
> only the line below (avoid duplicating what `NotificationSender` already logs).

- **`NotificationServiceImpl`** (`@Slf4j`):
  - `send`: `warn` when the notification is saved with status FAILED —
    `"Notification FAILED: type={}, userId={}, recipient={}"`. Successful sends are already logged by
    `NotificationSender`, so no extra `info` is needed here.

## Review Findings (see root `check.md` for full detail)

- ✅ **RESOLVED** — ~~`recipient` is a `userId.toString()` placeholder for every event except
  registration~~. Fixed by the "Mock SMS + Real Email" implementation below: a local `UserContact`
  table resolves real email/phone, and `SmtpEmailProvider`/`MockSmsProvider` actually dispatch.
- 🟢 **Retry scheduler isn't multi-instance safe.** With >1 notification replica, two `RetryScheduler`s
  would double-send (no locking/`ShedLock`). Note for scaling.
- 🟢 **Users who registered before this feature existed have no `UserContact` row** — their
  non-registration notifications are skipped (logged as a warn). Acceptable for MVP; would need a
  backfill job for production.

## Testing (DONE — 50 tests, all green)

Stack: JUnit 5 + Mockito + AssertJ (unit); `@SpringBootTest` + `@EmbeddedKafka` + Postgres
(`ehi_notification_test`, `create-drop`) for the flow IT. `ext['testcontainers.version'] = '1.20.6'`
override applied for Docker 29.x compatibility (testcontainers/spring-kafka-test added even though
the IT ultimately uses `@EmbeddedKafka` rather than a Testcontainers Kafka broker).

### Implemented test classes (all passing, 50 tests total)
- **`NotificationServiceImplTest`** (5): `send()` persists PENDING then updates to SENT when
  `NotificationSender` returns true (and FAILED when it returns false), passing the correctly
  built `Notification` to the sender and returning the mapped DTO; `getMyNotifications` scoped by
  `userId`; `getAllNotifications` pagination/mapping via `PagedResponse`; duplicate `(correlationId,
  type, channel)` short-circuits to the existing row without saving.
- **Consumer mapping tests** (24 across 7 classes, one class per consumer): every non-registration
  consumer (`PolicyCreated`, `PaymentCompleted`, `PaymentFailed`, `ClaimSubmitted`, `ClaimDecision`,
  `FraudDetected`) covers contact-found→EMAIL and no-contact→skip (no `notificationService`
  interaction); `PolicyCreated`, `ClaimSubmitted`, `ClaimDecision`, `FraudDetected` additionally cover
  contact-with-phone→EMAIL+SMS. `UserRegisteredEventConsumerTest` covers upsert-contact + EMAIL-only
  (no phone), upsert-contact + EMAIL+SMS (with phone), and updating an existing contact's
  email/phone. Original per-type assertions (POLICY_PENDING, POLICY_ACTIVATED/PAYMENT_SUCCESS,
  PAYMENT_FAILED, CLAIM_SUBMITTED, CLAIM_APPROVED/CLAIM_REJECTED/no-send for UNDER_REVIEW, FRAUD_ALERT
  only when `riskScore >= 70`) are preserved within the contact-found cases.
- **`RetrySchedulerTest`** (3): queries `findByStatusAndRetryCountLessThan(FAILED, 3)`; increments
  `retryCount` and sets SENT/FAILED based on `NotificationSender` result.
- **`NotificationControllerTest`** (5, `@WebMvcTest`): `GET /me` CUSTOMER-only, `GET ` (paginated)
  ADMIN-only, forbidden for other roles, unauthenticated → 4xx.
- **`NotificationFlowIT`** (9, `@EmbeddedKafka`): publishes each of the 7 domain events (2 cases for
  `payment.completed` and `claim.decision`) and asserts the persisted `Notification` row has the
  expected `type`/`subject`/`status`. Seeds a `UserContact` (email only, no phone) before each
  non-registration event so the lookup consumers don't skip. Runs under `@ActiveProfiles("it")` so
  `MockEmailProvider`/`MockSmsProvider` are active — no real SMTP needed.
- **`ChannelNotificationSenderTest`** (3): SMS channel → `smsProvider.send`, EMAIL channel →
  `emailProvider.send`, provider exception → `send()` returns `false`.
- **`SmtpEmailProviderTest`** (1): mocks `JavaMailSender`, asserts the `SimpleMailMessage`
  from/to/subject/body passed to `mailSender.send(...)`.

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
- `application.yml` had two top-level `spring:` keys (one for datasource/kafka, one added for
  `spring.mail.*`) — YAML's `DuplicateKeyException` failed every `ApplicationContext`-loading test.
  Merged into a single `spring:` block.

---

# IMPLEMENTATION PLAN — Mock SMS + Real Email Notifications

> **Audience: the implementing model.** These are build-from-this instructions. Read this whole
> section before writing code. Follow the CLAUDE.md workflow: do the steps in order, build after
> each, and do not skip the cross-repo prerequisites.

## 0. Context — what this is and where it comes from

The **working** notification service is the repo `e-health-insurance-notification_V1` (Java 21,
Spring Boot 3.3.5, 33 green tests). Today it only **logs** "sent" notifications and uses
`userId.toString()` as the recipient for every event except registration — so nothing can actually
be emailed or texted.

A **legacy** sibling repo, `e-health-insurance-notification` (the plain one, **NOT** `_V1`), already
contains the real SMS/email feature. **We are porting the FEATURE from the legacy repo into the
`_V1` repo.** Use the legacy repo only as a reference for the feature code.

> ⚠️ **Do NOT copy the legacy repo's platform versions.** The legacy repo is an older baseline
> (Java 17, Spring Boot 3.2.5, Lombok 1.18.30, `ddl-auto: none`). Keep `_V1`'s Java 21 / Spring
> Boot 3.3.5 / Lombok 1.18.34 / `ddl-auto: validate`. Only port the *feature*, never downgrade.

**Design of the feature (portfolio/diploma — no paid SMS):**
- The existing `NotificationSender` stays as the Kafka-event-driven entry point. It becomes a small
  **dispatcher** that, per `Notification`, calls either an `SmsProvider` or an `EmailProvider`
  depending on `channel`. Persistence, status (SENT/FAILED), retry, and dedup are unchanged.
- **SMS is never really sent.** `SmsProvider` has a **single** impl, `MockSmsProvider`, that logs
  `SMS TO {phone} : {message}`. No Twilio, no AWS, no provider profiles (only one impl, so no
  collision). A real provider can be slotted in later behind the same interface.
- **Email is actually sent** via `EmailProvider` → `SmtpEmailProvider` (Spring `JavaMailSender` +
  SMTP). For integration tests a `MockEmailProvider` (`@Profile("it")`) logs instead, so the build
  never hits a real mail server.
- A local **`user_contacts`** table (`{userId → email, phone}`) so non-registration events can
  resolve a *real* recipient. `UserRegisteredEventConsumer` upserts it; the other consumers read it.
- **Channel policy (Option 1):** every event sends a **real EMAIL**; *additionally*, when the user
  has a phone, it also sends a **mock SMS**. Both fire together when a phone exists.
- **Idempotency (Part B):** a `correlationId` on `Notification` + a dedup check keyed on
  `(correlationId, type, channel)`, so redelivery does not double-send *and* the parallel EMAIL+SMS
  of one event are not collapsed into each other. (See Step 6.)

> The legacy `e-health-insurance-notification` repo is still a useful reference for `UserContact`,
> the consumer rewrite, and the dedup wiring — but **ignore its AWS SNS/SES sender**; we are
> deliberately using the simpler mock-SMS + SMTP-email design above instead.

**Phone provisioning (decision: Option A — adopted).** SMS needs a phone number, which today is
never captured or published. Option A threads `phone` end-to-end:
`RegisterRequest` → `User` (IAM) → `UserRegisteredEvent` (infra) → `UserContact` (notification).
`phone` is **optional/nullable everywhere** (a user without a phone simply gets EMAIL). This
requires changes in **three other repos** — they are prerequisites, do them first:
- `e-health-insurance-infra` — see `docs/e-health-insurance-infra.md` → "Planned: phone on UserRegisteredEvent".
- `e-health-insurance-iam` — see `docs/e-health-insurance-iam.md` → "Planned: capture & publish phone".
- `e-health-insurance-frontend` — see `docs/e-health-insurance-frontend.md` → "Planned: phone on registration".

## 1. Prerequisites (other repos) — DO THESE FIRST

1. Apply the infra change (add `phone` to `UserRegisteredEvent`, make it a `@Builder` record) and run
   `./gradlew publishToMavenLocal` in `e-health-insurance-infra`. **Nothing downstream compiles until
   this is published.**
2. Apply the IAM change (RegisterRequest + User entity + `users.phone` migration + publish phone).
3. Apply the frontend change (optional phone input on the register form).

Only then start the notification-service steps below.

## 2. Step-by-step (notification `_V1` repo)

### Step 1 — Build wiring (`build.gradle`)

Add Spring Mail (for real email). **No AWS, no Twilio.** In `build.gradle`, inside
`dependencies { ... }`, add:
```groovy
implementation 'org.springframework.boot:spring-boot-starter-mail'
```
That is the only new dependency. Leave the version catalog, `test` task, Testcontainers override, and
Java toolchain exactly as `_V1` has them. Do **not** add any AWS or Twilio SDK, and do **not** import
the legacy repo's `test { exclude('**/*IT.class') }` / `integrationTest` split — `_V1` runs ITs via
`@EmbeddedKafka` in the normal `test` task.

### Step 2 — Mail config (`application.yml` + a small properties class)

In `src/main/resources/application.yml`, append (keep `ddl-auto: validate`):
```yaml
spring:
  mail:
    host: ${MAIL_HOST:smtp.gmail.com}
    port: ${MAIL_PORT:587}
    username: ${MAIL_USERNAME:}
    password: ${MAIL_PASSWORD:}
    properties:
      mail:
        smtp:
          auth: true
          starttls:
            enable: true

notification:
  email:
    from: ${MAIL_FROM:no-reply@ehi.example.com}
```
For a real inbox point `MAIL_*` at Gmail SMTP (use an app password) or any SMTP host; for a safe
sandbox use Mailtrap/MailHog. Credentials stay in env — never commit them.

Create `config/NotificationProperties.java`
(`@Component @ConfigurationProperties(prefix = "notification") @Data`) with a nested
`Email email = new Email()` where `Email` has `String from`. Spring Boot autoconfigures
`JavaMailSender` from the `spring.mail.*` block — no `@Bean` needed.

No change to `application-docker.yml` is required; set the `MAIL_*` env vars on the container when you
want Compose to send real mail. The `it` test profile uses `MockEmailProvider`, so tests need no SMTP.

### Step 3 — Providers + dispatcher

`_V1` currently has a single concrete class `service/impl/NotificationSender.java` (log-only).
Replace it with an interface + a dispatcher, plus the two provider abstractions:

1. **Delete** `service/impl/NotificationSender.java`.
2. `service/NotificationSender.java` (interface): `boolean send(Notification notification);`.
3. `service/SmsProvider.java` (interface): `void send(String phone, String message);`.
4. `service/EmailProvider.java` (interface): `void send(String to, String subject, String body);`.
5. `service/impl/MockSmsProvider.java` — `@Service @Slf4j`, implements `SmsProvider`:
   ```java
   public void send(String phone, String message) {
       log.info("SMS TO {} : {}", phone, message);
   }
   ```
   (No profile — it is the only `SmsProvider` bean.)
6. `service/impl/SmtpEmailProvider.java` — `@Service @Profile("!it") @RequiredArgsConstructor @Slf4j`,
   implements `EmailProvider`. Ctor-inject `JavaMailSender` and `NotificationProperties`. Build a
   `SimpleMailMessage` (`setFrom(props.getEmail().getFrom())`, `setTo(to)`, `setSubject(subject)`,
   `setText(body)`) and call `mailSender.send(msg)`. Let exceptions propagate — the dispatcher catches
   them and marks the notification FAILED so the retry scheduler retries.
7. `service/impl/MockEmailProvider.java` — `@Service @Profile("it") @Slf4j`, implements `EmailProvider`,
   logs `EMAIL TO {to} | {subject} | {body}`. Keeps integration tests off a real SMTP server.
8. `service/impl/ChannelNotificationSender.java` — `@Service @RequiredArgsConstructor @Slf4j`,
   implements `NotificationSender`. Ctor-inject `SmsProvider` + `EmailProvider`:
   ```java
   public boolean send(Notification n) {
       try {
           if (n.getChannel() == NotificationChannel.SMS) {
               smsProvider.send(n.getRecipient(), n.getBody());
           } else {
               emailProvider.send(n.getRecipient(), n.getSubject(), n.getBody());
           }
           return true;
       } catch (Exception e) {
           log.warn("Send failed: type={} channel={} recipient={}: {}",
                   n.getType(), n.getChannel(), n.getRecipient(), e.getMessage());
           return false;
       }
   }
   ```

`NotificationServiceImpl` already declares `private final NotificationSender notificationSender;` and
calls `notificationSender.send(...)` — it now resolves to the dispatcher interface, so **no change is
needed there** beyond the correlationId work in Step 6.

> There is exactly one `SmsProvider` bean (mock) and exactly one `EmailProvider` bean per profile
> (`SmtpEmailProvider` normally, `MockEmailProvider` under `it`), so wiring is unambiguous.

### Step 4 — `UserContact` entity + repository + Liquibase

Create `entity/UserContact.java` (`@Entity @Table(name = "user_contacts") @Data @Builder
@NoArgsConstructor @AllArgsConstructor`):
| Field | Type | Column |
|---|---|---|
| id | UUID | `@Id @GeneratedValue(strategy = GenerationType.UUID)` |
| userId | UUID | `@Column(nullable = false, unique = true)` |
| email | String | `@Column(nullable = false)` |
| phone | String | nullable |

Create `repository/UserContactRepository.java` extending `JpaRepository<UserContact, UUID>` with
`Optional<UserContact> findByUserId(UUID userId);`.

Because `_V1` runs `ddl-auto: validate`, add a changeset to
`src/main/resources/db/changelog/db.changelog-master.yaml` (mirror the existing `001` style with a
`not tableExists` precondition, `onFail: MARK_RAN`):
```yaml
  - changeSet:
      id: 002-create-user-contacts
      author: ehi
      preConditions:
        onFail: MARK_RAN
        not:
          tableExists:
            tableName: user_contacts
      changes:
        - createTable:
            tableName: user_contacts
            columns:
              - column: { name: id, type: uuid, constraints: { primaryKey: true, nullable: false } }
              - column: { name: user_id, type: uuid, constraints: { nullable: false, unique: true } }
              - column: { name: email, type: varchar(255), constraints: { nullable: false } }
              - column: { name: phone, type: varchar(50) }
```
(Use the same expanded YAML layout as the existing changeset — the inline form above is just for brevity.)

### Step 5 — Rewrite the 7 consumers to resolve recipients and fire email (+ mock SMS)

Inject `UserContactRepository` into every consumer (alongside `NotificationService`).

**Channel policy (Option 1):** always send the **EMAIL** notification; **additionally**, when the
user has a phone, send the **SMS** notification too (same `type`, same `correlationId`,
`channel = SMS`, `recipient = phone`). Both are persisted as separate `Notification` rows and go
through the dispatcher (email → real SMTP, SMS → mock log).

- **`UserRegisteredEventConsumer`** — **upsert** the contact from the event, then send WELCOME:
  ```java
  userContactRepository.findByUserId(event.userId()).ifPresentOrElse(
      existing -> { existing.setEmail(event.email()); existing.setPhone(event.phone()); userContactRepository.save(existing); },
      () -> userContactRepository.save(UserContact.builder()
              .userId(event.userId()).email(event.email()).phone(event.phone()).build()));
  ```
  (`event.phone()` comes from the infra change — Option A.) Then send EMAIL to `event.email()`, and
  **if `event.phone() != null`** also send SMS to `event.phone()`.
- **The other six** (`PolicyCreated`, `PaymentCompleted`, `PaymentFailed`, `ClaimSubmitted`,
  `ClaimDecision`, `FraudDetected`) — **look up** the contact; if absent, log a warn and `return`:
  ```java
  Optional<UserContact> contact = userContactRepository.findByUserId(event.userId());
  if (contact.isEmpty()) { log.warn("No contact for userId={}, skipping notification", event.userId()); return; }
  // EMAIL (always):
  notificationService.send(corrId, event.userId(), type, NotificationChannel.EMAIL, contact.get().getEmail(), subject, body);
  // SMS (mock) only when a phone exists:
  if (contact.get().getPhone() != null) {
      notificationService.send(corrId, event.userId(), type, NotificationChannel.SMS, contact.get().getPhone(), subject, body);
  }
  ```
  Keep each consumer's existing subject/body text, `type` mapping, and conditional logic
  **unchanged** (`FraudDetected` only when `riskScore >= 70`; `PaymentCompleted`
  POLICY_PREMIUM→POLICY_ACTIVATED vs CLAIM_PAYOUT→PAYMENT_SUCCESS; `ClaimDecision` APPROVED/REJECTED
  only). `corrId` is the event's own id: `policyId` / `paymentId` / `claimId`.

> The SMS reuses the email `body` (the mock just logs it); SMS has no subject, so the dispatcher
> ignores `subject` for the SMS channel. Passing a shorter SMS message instead is optional.
> Two `send(...)` calls per event when a phone exists is intentional: each channel is its own
> persisted, independently-retried `Notification`. The Step 6 dedup key includes `channel`, so the
> EMAIL and SMS rows do not collide.

### Step 6 — Idempotency: `correlationId` + dedup (Part B)

1. **Entity:** add `private UUID correlationId;` to `Notification` (nullable column, no constraint).
2. **Repository (`NotificationRepository`):** add (keyed on `channel` too, so the EMAIL+SMS of one
   event don't collide — import `com.ehi.infra.enums.NotificationType` and `NotificationChannel`):
   ```java
   boolean existsByCorrelationIdAndTypeAndChannel(UUID correlationId, NotificationType type, NotificationChannel channel);
   Notification findByCorrelationIdAndTypeAndChannel(UUID correlationId, NotificationType type, NotificationChannel channel);
   ```
3. **Service interface + impl:** change `send(...)` to take `UUID correlationId` as the **first**
   parameter:
   ```java
   NotificationDto send(UUID correlationId, UUID userId, NotificationType type,
                        NotificationChannel channel, String recipient, String subject, String body);
   ```
   At the top of the impl, short-circuit duplicates (note `channel` is now part of the key):
   ```java
   if (notificationRepository.existsByCorrelationIdAndTypeAndChannel(correlationId, type, channel)) {
       log.warn("Duplicate notification skipped: correlationId={}, type={}, channel={}", correlationId, type, channel);
       return notificationMapper.toDto(
               notificationRepository.findByCorrelationIdAndTypeAndChannel(correlationId, type, channel));
   }
   ```
   and set `.correlationId(correlationId)` on the builder. Everything else in `send()` is unchanged.
4. **Liquibase (forward-only — don't edit `001`):**
   - `003-add-correlation-id-column` — `addColumn` `correlation_id uuid` on `notifications`
     (precondition `not columnExists`, `onFail: MARK_RAN`).
   - `004-add-correlation-index` — `createIndex` `idx_notifications_correlation_type_channel` on
     `notifications(correlation_id, type, channel)` (precondition `not indexExists`, `onFail: MARK_RAN`).
5. All callers are the 7 consumers from Step 5 (they already pass the correlationId as first arg).

### Step 7 — Tests + build

- **New provider/dispatcher tests:**
  - `ChannelNotificationSenderTest` — mock `SmsProvider`/`EmailProvider`; assert the SMS channel calls
    `smsProvider.send`, the EMAIL channel calls `emailProvider.send`, and a thrown exception makes
    `send(...)` return `false`.
  - `SmtpEmailProviderTest` — mock `JavaMailSender`; verify a `SimpleMailMessage` with the right
    from/to/subject/body is passed to `mailSender.send(...)`.
  - (`MockSmsProvider`/`MockEmailProvider` are trivial log-only; a tiny test is optional.)
- **Do NOT port any `AwsNotificationSenderTest`** — there is no AWS sender.
- `NotificationServiceImplTest` mocks `NotificationSender` (now the dispatcher interface) — fine.
- **Fix every `new UserRegisteredEvent(...)` call** to pass the new `phone` arg (or the infra
  `@Builder`):
  - `src/test/java/com/ehi/notification/NotificationFlowIT.java:58`
  - `src/test/java/com/ehi/notification/kafka/UserRegisteredEventConsumerTest.java:27`
- **Update consumer unit tests** to stub `userContactRepository.findByUserId(...)`: with a phone →
  verify **two** `send(...)` calls (EMAIL then SMS); without a phone → verify **one** (EMAIL only);
  `Optional.empty()` → verify **no** send (skip) for the six lookup consumers. Include the leading
  `correlationId` arg in verifications.
- `NotificationFlowIT` runs under `@ActiveProfiles("it")`, so `MockEmailProvider` + `MockSmsProvider`
  are active — **no real email is sent**. Seed a `UserContact` (or let the `user.registered` event
  create it first) so the lookup consumers don't skip; give the contact a phone if you assert SMS rows.
- Run `./gradlew build` — must be green with **no** mail credentials (the `it` profile never touches
  SMTP).

## 3. Decisions & Notes (record after implementing)

- **No real SMS.** `SmsProvider` has only `MockSmsProvider`, which logs `SMS TO {phone} : {message}`.
  This is a common dev pattern; a real provider can be added later behind the interface with profiles.
- **Email is real** via Spring `JavaMailSender`/SMTP. Set `MAIL_*` env vars to send (Gmail app
  password for a real inbox, Mailtrap/MailHog for a sandbox). The `it` profile swaps in
  `MockEmailProvider`, so tests/CI never need credentials.
- **Channel policy:** every event → real EMAIL; **plus** a mock SMS when the user has a phone. Two
  independent `Notification` rows (own status/retry) when a phone exists.
- `UserContact` is the local read-model that fixes the old `userId.toString()` placeholder; it is
  populated only from `user.registered`, so events for users who registered *before* this service
  existed are skipped (logged as a warn — acceptable for MVP).
- `correlationId` dedup is keyed on **`(correlationId, type, channel)`** → idempotent against Kafka
  redelivery while still allowing the parallel EMAIL and SMS of the same event.
