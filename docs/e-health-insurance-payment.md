# e-health-insurance-payment

> Owns payments. Driven by events: a created policy triggers a premium charge, an approved claim
> triggers a payout. Payment processing is **mocked** — it auto-completes after a short delay.

| | |
|---|---|
| Port | 8084 |
| Database | `ehi_payment` (PostgreSQL) |
| Swagger | http://localhost:8084/swagger-ui.html |
| Produces | `payment.completed`, `payment.failed` |
| Consumes | `policy.created`, `claim.decision` |

## Responsibilities
- Charge policy premiums when a policy is created.
- Pay out approved claims.
- Record every payment and its outcome; expose payment history to customers and admins.

## Domain model

### `Payment` (`payments`)
| Field | Type | Notes |
|---|---|---|
| id | UUID | PK |
| userId | UUID | payer/payee |
| referenceId | UUID | the policy id (premium) or claim id (payout) |
| referenceType | `PaymentReferenceType` | `POLICY_PREMIUM` / `CLAIM_PAYOUT` |
| amount | BigDecimal | |
| status | `PaymentStatus` | `PENDING` → `COMPLETED` / `FAILED` |
| transactionId | String | mock transaction ref (`TXN-…`) on success |
| failureReason | String | on failure |
| createdAt / updatedAt | Instant | |

## API — `/api/v1/payments`
| Method | Path | Auth | Description |
|---|---|---|---|
| POST | `/process` | ADMIN | Manually trigger a payment `{ userId, referenceId, referenceType, amount }` |
| GET | `/me` | CUSTOMER | Own payments |
| GET | `/{id}` | authenticated | Get payment (owner or admin) |
| GET | `/` | ADMIN | All payments (paginated) |

`PaymentDto`: `{ id, userId, referenceId, referenceType, amount, status, transactionId, failureReason, createdAt }`.

## Events
- **Consumes `policy.created`** → process a `POLICY_PREMIUM` payment for the premium amount.
- **Consumes `claim.decision`** (APPROVED only) → process a `CLAIM_PAYOUT` for the approved amount.
- **Produces `payment.completed`** on success → policy activation / payout notification.
- **Produces `payment.failed`** on failure → policy cancellation / failure notification.

## Business logic
- **Mock processor**: a `PENDING` payment is saved, then a mock processor completes it (~2s) and
  publishes `payment.completed`. An invalid amount (`<= 0`) fails immediately → `payment.failed`.
- **Idempotent**: `processPayment` skips if a non-`FAILED` payment already exists for the same
  `(referenceId, referenceType)` — so a redelivered `policy.created`/`claim.decision` never
  double-charges or double-pays.
- `processPayment` is `@Transactional`.

## Configuration
`JWT_SECRET`, `SPRING_PROFILES_ACTIVE=docker`.

## Testing
Unit tests for premium/payout processing, the duplicate-skip guard, and invalid-amount failure;
integration test for the `policy.created` → `payment.completed` chain (the case that guards against
the double-charge regression).

## Notes & limitations
- No real payment gateway — this is a stand-in that always succeeds for valid amounts. Swapping in a
  real PSP would replace only the mock processor.
