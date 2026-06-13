# e-health-insurance-payment

## Status: DONE (tests complete; Epoint gateway integrated — provider-switchable)
## Port: 8084
## Database: ehi_payment

## What's Done
- [x] build.gradle (+ webflux, mockwebserver for Epoint)
- [x] Entity: Payment (+ Epoint fields), SavedCard
- [x] Repository: PaymentRepository, SavedCardRepository
- [x] Service: PaymentService, PaymentProcessor (MockPaymentProcessor | EpointPaymentProcessor), EpointPaymentService
- [x] Controller: PaymentController, EpointCallbackController
- [x] DTO: PaymentDto, CardRegistrationResponse, SavedCardDto
- [x] Epoint client: EpointClient, EpointSignature, request/response records
- [x] Kafka producer: payment.completed, payment.failed
- [x] Kafka consumer: policy.created, claim.decision (APPROVED)
- [x] GlobalExceptionHandler
- [x] application.yml (+ payment.provider flag, epoint.* config)
- [x] Dockerfile

## Payment Provider (mock | epoint)

`payment.provider` (env `PAYMENT_PROVIDER`, default `mock`) selects the `PaymentProcessor`
implementation via `@ConditionalOnProperty`:

- **`mock`** (default) — `MockPaymentProcessor`: 2s async delay then auto-COMPLETED. Unchanged
  legacy behaviour; runs with no Epoint keys.
- **`epoint`** — `EpointPaymentProcessor`: real Epoint.az gateway (see "Epoint Integration").

`EpointPaymentService` (callback handling, status refresh, payout-card registration) is always
present regardless of the flag, but its Epoint endpoints only do useful work when real keys are set.

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
| transactionId | String | nullable — mock: "TXN-" + UUID; Epoint: gateway transaction |
| failureReason | String | nullable — set if processing fails validation (truncated to 255) |
| epointTransaction | String | nullable — Epoint transaction id (key for get-status / reverse) |
| checkoutUrl | String | nullable (length 1024) — Epoint hosted payment page for POLICY_PREMIUM |
| bankTransaction | String | nullable — bank transaction from Epoint callback |
| rrn | String | nullable — Retrieval Reference Number (successful Epoint tx only) |
| cardMask | String | nullable — masked PAN from Epoint, format `123456******1234` |
| createdAt | Instant | `@PrePersist` |
| updatedAt | Instant | `@PrePersist`/`@PreUpdate` |

### SavedCard (`saved_cards`) — Epoint payout cards

| Field | Type | Notes |
|---|---|---|
| id | UUID | PK, generated |
| userId | UUID | not null, plain column, no FK |
| cardId | String | not null, **unique** — Epoint `card_id` from card-registration |
| cardMask | String | nullable — set on activation callback |
| cardName | String | nullable — cardholder name, set on activation callback |
| active | boolean | not null — `false` until Epoint registration callback confirms |
| createdAt | Instant | `@PrePersist` |
| updatedAt | Instant | `@PrePersist`/`@PreUpdate` |

## Endpoints
| Method | Path | Description | Auth |
|---|---|---|---|
| POST | /api/v1/payments/process | Manual trigger (demo) | ADMIN |
| GET | /api/v1/payments/me | My payments | CUSTOMER |
| GET | /api/v1/payments/{id} | Payment detail | authenticated (owner or ADMIN — 404 if non-owner & non-admin) |
| GET | /api/v1/payments?page=&size= | All payments, paginated | ADMIN |
| POST | /api/v1/payments/{id}/refresh-status | Re-query Epoint get-status & sync local payment | authenticated (owner or ADMIN — 404 pattern) |
| POST | /api/v1/payments/cards/register | Start Epoint payout-card registration → returns `redirectUrl` | CUSTOMER |
| GET | /api/v1/payments/cards/me | My saved payout cards | CUSTOMER |
| POST | /api/v1/payments/epoint/callback | Epoint server-to-server result (form `data`+`signature`) | **public** (signature-verified; gateway `public-paths`) |

