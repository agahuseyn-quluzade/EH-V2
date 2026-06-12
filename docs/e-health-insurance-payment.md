# e-health-insurance-payment

## Status: DONE
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
- Build verification: `./gradlew build` → BUILD SUCCESSFUL. Service complete.
- Dockerfile (multi-stage, same pattern as claim): `infra-build` stage publishes `e-health-insurance-infra` (via Compose's `additional_contexts: infra`) to `/root/.m2`, `build` stage compiles `bootJar`, runtime stage `eclipse-temurin:17-jre-jammy`. `gradle.properties` removed before building. `.dockerignore` excludes `.gradle/`, `build/`, `out/`.
- `application-docker.yml` overrides `spring.datasource.url` → `postgres:5432/ehi_payment` and `spring.kafka.bootstrap-servers` → `kafka:29092`, activated via `SPRING_PROFILES_ACTIVE=docker`.
