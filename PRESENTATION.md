<!--
SaglamOl — project presentation (Marp).
Render/export:  npx @marp-team/marp-cli PRESENTATION.md -o PRESENTATION.pdf
                npx @marp-team/marp-cli PRESENTATION.md -o PRESENTATION.pptx --allow-local-files
Speaker notes live in HTML comments and export into the PPTX notes pane.
-->
---
marp: true
theme: default
paginate: true
---

![w:160](logo.png)

# SaglamOl
## A Microservices E-Health Insurance Platform

Buy plans · Submit claims · AI-driven fraud detection & auto-decisions

**Team:** Nicat Nasirli · Ağahüseyn Quluzadə · Ramin Aliyev · Sadiq Səfərov

_Course: «...»  ·  «University / Department»  ·  «Date»_

<!-- This is SaglamOl, an online health-insurance platform we built as a set of microservices. Customers buy plans and submit claims; an AI scores each claim for fraud and auto-decides it, with staff reviewing only the uncertain ones. Over the next slides we'll walk through the architecture, the key flows, and how it all fits together. -->

---

# The Problem

Traditional health-insurance claim handling is:

- 🐢 **Slow** — every claim waits in a manual review queue
- 🧑‍💼 **Labor-heavy** — staff review *all* claims, even obvious ones
- 🎭 **Fraud-prone** — inflated or duplicate claims are hard to catch at scale
- 🔌 **Monolithic** — one big system; hard to scale or change one part

## Our goal

An online platform where customers self-serve (buy plans, file claims),
**AI scores fraud and auto-decides** routine claims, and staff focus only on
the uncertain ones — built as **independent, scalable microservices**.

<!-- Insurance claim processing is traditionally slow and manual — staff review every claim by hand, fraud slips through, and the systems are monolithic. Our goal was to automate the routine path: customers do everything online, an AI scores each claim and decides the clear-cut ones instantly, and humans only handle the genuinely uncertain cases. And we built it as microservices so each part can evolve and scale independently. -->

---

# What We Built

**SaglamOl** — a B2C **single-insurer** platform (the platform *is* the insurer).
_Deliberately scoped as an MVP: no multi-tenant insurers, hospitals, or doctors._

### The claim lifecycle, end to end

1. **Register / log in** → JWT-secured account
2. **Browse & buy a plan** → payment runs automatically → policy goes **ACTIVE**
3. **Submit a claim** → **AI scores fraud risk** → auto-decision:
   - low risk → **APPROVED** (payout paid automatically)
   - medium → **UNDER REVIEW** (staff queue)
   - high → **REJECTED**
4. **Track** policies, claims, payments & notifications · ask the **AI assistant**

### Three roles

👤 **Customer**  ·  🧑‍⚕️ **Staff** (review claims)  ·  🛠️ **Admin** (plans & users)

<!-- SaglamOl is a single-insurer platform — we are the insurer, not a marketplace. The whole lifecycle is online: you register, buy a plan, and payment plus activation happen automatically. When you file a claim, the AI scores it and decides on the spot — low-risk approved and paid, high-risk rejected, and the in-between ones go to a staff review queue. Three roles: customers, staff who review, and admins who manage plans and users. -->

---

# Key Features

| 👤 Customer | 🧑‍⚕️ Staff | 🛠️ Admin |
|---|---|---|
| Register / login | Review queue (uncertain claims) | Create & delete plans |
| Browse plan details | Approve / reject claims | View all users / policies |
| Buy & cancel a policy | Inspect AI fraud assessment | Search users |
| Submit claims + evidence | Member search | Change roles · suspend users |
| Track claims & payments | | View all claims / payments |
| Real-time notifications | | |
| AI assistant (chatbot) | | |

All actions are gated by **role-based access** (JWT).

<!-- Here's what each role can do. Customers self-serve the whole journey — plans, policies, claims with document uploads, notifications, and an AI assistant. Staff work the review queue: the uncertain claims the AI flagged, with the fraud assessment in front of them. Admins manage the catalog and users. Everything is gated by role through JWT. -->

---

# Tech Stack

| Layer | Technology |
|---|---|
| **Language / runtime** | Java 21 |
| **Framework** | Spring Boot 3.3.5 (Web, Data JPA, Security, Validation) |
| **Messaging** | Apache Kafka — event-driven backbone |
| **Database** | PostgreSQL 16 — one database per service |
| **Auth** | JWT (jjwt) + Spring Security |
| **AI** | OpenRouter LLM (`google/gemini-2.5-flash`) via WebClient |
| **Mapping / build** | MapStruct · Gradle · Liquibase (schema migrations) |
| **Frontend** | React 18 + TypeScript + Vite |
| **Infra / tooling** | Docker Compose · Swagger / OpenAPI · Kafka UI |

