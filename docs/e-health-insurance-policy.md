# e-health-insurance-policy

> Owns insurance **plans** (the catalog) and **policies** (a customer's purchased coverage). Drives
> the purchase saga: publishing a policy creation kicks off payment, and a completed payment
> activates the policy.

| | |
|---|---|
| Port | 8082 |
| Database | `ehi_policy` (PostgreSQL) |
| Swagger | http://localhost:8082/swagger-ui.html |
| Produces | `policy.created` |
| Consumes | `payment.completed`, `payment.failed` |

## Responsibilities
- Maintain the plan catalog (admin-created, publicly browsable).
- Let a customer purchase a policy (one active policy at a time).
- React to payment events: activate the policy on success, cancel it on failure.
- Let customers/admins view and cancel policies.

## Domain model

### `Plan` (`plans`)
| Field | Type | Notes |
|---|---|---|
| id | UUID | PK |
| name | String | unique |
| description | String | |
| coverageAmount | BigDecimal | max payout |
| premiumAmount | BigDecimal | yearly premium |
| durationMonths | int | coverage length |
| active | boolean | only active plans are purchasable/listed |
| createdAt / updatedAt | Instant | |

### `Policy` (`policies`)
| Field | Type | Notes |
|---|---|---|
| id | UUID | PK |
| policyNumber | String | unique, generated (`POL-…`) |
| userId | UUID | owner |
| plan | `Plan` | the purchased plan (ManyToOne) |
| status | `PolicyStatus` | `PENDING` → `ACTIVE` → `EXPIRED`/`CANCELLED` |
| premiumAmount | BigDecimal | snapshot of the plan premium |
| startDate / endDate | Instant | set on activation |
| createdAt / updatedAt | Instant | |

## API

### Plans — `/api/v1/plans`
| Method | Path | Auth | Description |
|---|---|---|---|
| GET | `/` | public | List active plans |
| GET | `/{id}` | public | Get plan |
| POST | `/` | ADMIN | Create plan `{ name, description, coverageAmount, premiumAmount, durationMonths }` |

### Policies — `/api/v1/policies`
| Method | Path | Auth | Description |
|---|---|---|---|
| POST | `/` | CUSTOMER | Purchase `{ planId }` — **rejected if user already has an ACTIVE/PENDING policy**; publishes `policy.created` |
| GET | `/me` | CUSTOMER | Own policies |
| GET | `/{id}` | CUSTOMER, ADMIN | Get policy (owner or admin) |
| PUT | `/{id}/cancel` | CUSTOMER, ADMIN | Cancel a PENDING/ACTIVE policy |
| GET | `/` | ADMIN | All policies (paginated) |

`PolicyDto`: `{ id, policyNumber, userId, planId, planName, status, premiumAmount, startDate, endDate }`.

## Events
- **Produces `policy.created`** on purchase (keyed by policyId) → consumed by payment (to charge the
  premium) and notification (POLICY_PENDING).
- **Consumes `payment.completed`** (referenceType `POLICY_PREMIUM`) → `activatePolicy`: sets status
  `ACTIVE`, fills start/end dates. Idempotent (`if status != PENDING return`).
- **Consumes `payment.failed`** (referenceType `POLICY_PREMIUM`) → `cancelPolicyOnPaymentFailure`:
  sets status `CANCELLED`. Idempotent (only acts on a PENDING policy).

## Business logic & purchase flow
1. `POST /policies` creates a `PENDING` policy and publishes `policy.created`. Response is `PENDING`.
2. Payment service charges the premium (mock, ~2s) and publishes `payment.completed`.
3. Policy consumes it and flips the policy to `ACTIVE` with start/end dates.
4. On payment failure → policy is `CANCELLED`.

Guards:
- **One active policy** — purchase throws `400` if the user already holds an ACTIVE or PENDING policy.
- Consumer-facing methods (`purchasePolicy`, `activatePolicy`, `cancelPolicyOnPaymentFailure`) are
  `@Transactional` so a failed publish rolls back and redelivery re-runs cleanly.

## Configuration
`JWT_SECRET`, `SPRING_PROFILES_ACTIVE=docker`, DB/Kafka hosts via the docker profile.

## Testing
Unit tests for `PolicyServiceImpl` / `PlanServiceImpl` (purchase guard, activation, cancellation,
plan creation) and an integration test for the `policy.created` → payment → `payment.completed` →
activation chain.

## Notes & limitations
- `EXPIRED` exists in the enum but expiry is not yet scheduled (no job downgrades ACTIVE→EXPIRED at
  endDate).
