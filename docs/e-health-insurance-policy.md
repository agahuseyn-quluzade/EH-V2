# e-health-insurance-policy

## Status: DONE (tests complete)
## Port: 8082
## Database: ehi_policy

## What's Done
- [x] build.gradle
- [x] Entity: Plan, Policy
- [x] Repository: PlanRepository, PolicyRepository
- [x] Security: SecurityConfig, JwtProvider (validation-only), JwtAuthenticationFilter, JwtProperties
- [x] Service: PlanService, PolicyService (interfaces in `service/`, impls in `service/impl/`)
- [x] Controller: PlanController, PolicyController
- [x] DTO: CreatePlanRequest, PurchasePolicyRequest (`dto/request/`), PlanDto, PolicyDto (`dto/response/`) + PlanMapper, PolicyMapper
- [x] Kafka producer: PolicyCreatedEvent → policy.created
- [x] Kafka consumer: payment.completed → activate policy
- [x] GlobalExceptionHandler
- [x] application.yml
- [x] Dockerfile

## Entities
### Plan (`plans` table)
| Field | Type | Notes |
|---|---|---|
| id | UUID | PK, generated |
| name | String | unique, not null |
| description | String | not null |
| coverageAmount | BigDecimal | not null |
| premiumAmount | BigDecimal | not null |
| durationMonths | int | not null |
| active | boolean | not null |
| createdAt | Instant | set on persist |
| updatedAt | Instant | set on persist/update |