## Kafka
- Produces: payment.completed, payment.failed
- Consumes: policy.created, claim.decision (only when `decision == APPROVED`)

## Epoint Integration

Integrates the Epoint.az gateway (API v1.0.3, see root `API Epoint en.pdf`). Active only when
`payment.provider=epoint`. Mock provider is the default and unaffected.

### Config (`epoint.*`)

| Key | Env | Default | Notes |
|---|---|---|---|
| base-url | EPOINT_BASE_URL | `https://epoint.az/api/1` | API root |
| public-key | EPOINT_PUBLIC_KEY | _(empty)_ | merchant id (`public_key`) |
| private-key | EPOINT_PRIVATE_KEY | _(empty)_ | secret signing key |
| language | EPOINT_LANGUAGE | `az` | page language |
| currency | _(fixed)_ | `AZN` | only supported value |
| success-redirect-url | EPOINT_SUCCESS_REDIRECT_URL | `http://localhost:5173/payments/success` | browser redirect |
| error-redirect-url | EPOINT_ERROR_REDIRECT_URL | `http://localhost:5173/payments/error` | browser redirect |
| timeout-seconds | EPOINT_TIMEOUT_SECONDS | `15` | WebClient call timeout |

`result_url` (server-to-server callback) is **not** a request param — it is configured once in the
Epoint merchant cabinet and must point at `…/api/v1/payments/epoint/callback` through the gateway.

### Signature
`EpointSignature.sign(privateKey, data)` = `base64(sha1(privateKey + data + privateKey))`, where
`data = base64(json)`. Verified against the official doc test vector in `EpointSignatureTest`.
Callbacks are authenticated by recomputing the signature over the received `data` (`verify`).

### Client (`EpointClient`, WebClient, `application/x-www-form-urlencoded`)

| Method | Endpoint | Used for |
|---|---|---|
| createPayment | `/request` | POLICY_PREMIUM checkout → returns `redirect_url` + `transaction` |
| getStatus | `/get-status` | refresh-status endpoint |
| registerPayoutCard | `/card-registration` (`refund=1`) | payout card setup → returns `card_id` + `redirect_url` |
| payout | `/refund-request` | CLAIM_PAYOUT disbursement to a saved card |
| reverse | `/reverse` | (available; not yet wired to an endpoint) |

### Flows

**POLICY_PREMIUM (purchase).** `policy.created` → `processPayment` saves PENDING →
`EpointPaymentProcessor.createCheckout` calls `/request`, stores `epointTransaction` + `checkoutUrl`,
payment **stays PENDING**. Customer is redirected to `checkoutUrl`, pays on Epoint, and Epoint POSTs
the result to the callback → `payment.completed` (or `payment.failed`). The HTTP purchase response is
PENDING; the frontend polls `/me` (or calls `/refresh-status`) for the final state.

**CLAIM_PAYOUT (disbursement).** Requires a registered payout card. `claim.decision(APPROVED)` →
`processPayment` saves PENDING → `EpointPaymentProcessor.payout` looks up the user's most recent
active card and calls `/refund-request`. Success → COMPLETED + `payment.completed` synchronously; no
card / gateway error → FAILED + `payment.failed`.

**Payout card registration.** `POST /cards/register` → `/card-registration` (`refund=1`) → a
`SavedCard` is persisted `active=false` and `redirect_url` is returned. Customer enters card details
on Epoint; the registration result hits the **same callback** with a `card_id` (no `order_id`) →
card is flipped `active=true` with mask/name.

**Callback dispatch.** `handleCallback` verifies the signature, base64+JSON-decodes `data`, then:
`order_id` present → payment result (COMPLETED / FAILED / REFUNDED by `status`); else `card_id`
present → card activation. `success` on an already-COMPLETED payment is an idempotent skip (mirrors
the existing duplicate-delivery guard). Unknown `order_id`/`card_id` is logged and ignored.

## Decisions & Notes

### Epoint integration (added later)

