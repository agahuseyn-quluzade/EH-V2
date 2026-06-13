# e-health-insurance-infra

## Status: DONE
## Type: Shared Library (no Spring Boot plugin, plain jar)

## What's Done
- [x] build.gradle
- [x] Enums (8 files)
- [x] KafkaTopics constants
- [x] Event DTOs (8 files)
- [x] Response DTOs (ApiResponse, PagedResponse, ErrorResponse)
- [x] Exceptions (BaseErrorService, BaseErrorEnum, BaseException + 4 subclasses, ServiceException)
- [x] Build & publishToMavenLocal verified
- [x] DateUtil

## Enums
| Enum | Values |
|---|---|
| UserRole | ADMIN, STAFF, CUSTOMER | <!-- was AGENT in v3-3; renamed back to STAFF -->
| PolicyStatus | PENDING, ACTIVE, EXPIRED, CANCELLED |
| ClaimStatus | SUBMITTED, UNDER_REVIEW, APPROVED, REJECTED |
| ClaimType | HOSPITALIZATION, MEDICATION, DENTAL, CONSULTATION |
| PaymentStatus | PENDING, COMPLETED, FAILED, REFUNDED |
| PaymentReferenceType | POLICY_PREMIUM, CLAIM_PAYOUT |
| NotificationType | WELCOME, POLICY_ACTIVATED, POLICY_PENDING, CLAIM_SUBMITTED, CLAIM_APPROVED, CLAIM_REJECTED, PAYMENT_SUCCESS, PAYMENT_FAILED, FRAUD_ALERT |
| NotificationChannel | EMAIL, SMS |

## Kafka Topics
| Constant | Value |
|---|---|
| USER_REGISTERED | user.registered |
| POLICY_CREATED | policy.created |
| PAYMENT_COMPLETED | payment.completed |
| PAYMENT_FAILED | payment.failed |
| CLAIM_SUBMITTED | claim.submitted |
| CLAIM_DECISION | claim.decision |
| FRAUD_DETECTED | fraud.detected |
| NOTIFICATION_SEND | notification.send |

## Event DTOs
| Class | Key Fields | Used By Topic |
|---|---|---|
| UserRegisteredEvent | userId, email, firstName, lastName | user.registered |
| PolicyCreatedEvent | policyId, userId, planId, policyNumber, premiumAmount | policy.created |
| PaymentCompletedEvent | paymentId, userId, referenceId, referenceType, amount, transactionId | payment.completed |
| PaymentFailedEvent | paymentId, userId, referenceId, referenceType, amount, reason | payment.failed |
| ClaimSubmittedEvent | claimId, userId, policyId, claimNumber, claimType, amount | claim.submitted |
| ClaimDecisionEvent | claimId, userId, policyId, decision, approvedAmount, rejectionReason, reviewedBy | claim.decision |
| FraudDetectedEvent | claimId, userId, riskScore, flags, aiExplanation | fraud.detected |
| NotificationEvent | userId, channel, type, recipient, subject, body | notification.send |

## Planned: phone on UserRegisteredEvent (for notification SMS — Option A)

> Required by the notification SMS/email feature (`docs/e-health-insurance-notification.md` →
> "IMPLEMENTATION PLAN"). Do this **first** and `./gradlew publishToMavenLocal`, because IAM and
> notification will not compile against the old 4-field record.

Add a nullable `phone` field to `UserRegisteredEvent`, making it the **5th** field:

```java
public record UserRegisteredEvent(
        UUID userId,
        String email,
        String firstName,
        String lastName,
        String phone        // nullable — null when the user gave no phone
) {
}
```

- Per CLAUDE.md ("records with 5+ fields get Lombok `@Builder`"), add `@Builder` to the record now
  that it has 5 fields, and update construction sites to use the builder (or keep positional — the
  builder is preferred to avoid positional-arg mistakes). Construction sites that must be updated to
  pass `phone` (or switch to the builder):
  - `e-health-insurance-iam` → `service/impl/AuthServiceImpl.java` (the `register` publish call).
  - `e-health-insurance-notification` (`_V1`) tests → `NotificationFlowIT` and
    `UserRegisteredEventConsumerTest`.
- `phone` is optional everywhere; consumers must treat `null` as "no phone → use email channel".
- After editing, run `./gradlew publishToMavenLocal` so IAM and notification resolve the new jar from
  `mavenLocal()`.

