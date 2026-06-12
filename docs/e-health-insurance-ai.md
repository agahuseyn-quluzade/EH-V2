# e-health-insurance-ai

## Status: DONE
## Port: 8085
## Database: ehi_ai

## What's Done
- [x] build.gradle
- [x] application.yml
- [x] Entity: FraudCheck, RiskProfile, ChatMessage
- [x] Repository layer
- [x] Service: AiClientService (OpenAI WebClient), FraudDetectionService, RiskProfileService, ChatbotService
- [x] Controller: FraudController, ChatbotController, RiskProfileController
- [x] DTO: ChatRequest, ChatResponse, FraudAiResponse, RiskAiResponse
- [x] Config: OpenAiConfig
- [x] Kafka producer: fraud.detected
- [x] Kafka consumer: claim.submitted
- [x] GlobalExceptionHandler
- [x] Dockerfile

## Entities

### FraudCheck (`fraud_checks`)
| Field | Type | Notes |
|---|---|---|
| id | UUID | PK, generated |
| claimId | UUID | plain column, no FK (cross-service) |
| userId | UUID | plain column, no FK |
| claimType | ClaimType | not null — copied from `ClaimSubmittedEvent`, used by `reanalyzeClaim` to rebuild the event |
| amount | BigDecimal | not null — copied from `ClaimSubmittedEvent`, used by `reanalyzeClaim` |
| ruleScore | Integer | not null, rule-engine pre-filter score |
| aiScore | Integer | nullable — only set if AI was triggered |
| finalScore | Integer | not null, combined score (or rule-only if AI not triggered) |
| flags | List\<String\> | nullable, `@ElementCollection` |
| aiExplanation | String | nullable |
| createdAt | Instant | `@PrePersist` |

### RiskProfile (`risk_profiles`)
| Field | Type | Notes |
|---|---|---|
| id | UUID | PK, generated |
| userId | UUID | unique, not null — one profile per user |
| totalClaims | int | not null |
| averageRiskScore | double | not null |
| highRiskCount | int | not null |
| lastClaimAt | Instant | nullable |
| updatedAt | Instant | `@PrePersist`/`@PreUpdate` |

### ChatMessage (`chat_messages`)
| Field | Type | Notes |
|---|---|---|
| id | UUID | PK, generated |
| userId | UUID | not null |
| sessionId | UUID | not null — groups messages into a conversation |
| role | String | not null — "user" / "assistant" / "system" |
| content | String | not null, `TEXT` column |
| createdAt | Instant | `@PrePersist` |

## Endpoints
| Method | Path | Description | Auth |
|---|---|---|---|
| GET | /api/v1/ai/fraud-checks/{claimId} | Fraud check result | STAFF, ADMIN |
| GET | /api/v1/ai/risk-profile/{userId} | User risk profile | STAFF, ADMIN |
| POST | /api/v1/ai/chatbot | AI chatbot | CUSTOMER |
| GET | /api/v1/ai/chatbot/history?sessionId= | Chat history by session | CUSTOMER |
| POST | /api/v1/ai/claims/{claimId}/analyze | Manual claim analysis | STAFF, ADMIN |

## Kafka
- Produces: fraud.detected
- Consumes: claim.submitted