- **Provider abstraction.** Introduced `PaymentProcessor` interface; `MockPaymentProcessor` and
  `EpointPaymentProcessor` are selected by `@ConditionalOnProperty(payment.provider)` (`mock` default,
  `matchIfMissing=true`). `PaymentServiceImpl` now depends on the interface, not the concrete mock —
  its logic (idempotency guard, invalid-amount → FAILED) is unchanged.
- **No new event types.** Epoint reuses the existing `payment.completed` / `payment.failed` events, so
  downstream consumers (policy activation, claim payout bookkeeping) are untouched.
- **POLICY_PREMIUM stays async via callback, not a 2s sleep.** `createCheckout` leaves the payment
  PENDING and the Epoint server-to-server callback drives the terminal state. `refresh-status`
  (`/get-status`) is a manual fallback if the callback is missed.
- **Callback is the only public payment endpoint.** Added to both `SecurityConfig.permitAll` and the
  gateway `public-paths`. It is authenticated by Epoint signature verification, not JWT (Epoint sends
  no token). All other new endpoints keep the role-based `@PreAuthorize` pattern.
- **One callback endpoint, two payload shapes.** Epoint posts both payment results and card-registration
  results to `result_url`; dispatch is by presence of `order_id` (payment) vs `card_id` (card).
- **`order_id` == our `payment.getId()`.** We send the payment UUID as Epoint's `order_id`, so the
  callback maps straight back with no extra lookup table.
- **Liquibase migrations added; `ddl-auto: none`.** Three changelogs: `001-create-payments` (original
  table), `002-add-epoint-columns` (epointTransaction, checkoutUrl, bankTransaction, rrn, cardMask),
  `003-create-saved-cards`. `ddl-auto` is `none`; schema is fully managed by Liquibase.
- **`failureReason` truncated to 255 chars** before persist (Epoint messages can be long).
- **`reverse`/refund path is built in the client but not wired** to an endpoint — REFUNDED status is
  only reachable via a `returned` callback today. Left as a deliberate extension point.
- Added `spring-boot-starter-webflux` (WebClient) and test-scope `okhttp3:mockwebserver` — same as the
  ai service.

### Original (mock) notes

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

## Testing (DONE — 45 tests, all green)

Stack: JUnit 5 + Mockito + AssertJ (unit); `@SpringBootTest` + `@EmbeddedKafka` + running Compose
Postgres (IT). Original 22 mock-flow tests + 23 Epoint tests. Run in a `gradle:8.10.2-jdk17` Docker
container (host has no JDK 17); IT needs the `ehi_payment_test` DB on Compose Postgres.

### Epoint test classes (23 tests)

- **`EpointSignatureTest`** (4 tests): `sign` reproduces the **official Epoint doc test vector**
  (`bH9cG854p/wHLf5j6pp6LBI+wBs=`); `verify` accepts a valid signature and rejects tampered data /
  wrong key. This pins the signing scheme against the spec.
- **`EpointPaymentProcessorTest`** (7 tests): unknown payment id is a no-op; POLICY_PREMIUM stores
  `checkoutUrl`/`epointTransaction` and stays PENDING (no events); POLICY_PREMIUM → FAILED +
  `payment.failed` when Epoint returns an error status and when the client throws; CLAIM_PAYOUT →
  FAILED when no active card (no `/refund-request` call); CLAIM_PAYOUT → COMPLETED + `payment.completed`
  with bank/rrn/mask populated on payout success; → FAILED + event when payout is rejected.
- **`EpointPaymentServiceImplTest`** (12 tests): callback rejects an invalid signature (no repo
  access); `success` completes + publishes `payment.completed`; duplicate `success` on a COMPLETED
  payment is an idempotent skip; `failed` → FAILED + `payment.failed`; unknown `order_id` ignored;
  card-registration `success` activates the card (mask/name), `failed` leaves it inactive;
  `refreshStatus` 404s for non-owner, errors when no Epoint transaction, and applies a `success`
  get-status result; `startCardRegistration` saves an inactive card + returns redirect, and throws
  when Epoint rejects.

### Implemented test classes (original mock flow, 22 tests)

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