### Policy (`policies` table)
| Field | Type | Notes |
|---|---|---|
| id | UUID | PK, generated |
| policyNumber | String | unique, not null |
| userId | UUID | not null (no FK — user lives in iam's DB) |
| plan | Plan | @ManyToOne lazy, not null (`plan_id`) |
| status | PolicyStatus (infra enum) | not null, stored as STRING |
| premiumAmount | BigDecimal | not null, copied from plan at purchase |
| startDate | Instant | nullable, set on activation |
| endDate | Instant | nullable, set on activation (start + durationMonths) |
| createdAt | Instant | set on persist |
| updatedAt | Instant | set on persist/update |

## Endpoints
| Method | Path | Description | Auth |
|---|---|---|---|
| GET | /api/v1/plans | List active plans | Public |
| GET | /api/v1/plans/{id} | Plan details | Public |
| POST | /api/v1/plans | Create plan | ADMIN |
| POST | /api/v1/policies | Purchase policy | CUSTOMER |
| GET | /api/v1/policies/me | My policies | CUSTOMER |
| GET | /api/v1/policies/{id} | Policy detail | CUSTOMER, ADMIN |
| PUT | /api/v1/policies/{id}/cancel | Cancel policy | CUSTOMER, ADMIN |
| GET | /api/v1/policies | All policies | ADMIN |

## Kafka
- Produces: policy.created (`PolicyCreatedEventProducer`, `kafka/`, fires from `PolicyServiceImpl.purchasePolicy()` after save, key = policyId, value = `PolicyCreatedEvent`)
- Consumes: payment.completed (`PaymentCompletedEventConsumer`, `kafka/`, `groupId=policy-service`; ignores events where `referenceType != POLICY_PREMIUM`, else calls `PolicyService.activatePolicy(referenceId)`)

## Decisions & Notes
- Package root: `com.ehi.policy`.
- Same Gradle setup as iam: Gradle 8.10.2 wrapper (Spring Boot 3.2.5 plugin breaks on Gradle 9), `io.spring.dependency-management` 1.1.7, `gradle.properties` with `org.gradle.java.home=/opt/homebrew/opt/openjdk@17/libexec/openjdk.jdk/Contents/Home` (full `Contents/Home` path required for IntelliJ sync).
- Same dependency set as iam (web, data-jpa, security, validation, spring-kafka, jjwt, postgresql, lombok+mapstruct, infra via mavenLocal) — policy validates iam-issued JWTs, so it needs jjwt + the shared `jwt.secret` (no token generation, so no expiration properties).
- `application.yml` has both Kafka producer (policy.created) and consumer (payment.completed) config; consumer `group-id: policy-service`, `spring.json.trusted.packages: com.ehi.infra.event`.
- `Policy.userId` is a plain UUID column, not a JPA relation — users live in iam's database (database-per-service).
- `Policy.premiumAmount` is copied from the plan at purchase time so later plan price changes don't affect existing policies.
- `startDate`/`endDate` stay null while PENDING; set when the payment.completed consumer activates the policy (endDate = startDate + plan.durationMonths).
- Money fields are `BigDecimal`.
- `PlanRepository`: `findByActiveTrue()` (public plan listing), `existsByName` (duplicate check on create). `PolicyRepository`: `findByUserId` (my policies); paginated `findAll(Pageable)` comes inherited from `JpaRepository`.
- Security is validation-only: `JwtProvider` (`security/`) has no token generation — only `isTokenValid`/`getUserId`/`getRole` against the shared `jwt.secret` (same value as iam). `JwtProperties` (`config/`) binds just `jwt.secret`.
- `JwtAuthenticationFilter` sets the **userId (UUID string) as principal name** — unlike iam, which uses email — because policy identifies users by `Policy.userId`; controllers read it via `Authentication.getName()`.
- `SecurityConfig`: stateless, permits `GET /api/v1/plans/**` (public plan browsing), everything else authenticated; role checks via `@PreAuthorize` on controllers. No `PasswordEncoder` — no credentials handled here.
- DTOs: `CreatePlanRequest` (5 fields → `@Builder`), `PurchasePolicyRequest` (just `planId`, no builder) in `dto/request/`; `PlanDto` (7 fields), `PolicyDto` (9 fields, flattens plan to `planId`+`planName`) in `dto/response/`, both `@Builder`.
- `PlanMapper`: direct field-name mapping. `PolicyMapper`: `@Mapping(plan.id → planId, plan.name → planName)` — flattening avoids serializing the lazy `Plan` relation in responses.
- `PlanService`: getActivePlans (public list), getPlanById, createPlan (`DuplicateResourceException` on existing name, new plans start `active=true`).
- `PolicyService`: purchasePolicy (plan must exist and be active → `BadRequestException` otherwise; creates `PENDING` policy, copies premium, generates `policyNumber` as `POL-` + 8 hex chars), getMyPolicies, getPolicyById/cancelPolicy (ownership-checked: non-admin requesting someone else's policy gets `NotFoundException` — 404 instead of 403 so policy existence isn't leaked), getAllPolicies (paginated `PagedResponse`), activatePolicy (consumer entry point: `PENDING` → `ACTIVE`, sets startDate=now, endDate=start+plan.durationMonths; idempotent — non-PENDING policies are skipped).
- Cancel allowed only from `PENDING`/`ACTIVE`, else `BadRequestException`. All errors use infra generics — no `PolicyErrorEnum` needed so far.
- `getPolicyById`/`cancelPolicy` take `(policyId, requesterId, admin)` — controllers pass userId from `Authentication.getName()` and an isAdmin flag from authorities.
- `PlanController` (`/api/v1/plans`): GET list/by-id public (per `SecurityConfig`); `POST` requires `ROLE_ADMIN`.
- `PolicyController` (`/api/v1/policies`): all endpoints parse `Authentication.getName()` as a `UUID` (set by `JwtAuthenticationFilter` to the userId). `isAdmin(authentication)` checks for `ROLE_ADMIN` authority and is passed into `getPolicyById`/`cancelPolicy` for ownership bypass. `purchasePolicy`/`getMyPolicies` require `ROLE_CUSTOMER`; `getPolicyById`/`cancelPolicy` allow `CUSTOMER` or `ADMIN`; `getAllPolicies` requires `ROLE_ADMIN`.
- `PolicyCreatedEventProducer.publish()` is called from `purchasePolicy` right after save, using the saved policy's id/userId/policyNumber/premiumAmount and the plan's id.
- `PaymentCompletedEventConsumer` relies on `@KafkaListener` default JSON deserialization (type info from the producer's `__TypeId__` header + `spring.json.trusted.packages: com.ehi.infra.event` in `application.yml`) — no custom `ConsumerFactory`/`ContainerFactory` needed.
- `exception/PolicyErrorEnum implements BaseErrorService`: `FORBIDDEN` → `"POLICY-FORBIDDEN-0001"` / 403 — same pattern as iam's `IamErrorEnum`, used only by `GlobalExceptionHandler` to format Spring Security's `AccessDeniedException` (infra's `BaseErrorEnum` tops out at 401).
- `exception/GlobalExceptionHandler` (`@RestControllerAdvice`, `@Slf4j`): identical structure to iam's — handles `BaseException`, `MethodArgumentNotValidException` (400, field errors in `details`), `AccessDeniedException` (403, `PolicyErrorEnum.FORBIDDEN`), generic `Exception` (500). All responses `ApiResponse<ErrorResponse>` with `success=false`.
- Step 10 (Build & verify): `./gradlew build` → BUILD SUCCESSFUL. `application.yml` uses `ddl-auto: validate` and `show-sql: false`.
- Dockerfile (multi-stage, same pattern as iam): `infra-build` stage publishes `e-health-insurance-infra` (via Compose's `additional_contexts: infra`) to `/root/.m2`, `build` stage compiles `bootJar`, runtime stage `eclipse-temurin:17-jre-jammy`. `gradle.properties` removed before building. `.dockerignore` excludes `.gradle/`, `build/`, `out/`.
- `application-docker.yml` overrides `spring.datasource.url` → `postgres:5432/ehi_policy` and `spring.kafka.bootstrap-servers` → `kafka:29092`, activated via `SPRING_PROFILES_ACTIVE=docker`.
- Full-stack smoke test (via Docker Compose, gw → iam/policy/claim/payment/ai/notification): register → create plan → purchase policy → policy.created → payment processes POLICY_PREMIUM → payment.completed activates policy → submit claim → ai fraud check → approve claim → payment processes CLAIM_PAYOUT, all verified end-to-end.
- **Bug fix**: `PolicyServiceImpl.activatePolicy()` threw `LazyInitializationException` on `policy.getPlan().getDurationMonths()` when invoked from `PaymentCompletedEventConsumer` — the repository call's transaction/session closed before the lazy `Plan` proxy was accessed, so policies never activated after payment. Fixed by adding `@Transactional` to `activatePolicy()`.

## Logging (SLF4J) — DONE

> Add `@Slf4j` only to the classes below, only the listed lines. Producers/consumers and
> `GlobalExceptionHandler` already log; `activatePolicy` already logs. Follow the existing
> convention (parameterized `{}`, `info` for state changes, `warn` for rejected actions).

- **`PolicyServiceImpl`** (`@Slf4j`):
  - `purchasePolicy`: `info` after save — `"Policy purchased: policyId={}, userId={}, planId={}"`.
  - `cancelPolicy`: `info` — `"Policy cancelled: policyId={}, userId={}"`.
- **`PlanServiceImpl`** (`@Slf4j`) — admin action:
  - `createPlan`: `info` — `"Plan created: planId={}, name={}"`.

## Review Findings (see root `check.md` for full detail)

- 🟡 **Cancelling an ACTIVE policy issues no refund.** `cancelPolicy` flips status to CANCELLED; the
  already-charged premium is never refunded (no REFUND payment emitted). Decide if intended and
  document.
- 🟢 **No cap on duplicate purchases.** A customer can buy the same plan repeatedly (N PENDING/ACTIVE
  policies).
- 🟢 **`EXPIRED` is never set.** `PolicyStatus.EXPIRED` exists but nothing transitions a policy past
  its `endDate` — policies stay ACTIVE forever. Missing piece: a scheduled "expire past endDate" job.

## Testing (DONE — 22 tests, all green)

Stack: JUnit 5 + Mockito + AssertJ (unit); `@SpringBootTest` + `@EmbeddedKafka` + running Compose
Postgres (IT).

### Implemented test classes (all passing, 22 tests total)

- **`PlanServiceImplTest`** (4 tests): `createPlan` duplicate-name → `DuplicateResourceException`,
  new plans start `active=true`; `getActivePlans` returns only active; `getPlanById` 404 on missing.
- **`PolicyServiceImplTest`** (9 tests): `purchasePolicy` rejects a missing plan (404) and an
  inactive plan (`BadRequestException`), creates a `PENDING` policy with a `POL-` number, copies the
  premium, and publishes `PolicyCreatedEvent`; `getPolicyById` ownership → 404 for a non-owner
  non-admin, admin bypasses ownership; `cancelPolicy` allowed only from PENDING/ACTIVE
  (`BadRequestException` otherwise); **`activatePolicy` idempotency** — a non-PENDING policy is left
  unchanged, a PENDING one becomes ACTIVE with `endDate = start + durationMonths`.
- **`PolicyControllerTest`** (`@WebMvcTest`, 7 tests): public `GET /plans`, ADMIN-only
  `POST /plans`, CUSTOMER-only `purchasePolicy`, ADMIN-only `getAllPolicies`.
- **`PolicyActivationIT`** (`@SpringBootTest` + `@EmbeddedKafka` + `ehi_policy_test` DB, 2 tests):
  publish a `PaymentCompletedEvent` with `referenceType=POLICY_PREMIUM` → assert the policy
  activates (`ACTIVE`, `startDate`/`endDate` set correctly); a `CLAIM_PAYOUT` event is ignored
  (policy stays `PENDING`). Guards against the `LazyInitializationException` regression fixed by
  `@Transactional` on `activatePolicy`.

### Implementation decisions / deviations
- `@WebMvcTest` doesn't auto-scan `SecurityConfig`, so `PolicyControllerTest` uses an inline
  `@TestConfiguration @EnableMethodSecurity` + `@MockBean JwtProvider` (same pattern as IAM), with
  `/api/v1/plans/**` permitted and everything else authenticated. `@WithMockUser(username = "<uuid>")`
  is required for policy-owning endpoints since `JwtAuthenticationFilter` sets the principal name to
  the userId (not an email, unlike IAM).
- `PolicyActivationIT` uses `@EmbeddedKafka` (in-memory broker, `spring-kafka-test`) instead of the
  Compose Kafka broker — avoids consumer-group/offset coordination with other services and the
  Testcontainers Docker 29.x incompatibility. `application-it.yml` sets
  `kafka.bootstrap-servers: ${spring.embedded.kafka.brokers}`. Postgres still uses the running
  Compose container (`ehi_policy_test` DB, `ddl-auto: create-drop`), same as IAM.
- `ext['testcontainers.version'] = '1.20.6'` and `org.apache.httpcomponents.client5:httpclient5`
  carried forward from IAM (kept ready, not directly exercised since IT uses HTTP-free
  repository/Kafka assertions rather than `TestRestTemplate`).
