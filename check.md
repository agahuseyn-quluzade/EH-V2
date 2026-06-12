# System Review — Issues, Errors & Improvements

> Code-level review of all services (infra, iam, policy, claim, payment, ai, notification, gw)
> plus the frontend. Findings are graded; each has a concrete fix. Nothing here has been
> changed — this is a review only.

**Severity:** 🔴 Critical · 🟠 High · 🟡 Medium · 🟢 Low / polish

Context: this is a B2C MVP. Several findings are acceptable "MVP shortcuts" and are tagged as
such — they matter only before a real production deploy. The genuinely surprising correctness
bugs are called out first.

---

## Top correctness bugs (read these first)

### 🔴 1. Payments are not idempotent — duplicate Kafka delivery = double charge / double payout
`PaymentServiceImpl.processPayment()` unconditionally inserts a new `Payment` row every time a
`PolicyCreatedEvent` or approved `ClaimDecisionEvent` arrives. Kafka is **at-least-once**: on a
consumer rebalance, redelivery, or producer retry the same event is processed twice, creating
**two POLICY_PREMIUM charges** or **two CLAIM_PAYOUT payouts** for one policy/claim.

- Files: `payment/.../PolicyCreatedEventConsumer.java`, `ClaimDecisionEventConsumer.java`,
  `service/impl/PaymentServiceImpl.java`.
- Fix: dedupe on the natural key. Add a unique constraint on `(referenceId, referenceType)` and,
  in `processPayment`, `findByReferenceIdAndReferenceType(...)` first — if a non-FAILED payment
  exists, skip (log and return it). This makes the whole automatic flow safe under redelivery.
- Every other consumer in the system *is* effectively idempotent (`activatePolicy` guards on
  `PENDING`, `applyFraudResult` guards on `SUBMITTED`) — payment is the one gap, and it's the
  one that moves money.

### 🟠 2. `reviewClaim` lets an agent approve more than was claimed
`ClaimServiceImpl.reviewClaim()` requires `approvedAmount` on APPROVED but never checks it
against `claim.getAmount()` (or any policy coverage limit). An agent can approve **10,000** on a
**100** claim and payment will pay it out.
- Fix: validate `approvedAmount > 0 && approvedAmount <= claim.getAmount()`; throw
  `BadRequestException` otherwise.

### 🟠 3. No coverage / policy-validity enforcement on claims
`submitClaim()` stores whatever `policyId` the customer sends — it never verifies the policy
exists, belongs to the user, is ACTIVE, or that the claim amount is within the plan's
`coverageAmount`. A customer can file a claim against a cancelled policy, someone else's policy,
or a random UUID, and the AI auto-approve path (`applyFraudResult`, score `<40`) will pay out the
full amount.
- This is partly inherent to database-per-service (claim can't read policy's DB directly), but
  the gap is real. Options, cheapest first:
  1. Have claim consume `policy.created`/`payment.completed` and keep a local read-model of
     `{policyId → userId, status, coverageAmount}` to validate against at submit time.
  2. A synchronous gateway/internal call to policy at submit (heavier; the project deliberately
     avoids `/internal/*`).
- At minimum, cap auto-approved `approvedAmount` and flag claims whose amount exceeds plan
  coverage.

### 🟠 4. AI call has no timeout and runs on the Kafka consumer thread
`AiClientServiceImpl.chatCompletion()` does `.block()` on the WebClient with **no `.timeout(...)`**.
It's called synchronously from `evaluateClaim()`, which is called straight from
`ClaimSubmittedEventConsumer`. If OpenRouter hangs, the consumer thread blocks indefinitely and
**fraud processing stalls for every subsequent claim** on that partition.
- Fix: add `.timeout(Duration.ofSeconds(15))` (and ideally a connect/response timeout on the
  `WebClient`/`HttpClient`). The fraud path already falls back to rule-only score on exception, so
  a timeout degrades gracefully — it just needs to actually fire.

---

## Cross-cutting / architecture

### 🟠 5. No Kafka error handling — poison-pill risk, no DLT
No service configures `ErrorHandlingDeserializer`, a `DefaultErrorHandler` with backoff, or a
dead-letter topic (grep confirms zero usages). A single malformed payload on any topic can cause
deserialization to fail repeatedly; with the raw `JsonDeserializer` this can wedge the partition,
and even with Boot's default handler the record is silently dropped after retries with nowhere to
inspect it.
- Fix: wrap value deserializers in `ErrorHandlingDeserializer` and register a `DefaultErrorHandler`
  with a `DeadLetterPublishingRecoverer` (→ `<topic>.DLT`) and a finite `FixedBackOff`. One small
  `KafkaConsumerConfig` could live in infra and be reused.

