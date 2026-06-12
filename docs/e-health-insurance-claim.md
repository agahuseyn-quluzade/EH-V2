# e-health-insurance-claim

## Status: DONE (tests complete)
## Port: 8083
## Database: ehi_claim

## What's Done
- [x] build.gradle
- [x] Entity: Claim, ClaimEvidence
- [x] Repository: ClaimRepository, ClaimEvidenceRepository
- [x] Service: ClaimService
- [x] Controller: ClaimController
- [x] DTO: SubmitClaimRequest, ReviewClaimRequest, ClaimDto
- [x] Kafka producer: claim.submitted, claim.decision
- [x] Kafka consumer: fraud.detected
- [x] File upload (multipart) for evidence
- [x] GlobalExceptionHandler
- [x] application.yml
- [x] Dockerfile

## Entities

### Claim (`claims`)
| Field | Type | Notes |
|---|---|---|
| id | UUID | PK, generated |
| claimNumber | String | unique, not null |
| userId | UUID | plain column, no FK |
| policyId | UUID | plain column, no FK (cross-service) |
| claimType | ClaimType (enum) | infra enum, STRING |
| amount | BigDecimal | not null |
| description | String | not null |
| status | ClaimStatus (enum) | infra enum, STRING, not null |
| approvedAmount | BigDecimal | nullable |
| rejectionReason | String | nullable |
| reviewedBy | UUID | nullable |
| riskScore | Integer | nullable |
| fraudFlags | List\<String\> | nullable, `@ElementCollection` |
| aiExplanation | String | nullable |
| createdAt / updatedAt | Instant | `@PrePersist`/`@PreUpdate` |

### ClaimEvidence (`claim_evidence`)
| Field | Type | Notes |
|---|---|---|
| id | UUID | PK, generated |
| claim | Claim | `@ManyToOne(LAZY)`, `@JoinColumn(claim_id)`, not null |
| fileName | String | not null |
| filePath | String | not null, path under `app.upload-dir` |
| contentType | String | not null |
| uploadedAt | Instant | `@PrePersist` |

## Endpoints
| Method | Path | Description | Auth |
|---|---|---|---|
| POST | /api/v1/claims | Submit claim | CUSTOMER |
| GET | /api/v1/claims/me | My claims | CUSTOMER |
| GET | /api/v1/claims/{id} | Claim detail | authenticated (owner, AGENT, or ADMIN — 404 if non-owner & non-staff) |
| POST | /api/v1/claims/{id}/evidence | Upload evidence (multipart `file`) | authenticated (owner, AGENT, or ADMIN — 404 if non-owner & non-staff) |
| GET | /api/v1/claims?status=&page=&size= | All claims, optional status filter, paginated | AGENT, ADMIN |
| PUT | /api/v1/claims/{id}/review | Approve/reject claim | AGENT, ADMIN |

## Kafka
- Produces:
  - `claim.submitted` (`ClaimSubmittedEventProducer`, key=claimId) — published on `submitClaim`.
  - `claim.decision` (`ClaimDecisionEventProducer`, key=claimId) — published on `reviewClaim` AND on AI auto-decisions (see `applyFraudResult` below).
- Consumes:
  - `fraud.detected` (`FraudDetectedEventConsumer`, group `claim-service`) — calls `claimService.applyFraudResult(event)`, which applies the AI **auto-decision** (see Decisions & Notes).

