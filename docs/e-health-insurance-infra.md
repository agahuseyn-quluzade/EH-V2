# e-health-insurance-infra

> Shared library used by every backend service. Holds the cross-service contract: enums, Kafka
> topic names, event DTOs, the common API response envelope, and base exceptions.

| | |
|---|---|
| Type | Plain Java library (no Spring Boot plugin) — built as a jar |
| Distribution | Published to **mavenLocal** (`com.ehi:e-health-insurance-infra:0.0.1-SNAPSHOT`) |
| Consumed by | iam, policy, claim, payment, ai, notification (not the gateway) |

## Why it exists
Every service speaks the same language: the same enum values, the same Kafka topic strings, the same
event shapes, and the same response wrapper. Centralizing them here guarantees a producer and a
consumer agree on the contract, and avoids duplicated/clashing definitions across repos.

> After any change here, run `./gradlew publishToMavenLocal`, then rebuild the dependent services.

## Enums (`com.ehi.infra.enums`)
| Enum | Values |
|---|---|
| `UserRole` | `ADMIN`, `STAFF`, `CUSTOMER` |
| `PolicyStatus` | `PENDING`, `ACTIVE`, `EXPIRED`, `CANCELLED` |
| `ClaimStatus` | `SUBMITTED`, `UNDER_REVIEW`, `APPROVED`, `REJECTED` |
| `ClaimType` | `HOSPITALIZATION`, `MEDICATION`, `DENTAL`, `CONSULTATION` |
| `PaymentStatus` | `PENDING`, `COMPLETED`, `FAILED`, `REFUNDED` |
| `PaymentReferenceType` | `POLICY_PREMIUM`, `CLAIM_PAYOUT` |
| `NotificationType` | `WELCOME`, `POLICY_ACTIVATED`, `POLICY_PENDING`, `CLAIM_SUBMITTED`, `CLAIM_APPROVED`, `CLAIM_REJECTED`, `PAYMENT_SUCCESS`, `PAYMENT_FAILED`, `FRAUD_ALERT` |
| `NotificationChannel` | `EMAIL`, `SMS` |

## Kafka topics (`com.ehi.infra.config.KafkaTopics`)
Constants — services never hardcode topic strings.

| Constant | Topic |
|---|---|
| `USER_REGISTERED` | `user.registered` |
| `POLICY_CREATED` | `policy.created` |
| `PAYMENT_COMPLETED` | `payment.completed` |
| `PAYMENT_FAILED` | `payment.failed` |
| `CLAIM_SUBMITTED` | `claim.submitted` |
| `CLAIM_DECISION` | `claim.decision` |
| `FRAUD_DETECTED` | `fraud.detected` |

## Event DTOs (`com.ehi.infra.event`) — Java records
| Event | Key fields |
|---|---|
| `UserRegisteredEvent` | userId, email, firstName, lastName, **phone** (nullable) |
| `PolicyCreatedEvent` | policyId, userId, planId, policyNumber, premiumAmount |
| `PaymentCompletedEvent` | paymentId, userId, referenceId, referenceType, amount, transactionId |
| `PaymentFailedEvent` | paymentId, userId, referenceId, referenceType, amount, reason |
| `ClaimSubmittedEvent` | claimId, userId, policyId, claimNumber, claimType, amount |
| `ClaimDecisionEvent` | claimId, userId, policyId, decision, approvedAmount, rejectionReason, reviewedBy |
| `FraudDetectedEvent` | claimId, userId, riskScore, flags, aiExplanation |

All events serialize/deserialize as JSON (`JsonSerializer`/`JsonDeserializer`); consumers trust the
`com.ehi.infra.event` package.

## Response DTOs (`com.ehi.infra.dto`)
- **`ApiResponse<T>`** — `{ success, data, error, timestamp }`. Every endpoint wraps its result here
  via `ApiResponse.ok(data)` / `ApiResponse.error(message)`.
- **`PagedResponse<T>`** — `{ content, page, size, totalElements, totalPages, last }` for paginated lists.
- **`ErrorResponse`** — `{ status, message, details, timestamp }`; `details.errorCode` carries the
  domain error code.

## Exceptions (`com.ehi.infra.exception`)
- `BaseErrorService` (interface: errorCode, message, httpStatus), `BaseErrorEnum` (generic codes:
  NOT_FOUND, UNAUTHORIZED, BAD_REQUEST, DUPLICATE_RESOURCE, VALIDATION_ERROR, INTERNAL_ERROR).
- `BaseException` (abstract) + ready-made subclasses: `NotFoundException`, `UnauthorizedException`,
  `BadRequestException`, `DuplicateResourceException`.
- `ServiceException` — throw with a domain `XxxErrorEnum implements BaseErrorService` for
  service-specific errors (instead of writing per-domain exception subclasses).

## Notes
- No Spring beans — this is a plain jar; services import the classes and wire their own beans.
- Adding a field to an event (e.g. `phone` on `UserRegisteredEvent`) is the one place the contract
  changes ripple from: publish to mavenLocal, then rebuild producers and consumers.