<!-- The backend is Java 21 and Spring Boot, with Kafka as the event backbone and PostgreSQL as a separate database per service. Auth is JWT. The AI features call an LLM through OpenRouter. Schema is version-controlled with Liquibase. The frontend is React with TypeScript, and the whole stack runs under Docker Compose, with Swagger and a Kafka UI for inspection. -->

---

# Architecture Overview

<!-- TODO: optionally replace this ASCII block with a polished diagram image (e.g. draw.io export) -->

```
   React SPA  ──▶  API Gateway :8080   (JWT validation + routing)
                        │  /api/v1/**
 ┌───────┬───────┬──────┼──────┬───────┬──────────────┐
 ▼       ▼       ▼      ▼      ▼       ▼              ▼
IAM    Policy  Claim  Payment  AI   Notification    each service
:8081  :8082   :8083  :8084  :8085    :8086        → own PostgreSQL DB
 │       │       │      │      │        │
 └───────┴───────┴───┬──┴──────┴────────┘
                     ▼
            Apache Kafka — domain events
```

- **Gateway** — single entry point; validates the JWT, routes to services
- **6 services** — each owns its **own database** (no shared tables)
- **Kafka** — services communicate by **events**, never direct service-to-service calls
- **Kafka UI + Swagger** — inspect events and APIs

<!-- Everything goes through one API gateway, which validates the JWT and routes to the right service. Six backend services, each with its own PostgreSQL database — they never share tables. They coordinate purely through Kafka events rather than calling each other directly, which keeps them decoupled. The React frontend only ever talks to the gateway. -->

---

# Why Microservices + Event-Driven?

### Why split into services?
- **Independent scaling & deployment** — change AI/claims without touching the rest
- **Clear ownership** — each service owns its data and its domain
- **Fault isolation** — one service failing doesn't take down the others

### Why Kafka events instead of direct calls?
- The real flows are **naturally asynchronous** (payment ~2s, AI scoring)
- **Decoupling** — the claim service doesn't need to know payment or AI exist
- **Resilience** — if a consumer is down, events wait and are processed later

### The honest trade-off
Distributed systems add complexity: **eventual consistency** and the need for
**idempotent consumers** — which we handle explicitly (see "Reliability").

<!-- We didn't pick microservices for buzzwords — the domain fits it. The flows are genuinely asynchronous: payment takes a couple seconds, AI scoring takes longer, so blocking HTTP calls would be wrong. Events let services stay decoupled — the claim service just announces 'a claim was submitted' and doesn't care who listens. The honest cost is eventual consistency and the need for idempotent consumers, which we addressed deliberately rather than ignored. -->

---

# The Services at a Glance

| Service | Responsibility | Events (→ produces / ← consumes) |
|---|---|---|
| **Gateway** | Single entry; JWT validation + routing | — |
| **IAM** | Users, auth (JWT), roles | → `user.registered` |
| **Policy** | Plans & policies | → `policy.created` · ← payment |
| **Payment** | Premium charges & payouts (mock) | → `payment.completed/failed` · ← policy, claim |
| **Claim** | Claims, evidence, auto-decision | → `claim.submitted/decision` · ← `fraud.detected` |
| **AI** | Fraud scoring + chatbot | → `fraud.detected` · ← `claim.submitted`, `policy.created` |
| **Notification** | Email (real) + SMS (mock) | ← all domain events |
| **infra** | Shared library: events, enums, DTOs | _(no runtime)_ |

IAM (produces only) and Notification (consumes only) are the two "pure" ends.

<!-- Here's the whole system on one slide. The gateway is the front door. IAM owns identity. Policy manages plans and policies. Payment handles the money — mocked here. Claim is the heart of the workflow. AI does fraud scoring and the chatbot. Notification is a pure consumer that reacts to everything and sends emails and SMS. And 'infra' is a shared library holding the event and DTO contracts all services agree on. Note the arrows: IAM only produces, Notification only consumes. -->

---

# Policy Service — Buying a Policy

**Owns** the plan catalog + customer policies. **Showcases the payment saga.**

```
Customer ──POST /policies──▶ Policy: create policy (PENDING)
                               │ publish policy.created
                               ▼
                            Payment: charge premium (mock ~2s)
                               │ publish payment.completed
                               ▼
                            Policy: policy → ACTIVE (start/end dates)
                            Notification: "policy activated" email
```

- HTTP response returns **PENDING** immediately → frontend re-queries → **ACTIVE**
- **One active policy rule** — purchasing a second is rejected (`400`)
- Payment **fails** → `payment.failed` → policy auto-**CANCELLED** + notification
- Event-handling methods are **`@Transactional`** — safe on redelivery