## Decisions & Notes
- Hybrid fraud detection: rule engine (pre-filter) + OpenRouter/Gemini 2.5 Flash API (deep analysis).
- Rule threshold ≥ 40 triggers AI call. Final score = rules×0.4 + AI×0.6.
- Chatbot uses multi-turn conversation with session history.
- Fallback: if OpenAI API fails, use rule-only score.
- Package root: `com.ehi.ai`.
- Same Gradle setup as iam/policy/claim: Gradle 8.10.2 wrapper, `io.spring.dependency-management` 1.1.7, `gradle.properties` with full JDK 17 `Contents/Home` path.
- Same dependency set as claim, plus `spring-boot-starter-webflux` (for `WebClient` to call the OpenAI API — app remains a servlet/MVC app, webflux is added only as an HTTP client library).
- `application.yml`: port 8085, `jdbc:postgresql://localhost:5432/ehi_ai`, JWT validation-only (shared secret with iam), Kafka producer (fraud.detected) + consumer (claim.submitted, group-id `ai-service`, trusted packages `com.ehi.infra.event`).
- AI client config via `openai.*` properties (OpenRouter, OpenAI-compatible): `openai.api-key` (env `OPENROUTER_API_KEY`, blank default), `openai.base-url` (default `https://openrouter.ai/api/v1`), `openai.model` (default `google/gemini-2.5-flash`). Key stored in `e-health-insurance-infra/.env` (auto-loaded by Docker Compose).
- Step 1 build verification: `./gradlew build` → BUILD SUCCESSFUL. `application.yml` uses `ddl-auto: validate` and `show-sql: false`. `build.gradle` includes `runtimeOnly 'org.liquibase:liquibase-core'`.
- `FraudCheck.flags` stored via `@ElementCollection` (separate collection table `fraud_check_flags`), same pattern as claim's `Claim.fraudFlags`.
- `RiskProfile` is one row per user (`userId` unique), updated/upserted as new fraud checks and claim decisions come in — aggregated stats rather than per-claim history.
- `ChatMessage.sessionId` (UUID) groups a multi-turn conversation; `role` kept as plain `String` (not an enum) to map directly to OpenAI's chat message roles ("user"/"assistant"/"system").
- Repositories: `FraudCheckRepository` (`findByClaimId` for the per-claim result, `findByUserId` for risk profile aggregation), `RiskProfileRepository` (`findByUserId`), `ChatMessageRepository` (`findBySessionIdAndUserIdOrderByCreatedAtAsc` for ownership-scoped chat history — a CUSTOMER only sees their own session's messages).
- Security setup mirrors claim/policy exactly: `JwtProperties` (jwt.secret only), `JwtProvider` (validation-only), `JwtAuthenticationFilter` (userId as principal name), `SecurityConfig` (stateless, `@EnableMethodSecurity`, `.anyRequest().authenticated()` — no public endpoints, all 5 AI endpoints require auth + role-based `@PreAuthorize` in controllers).
- `OpenAiProperties` (`@ConfigurationProperties(prefix="openai")`): `apiKey`, `baseUrl`, `model` — bound from `application.yml`'s `openai.*` properties (Step 1).
- `OpenAiConfig`: single `WebClient openAiWebClient()` bean, `baseUrl` = `openai.base-url`, default headers `Authorization: Bearer <apiKey>` and `Content-Type: application/json`. Used by `AiClientService` (Step 7) to call the chat completions endpoint.
- DTOs:
  - `ChatRequest` (2 fields, no `@Builder`): `sessionId` (nullable — null starts a new conversation), `message` (`@NotBlank`).
  - `ChatResponse` (3 fields, no `@Builder`): `sessionId`, `reply`, `timestamp`.
  - `ChatMessageDto` (5 fields, `@Builder`): chat history entry — `id`, `sessionId`, `role`, `content`, `createdAt`.
  - `FraudAiResponse` (8 fields, `@Builder`): mirrors `FraudCheck` fields exactly (`claimId`, `userId`, `ruleScore`, `aiScore`, `finalScore`, `flags`, `aiExplanation`, `createdAt`) — used by both `GET /fraud-checks/{claimId}` and `POST /claims/{claimId}/analyze`.
  - `RiskAiResponse` (5 fields, `@Builder`): mirrors `RiskProfile` fields exactly (`userId`, `totalClaims`, `averageRiskScore`, `highRiskCount`, `lastClaimAt`). `totalApproved`/`totalRejected` were dropped (Step 7) — the AI service only consumes `claim.submitted`, so approval/rejection counts could never be populated.
- Mappers: `FraudCheckMapper` (`FraudCheck` → `FraudAiResponse`, direct field match), `RiskProfileMapper` (`RiskProfile` → `RiskAiResponse`, direct field match, now 5 fields), `ChatMessageMapper` (`ChatMessage` → `ChatMessageDto`, `userId` left unmapped — not needed in the response).

### Step 7 — Service layer
- `OpenAiMessage` (record, 2 fields: `role`, `content`), `OpenAiChatRequest` (record, `@Builder`: `model`, `messages`, `temperature`), `OpenAiChatResponse` (record tree matching OpenAI's chat completions response — `choices[0].message.content` is the text result) — all in `com.ehi.ai.client`.
- `AiClientService` / `AiClientServiceImpl`: `String chatCompletion(List<OpenAiMessage> messages)` — posts an `OpenAiChatRequest` (model/temperature from `OpenAiProperties`) to `openAiWebClient()`'s `/chat/completions`, blocks on the response, returns the first choice's message content.
- `RiskProfileService` / `RiskProfileServiceImpl`: `HIGH_RISK_USER_THRESHOLD = 2`. `getRiskProfile` throws `NotFoundException` if absent. `recordFraudCheck(userId, score, highRisk)` upserts the profile (creates with `totalClaims=0`/`averageRiskScore=0.0`/`highRiskCount=0` if absent), recomputes the running average, increments `highRiskCount` if `highRisk`, sets `lastClaimAt = Instant.now()`. `isHighRiskUser` returns `highRiskCount >= HIGH_RISK_USER_THRESHOLD`.
- `FraudDetectedEventProducer` (created in Step 7, ahead of the Step 9 Kafka schedule, since `FraudDetectionServiceImpl` needs it): publishes `FraudDetectedEvent` to `KafkaTopics.FRAUD_DETECTED` keyed by `claimId`.
- `FraudDetectionService` / `FraudDetectionServiceImpl`:
  - Constants: `AI_TRIGGER_THRESHOLD=40`, `HIGH_RISK_THRESHOLD=70`, `REPEAT_HIGH_RISK_USER_SCORE=20`, `AMOUNT_ABOVE_THRESHOLD_SCORE=40`, `AMOUNT_FAR_ABOVE_THRESHOLD_SCORE=20`.
  - Per-claim-type amount thresholds (`thresholdFor`): HOSPITALIZATION=10000, MEDICATION=2000, DENTAL=1500, CONSULTATION=500.
  - `evaluateClaim(event)`: rule engine computes `ruleScore` (amount-vs-threshold + repeat-high-risk-user checks, capped at 100, each contributing a flag). If `ruleScore >= AI_TRIGGER_THRESHOLD`, calls the LLM for a deeper assessment (`AiAssessment` record: `score`, `explanation`, `flags`, parsed from JSON via `ObjectMapper`); on any exception, falls back to rule-only scoring. The model reply is stripped of markdown code fences (e.g. ` ```json … ``` `) before parsing — Gemini 2.5 Flash wraps JSON in a fenced block, which would otherwise throw `JsonParseException` and silently degrade to rule-only. `finalScore = ruleScore*0.4 + aiScore*0.6` (rounded) when AI ran, else `ruleScore`. Upserts `FraudCheck` by `claimId`. Calls `riskProfileService.recordFraudCheck` only when the `FraudCheck` is newly created (so re-analysis via `reanalyzeClaim` doesn't double-count risk-profile stats). Publishes `FraudDetectedEvent`.
  - `getFraudCheck(claimId)`: throws `NotFoundException` if no `FraudCheck` exists for the claim.
  - `reanalyzeClaim(claimId)`: loads the existing `FraudCheck`, rebuilds a `ClaimSubmittedEvent` from its stored `claimType`/`amount` (this is why those fields were added to `FraudCheck`), and delegates to `evaluateClaim` — avoids a cross-service call back to claim-service for manual re-analysis.
- `ChatbotService` / `ChatbotServiceImpl`: `SYSTEM_PROMPT` constant sets the assistant's persona (health-insurance helper). `sendMessage(userId, request)`: reuses `request.sessionId()` or generates a new one, loads prior history via `findBySessionIdAndUserIdOrderByCreatedAtAsc`, builds an `OpenAiMessage` list (system prompt + history + new user message), calls `aiClientService.chatCompletion`, persists both the user message and the assistant reply as `ChatMessage` rows, returns `ChatResponse(sessionId, reply, Instant.now())`. `getHistory(userId, sessionId)` maps the same repository query through `ChatMessageMapper`.
- Step 7 build verification: `./gradlew compileJava` → BUILD SUCCESSFUL.

### Step 8 — Controllers
- All endpoints corrected to the `/api/v1/` prefix (the earlier `/api/ai/...` paths in the doc were inconsistent with the rest of the project and were fixed here).
- `FraudController` (`/api/v1/ai`): `GET /fraud-checks/{claimId}` (STAFF/ADMIN) → `fraudDetectionService.getFraudCheck`; `POST /claims/{claimId}/analyze` (STAFF/ADMIN) → `fraudDetectionService.reanalyzeClaim`.
- `RiskProfileController` (`/api/v1/ai`): `GET /risk-profile/{userId}` (STAFF/ADMIN) → `riskProfileService.getRiskProfile`.
- `ChatbotController` (`/api/v1/ai/chatbot`): `POST /` (CUSTOMER) → `chatbotService.sendMessage(userId, request)`; `GET /history?sessionId=` (CUSTOMER) → `chatbotService.getHistory(userId, sessionId)`. `userId` taken from `authentication.getName()` (JWT principal), same pattern as `ClaimController`.
- Step 8 build verification: `./gradlew compileJava` → BUILD SUCCESSFUL.

### Step 9 — Kafka
- `ClaimSubmittedEventConsumer` (`com.ehi.ai.kafka`): `@KafkaListener(topics = KafkaTopics.CLAIM_SUBMITTED, groupId = "ai-service")`, delegates directly to `fraudDetectionService.evaluateClaim(event)`.
- `FraudDetectedEventProducer` was already created in Step 7 (needed by `FraudDetectionServiceImpl`) — no changes here.
- Step 9 build verification: `./gradlew compileJava` → BUILD SUCCESSFUL.

### Step 10 — Error handling
- `AiErrorEnum implements BaseErrorService` (`com.ehi.ai.exception`): single `FORBIDDEN("AI-FORBIDDEN-0001", "Access denied", 403)` entry, same pattern as `ClaimErrorEnum`. `NotFoundException` (from infra) is used directly for `FraudCheck`/`RiskProfile` not-found cases — no domain-specific not-found codes needed.
- `GlobalExceptionHandler` (`@RestControllerAdvice @Slf4j`): handles `BaseException` → `ApiResponse.error` + `ErrorResponse` (status/message/`errorCode`/timestamp), `MethodArgumentNotValidException` → `VALIDATION_ERROR` with per-field details, `AccessDeniedException` → `AiErrorEnum.FORBIDDEN`, generic `Exception` → `INTERNAL_ERROR`. Identical structure to `claim`'s handler.
- Step 10 build verification: `./gradlew compileJava` → BUILD SUCCESSFUL.

### Step 11 — Build & verify
- `./gradlew build` → BUILD SUCCESSFUL. Service complete.

### Docker
- Dockerfile (multi-stage, same pattern as claim/payment): `infra-build` stage publishes `e-health-insurance-infra` (via Compose's `additional_contexts: infra`) to `/root/.m2`, `build` stage compiles `bootJar`, runtime stage `eclipse-temurin:17-jre-jammy`. `gradle.properties` removed before building. `.dockerignore` excludes `.gradle/`, `build/`, `out/`.
- `application-docker.yml` overrides `spring.datasource.url` → `postgres:5432/ehi_ai` and `spring.kafka.bootstrap-servers` → `kafka:29092`, activated via `SPRING_PROFILES_ACTIVE=docker`. `openai.*` properties remain env-var driven (`OPENAI_API_KEY`, etc.) — set via docker-compose environment, no profile override needed.

## Logging (SLF4J) — DONE

> Add `@Slf4j` only to the classes below, only the listed lines. `FraudDetectionServiceImpl`,
> producers, and the consumer already log. Follow the existing convention (parameterized `{}`,
> `info`/`warn`). Never log full chatbot message bodies (PII) — log ids/lengths only.

- **`ChatbotServiceImpl`** (`@Slf4j`) — covers the "AI failure → raw 500" review finding:
  - `sendMessage`: `warn` when the `chatCompletion` call fails before the fallback reply —
    `"Chatbot AI call failed for userId={}, sessionId={}"` (log the exception, not the message text).
- **`AiClientServiceImpl`** (`@Slf4j`):
  - `chatCompletion`: `warn` on timeout/HTTP error before propagating — `"OpenRouter call failed
    (model={})"`. (The fraud path already logs the rule-only fallback; this records the root cause.)

## Review Findings (see root `check.md` for full detail)

- ✅ **AI call has no timeout and runs on the Kafka consumer thread** (resolved). Added
  `openai.timeout-seconds` (`OpenAiProperties`, default 15, env `OPENROUTER_TIMEOUT_SECONDS`) and
  `.timeout(Duration.ofSeconds(...))` on the `bodyToMono(...)` chain in
  `AiClientServiceImpl.chatCompletion()`. The fraud path already falls back to rule-only on
  exception, so the timeout degrades gracefully. Covered by
  `AiClientServiceImplTest.chatCompletion_slowResponse_timesOut`.
- ✅ **Double DB read in `evaluateClaim`** (resolved). `findByClaimId` is now called once;
  `isNewCheck = existing.isEmpty()` and `FraudCheck` is built via `existing.orElseGet(...)`.
- 🟢 **Stale "OpenAI" strings.** `FraudDetectionServiceImpl` still logs `"OpenAI fraud assessment
  failed..."` after the OpenRouter switch. Cosmetic. (Config keys staying `openai.*` is intentional.)
- 🟢 **Chatbot AI failure → raw 500.** `ChatbotServiceImpl.sendMessage` doesn't catch
  `chatCompletion` failures; an LLM outage surfaces as an unhandled 500. A friendly fallback reply is
  better UX.
- 🟢 Claim `description` is never sent to the fraud LLM (only type + amount), limiting detection
  quality.

## Testing (DONE — 40 tests, all green)

Stack: JUnit 5 + Mockito + AssertJ (unit); MockWebServer for OpenRouter; `@SpringBootTest` +
`@EmbeddedKafka` + running Compose Postgres (IT).

### Implemented test classes (all passing, 40 tests total)

- **`FraudDetectionServiceImplTest`** (7 tests): `computeRuleScore` per-claim-type thresholds
  (HOSPITALIZATION 10000 / MEDICATION 2000 / DENTAL 1500 / CONSULTATION 500), the above-threshold and
  far-above-threshold flags, and the repeat-high-risk-user bump; AI is triggered only at
  `ruleScore >= 40`; markdown-fence stripping (` ```json … ``` `) + JSON parsing of the AI response;
  `finalScore = round(rule*0.4 + ai*0.6)` when AI ran, else rule-only; **fallback to rule-only on AI
  exception**; `recordFraudCheck` called only when the `FraudCheck` is newly created (re-analysis via
  `reanalyzeClaim` doesn't double-count); `getFraudCheck` 404 when missing.
- **`AiClientServiceImplTest`** (3 tests, MockWebServer): successful `chatCompletion` parses
  `choices[0].message.content`; empty `choices` → `IllegalStateException`; a slow response with
  `openai.timeout-seconds=1` throws (timeout fires).
- **`RiskProfileServiceImplTest`** (8 tests): `getRiskProfile` returns the mapped DTO / throws
  `NotFoundException` when absent; `recordFraudCheck` creates a new profile (`totalClaims=1`,
  `averageRiskScore=score`, `highRiskCount=0`) when none exists, updates the running average
  (`(oldAvg*oldTotal + newScore) / newTotal`) and increments `highRiskCount` only when `highRisk` is
  true; `isHighRiskUser` is true at `highRiskCount >= 2`, false below, and false when no profile.
- **`ChatbotServiceImplTest`** (5 tests): `sendMessage` generates a new `sessionId` when none is
  provided and reuses a provided one; the AI prompt sent to `aiClientService.chatCompletion` is
  `[system prompt, ...session history, new user message]`; both the user and assistant messages are
  persisted as `ChatMessage` rows; `getHistory` returns mapped `ChatMessageDto`s scoped by
  `sessionId` + `userId`.
- **`FraudControllerTest` / `RiskProfileControllerTest` / `ChatbotControllerTest`** (`@WebMvcTest`,
  6 + 4 + 5 tests): fraud-check and risk-profile endpoints are STAFF/ADMIN-only (403 for CUSTOMER);
  chatbot endpoints are CUSTOMER-only (403 for STAFF); all endpoints reject unauthenticated requests.
- **`AiFlowIT`** (`@SpringBootTest` + `@EmbeddedKafka` + `ehi_ai_test` DB, 2 tests): publishing
  `ClaimSubmittedEvent` on `claim.submitted` creates a `FraudCheck` row and publishes
  `fraud.detected` with the matching `riskScore` — below-threshold (`ruleScore=0`, no AI call) and
  above-threshold (`ruleScore=40`, AI call falls back to rule-only since `openai.base-url` is
  unreachable in the test profile).

### Implementation decisions / deviations
- Fix #4 (AI timeout) and the double-DB-read fix were implemented first (Step 2), ahead of the test
  suite, per the Review Findings above.
- `FraudDetectionServiceImplTest` constructs `FraudDetectionServiceImpl` manually via its
  constructor (not `@InjectMocks`) so a real `ObjectMapper` can be passed alongside the mocked
  repository/mapper/clients — needed because the private nested `AiAssessment` record can't be
  referenced from the test class, and Jackson 2.12+ deserializes records natively from a plain
  `new ObjectMapper()`. The class is annotated `@MockitoSettings(strictness = Strictness.LENIENT)`
  since the shared `@BeforeEach` `fraudCheckRepository.save(...)` stub isn't exercised by every test.
- `AiClientServiceImplTest` builds a real `WebClient` pointed at a `MockWebServer` instance and a
  real `OpenAiProperties` (not mocked) — the timeout test lowers `timeoutSeconds` to 1 and uses
  `MockResponse#setBodyDelay(3, SECONDS)`.
- Controller tests follow the established inline `@TestConfiguration @EnableMethodSecurity` +
  `@MockBean JwtProvider` + `.anyRequest().authenticated()` pattern from claim/payment, with
  `@WithMockUser(username="<uuid>", roles="...")`.
- `AiFlowIT` reuses the `StringDeserializer` key + `JsonDeserializer<Object>` value test-consumer
  pattern from `ClaimFlowIT`/`PaymentFlowIT`. `application-it.yml` points `openai.base-url` at
  `http://localhost:0` (unreachable), so the above-threshold case exercises the rule-only fallback
  end-to-end. Polling uses `assertThat(Optional).isPresent()` (not `.orElseThrow()`) inside
  `await().untilAsserted` since Awaitility only retries on `AssertionError`, and the `flags`
  `@ElementCollection` is not asserted in the IT (lazy-loaded, already covered by the unit test).
- **`AiFlowIT`** (Testcontainers): publish `ClaimSubmittedEvent` → assert a `FraudCheck` row and a
  `fraud.detected` event with the combined score.
