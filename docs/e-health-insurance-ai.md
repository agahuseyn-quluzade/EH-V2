# e-health-insurance-ai

> Two AI capabilities: **fraud detection** (scores claims and feeds the auto-decision) and a
> **customer chatbot** scoped to insurance topics. Uses an LLM via OpenRouter (OpenAI-compatible API).

| | |
|---|---|
| Port | 8085 |
| Database | `ehi_ai` (PostgreSQL) |
| Swagger | http://localhost:8085/swagger-ui.html |
| Produces | `fraud.detected` |
| Consumes | `claim.submitted` |
| External | OpenRouter (`google/gemini-2.5-flash` by default) via WebClient |

## Responsibilities
- Score the fraud risk of each submitted claim (rules + LLM) and publish the result.
- Maintain a per-user risk profile aggregated across their claims.
- Answer customer questions via a chatbot that remembers the conversation and stays on-topic.

## Domain model

### `FraudCheck` (`fraud_checks`)
| Field | Type | Notes |
|---|---|---|
| id | UUID | PK |
| claimId | UUID | the scored claim |
| userId | UUID | claimant |
| claimType | `ClaimType` | |
| amount | BigDecimal | |
| ruleScore | Integer | deterministic rule-based score |
| aiScore | Integer | LLM score (null if LLM not invoked) |
| finalScore | Integer | blended score (see below) |
| flags | List<String> | risk flags |
| aiExplanation | String | LLM rationale |
| createdAt | Instant | |

### `RiskProfile` (`risk_profiles`)
| Field | Type | Notes |
|---|---|---|
| id | UUID | PK |
| userId | UUID | unique |
| totalClaims | int | claims scored |
| averageRiskScore | double | running average |
| highRiskCount | int | count of high-risk claims |
| lastClaimAt / updatedAt | Instant | |

### `ChatMessage` (`chat_messages`)
| Field | Type | Notes |
|---|---|---|
| id | UUID | PK |
| userId | UUID | |
| sessionId | UUID | conversation id |
| role | String | `user` / `assistant` |
| content | String (TEXT) | message text |
| createdAt | Instant | ordering within a session |

## API

### Chatbot — `/api/v1/ai/chatbot`
| Method | Path | Auth | Description |
|---|---|---|---|
| POST | `/` | CUSTOMER | `{ sessionId?, message }` → `{ sessionId, reply, timestamp }` |
| GET | `/history?sessionId=` | CUSTOMER | Full message history for a session |

### Fraud & risk — `/api/v1/ai`
| Method | Path | Auth | Description |
|---|---|---|---|
| GET | `/fraud-checks/{claimId}` | STAFF, ADMIN | Fraud assessment for a claim |
| POST | `/claims/{claimId}/analyze` | STAFF, ADMIN | Re-run fraud analysis |
| GET | `/risk-profile/{userId}` | STAFF, ADMIN | Aggregated risk profile |

## Events
- **Consumes `claim.submitted`** → `evaluateClaim`: scores the claim and publishes the result.
- **Produces `fraud.detected`** (claimId, riskScore, flags, aiExplanation) → consumed by claim
  (auto-decision) and notification (FRAUD_ALERT when risky).

## Business logic

### Fraud scoring
1. Compute a deterministic **rule score** from the claim (e.g. amount-vs-type thresholds add points;
   `AMOUNT_ABOVE_THRESHOLD = +40`, `AMOUNT_FAR_ABOVE = +20`, repeat-high-risk user `+20`).
2. If `ruleScore ≥ 40` (`AI_TRIGGER_THRESHOLD`), call the LLM for a second opinion.
3. **Final score** = `ruleScore` alone, or `round(ruleScore*0.4 + aiScore*0.6)` when the LLM ran.
4. Persist a `FraudCheck` (upsert by claimId) and update the user's `RiskProfile` (high-risk when
   `finalScore ≥ 70`). The risk-profile increment only happens for a newly created check, so a
   redelivered event doesn't inflate the profile.

The downstream **claim** service maps the score to a decision (`<40` approve, `40–69` review, `≥70` reject).

### Chatbot
- Each call loads the session's prior messages, prepends the system prompt, calls the LLM, and saves
  both the question and reply — so the assistant **remembers the conversation** within a session.
- The system prompt **scopes** it to e-health insurance topics (plans, policies, claims, coverage,
  payments, using the platform) and instructs it to politely decline off-topic requests (e.g. writing
  code, general knowledge).

## Configuration
| Env var | Default | Purpose |
|---|---|---|
| `OPENROUTER_API_KEY` | — | API key for the LLM (required for real responses) |
| `OPENROUTER_BASE_URL` | `https://openrouter.ai/api/v1` | OpenAI-compatible endpoint |
| `OPENROUTER_MODEL` | `google/gemini-2.5-flash` | Model id |
| `JWT_SECRET` | — | Auth |

## Testing
Unit tests for the rule scoring, score blending, idempotent risk-profile update, and chatbot session
handling; the OpenRouter call is stubbed (MockWebServer/WireMock) so tests don't hit the network.

## Notes & limitations
- If the LLM call fails, scoring falls back to the rule score only (claims still get decided).
- Chatbot topic-scoping is a strong system-prompt guardrail, not a hard classifier — determined
  prompt injection could still bypass it.
