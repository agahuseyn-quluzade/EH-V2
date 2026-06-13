# e-health-insurance-claim

> Owns claims and their evidence. On submission it asks the AI service to score fraud, then
> auto-decides the claim; uncertain claims go to a staff review queue. Approved claims trigger a payout.

| | |
|---|---|
| Port | 8083 |
| Database | `ehi_claim` (PostgreSQL) |
| Swagger | http://localhost:8083/swagger-ui.html |
| Produces | `claim.submitted`, `claim.decision` |
| Consumes | `fraud.detected` |

## Responsibilities
- Accept claim submissions and evidence uploads.
- Kick off AI fraud scoring (via `claim.submitted`) and apply the auto-decision when the score returns.
- Provide a staff review queue and manual approve/reject for uncertain claims.
- Announce decisions (`claim.decision`) that drive payouts and notifications.

## Domain model

### `Claim` (`claims`)
| Field | Type | Notes |
|---|---|---|
| id | UUID | PK |
| claimNumber | String | unique, generated (`CLM-…`) |
| userId | UUID | claimant |
| policyId | UUID | claimed-against policy |
| claimType | `ClaimType` | HOSPITALIZATION / MEDICATION / DENTAL / CONSULTATION |
| amount | BigDecimal | claimed amount |
| description | String | |
| status | `ClaimStatus` | SUBMITTED → UNDER_REVIEW / APPROVED / REJECTED |
| approvedAmount | BigDecimal | set when approved |
| rejectionReason | String | set when rejected |
| reviewedBy | UUID | staff reviewer (null for auto-decisions) |
| riskScore | Integer | from AI |
| fraudFlags | List<String> | from AI |
| aiExplanation | String | from AI |
| createdAt / updatedAt | Instant | |

### `ClaimEvidence` (`claim_evidence`)
| Field | Type | Notes |
|---|---|---|
| id | UUID | PK |
| claim | `Claim` | owning claim |
| fileName / filePath / contentType | String | stored upload metadata |
| uploadedAt | Instant | |

## API — `/api/v1/claims`
| Method | Path | Auth | Description |
|---|---|---|---|
| POST | `/` | CUSTOMER | Submit `{ policyId, claimType, amount, description }`; publishes `claim.submitted` |
| GET | `/me` | CUSTOMER | Own claims |
| GET | `/{id}` | authenticated | Get claim (owner, or STAFF/ADMIN) |
| POST | `/{id}/evidence` | authenticated | Upload file (`multipart/form-data`, field `file`) |
| GET | `/?status=&page=&size=` | STAFF, ADMIN | List/filter claims (paginated) |
| PUT | `/{id}/review` | STAFF, ADMIN | `{ decision, approvedAmount?, rejectionReason? }`; publishes `claim.decision` |

`ClaimDto` includes all claim fields above (status, approvedAmount, riskScore, fraudFlags, etc.).

## Events
- **Produces `claim.submitted`** on submission (keyed by claimId) → AI scores fraud.
- **Consumes `fraud.detected`** → `applyFraudResult`: stores the AI score/flags/explanation and
  auto-decides. Idempotent (`if status != SUBMITTED return`).
- **Produces `claim.decision`** when a claim becomes APPROVED or REJECTED (auto or manual) → payment
  (payout if approved) and notification.

## Business logic & auto-decision
On `fraud.detected`, the claim is decided by the AI **risk score**:

| Risk score | Outcome |
|---|---|
| `< 40` | **APPROVED** (approvedAmount = claim amount) → publishes `claim.decision` → triggers payout |
| `40–69` | **UNDER_REVIEW** — enters the staff queue, no event |
| `≥ 70` | **REJECTED** (auto reason) → publishes `claim.decision` |

- **Manual review** (`PUT /{id}/review`) only works on `SUBMITTED`/`UNDER_REVIEW` claims; it can't
  re-decide an already-finalized claim.
- `submitClaim`, `reviewClaim`, and `applyFraudResult` are `@Transactional` so a failed publish rolls
  back and the operation re-runs cleanly on redelivery.

## Configuration
`JWT_SECRET`, `SPRING_PROFILES_ACTIVE=docker`; max upload size configured for evidence
(`spring.servlet.multipart.*`).

## Testing
Unit tests for the decision thresholds, ownership/404 logic, and review rules; integration test for
the `claim.submitted` → `fraud.detected` → decision chain.

## Notes & limitations
- Evidence files are stored on the service's filesystem/volume (path recorded in `claim_evidence`),
  not in object storage.