<!-- The policy service owns plans and policies, and it's the best example of our event-driven saga. When you buy a policy, it's created as PENDING and an event is published. Payment picks that up, charges the premium — mocked, about two seconds — and publishes 'payment completed'. Policy hears that and flips to ACTIVE. So the HTTP call returns instantly as PENDING and the UI re-queries a moment later. We guard against a second active policy, and if payment fails the policy is automatically cancelled. -->

---

# Claim Service — Submit & Auto-Decide

**Owns** claims + evidence. The heart of the workflow.

```
Customer ──POST /claims──▶ Claim (SUBMITTED) ──claim.submitted──▶ AI
                                                                  │ scores fraud
                            Claim ◀──── fraud.detected ───────────┘
                              │ auto-decide by risk score
       ┌──────────────────────┼───────────────────────┐
  score < 40               40–69                     ≥ 70
  APPROVED              UNDER_REVIEW               REJECTED
  → payout              → staff queue              (auto reason)
```

- **Auto-decision** removes humans from the routine path
- Staff review only **UNDER_REVIEW** claims (`PUT /claims/{id}/review`)
- APPROVED → `claim.decision` → **payout** + notification
- Evidence files uploaded via `multipart` and linked to the claim

<!-- The claim service is the heart of the system. A submitted claim is saved as SUBMITTED and an event goes to the AI service, which scores the fraud risk and sends back a 'fraud detected' event with a number. The claim service auto-decides: under 40 approved and paid automatically, 70 and above rejected, the middle band goes to the human review queue. So staff only ever see the genuinely uncertain claims — everything clear-cut is instant. -->

---

# AI — Fraud Detection

A **hybrid** score: deterministic rules + an LLM second opinion.

### 1. Rule score (fast, explainable)
- Claim amount vs the policy's **coverage** → **plan-based, not hardcoded**
  - `> 50%` of coverage → +40 · `> 100%` → +20
- Repeat high-risk user → +20

### 2. LLM assessment (only if rule score ≥ 40)
- Sends claim context to the LLM → returns a score + explanation + flags

### 3. Final score = `rule × 0.4 + ai × 0.6`
→ published as `fraud.detected` → drives the claim's auto-decision

_The AI learns each policy's coverage from a `policy.created` read-model —
event-driven, no synchronous call to the policy service._

<!-- The fraud score is a hybrid. First a fast rule-based score — the key part is the threshold is based on the policy's coverage, not a hardcoded number, so a big claim against a small plan is suspicious while the same amount against a large plan isn't. If the rule score is high enough, we ask the LLM for a second opinion with an explanation. The two blend — 40% rules, 60% AI — into a final score that drives the auto-decision. The AI learns each policy's coverage by listening to the policy-created event, so it never calls the policy service directly. -->

---

# AI — Customer Assistant (Chatbot)

A support chatbot for insurance questions, powered by the same LLM.

- 💬 **Conversational** — remembers context within a session
  (each message replays the session history to the model)
- 🎯 **Scoped** — system prompt restricts it to insurance topics
  (plans, policies, claims, coverage); politely declines off-topic asks
- 🗂️ **Persisted** — every message stored (`chat_messages`), retrievable by session
- 🔌 Same OpenRouter LLM as fraud detection, isolated in the **AI service**

`POST /api/v1/ai/chatbot { sessionId?, message }` → `{ sessionId, reply }`

<!-- The second AI feature is a customer chatbot for insurance questions. It's conversational — it remembers the session by replaying the history to the model each turn — and it's deliberately scoped: the system prompt keeps it on insurance topics and it declines off-topic requests like 'write me code'. Every message is stored so a conversation can be reloaded. Same LLM as fraud detection, but both live inside the isolated AI service. -->

---

# Reliability & Correctness

Event-driven systems can double-act or diverge on redelivery. We handled it.

### Idempotent consumers
- **Payment** — _a **mock** processor (auto-completes, no real gateway)_, but it
  still **skips if a payment already exists** for the reference → the same guard
  that would prevent a double charge / double payout against a real provider
- **Policy / Claim** — status guards (`if not PENDING/SUBMITTED → return`)
- **Notification** — `correlationId` dedup → no duplicate emails

### Atomic save-then-publish
- State-changing handlers are **`@Transactional`** → if the publish fails, the DB
  write rolls back and redelivery re-runs cleanly (no state/event divergence)

### Message keying
- Every event keyed by its domain id → **per-entity ordering** preserved

<!-- The slide I'm most proud of. Kafka can redeliver a message, so a naive consumer could charge twice or send duplicate emails. We made every consumer idempotent — payment skips if it already processed that reference, policy and claim use status guards, notifications dedupe by a correlation id. We made save-and-publish transactional, so if publishing an event fails after the DB write, the write rolls back and re-runs cleanly. Every event is keyed by its entity id so per-entity ordering is preserved. Payment is a mock processor, but the same guard would protect a real provider. -->

