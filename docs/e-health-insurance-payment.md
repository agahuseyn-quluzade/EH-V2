# e-health-insurance-payment

## Status: DONE (tests complete)
## Port: 8084
## Database: ehi_payment

## What's Done
- [x] build.gradle
- [x] Entity: Payment
- [x] Repository: PaymentRepository
- [x] Service: PaymentService, MockPaymentProcessor
- [x] Controller: PaymentController
- [x] DTO: PaymentDto
- [x] Kafka producer: payment.completed, payment.failed
- [x] Kafka consumer: policy.created, claim.decision (APPROVED)
- [x] GlobalExceptionHandler
- [x] application.yml
- [x] Dockerfile

## Entities

### Payment (`payments`)
| Field | Type | Notes |
|---|---|---|
| id | UUID | PK, generated |
| userId | UUID | not null, plain column, no FK |
| referenceId | UUID | not null — policyId (POLICY_PREMIUM) or claimId (CLAIM_PAYOUT) |
| referenceType | PaymentReferenceType | not null — POLICY_PREMIUM, CLAIM_PAYOUT |
| amount | BigDecimal | not null |
| status | PaymentStatus | not null — PENDING, COMPLETED, FAILED, REFUNDED |
| transactionId | String | nullable — set on completion ("TXN-" + UUID) |
| failureReason | String | nullable — set if processing fails validation |
| createdAt | Instant | `@PrePersist` |
| updatedAt | Instant | `@PrePersist`/`@PreUpdate` |

## Endpoints
| Method | Path | Description | Auth |
|---|---|---|---|
| POST | /api/v1/payments/process | Manual trigger (demo) | ADMIN |
| GET | /api/v1/payments/me | My payments | CUSTOMER |
| GET | /api/v1/payments/{id} | Payment detail | authenticated (owner or ADMIN — 404 if non-owner & non-admin) |
| GET | /api/v1/payments?page=&size= | All payments, paginated | ADMIN |

## Kafka
- Produces: payment.completed, payment.failed
- Consumes: policy.created, claim.decision (only when `decision == APPROVED`)

## Decisions & Notes
- Payment is MOCK for MVP — 2 second async delay then auto-complete (`MockPaymentProcessor`).
- Endpoints corrected from the doc's original `/api/payments/...` to `/api/v1/payments/...` (same correction as ai/gw).
- Package root: `com.ehi.payment`. Same Gradle setup as claim (Gradle 8.10.2 wrapper, `io.spring.dependency-management` 1.1.7, JDK 17 `Contents/Home` path in `gradle.properties`).
- Security setup mirrors claim exactly: `JwtProperties`, `JwtProvider` (validation-only), `JwtAuthenticationFilter`, `SecurityConfig` (stateless, `@EnableMethodSecurity`, `.anyRequest().authenticated()`, role-based `@PreAuthorize` per endpoint).
- `PaymentApplication` annotated `@EnableAsync` to support `MockPaymentProcessor`'s `@Async` processing.
- DTOs: `PaymentDto` (9 fields, `@Builder`, mirrors `Payment`); `ProcessPaymentRequest` (4 fields, `@NotNull`/`@Positive` validated — `userId`, `referenceId`, `referenceType`, `amount`).
- `PaymentMapper` (MapStruct): `Payment` → `PaymentDto`, direct field match.
- `PaymentRepository`: `findByUserId` for `/me`; pagination for `/` uses `findAll(Pageable)` directly.
- `PaymentService`/`PaymentServiceImpl`:
  - `processPayment(userId, referenceId, referenceType, amount)`: if `amount <= 0`, immediately saves a `FAILED` payment with `failureReason="Invalid payment amount"` and publishes `PaymentFailedEvent`. Otherwise saves a `PENDING` payment and hands off to `MockPaymentProcessor.process(paymentId)` (async).
  - `getPaymentById`: same owner-or-privileged 404 pattern as claim's `findAccessibleClaim` — non-owner, non-admin requesters get `NotFoundException` (not 403) to avoid leaking existence.
  - `getAllPayments`: `PagedResponse` built from `Page<Payment>`, same pattern as `ClaimServiceImpl.getAllClaims`.