## Decisions & Notes
- Package root: `com.ehi.claim`.
- Same Gradle setup as iam/policy: Gradle 8.10.2 wrapper, `io.spring.dependency-management` 1.1.7, `gradle.properties` with full JDK 17 `Contents/Home` path.
- Same dependency set as policy (web, data-jpa, security, validation, spring-kafka, jjwt, postgresql, lombok+mapstruct, infra via mavenLocal) — JWT validation-only (shared secret with iam).
- `application.yml` has Kafka producer (claim.submitted, claim.decision) and consumer (fraud.detected) config, `group-id: claim-service`, `spring.json.trusted.packages: com.ehi.infra.event`.
- Multipart upload config: `spring.servlet.multipart.max-file-size`/`max-request-size` = 10MB. Evidence files stored on local disk under `app.upload-dir` (`./uploads/claims`, overridable via `UPLOAD_DIR` env var).
- `Claim.status` defaults are set in the service layer (not via `@Builder.Default`), matching policy's pattern of setting status at creation time in `ClaimServiceImpl`.
- `fraudFlags` stored via `@ElementCollection` (separate collection table, default naming `claim_fraud_flags`) — simplest mapping for a `List<String>`, avoids JSON serialization complexity for MVP.
- `riskScore`, `aiExplanation`, `reviewedBy`, `approvedAmount`, `rejectionReason` all nullable — populated later by review/fraud-detection flows (Steps 7+).
- `ClaimRepository`: `findByUserId(UUID)` for "my claims", `findByStatus(ClaimStatus, Pageable)` for filterable admin/agent listing.
- `ClaimEvidenceRepository`: `findByClaim_Id(UUID)` to fetch all evidence files for a claim.
- Security setup mirrors policy exactly: `JwtProperties` (jwt.secret only), `JwtProvider` (validation-only — `isTokenValid`/`getUserId`/`getRole`), `JwtAuthenticationFilter` sets userId (UUID string) as the Authentication principal name. Shared JWT secret with iam.
- `SecurityConfig`: stateless, `@EnableMethodSecurity`, **all** endpoints require authentication (`.anyRequest().authenticated()`) — claim service has no public endpoints (unlike policy's public `/api/v1/plans/**` GETs). Role-based access (`@PreAuthorize`) handled per-endpoint in the controller.
- DTOs:
  - `SubmitClaimRequest` (4 fields, no `@Builder` per convention): `policyId`, `claimType`, `amount` (`@Positive`), `description` (`@NotBlank`).
  - `ReviewClaimRequest` (3 fields, no `@Builder`): `decision` (`ClaimStatus`, expected APPROVED/REJECTED — validated in service layer), `approvedAmount`, `rejectionReason`.
  - `ClaimDto` (15 fields, `@Builder`): full claim view including fraud/AI fields (`riskScore`, `fraudFlags`, `aiExplanation`) and review fields (`reviewedBy`, `approvedAmount`, `rejectionReason`).
  - `ClaimEvidenceDto` (6 fields, `@Builder`): flattens `claim.id` → `claimId`.
- Mappers: `ClaimMapper` (direct `Claim` → `ClaimDto`), `ClaimEvidenceMapper` (`@Mapping(target="claimId", source="claim.id")`).
- `ClaimService`/`ClaimServiceImpl`:
  - `submitClaim`: generates `claimNumber` (`"CLM-" + UUID.randomUUID().substring(0,8).toUpperCase()`), sets `status=SUBMITTED`, saves, publishes `ClaimSubmittedEvent`.
  - `getMyClaims`: `findByUserId` + map.
  - `getClaimById`/`uploadEvidence`: both go through private `findAccessibleClaim(claimId, requesterId, privileged)` — non-privileged requester accessing another user's claim gets `NotFoundException` (404, not 403), same ownership pattern as policy. `privileged` = AGENT or ADMIN.
  - `uploadEvidence`: stores file on disk under `app.upload-dir/{claimId}/{UUID}_{originalFilename}` via `Files.createDirectories` + `MultipartFile.transferTo`; IO errors wrapped in `UncheckedIOException`.
  - `getAllClaims`: paginated, optional `ClaimStatus` filter — `findByStatus` if provided, else `findAll`.
  - `reviewClaim`: only allowed when claim status is `SUBMITTED` or `UNDER_REVIEW` (else `BadRequestException`); `decision` must be `APPROVED` or `REJECTED`; `APPROVED` requires `approvedAmount`, `REJECTED` requires `rejectionReason`; sets `reviewedBy`, saves, publishes `ClaimDecisionEvent`.
  - `applyFraudResult`: called by the `fraud.detected` Kafka consumer — **AI auto-decision**. Guards `if (status != SUBMITTED) return;` so a manual review is never overwritten. Sets `riskScore`/`fraudFlags`/`aiExplanation` from `FraudDetectedEvent`, then decides by score: `<40` → `APPROVED` (`approvedAmount` = full claim amount); `40–69` → `UNDER_REVIEW` (goes to the agent queue); `≥70` → `REJECTED` (rejection reason notes the score). For the auto APPROVED/REJECTED branches it also publishes a `ClaimDecisionEvent` (with `reviewedBy = null`) so payment/notification react exactly as for a manual decision. Agents can still override `UNDER_REVIEW` claims via `reviewClaim`.
- Kafka producers created ahead of schedule (needed by service layer): `ClaimSubmittedEventProducer` (topic `claim.submitted`, key=claimId), `ClaimDecisionEventProducer` (topic `claim.decision`, key=claimId) — both follow `PolicyCreatedEventProducer`'s structure exactly. The `fraud.detected` consumer is still pending for Step 8.
- `ClaimController` (`/api/v1/claims`):
  - `getClaimById`/`uploadEvidence` have no `@PreAuthorize` — any authenticated user can call them, but the service-layer ownership check (`findAccessibleClaim`) returns 404 for non-owners who aren't AGENT/ADMIN.
  - `uploadEvidence` accepts `multipart/form-data` with a `file` part (`@RequestParam("file") MultipartFile`).
  - `getAllClaims` takes optional `status` query param (`ClaimStatus`) plus `Pageable` (page/size/sort via Spring's standard binding).
  - Private `isStaff(Authentication)` checks for `ROLE_AGENT` or `ROLE_ADMIN`, passed as the `privileged` flag to the service.
- `GlobalExceptionHandler` + `ClaimErrorEnum`: identical structure/content to policy's — handles `BaseException`, `MethodArgumentNotValidException`, `AccessDeniedException` (→ `ClaimErrorEnum.FORBIDDEN`, `"CLAIM-FORBIDDEN-0001"`, 403), and generic `Exception` → 500.
- Step 10 build verification: `./gradlew build` → BUILD SUCCESSFUL. No local Postgres/Kafka available, so no `bootRun` smoke test (same as policy).
- Dockerfile (multi-stage, same pattern as iam/policy): `infra-build` stage publishes `e-health-insurance-infra` (via Compose's `additional_contexts: infra`) to `/root/.m2`, `build` stage compiles `bootJar`, runtime stage `eclipse-temurin:17-jre-jammy`. `gradle.properties` removed before building. `.dockerignore` excludes `.gradle/`, `build/`, `out/`, `uploads/`.
- `application-docker.yml` overrides `spring.datasource.url` → `postgres:5432/ehi_claim` and `spring.kafka.bootstrap-servers` → `kafka:29092`, activated via `SPRING_PROFILES_ACTIVE=docker`.

## Review Findings (see root `check.md` for full detail)

- ✅ **Fixed** — `reviewClaim` lets an agent approve more than was claimed. `reviewClaim` now
  validates `0 < approvedAmount <= claim.getAmount()` for `APPROVED` decisions, throwing
  `BadRequestException` otherwise. Covered by `ClaimServiceImplTest#reviewClaim_throwsBadRequest_whenApprovedAmountExceedsClaimAmount`
  and `#reviewClaim_throwsBadRequest_whenApprovedAmountNotPositive`.
- 🟠 **No policy/coverage validation on submit.** `submitClaim` stores whatever `policyId` the
  customer sends — it never verifies the policy exists, belongs to the user, is ACTIVE, or that the
  amount is within plan coverage. Combined with the AI auto-approve path (score `<40`), a claim
  against a cancelled/foreign/nonexistent policy can pay out the full amount. Cheapest fix that fits
  database-per-service: have claim consume `policy.created`/`payment.completed` into a local
  read-model `{policyId → userId, status, coverageAmount}` and validate at submit; at minimum cap
  auto-approved payouts to plan coverage.
- 🟡 **Evidence upload trusts client content-type and filename** with no MIME/extension allow-list or
  magic-byte check (path traversal is blunted by the UUID prefix + per-claim dir). Add an allow-list
  (pdf/jpg/png) and validate.
- 🟢 `MaxUploadSizeExceededException` (>10MB) isn't handled by `GlobalExceptionHandler` → generic 500
  instead of 413. Add a handler.
- 🟢 `applyFraudResult` save + `claim.decision` publish isn't transactional (see cross-cutting #9 in
  CLAUDE.md).

## Testing (DONE — 27 tests, all green)

Stack: JUnit 5 + Mockito + AssertJ (unit); `@SpringBootTest` + `@EmbeddedKafka` + running Compose
Postgres (IT).

### Implemented test classes (all passing, 27 tests total)

- **`ClaimServiceImplTest`** (14 tests): `submitClaim` sets `SUBMITTED`, generates a `CLM-` number,
  publishes `ClaimSubmittedEvent`; `getClaimById`/`uploadEvidence` return 404 for a non-owner
  non-privileged requester, and succeed for a privileged (AGENT/ADMIN) requester regardless of
  owner; `reviewClaim` rejects non-`SUBMITTED`/`UNDER_REVIEW` status, requires `approvedAmount` on
  APPROVE and `rejectionReason` on REJECT, rejects `approvedAmount` that is `<= 0` or
  `> claim.getAmount()` (fix #2), and on success sets `reviewedBy`/`approvedAmount` and publishes
  `ClaimDecisionEvent`; **`applyFraudResult` decision matrix** — score `<40` → APPROVED +
  `approvedAmount` = full + `ClaimDecisionEvent`; `40–69` → UNDER_REVIEW + **no** decision event;
  `≥70` → REJECTED + event; and the **status guard** — a non-`SUBMITTED` claim is left untouched
  (no overwrite of a manual review, no save, no event).
- **`ClaimControllerTest`** (`@WebMvcTest`, 9 tests): CUSTOMER can `POST /claims` and
  `GET /claims/me`; AGENT gets 403 on `POST /claims` (CUSTOMER-only); AGENT can `GET /claims` and
  `PUT /{id}/review`; CUSTOMER gets 403 on both `GET /claims` and `PUT /{id}/review`;
  `GET /{id}` (no `@PreAuthorize`) works for any authenticated role and is rejected when
  unauthenticated.
- **`ClaimFlowIT`** (`@SpringBootTest` + `@EmbeddedKafka` + `ehi_claim_test` DB, 4 tests):
  `submitClaim` → assert `claim.submitted` is published with the correct `claimId`/`userId`/`amount`;
  publish `FraudDetectedEvent` with a low score → claim becomes `APPROVED` with `approvedAmount` =
  full amount + `claim.decision` (`reviewedBy=null`) is published; high score → `REJECTED` with a
  non-blank `rejectionReason` + `claim.decision`; mid score → `UNDER_REVIEW` and **no**
  `claim.decision` is ever published for that claim.

### Implementation decisions / deviations
- **Fix #2 implemented first** (per check.md 🟠 finding): `reviewClaim` now validates
  `0 < approvedAmount <= claim.getAmount()` for `APPROVED` decisions, throwing
  `BadRequestException` otherwise — see Review Findings below.
- `@WebMvcTest` doesn't auto-scan `SecurityConfig`, so `ClaimControllerTest` uses an inline
  `@TestConfiguration @EnableMethodSecurity` + `@MockBean JwtProvider` (same pattern as
  IAM/policy), with `.anyRequest().authenticated()` — claim has no public endpoints, unlike
  policy's `/api/v1/plans/**`. `@WithMockUser(username = "<uuid>")` is required since
  `JwtAuthenticationFilter` sets the principal name to the userId.
- `ClaimFlowIT` uses `@EmbeddedKafka` (same as `PolicyActivationIT`) with Compose Postgres
  (`ehi_claim_test` DB, `ddl-auto: create-drop`). To verify emitted Kafka events, the test creates
  its own `Consumer<String, Object>` via `DefaultKafkaConsumerFactory` (key=`StringDeserializer`,
  value=`JsonDeserializer<Object>` with `trustedPackages("*")`) subscribed to `claim.submitted` and
  `claim.decision`, and polls/filters by `claimId` key (records from earlier tests in the same
  embedded broker are tolerated since each test uses a fresh random `claimId`).
- `ext['testcontainers.version'] = '1.20.6'`, `org.apache.httpcomponents.client5:httpclient5`, and
  `spring-kafka-test` carried forward from IAM/policy (testcontainers/httpclient5 kept ready but not
  directly exercised — IT uses `@EmbeddedKafka` + repository assertions, no `TestRestTemplate`).