---

# Security

- 🔑 **JWT authentication** — issued by IAM on login; validated at the gateway
  *and* by each service; signed with a shared secret (HS384)
- 👮 **Role-based access** — `@PreAuthorize` enforces CUSTOMER / STAFF / ADMIN
  on every protected endpoint
- 🚪 **Gateway as the single entry** — only `/api/v1/auth/**` and `GET /plans`
  are public; everything else requires a valid token
- 🔒 **Passwords** — BCrypt-hashed, never stored or returned in plaintext
- 🗝️ **Secrets via environment** — JWT secret & API keys injected from `.env`
  (git-ignored); compose **fails fast** if the secret is unset

_Honest note: this is dev-grade config — production would add a secrets manager
and key rotation._

<!-- Security is JWT-based: IAM issues a token on login, validated both at the gateway and in each service. Access is role-based — every protected endpoint declares which roles can use it. The gateway is the only public entry; only auth and browsing plans are open. Passwords are BCrypt-hashed. Secrets like the JWT key and API keys come from environment variables in a git-ignored file, and the stack refuses to start if the JWT secret is missing. We're honest this is dev-grade — production would use a real secrets manager. -->

---

# Tooling & Testing

### Testing — **217 tests** across services, all green
- **Unit tests** (JUnit 5 · Mockito · AssertJ) — service logic: decision
  thresholds, fraud scoring, ownership/validation
- **Integration tests** (`@SpringBootTest` · Testcontainers / Embedded Kafka) —
  full event chains (e.g. `policy.created → payment → activation`)

### Tooling & developer experience
- **Swagger / OpenAPI** per service — interactive API docs + "Authorize" with JWT
- **Kafka UI** (`:8090`) — browse topics, messages & consumer-group lag
- **Postman collection** — auto-chains tokens & ids through the whole flow
- **Docker Compose** — one command brings up the whole stack

<!-- Every service ships with tests — unit tests for the business logic like decision thresholds and fraud scoring, and integration tests that spin up real Postgres and Kafka in containers to verify the full event chains end to end — 217 in total. For developer experience and the demo, each service exposes Swagger with a JWT authorize button, there's a Kafka UI to watch events and consumer lag, a Postman collection that chains the whole flow, and the entire stack comes up with one Docker Compose command. -->

---

# Live Demo

A full customer journey, end to end:

1. **Register** a customer → welcome email arrives _(show inbox / logs)_
2. **Browse plans** → **buy a policy** → re-query → status **ACTIVE** _(async activation)_
3. **Submit a claim** → watch the **AI auto-decision** (approved / review / rejected)
4. **Kafka UI** (`:8090`) → show the events flowing across topics
5. **Staff** logs in → review an **UNDER_REVIEW** claim → approve → payout
6. **AI assistant** → ask an insurance question, then an off-topic one _(it declines)_
7. **Admin** → create a plan · view users / policies

_Fallback: screenshots of each step in case of live issues._

<!-- Now a live walkthrough. I'll register as a customer and we'll see the welcome notification. Buy a policy and watch it activate asynchronously. Submit a claim and watch the AI decide it in real time, and pull up the Kafka UI to show the events moving between services. Then switch to staff to review a flagged claim, ask the AI assistant a question — including an off-topic one to show the guardrail — and finish on the admin side. If anything misbehaves live, we have screenshots as a fallback. -->

---

# Challenges, Decisions & Next Steps

### Challenges we solved
- **Redelivery → double actions** → idempotent consumers + `@Transactional`
- **Async UX** (payment/activation not instant) → PENDING + re-query pattern
- **AI for fraud** → hybrid rule + LLM, **plan-based** thresholds
- **Cross-service contracts** → shared `infra` library (events / DTOs / enums)

### Deliberate scope cuts (MVP)
- Mock payment (no real gateway) · single-insurer · no Kafka dead-letter topic yet

### Next steps
- Dead-letter topics + retry/backoff · real payment integration
- Observability (tracing / metrics) · frontend tests · production secrets manager

<!-- To wrap up: the interesting challenges were handling Kafka redelivery without double-charging, designing the async UX around payment and activation, building the hybrid AI fraud scoring with plan-based thresholds, and keeping service contracts consistent through a shared library. We made deliberate MVP scope cuts — mocked payment, single insurer, no dead-letter topic yet — and the natural next steps are dead-letter and retry handling, a real payment integration, observability, and production secrets. -->

---

![w:120](logo.png)

# Thank You

## Questions?

**SaglamOl** — a microservices e-health insurance platform

**Team:** Nicat Nasirli · Ağahüseyn Quluzadə · Ramin Aliyev · Sadiq Səfərov

<!-- Thank you — we're happy to take questions. -->