- `MockPaymentProcessor` (`@Component`, `@Async process(UUID paymentId)`): sleeps 2s, reloads the `Payment`, sets `status=COMPLETED` + generates `transactionId`, saves, publishes `PaymentCompletedEvent`.
- Kafka producers: `PaymentCompletedEventProducer` (`payment.completed`, keyed by `paymentId`), `PaymentFailedEventProducer` (`payment.failed`, keyed by `paymentId`).
- Kafka consumers (`groupId = "payment-service"`):
  - `PolicyCreatedEventConsumer` — on `policy.created`, calls `processPayment(userId, policyId, POLICY_PREMIUM, premiumAmount)`.
  - `ClaimDecisionEventConsumer` — on `claim.decision`, only acts if `decision == APPROVED`, calls `processPayment(userId, claimId, CLAIM_PAYOUT, approvedAmount)`.
- `PaymentErrorEnum implements BaseErrorService`: single `FORBIDDEN("PAYMENT-FORBIDDEN-0001", "Access denied", 403)` entry, same pattern as `ClaimErrorEnum`/`AiErrorEnum`. `NotFoundException` (infra) used directly for not-found cases.
- `GlobalExceptionHandler`: identical structure to claim/ai — `BaseException` → `ApiResponse.error`, `MethodArgumentNotValidException` → `VALIDATION_ERROR`, `AccessDeniedException` → `PaymentErrorEnum.FORBIDDEN`, generic `Exception` → `INTERNAL_ERROR`.
- Build verification: `./gradlew build` → BUILD SUCCESSFUL. `application.yml` uses `ddl-auto: validate` and `show-sql: false`.
- Dockerfile (multi-stage, same pattern as claim): `infra-build` stage publishes `e-health-insurance-infra` (via Compose's `additional_contexts: infra`) to `/root/.m2`, `build` stage compiles `bootJar`, runtime stage `eclipse-temurin:17-jre-jammy`. `gradle.properties` removed before building. `.dockerignore` excludes `.gradle/`, `build/`, `out/`.
- `application-docker.yml` overrides `spring.datasource.url` → `postgres:5432/ehi_payment` and `spring.kafka.bootstrap-servers` → `kafka:29092`, activated via `SPRING_PROFILES_ACTIVE=docker`.

## Logging (SLF4J) — DONE

> Add `@Slf4j` only to `PaymentServiceImpl`, only the listed lines. Producers/consumers and
> `MockPaymentProcessor` already log. The idempotency skip is the most important to log — it's the
> dedup path that prevents double charges/payouts. Follow the existing convention (parameterized
> `{}`, `info` for state changes, `warn` for skipped/failed).

- **`PaymentServiceImpl`** (`@Slf4j`):
  - `processPayment`: `info` when a PENDING payment is created — `"Processing payment: referenceId={},
    type={}, amount={}"`; `warn` on the **idempotency skip** (existing non-FAILED payment found) —
    `"Duplicate payment skipped for referenceId={}, type={}"`; `warn` on invalid amount → FAILED —
    `"Payment failed (invalid amount): referenceId={}, amount={}"`.

## Review Findings (see root `check.md` for full detail)

- ✅ **Fixed** — payments were not idempotent (duplicate Kafka delivery = double charge / double
  payout). `processPayment` now calls
  `PaymentRepository.findFirstByReferenceIdAndReferenceTypeAndStatusNot(referenceId, referenceType, FAILED)`
  first — if a non-FAILED payment already exists for that `(referenceId, referenceType)`, it is
  returned as-is instead of inserting a duplicate. Covered by
  `PaymentServiceImplTest#processPayment_idempotent_returnsExistingPayment_whenNonFailedPaymentExists`
  and `#processPayment_createsNew_whenExistingPaymentForReferenceIsFailed`.
- 🟡 **Async read-after-write race.** `processPayment` saves a PENDING row then hands the id to
  `@Async MockPaymentProcessor.process`, which `findById`s it; this works only because the 2s sleep
  masks the commit. With a real fast processor it races. Pass the data into the async call, or make
  the write transactional and fire the async step after commit (`@TransactionalEventListener`).
- 🟢 `MockPaymentProcessor` swallows `InterruptedException` and returns, leaving the payment PENDING
  forever with no retry (acceptable for a mock).

## Testing (DONE — 22 tests, all green)

Stack: JUnit 5 + Mockito + AssertJ (unit); `@SpringBootTest` + `@EmbeddedKafka` + running Compose
Postgres (IT).

### Implemented test classes (all passing, 22 tests total)

- **`PaymentServiceImplTest`** (9 tests): `processPayment` saves PENDING and hands off to
  `MockPaymentProcessor` for positive amounts; saves FAILED + publishes `PaymentFailedEvent` for
  zero/negative amounts; **idempotency (fix #1)** — a non-FAILED payment already existing for the
  same `(referenceId, referenceType)` is returned as-is with no new save/processor call/event, while
  an existing **FAILED** payment for that reference does not block a new attempt; `getPaymentById`
  throws `NotFoundException` for a missing payment and for a non-owner non-privileged requester, and
  succeeds for a privileged requester regardless of owner; `getAllPayments` pagination shape
  (`PagedResponse` record accessors).
- **`MockPaymentProcessorTest`** (2 tests): a PENDING payment is completed with a `TXN-` transaction
  id and `PaymentCompletedEvent` is published; a missing payment id is a no-op (no save, no publish).
- **`PaymentControllerTest`** (`@WebMvcTest`, 8 tests): `POST /payments/process` is ADMIN-only (200
  for ADMIN, 403 for CUSTOMER); `GET /payments/me` is CUSTOMER-only (200 for CUSTOMER, 403 for
  ADMIN); `GET /payments/{id}` (no `@PreAuthorize`) works for any authenticated role and is rejected
  when unauthenticated; `GET /payments` is ADMIN-only (200 for ADMIN, 403 for CUSTOMER).
- **`PaymentFlowIT`** (`@SpringBootTest` + `@EmbeddedKafka` + `ehi_payment_test` DB, 3 tests):
  publishing `PolicyCreatedEvent` results in a COMPLETED `POLICY_PREMIUM` payment (awaiting the 2s
  async `MockPaymentProcessor`) with `payment.completed` emitted, keyed by `paymentId`, carrying the
  matching `referenceId`/`referenceType`; an approved `ClaimDecisionEvent` results in a COMPLETED
  `CLAIM_PAYOUT` payment for the correct user/amount; a `REJECTED` `ClaimDecisionEvent` creates no
  payment for that `claimId`.

### Implementation decisions / deviations
- **Fix #1 implemented first** (per check.md 🔴 finding): added
  `PaymentRepository.findFirstByReferenceIdAndReferenceTypeAndStatusNot(referenceId, referenceType, status)`
  and, at the top of `processPayment`, return the existing non-FAILED payment for that reference
  instead of inserting a duplicate — see Review Findings above.
- `@WebMvcTest` doesn't auto-scan `SecurityConfig`, so `PaymentControllerTest` uses an inline
  `@TestConfiguration @EnableMethodSecurity` + `@MockBean JwtProvider` (same pattern as
  IAM/policy/claim), with `.anyRequest().authenticated()` — payment has no public endpoints.
  `@WithMockUser(username = "<uuid>")` is required since `JwtAuthenticationFilter` sets the principal
  name to the userId.
- `PaymentFlowIT` uses `@EmbeddedKafka` (same as `ClaimFlowIT`) with Compose Postgres
  (`ehi_payment_test` DB, `ddl-auto: create-drop`). To verify the emitted `payment.completed` event,
  the test creates its own `Consumer<String, Object>` via `DefaultKafkaConsumerFactory`
  (key=`StringDeserializer`, value=`JsonDeserializer<Object>` with `trustedPackages("*")`) subscribed
  to `payment.completed`, polling/filtering by `paymentId` key. Payment-status assertions poll
  `PaymentRepository.findFirstByReferenceIdAndReferenceTypeAndStatusNot(...)` via Awaitility
  (`Optional` checked with `assertThat(...).isPresent()` rather than `orElseThrow()`, so Awaitility
  retries instead of failing fast on the first empty poll).
- `BigDecimal` amount assertions use `isEqualByComparingTo` (not `isEqualTo`) since Postgres returns
  `150.00` for a stored `150`.
- `ext['testcontainers.version'] = '1.20.6'`, `org.apache.httpcomponents.client5:httpclient5`, and
  `spring-kafka-test` carried forward from IAM/policy/claim (testcontainers/httpclient5 kept ready but
  not directly exercised — IT uses `@EmbeddedKafka` + repository assertions, no `TestRestTemplate`).
