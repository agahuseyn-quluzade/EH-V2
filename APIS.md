# E-Health Insurance — API Documentation

REST API reference for the E-Health Insurance platform (B2C single-insurer MVP).

All traffic goes through the **API Gateway** at `http://localhost:8080`. Each microservice is
also directly reachable in local dev (ports `8081`–`8086`), but clients should use the gateway.

| Service | Port | Path prefix |
|---|---|---|
| Gateway | 8080 | (routes all `/api/v1/**`) |
| IAM | 8081 | `/api/v1/auth`, `/api/v1/users` |
| Policy | 8082 | `/api/v1/plans`, `/api/v1/policies` |
| Claim | 8083 | `/api/v1/claims` |
| Payment | 8084 | `/api/v1/payments` |
| AI | 8085 | `/api/v1/ai/**` |
| Notification | 8086 | `/api/v1/notifications` |

---

## Conventions

### Authentication
- JWT Bearer token: `Authorization: Bearer <accessToken>`.
- Obtain tokens via `POST /api/v1/auth/login` or `/register`.
- The gateway validates the JWT for all routes **except** the public ones below.

**Public routes (no token required):**
- `POST /api/v1/auth/**` (register, login, refresh)
- `GET /api/v1/plans/**` (browse plans)

**Roles:** `CUSTOMER`, `STAFF`, `ADMIN`.

### Response envelope
Every endpoint returns an `ApiResponse<T>`:
```json
{
  "success": true,
  "data": { ... },
  "error": null,
  "timestamp": "2026-06-13T12:00:00Z"
}
```
On error, `success: false`, `data: null`, and `error` holds the message. The error body may also
include an `ErrorResponse` `{ status, message, details, timestamp }` where `details.errorCode`
carries the domain error code.

### Pagination
Paginated endpoints accept Spring `Pageable` query params (`?page=0&size=20&sort=field,asc`) and
return a `PagedResponse<T>`:
```json
{ "content": [ ... ], "page": 0, "size": 20, "totalElements": 42, "totalPages": 3, "last": false }
```

### Enums
| Enum | Values |
|---|---|
| UserRole | `ADMIN`, `STAFF`, `CUSTOMER` |
| PolicyStatus | `PENDING`, `ACTIVE`, `EXPIRED`, `CANCELLED` |
| ClaimStatus | `SUBMITTED`, `UNDER_REVIEW`, `APPROVED`, `REJECTED` |
| ClaimType | `HOSPITALIZATION`, `MEDICATION`, `DENTAL`, `CONSULTATION` |
| PaymentStatus | `PENDING`, `COMPLETED`, `FAILED`, `REFUNDED` |
| PaymentReferenceType | `POLICY_PREMIUM`, `CLAIM_PAYOUT` |
| NotificationType | `WELCOME`, `POLICY_ACTIVATED`, `POLICY_PENDING`, `CLAIM_SUBMITTED`, `CLAIM_APPROVED`, `CLAIM_REJECTED`, `PAYMENT_SUCCESS`, `PAYMENT_FAILED`, `FRAUD_ALERT` |
| NotificationChannel | `EMAIL`, `SMS` |

---

## IAM Service

### Auth — `/api/v1/auth` (public)

#### `POST /api/v1/auth/register`
Register a new customer account. Returns tokens. Publishes `UserRegisteredEvent`.
```json
// Request
{ "email": "user@example.com", "password": "min8chars", "firstName": "John", "lastName": "Doe", "phone": "+994501234567" }
```
`phone` is optional. Response `data`: `AuthResponse` `{ userId, email, role, accessToken, refreshToken }`.

#### `POST /api/v1/auth/login`
```json
{ "email": "user@example.com", "password": "secret" }
```
Response: `AuthResponse`.

#### `POST /api/v1/auth/refresh`
```json
{ "refreshToken": "<token>" }
```
Response: `AuthResponse` (new token pair).

### Users — `/api/v1/users`

| Method | Path | Auth | Description |
|---|---|---|---|
| GET | `/me` | authenticated | Current user's profile |
| PUT | `/me` | authenticated | Update own first/last name |
| POST | `/me/password` | authenticated | Change own password |
| GET | `/` | ADMIN | List all users (paginated) |
| GET | `/{id}` | ADMIN, STAFF | Get user by id |
| GET | `/search?query=...` | ADMIN, STAFF | Search users (paginated) |
| PATCH | `/{id}/role` | ADMIN | Change a user's role |
| PATCH | `/{id}/status` | ADMIN | Activate/suspend a user |