## Response DTOs
| Class | Purpose |
|---|---|
| ApiResponse\<T\> | Generic wrapper: success, data, error, timestamp. Static: `ok(data)`, `error(msg)` |
| PagedResponse\<T\> | Paginated list: content, page, size, totalElements, totalPages, last |
| ErrorResponse | Error details: status, message, details (Map), timestamp |

## Exceptions

Package layout: `com.ehi.infra.exception.base` holds the base types; `com.ehi.infra.exception` holds the ready-made subclasses.

| Class | Package | Error Code | Status Code | Constructor |
|---|---|---|---|---|
| BaseErrorService | `exception.base` | (interface) | — | getErrorCode(), getMessage(), getHttpStatus() |
| BaseErrorEnum | `exception.base` | (enum) | — | NOT_FOUND, UNAUTHORIZED, BAD_REQUEST, DUPLICATE_RESOURCE, VALIDATION_ERROR, INTERNAL_ERROR |
| BaseException | `exception.base` | (abstract) | from errorService | (errorService, message) / (errorService) |
| BadRequestException | `exception.base` | BASE-BAD-REQUEST-0003 | 400 | (message) |
| NotFoundException | `exception` | BASE-NOT-FOUND-0001 | 404 | (entity, id) |
| UnauthorizedException | `exception` | BASE-UNAUTHORIZED-0002 | 401 | (message) |
| DuplicateResourceException | `exception` | BASE-DUPLICATE-RESOURCE-0004 | 409 | (message) |
| ServiceException | `exception` | from errorService | from errorService | (errorService) / (errorService, message) — for service-defined `XxxErrorEnum implements BaseErrorService` |

Services import `BaseErrorEnum`/`BaseException`/`BaseErrorService`/`BadRequestException` from `com.ehi.infra.exception.base.*`; `NotFoundException`, `UnauthorizedException`, `DuplicateResourceException`, `ServiceException` from `com.ehi.infra.exception.*`.

## Utilities
| Class | Method | Purpose |
|---|---|---|
| DateUtil | `now()` | Current `Instant` |
| DateUtil | `isExpired(Instant)` | True if given instant is in the past |
| DateUtil | `plusDays(Instant, long)` | Add days to an `Instant` |
| DateUtil | `plusYears(Instant, long)` | Add years to an `Instant` (via UTC zone conversion) |

## Decisions & Notes
- No Spring dependency in infra — keeps it lightweight.
- Jackson annotations for JSON serialization, jakarta.validation for annotations.
- All timestamps use `Instant`, never `LocalDateTime`.
- All IDs are `UUID`.
- Standalone repo: own `settings.gradle`, `build.gradle`, `gradle/libs.versions.toml` (no monorepo root).
- `java-library` + `maven-publish` plugins. Coordinates: `com.ehi:e-health-insurance-infra:0.0.1-SNAPSHOT`.
- Published via `./gradlew publishToMavenLocal`; consumed by services from `mavenLocal()`.
- Event DTOs and response DTOs are Java records (immutable) — Jackson 2.15.4 supports records natively.
- Records with 5+ fields (or several same-typed fields, e.g. multiple UUIDs) get Lombok `@Builder`: PolicyCreatedEvent, PaymentCompletedEvent, PaymentFailedEvent, ClaimSubmittedEvent, ClaimDecisionEvent, FraudDetectedEvent, NotificationEvent. UserRegisteredEvent (4 fields) has none.
- Error-code-driven exception model (adapted from teacher-provided example): `BaseErrorService` interface + `BaseErrorEnum` (generic codes) replace the old `(message, statusCode)` constructor on `BaseException`. Per-service errors implement `BaseErrorEnum`'s sibling interface as `XxxErrorEnum implements BaseErrorService` in the service's own `exception` package and are thrown via `ServiceException` — no per-domain exception subclasses, no `ProblemDetail`. `ApiResponse`/`ErrorResponse`/`PagedResponse` from Step 6 remain the response envelope.
- Build requires a Java 21 toolchain. On macOS with only a newer JDK installed, install via `brew install openjdk@21` and point Gradle at it with `org.gradle.java.installations.paths=/opt/homebrew/opt/openjdk@21` in `~/.gradle/gradle.properties` (machine-level, not part of any repo).
- `./gradlew build` and `./gradlew publishToMavenLocal` both verified successful; jar published to `~/.m2/repository/com/ehi/e-health-insurance-infra/0.0.1-SNAPSHOT/`.
