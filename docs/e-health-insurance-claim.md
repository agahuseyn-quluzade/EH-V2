# e-health-insurance-claim

## Status: DONE
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
  - `claim.decision` (`ClaimDecisionEventProducer`, key=claimId) — published on `reviewClaim`.
- Consumes:
  - `fraud.detected` (`FraudDetectedEventConsumer`, group `claim-service`) — calls `claimService.applyFraudResult(event)`, which sets `riskScore`/`fraudFlags`/`aiExplanation` and transitions `SUBMITTED` → `UNDER_REVIEW`.

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
  - `applyFraudResult`: called by the `fraud.detected` Kafka consumer (Step 8) — sets `riskScore`/`fraudFlags`/`aiExplanation` from `FraudDetectedEvent`; if claim was still `SUBMITTED`, transitions it to `UNDER_REVIEW` so it's ready for agent review with fraud context attached.
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