```json
// PUT /me
{ "firstName": "John", "lastName": "Doe" }

// POST /me/password
{ "currentPassword": "old", "newPassword": "min8chars" }

// PATCH /{id}/role
{ "role": "STAFF" }

// PATCH /{id}/status
{ "active": false }
```
Response `data` (`UserDto`):
```json
{ "id": "uuid", "email": "u@x.com", "firstName": "John", "lastName": "Doe", "role": "CUSTOMER", "createdAt": "...", "active": true }
```

---

## Policy Service

### Plans — `/api/v1/plans`

| Method | Path | Auth | Description |
|---|---|---|---|
| GET | `/` | public | List active plans |
| GET | `/{id}` | public | Get plan by id |
| POST | `/` | ADMIN | Create a plan |

```json
// POST /plans
{ "name": "Gold", "description": "Full coverage", "coverageAmount": 50000, "premiumAmount": 1200, "durationMonths": 12 }
```
Response `data` (`PlanDto`):
```json
{ "id": "uuid", "name": "Gold", "description": "...", "coverageAmount": 50000, "premiumAmount": 1200, "durationMonths": 12, "active": true }
```

### Policies — `/api/v1/policies`

| Method | Path | Auth | Description |
|---|---|---|---|
| POST | `/` | CUSTOMER | Purchase a policy. Rejected if the user already has an ACTIVE or PENDING policy. Publishes `PolicyCreatedEvent`; payment + activation happen asynchronously. |
| GET | `/me` | CUSTOMER | List own policies |
| GET | `/{id}` | CUSTOMER, ADMIN | Get policy by id (owner or admin) |
| PUT | `/{id}/cancel` | CUSTOMER, ADMIN | Cancel a PENDING/ACTIVE policy |
| GET | `/` | ADMIN | List all policies (paginated) |

```json
// POST /policies
{ "planId": "uuid" }
```
Response `data` (`PolicyDto`):
```json
{ "id": "uuid", "policyNumber": "POL-...", "userId": "uuid", "planId": "uuid", "planName": "Gold",
  "status": "PENDING", "premiumAmount": 1200, "startDate": null, "endDate": null }
```
> **Async note:** purchase responds `PENDING`. A mock payment completes (~2s) → policy auto-activates
> (`ACTIVE` with start/end dates). Re-query `/me` a few seconds later. A failed premium payment cancels the policy.

---

## Claim Service — `/api/v1/claims`

| Method | Path | Auth | Description |
|---|---|---|---|
| POST | `/` | CUSTOMER | Submit a claim. AI auto-scores fraud on submission. Publishes `ClaimSubmittedEvent`. |
| GET | `/me` | CUSTOMER | List own claims |
| GET | `/{id}` | authenticated | Get claim by id (owner, or STAFF/ADMIN) |
| POST | `/{id}/evidence` | authenticated | Upload an evidence file (`multipart/form-data`, field `file`) |
| GET | `/?status=...` | STAFF, ADMIN | List/filter all claims (paginated; optional `status`) |
| PUT | `/{id}/review` | STAFF, ADMIN | Manually approve/reject an UNDER_REVIEW claim. Publishes `ClaimDecisionEvent`. |

```json
// POST /claims
{ "policyId": "uuid", "claimType": "CONSULTATION", "amount": 250.00, "description": "Visit" }

// PUT /{id}/review
{ "decision": "APPROVED", "approvedAmount": 250.00, "rejectionReason": null }
```
Response `data` (`ClaimDto`):
```json
{ "id": "uuid", "claimNumber": "CLM-...", "userId": "uuid", "policyId": "uuid", "claimType": "CONSULTATION",
  "amount": 250.00, "description": "Visit", "status": "APPROVED", "approvedAmount": 250.00,
  "rejectionReason": null, "reviewedBy": null, "riskScore": 12, "fraudFlags": [], "aiExplanation": "...", "createdAt": "..." }
```
> **AI auto-decision on submission:** risk `<40` → auto-`APPROVED`; `40–69` → `UNDER_REVIEW` (staff queue);
> `≥70` → auto-`REJECTED`. Manual review only overrides `UNDER_REVIEW` claims.