### 🟠 6. Double JWT validation; gateway's `X-User-Id`/`X-User-Role` headers are dead code
The gateway (`JwtAuthFilter`) validates the JWT and injects `X-User-Id` / `X-User-Role`, but **no
downstream service reads those headers** — each service re-parses the `Authorization` header in its
own `JwtAuthenticationFilter`. So:
- The gateway's header injection is wasted work.
- Auth logic (and the shared secret) is duplicated in 6 services.
- It also means every service is independently a trust boundary — fine today because they all
  re-validate, but it makes the gateway non-authoritative.
- Pick one model: **(a)** gateway validates and downstream trusts `X-User-*` headers (lighter
  services, but then services must *only* be reachable via the gateway — see #7), or **(b)** keep
  per-service validation and delete the gateway header injection to avoid implying otherwise.
- Minor latent issue in (a): the gateway uses `request.mutate().header(...)` which **appends**; it
  should strip any client-supplied `X-User-Id`/`X-User-Role` first so a caller can't smuggle them.

### 🟠 7. All services expose their ports to the host, bypassing the gateway
`docker-compose.yml` maps `8081..8086` to the host. Anyone who can reach the host can call iam,
claim, payment, etc. directly, skipping the gateway. It works only because each service
re-validates the JWT (#6). For anything beyond local dev, expose **only** the gateway (`8080`) and
the frontend; put the services on the internal network without host port mappings.

### 🔴 8. Committed JWT secret / DB password — must not reach production
`JWT_SECRET` defaults to the literal `change-me-to-a-secure-256-bit-secret-key-for-jwt-signing-please`
in every `application.yml` **and** is hardcoded in `docker-compose.yml` (`x-jwt-secret` anchor).
Postgres is `postgres/postgres`. If deployed without overriding `JWT_SECRET`, anyone can forge an
ADMIN token against the known secret.
- Fine for local MVP, but flag clearly: production deploy must inject a real secret (and DB creds)
  via env/secret manager, and the compose default should be removed or made to fail-fast when unset.

### 🟡 9. Service-layer writes + event publish are not transactional
`submitClaim`, `reviewClaim`, `applyFraudResult` (claim), `evaluateClaim` (ai), `processPayment`
(payment) all do `repository.save(...)` then `producer.publish(...)` with **no `@Transactional`**
and no outbox. If the publish throws after the save commits, state and events diverge (e.g. claim
is APPROVED but no `claim.decision` is ever emitted → no payout). Policy added `@Transactional`
only on `activatePolicy` (for a lazy-init fix), so the pattern is inconsistent.
- MVP-acceptable, but note it. The "correct" fix is a transactional outbox; the cheap fix is at
  least `@Transactional` on the consumer-facing methods so the DB write rolls back if publish fails
  (still not atomic with Kafka, but avoids committed-state-without-event).

### 🟡 10. `ddl-auto: update` and `show-sql: true` everywhere
Known MVP choice (documented), but list it as a pre-prod task: switch to Flyway/Liquibase
migrations and turn off `show-sql`. The new IAM `active` column is the first case where this bites
— it needs the manual backfill `UPDATE users SET active = true WHERE active IS NULL;` precisely
because `ddl-auto=update` can't add a non-null column to a populated table.

---

## Per-service

### infra 🟢
- Solid. `ApiResponse` carries both a `data` and an `error` string; `GlobalExceptionHandler`s wrap
  an `ErrorResponse` *inside* `data` rather than using the `error` field — slightly redundant shape
  but consistent across services and the frontend unwraps it fine. Leave as-is.

### iam 🟡
- 🟡 **No self-guard on role/status change.** `changeRole`/`changeStatus` let an ADMIN demote or
  suspend themselves (the frontend hides it, but the API allows it). Documented as deliberately
  skipped; worth a one-line server check (`if (id.equals(currentUserId)) throw ...`) so it can't be
  done by hand.
- 🟢 **`search` query has no pagination guardrail.** `Pageable` is bound straight from the request,
  so `?size=100000` is allowed. Cap it (e.g. `@PageableDefault(size=20)` + a max) — applies to every
  paginated endpoint in the system, not just iam.
- 🟢 Confirm MapStruct `UserMapper` picks up the new `active` field (it compiles, so it does) and
  that `getAllUsers`/`search` never leak `password` (they don't — `UserDto` omits it). Good.

### policy 🟡
- 🟡 **Cancelling an ACTIVE policy issues no refund.** `cancelPolicy` just flips status to
  CANCELLED; the premium already charged is never refunded (no REFUND payment emitted). Decide if
  that's intended for the MVP and document it.
- 🟢 **No cap on duplicate purchases.** A customer can buy the same plan repeatedly (N PENDING/ACTIVE
  policies). Probably fine, but note it.
- 🟢 `EXPIRED` exists in `PolicyStatus` but nothing ever sets it — `endDate` passes with the policy
  staying ACTIVE forever. A scheduled "expire policies past endDate" job is the missing piece.

### claim 🟠
- See #2, #3 above (the two real bugs).
- 🟡 **Evidence upload trusts client content-type and filename.** `uploadEvidence` stores
  `file.getOriginalFilename()` and `file.getContentType()` verbatim. Filename is UUID-prefixed and
  joined under `uploadDir/{claimId}/`, which blunts path traversal, but there's no MIME/extension
  allow-list or magic-byte check — any file type can be uploaded. Add an allow-list
  (pdf/jpg/png) and validate.
- 🟢 `MaxUploadSizeExceededException` (file > 10MB) isn't handled by the `GlobalExceptionHandler`, so
  an oversized upload returns a generic 500 instead of 413. Add a handler.
- 🟢 `UncheckedIOException` from a failed file write also falls through to the generic 500 handler —
  acceptable, but a dedicated 500 with a clean message would be nicer.

### payment 🔴/🟡
- See #1 above (the idempotency bug — highest priority in the whole review).
- 🟡 **Async read-after-write across threads with no transaction.** `processPayment` saves a PENDING
  row then hands the id to `@Async MockPaymentProcessor.process`, which `findById`s it. It works only
  because of the 2s sleep masking the commit timing. With a real (fast) processor this becomes a
  race. Pass the needed data into the async call, or make the write transactional and trigger the
  async step after commit (`TransactionSynchronization`/`@TransactionalEventListener`).
- 🟢 `MockPaymentProcessor` on `InterruptedException` just returns, leaving the payment PENDING
  forever (no retry). Fine for a mock; note it.

### ai 🟠/🟡
- See #4 above (no timeout).
- 🟡 **Double DB read in `evaluateClaim`.** It calls `fraudCheckRepository.findByClaimId(...)` twice
  (once for the `isNewCheck` boolean, once for the `orElseGet`). Read once into a variable and derive
  `isNewCheck = optional.isEmpty()`.
- 🟢 **Stale log/message strings after the OpenAI→OpenRouter switch.** `FraudDetectionServiceImpl`
  still logs `"OpenAI fraud assessment failed..."`. Cosmetic, but rename to OpenRouter for clarity
  (the config keys are still `openai.*` too — intentional per the docs, but worth a comment).
- 🟢 **Chatbot AI failure → raw 500.** `ChatbotServiceImpl.sendMessage` doesn't catch
  `chatCompletion` failures, so an LLM outage surfaces as an unhandled 500. A friendly "assistant
  unavailable" response would be better UX.
- 🟢 Claim `description` is never sent to the fraud LLM (only type + amount), so the model can't use
  the richest signal. Intentional-looking, but it limits detection quality.

### notification 🟢
- 🟢 **`recipient` is a `userId.toString()` placeholder for every event except registration**, so
  nothing could actually be emailed/SMSed even if a real sender were wired in. Documented as MVP, but
  it means the whole notification channel is non-functional beyond the welcome path. To make it real,
  notification needs the user's email — either enrich the domain events with it or have notification
  consume `user.registered` into a local `{userId → email}` table.
- 🟢 `RetryScheduler` runs `findByStatusAndRetryCountLessThan` on a single instance every 5 min —
  fine now, but with >1 notification replica two schedulers would double-send. Note for scaling.

### gw 🟡
- See #6 and #7. Otherwise the routing/public-path setup is correct (POST `/plans` is *not* public
  because `public-get-paths` is GET-only, so admin-create is still protected — good catch in the
  design).
- 🟢 The gateway returns a bare `401` with an empty body on auth failure; downstream services return
  the structured `ApiResponse` error shape. Inconsistent for clients. Consider writing a small JSON
  body on the gateway 401.

### frontend 🟢
- 🟢 `extractError` / `ApiResponse` unwrap is consistent. Main note: the gateway 401 (empty body, #gw)
  won't carry a message, so `extractError` should have a sensible fallback for empty-body errors.
- 🟢 Pagination controls assume `totalPages`; fine since backends always send it.

---

## Suggested fix order

| # | Severity | Fix | Effort |
|---|---|---|---|
| 1 | 🔴 | Payment idempotency on `(referenceId, referenceType)` | S |
| 8 | 🔴 | Externalize `JWT_SECRET` / DB creds before any deploy | S |
| 2 | 🟠 | Validate `approvedAmount ≤ claim amount` in `reviewClaim` | XS |
| 4 | 🟠 | Add `.timeout(...)` to the AI WebClient call | XS |
| 3 | 🟠 | Claim-side policy/coverage validation (local read-model) | M |
| 5 | 🟠 | Kafka `ErrorHandlingDeserializer` + DLT + backoff (infra-wide) | M |
| 6/7 | 🟠 | Decide gateway-trust model; stop exposing service ports | S–M |
| 9 | 🟡 | `@Transactional` on consumer-facing service methods | S |
| — | 🟡 | Pageable size caps; evidence MIME allow-list; IAM self-guard | S |
| 10 | 🟡 | Flyway migrations; disable `show-sql` (pre-prod) | M |

XS ≈ minutes · S ≈ <1h · M ≈ a few hours.

---

## Architecture assessment

### What's done well (keep it)
- **Database-per-service** is applied cleanly — each service owns its schema, no cross-DB FKs, IDs
  passed as plain UUID columns. This is the right boundary and the hardest part to retrofit later.
- **Choreographed event flow** (purchase → `policy.created` → payment → `payment.completed` →
  activate; claim → fraud → decision → payout) is a reasonable fit for the domain and keeps services
  decoupled.
- **Shared infra jar** for events/enums/exceptions/response-envelopes gives consistency without a
  monorepo.
- **Consistent layering** (controller / service+impl / repository / mapper / dto) across all
  services makes the codebase easy to navigate.

### The five changes that would most improve the architecture

1. **Idempotency (inbox) + transactional outbox — the structural fix for the money bugs.**
   The duplicate-payment bug (#1) and the save-then-publish gap (#9) are two faces of the same
   missing pattern. Add, in shared infra:
   - an **outbox**: service writes domain state + an `outbox` row in one DB transaction; a poller
     publishes to Kafka. Guarantees "state changed ⇒ event eventually sent," exactly once from the
     producer side.
   - an **inbox / processed-events table**: each consumer records `eventId` before acting and skips
     duplicates. Generic dedupe for *every* consumer, not just payment.
   This is the single highest-leverage architectural addition.

2. **Observability — currently the biggest operational gap.** Six services exchanging async events
   with no correlation IDs, distributed tracing, metrics, or centralized logs means debugging "where
   did my policy get stuck?" is log-archaeology across containers. Add:
   - a **correlation/trace id** generated at the gateway, propagated via HTTP headers **and Kafka
     message headers** through the whole chain;
   - **Micrometer + OpenTelemetry** tracing (Spring Boot has first-class support) to a collector
     (Tempo/Jaeger);
   - Spring Boot **Actuator** health/readiness probes (compose only health-checks Postgres today).

3. **Decide and converge the auth model.** Today you pay for two (gateway + per-service). Recommended
   end state for this MVP: **keep per-service JWT validation** (defense in depth) **but** (a) stop
   exposing service ports so the gateway is the real front door, (b) delete the unused gateway
   `X-User-*` injection, and (c) extract the near-identical `JwtProvider`/`JwtAuthenticationFilter`/
   `SecurityConfig` into a small shared **auth starter** in infra so 6 copies don't drift. Longer
   term, consider **RS256** (asymmetric): only IAM holds the signing key; others verify with the
   public key, so a compromised downstream service can't mint tokens (today they all share the HMAC
   secret and any one of them could).

4. **Resilience on outbound calls.** The AI → OpenRouter call is the only external dependency and has
   no timeout, retry, or circuit breaker. Add **Resilience4j** (timeout + circuit breaker + a bounded
   retry) around `AiClientService`. This is small and removes the "one slow API stalls fraud
   processing" failure mode (#4) at the architectural level rather than per-call.

5. **Local read-models instead of reaching across services.** The claim-validation gap (#3) is the
   canonical database-per-service question. The idiomatic answer here is **not** a synchronous call to
   policy (the project deliberately avoids `/internal/*`) but a **local projection**: claim subscribes
   to policy/payment events and maintains a small read-only `{policyId → userId, status,
   coverageAmount}` table to validate against at submit time. Same technique applies if notification
   needs user emails (project it from `user.registered`).

### Smaller architectural notes
- **Event versioning.** Every service is coupled to one infra jar version; a breaking change to an
  event record ripples to all. For MVP fine. If events start evolving, add explicit versioning (new
  fields nullable/additive only) or a schema registry — and keep the shared lib limited to genuinely
  stable contracts.
- **No automated tests.** The docs note "no test sources yet." For async event chains this is the
  riskiest gap after observability — add **Testcontainers** integration tests (Postgres + Kafka) that
  assert the end-to-end flows (purchase→active, claim→decision→payout). Manual smoke testing won't
  catch redelivery/ordering regressions like the payment bug.
- **Gateway error shape.** Make the gateway emit the same `ApiResponse` error envelope as the
  services so clients see one contract.
- **Saga visibility.** Pure choreography is fine at this size, but as flows grow, the lack of a single
  place that knows "the state of a purchase" hurts. Correlation IDs (point 2) are the cheap first step;
  a light orchestrator is only worth it if flows get more branchy.

### What NOT to change
- Don't adopt the multi-tenant SaglamOL model (already a documented decision).
- Don't merge services or go monorepo — the boundaries are sound for the domain.
- Don't replace Kafka choreography with synchronous REST between services — it would re-couple them.
