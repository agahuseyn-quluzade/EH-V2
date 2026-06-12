# CLAUDE.md

## Project

E-Health Insurance — microservices backend (Java 17, Spring Boot 3.2.5, Gradle, Kafka, PostgreSQL).

## Scope & Direction

This is a **B2C single-insurer MVP**: the platform *is* the insurer. Customers buy plans,
submit claims, an AI scores fraud and auto-decides, agents review the uncertain ones.
Keep it that way — do **not** introduce the multi-tenant model (multiple insurance
companies, hospitals as providers, doctors, provider contracts, per-company/hospital
scoping, service-to-service `/internal/*` APIs). The root `apis.md` describes a larger
"SaglamOL" platform; it is **reference knowledge only** and is intentionally not adopted.

Behaviours already in place (don't re-derive):
- **Payment flow is automatic.** Purchase policy → Kafka → mock payment completes (~2s) →
  policy auto-activates (ACTIVE + start/end dates). Claim approval → Kafka → `CLAIM_PAYOUT`
  payment. The purchase HTTP response returns `PENDING`; re-query a few seconds later.
- **AI auto-decides claims** on submission via `applyFraudResult` in claim-service:
  risk score `<40` → auto-APPROVED, `40–69` → UNDER_REVIEW (staff queue), `≥70` →
  auto-REJECTED. Manual review only overrides UNDER_REVIEW claims.

Planned incremental work (specs live in the per-service docs, not yet built): four IAM
endpoints (change password, admin change-role, admin suspend/activate, user search) —
see `docs/e-health-insurance-iam.md` and the frontend wiring in
`docs/e-health-insurance-frontend.md`, both under "Planned Additions".

## Repository Structure

Multi-repo: each module below is its own git repository — no monorepo, no Gradle `include()`.
Each repo is self-contained with its own `settings.gradle`, `build.gradle`, `gradle/libs.versions.toml`, and Gradle wrapper.

| Repo | Type |
|---|---|
| e-health-insurance-infra | Shared library (plain jar, published to mavenLocal) |
| e-health-insurance-iam | Spring Boot service |
| e-health-insurance-policy | Spring Boot service |
| e-health-insurance-claim | Spring Boot service |
| e-health-insurance-payment | Spring Boot service |
| e-health-insurance-ai | Spring Boot service |
| e-health-insurance-notification | Spring Boot service |
| e-health-insurance-gw | Spring Boot service (gateway) |

## Workflow

### Before any work
1. You already read this file (auto-loaded).
2. Read `docs/<service-name>.md` for the target microservice before writing any code.
3. If working on a service that depends on infra, read `docs/e-health-insurance-infra.md` too.

### Step-by-step execution
**Do NOT build an entire microservice at once.**
After reading the service doc, break the work into logical steps and list them. Then:
- **Wait for my approval before starting each step.**
- Complete one step, show me what was created, then ask if you should proceed to the next.
- If a step fails (build error, etc.), fix it before moving on.

Example steps for a typical service:
```
Step 1: build.gradle + application.yml
Step 2: Entity classes
Step 3: Repository interfaces
Step 4: Service layer
Step 5: DTOs (request/response)
Step 6: Controller + endpoints
Step 7: Kafka producers/consumers
Step 8: GlobalExceptionHandler
Step 9: Build & verify
```
Ask me: "Here are the steps I'll follow. Should I start with Step 1?" — then wait.

### After completing a service
Update `docs/<service-name>.md`:
- Check off completed items.
- Fill in entity fields, endpoint details, Kafka wiring.
- Add any decisions or deviations to "Decisions & Notes".

## Code Rules

### Style
- Keep it simple. Do not over-engineer unless functionality requires it.
- Minimal comments. Code should be self-explanatory through clear naming.
- No Javadoc unless it's a public API or shared infra class.
- No commented-out code. Ever.
- No unnecessary abstractions for one-off helpers (mappers, utils, etc.) — but the service layer always uses an interface + implementation (see Naming).

### Structure
- Follow existing package conventions: `controller/`, `service/` (+ `service/impl/`), `repository/`, `entity/`, `dto/` (+ `dto/request/`, `dto/response/`), `kafka/`, `config/`, `mapper/`.
- One class per file.
- Lombok everywhere: `@Data`, `@Builder`, `@RequiredArgsConstructor`, `@Slf4j` — for entities, services, controllers, exceptions, etc.
- DTOs and Kafka event classes are Java records (immutable — Jackson 2.12+ supports records natively).
- Add Lombok `@Builder` to records with 5+ fields (or multiple same-typed fields like several UUIDs) to avoid positional-argument errors. Skip it for small records (≤4 fields).
- Constructor injection (`@RequiredArgsConstructor`), never `@Autowired`.

### Naming
- Entities: singular (`User`, `Claim`, `Policy`).
- DTOs: suffixed (`RegisterRequest`, `LoginResponse`, `ClaimDto`) — implemented as records. Request DTOs live in `dto/request/`, response DTOs in `dto/response/`.
- Kafka producers: `XxxEventProducer`.
- Kafka consumers: `XxxEventConsumer`.
- Services: interface `XxxService` directly in `service/`, implementation `XxxServiceImpl` in `service/impl/` (annotated `@Service`, implements the interface). Controllers and other beans depend on the `XxxService` interface.

### Mapping
- Entity ↔ DTO conversions use MapStruct (`@Mapper(componentModel = "spring")`), interfaces in `mapper/` package.
- MapStruct maps directly to/from record DTOs via constructor mapping — no hand-written `toDto()`/`toEntity()` methods.
- Annotation processor order matters: `lombok`, then `lombok-mapstruct-binding`, then `mapstruct-processor`.

### Dependencies
- All shared enums, events, DTOs, exceptions live in `e-health-insurance-infra` (separate repo).
- `e-health-insurance-infra` publishes to `mavenLocal()` via the `maven-publish` plugin: run `./gradlew publishToMavenLocal` after any infra change, before building dependent services.
- Every service depends on `com.ehi:e-health-insurance-infra:0.0.1-SNAPSHOT` resolved via `mavenLocal()` — never `project(":e-health-insurance-infra")`.
- Never duplicate infra classes in service modules.

### Kafka
- Topic names from `com.ehi.infra.config.KafkaTopics` constants. Never hardcode topic strings.
- Events from `com.ehi.infra.event` package. Never create local event classes.
- `JsonSerializer` / `JsonDeserializer` for all Kafka messages.

### Database
- Each service has its own PostgreSQL database (`ehi_iam`, `ehi_policy`, etc.).
- `spring.jpa.hibernate.ddl-auto=update` for MVP.
- UUIDs for all primary keys (`@GeneratedValue(strategy = GenerationType.UUID)`).
- `Instant` for all timestamps, never `LocalDateTime`.

### Error Handling
- Shared exceptions from `com.ehi.infra.exception`: `BaseErrorService` (interface: errorCode, message, httpStatus), `BaseErrorEnum` (generic codes — NOT_FOUND, UNAUTHORIZED, BAD_REQUEST, DUPLICATE_RESOURCE, VALIDATION_ERROR, INTERNAL_ERROR), `BaseException` (abstract, holds a `BaseErrorService`), and 4 ready-made subclasses: `NotFoundException`, `UnauthorizedException`, `BadRequestException`, `DuplicateResourceException`.
- For domain-specific errors, define an `XxxErrorEnum implements BaseErrorService` in the service's `exception` package (e.g. `ClaimErrorEnum.CLAIM_NOT_FOUND` → `"CLAIM-NOT-FOUND-0002"`) and throw it via `com.ehi.infra.exception.ServiceException` — don't write per-domain exception subclasses.
- One `GlobalExceptionHandler` per service (`@RestControllerAdvice`, `@Slf4j`) catching `BaseException` → `ApiResponse.error(...)` wrapping an `ErrorResponse` (status, message, details incl. `errorCode`, timestamp).

### API
- All responses wrapped in `ApiResponse<T>` from infra.
- Paginated lists use `PagedResponse<T>` from infra.
- Endpoints versioned and prefixed with `/api/v1/` (e.g. `/api/v1/auth/login`).

### Testing
- **Every microservice must ship with tests.** None exist today (see each `docs/<service>.md`
  "Testing" section and `check.md`) — this is the biggest structural gap after observability.
- Stack: **JUnit 5 + Mockito + AssertJ** for unit tests; Spring Boot `@SpringBootTest` +
  **Testcontainers** (PostgreSQL + Kafka) for integration tests; **MockWebServer/WireMock** for the
  AI service's OpenRouter calls; **Vitest + React Testing Library + MSW** for the frontend.
- Unit-test the **service layer** (business rules, decision thresholds, validation, ownership/404
  logic) with mocked repositories and producers.
- Integration-test the **event chains**: publish the input event, assert DB state *and* the emitted
  output event (Testcontainers Kafka or `@EmbeddedKafka`). These are what catch redelivery/ordering
  regressions like the payment double-charge bug.
- Tests live under `src/test/java/...` mirroring the main package; name `XxxServiceImplTest`
  (unit), `XxxControllerTest` (web layer / `@WebMvcTest`), `XxxConsumerIT` / `XxxFlowIT`
  (integration). `./gradlew test` must pass. Prioritize meaningful coverage of decision logic and
  consumers over a coverage percentage.

### Git
- Do not create `.gitignore`, `README.md`, or CI files unless asked.

## Module Dependency Graph

```
infra ← iam
infra ← policy
infra ← claim
infra ← payment
infra ← ai
infra ← notification
gateway (no infra dependency, only routing + JWT filter)
```

`infra ←` means "depends on the published `com.ehi:e-health-insurance-infra` jar via mavenLocal", not a Gradle subproject reference — each module is a separate repo.

## Tech Stack

| Concern | Tool |
|---|---|
| Framework | Spring Boot 3.2.5 |
| Build | Gradle Groovy DSL + Version Catalog (per-repo, multi-repo setup) |
| DB | PostgreSQL 16 (database-per-service) |
| Messaging | Apache Kafka (Confluent 7.5.0) |
| Auth | JWT (jjwt 0.12.5) |
| Mapping | MapStruct 1.5.5.Final |
| Gateway | Spring Cloud Gateway |
| Containers | Docker Compose |
| AI | OpenRouter (OpenAI-compatible API), model `google/gemini-2.5-flash`, via WebClient |

## Known Issues & Tech Debt (cross-cutting)

> Full review with file references and fixes in the root `check.md`. The items below span
> multiple services; service-specific findings live in each `docs/<service>.md` under
> "Review Findings". Severity: 🔴 critical · 🟠 high · 🟡 medium · 🟢 low.

- 🔴 **Committed JWT secret / DB creds.** `JWT_SECRET` defaults to the literal
  `change-me-...` in every `application.yml` and is hardcoded in `docker-compose.yml`; Postgres is
  `postgres/postgres`. Fine for local dev — but a prod deploy without overriding `JWT_SECRET` lets
  anyone forge ADMIN tokens. Pre-deploy: inject real secrets via env/secret manager; make the
  compose default fail-fast when unset.
- 🟠 **No Kafka error handling / DLT.** No service configures `ErrorHandlingDeserializer`, a
  `DefaultErrorHandler` with backoff, or a dead-letter topic. A malformed payload can wedge a
  partition or be silently dropped. Add a shared consumer config (ErrorHandlingDeserializer +
  `DeadLetterPublishingRecoverer` → `<topic>.DLT` + finite `FixedBackOff`).
- 🟠 **Double JWT validation; gateway `X-User-*` headers are dead code.** The gateway validates the
  JWT and injects `X-User-Id`/`X-User-Role`, but no downstream service reads them — each re-parses
  the `Authorization` header itself. Pick one model: gateway-authoritative (services trust headers,
  and are only reachable via the gateway) **or** per-service validation (then delete the gateway
  header injection). If trusting headers, strip client-supplied `X-User-*` first (the gateway
  currently appends, not replaces).
- 🟠 **All service ports exposed to host.** `docker-compose.yml` maps `8081..8086`, so services are
  reachable directly, bypassing the gateway. For non-local, expose only the gateway (`8080`) and
  frontend; keep services internal.
- 🟡 **save + publish is not transactional.** Service methods that write then publish a Kafka event
  (`submitClaim`, `reviewClaim`, `applyFraudResult`, `evaluateClaim`, `processPayment`) lack
  `@Transactional` and an outbox — a publish failure after commit diverges state from events.
  Cheap fix: `@Transactional` on the consumer-facing methods. Correct fix: transactional outbox.
- 🟡 **`ddl-auto: validate` + `show-sql: false`** is now set in iam, claim, ai, payment, and policy. Only notification still uses `ddl-auto: update`/`show-sql: true`. Pre-prod task: switch notification + add Flyway/Liquibase migrations (Liquibase already on the classpath for iam, claim, ai). The IAM `active` column may still need a backfill: `UPDATE users SET active = true WHERE active IS NULL;`.
- 🟢 **No pagination caps.** Paginated endpoints bind `Pageable` straight from the request, so
  `?size=100000` is allowed. Add `@PageableDefault` + a max page size project-wide.