---

## Payment Service — `/api/v1/payments`

| Method | Path | Auth | Description |
|---|---|---|---|
| POST | `/process` | ADMIN | Manually trigger a payment (normally driven by Kafka events) |
| GET | `/me` | CUSTOMER | List own payments |
| GET | `/{id}` | authenticated | Get payment by id (owner or admin) |
| GET | `/` | ADMIN | List all payments (paginated) |

```json
// POST /payments/process
{ "userId": "uuid", "referenceId": "uuid", "referenceType": "POLICY_PREMIUM", "amount": 1200 }
```
Response `data` (`PaymentDto`):
```json
{ "id": "uuid", "userId": "uuid", "referenceId": "uuid", "referenceType": "POLICY_PREMIUM",
  "amount": 1200, "status": "COMPLETED", "transactionId": "TXN-...", "failureReason": null, "createdAt": "..." }
```
> Payments are normally created by Kafka events (`PolicyCreatedEvent`, approved `ClaimDecisionEvent`),
> not direct HTTP calls. Idempotent per `(referenceId, referenceType)` — no double charge on redelivery.

---

## AI Service — `/api/v1/ai`

### Chatbot — `/api/v1/ai/chatbot`

| Method | Path | Auth | Description |
|---|---|---|---|
| POST | `/` | CUSTOMER | Send a message; returns the assistant reply. Maintains per-session history. Scoped to e-health insurance topics only. |
| GET | `/history?sessionId=...` | CUSTOMER | Get the message history for a session |

```json
// POST /ai/chatbot
{ "sessionId": "uuid-or-null", "message": "What does my plan cover?" }
```
Response `data` (`ChatResponse`):
```json
{ "sessionId": "uuid", "reply": "...", "timestamp": "..." }
```
> Pass `sessionId: null` to start a new conversation; reuse the returned `sessionId` to continue it.

### Fraud — `/api/v1/ai`

| Method | Path | Auth | Description |
|---|---|---|---|
| GET | `/fraud-checks/{claimId}` | STAFF, ADMIN | Get the fraud assessment for a claim |
| POST | `/claims/{claimId}/analyze` | STAFF, ADMIN | Re-run fraud analysis for a claim |

Response `data` (`FraudAiResponse`):
```json
{ "claimId": "uuid", "userId": "uuid", "ruleScore": 30, "aiScore": 60, "finalScore": 48,
  "flags": ["AMOUNT_ABOVE_TYPE_THRESHOLD"], "aiExplanation": "...", "createdAt": "..." }
```

### Risk Profile — `/api/v1/ai`

| Method | Path | Auth | Description |
|---|---|---|---|
| GET | `/risk-profile/{userId}` | STAFF, ADMIN | Get the aggregated risk profile for a user |

---

## Notification Service — `/api/v1/notifications`

| Method | Path | Auth | Description |
|---|---|---|---|
| GET | `/me` | CUSTOMER | List own notifications (EMAIL+SMS rows for the same event collapsed into one) |
| GET | `/` | ADMIN | List all notifications (paginated) |

Response `data` (`NotificationDto`):
```json
{ "id": "uuid", "userId": "uuid", "type": "WELCOME", "channel": "EMAIL", "recipient": "u@x.com",
  "subject": "Welcome", "body": "...", "status": "SENT", "retryCount": 0, "createdAt": "..." }
```
> The notification service is **consumer-only** — notifications are produced by reacting to domain
> events (user registered, policy created, payment completed/failed, claim submitted/decided, fraud
> detected). Email is sent for real (SMTP); SMS is mocked (logged).

---

## Event-driven flows (background, no direct API)

These happen asynchronously via Kafka after the HTTP calls above:

```
register            → UserRegisteredEvent  → welcome email/SMS
purchase policy     → PolicyCreatedEvent   → payment (premium) → PaymentCompletedEvent → policy ACTIVE + notification
                                            → (on failure) PaymentFailedEvent → policy CANCELLED + notification
submit claim        → ClaimSubmittedEvent  → AI fraud scoring → FraudDetectedEvent → auto-decision
claim approved      → ClaimDecisionEvent   → payment (payout) → PaymentCompletedEvent → notification
```
