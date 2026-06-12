# e-health-insurance-frontend

## Status: Networked into Docker Compose (pre-existing app, adapted from EH-V2 → V3)
## Port: 5173
## Database: (none)

## What's Done
- [x] Dockerfile (Vite dev server, `npm run dev -- --host 0.0.0.0`, port 5173)
- [x] `.dockerignore` (`node_modules/`, `dist/`)
- [x] Wired into `e-health-insurance-infra/docker-compose.yml` as the `frontend` service
- [x] Full reconciliation of all API calls, types, and pages against V3 backend (see Reconciliation Progress)
- [x] TypeScript build passing: `tsc -b --noEmit` returns zero errors

## Stack
- Vite + React + TypeScript, axios-based API client (`src/api/client.ts`), JWT stored in `localStorage`.
- `src/api/` has one module per backend service: `iam.ts`, `policy.ts`, `claim.ts`, `ai.ts`, `notification.ts`.
- Pages split by role under `src/pages/{public,member,staff,admin}`.

## Docker / Networking
- `docker-compose.yml` (in `e-health-insurance-infra`) adds a `frontend` service: build context `../e-health-insurance-frontend`, port `5173:5173`, `depends_on: gw`, network `ehi-network`.
- `vite.config.ts` proxies all `/api/**` requests server-side to `VITE_GATEWAY_URL` (default `http://localhost:8080`). In Compose this is overridden to `VITE_GATEWAY_URL: http://gw:8080` — the Vite dev server resolves `gw` via Docker DNS, so the browser never talks to the backend directly and there are no CORS concerns.

## Decisions & Notes
- `.env` ships with `VITE_USE_MOCKS=false` and `VITE_GATEWAY_URL=http://localhost:8080` — fine for `npm run dev` on the host, but Docker Compose overrides `VITE_GATEWAY_URL` to `http://gw:8080` via the `environment:` block.
- **Role enum**: backend uses `CUSTOMER | STAFF | ADMIN`. The EH-V2 frontend used `MEMBER | STAFF | ADMIN`; `STAFF` is unchanged. `MEMBER` → `CUSTOMER` was the only role rename.
- **ApiResponse wrapper**: every backend response is wrapped in `{ success: boolean, data: T }`. A response interceptor in `client.ts` unwraps this so callers always get `T` directly.
- **JWT claim**: backend `JwtProvider` sets `userId` (camelCase) not `user_id`. Fixed in `decodeJwt()`.
- **`GET /api/v1/policies/me`**: returns `List<PolicyDto>` (array), not a single object. All call sites pick the first ACTIVE policy or the first item.
- **`logout`** is client-side only (no backend logout endpoint). `AuthContext.logout()` is synchronous — clears localStorage tokens and resets state.

## Reconciliation Progress (frontend ↔ V3 backend)

- [x] Step 1 — `src/api/client.ts`: unwrap `ApiResponse<T>`, fix token refresh path (`/api/v1/auth/refresh`), fix JWT decode claim name (`userId`), fix `extractError` for wrapped error format.
- [x] Step 2 — `src/types.ts`: complete rewrite to match backend DTOs. Role is `CUSTOMER|STAFF|ADMIN`. Plan has `active: boolean` (not `status`). Policy has `policyNumber, premiumAmount`. Claim is flat (no nested decision object): `claimNumber, claimType, amount, description, riskScore, fraudFlags, aiExplanation, approvedAmount, rejectionReason, reviewedBy`. `RegisterRequest` has no `phone`. `UpdateUserRequest` has only `firstName/lastName`. Removed non-existent types: `PolicyStatistics`, `CoverageCheck`, `PlanStatus`.
- [x] Step 3 — `src/api/iam.ts`: ROOT changed from `/api/iam` to `/api/v1`. Removed `logout()` (no endpoint), `changeRole()` (no endpoint). Remaining: `register, login, me, updateMe, getUser`.
- [x] Step 4 — `src/api/policy.ts`: ROOT changed to `/api/v1`. `myPolicy()` → `myPolicies()` returns `Policy[]`. `cancelPolicy` uses PUT (no body). Added `getAllPolicies(page, size)`. Removed: `comparePlans, updatePlan, retirePlan, activatePlan, myPolicyHistory, renewPolicy, checkCoverage, adminSearchPolicies, adminMemberPolicies, adminPurchaseForMember, adminStatistics`.
- [x] Step 5 — `src/api/claim.ts`: ROOT changed to `/api/v1/claims`. `review()` uses PUT. `uploadEvidence` sends only `file` (no `fileType`). Added `getAllClaims(status?, page, size)` returning `SpringPage<Claim>`. Removed: `queue()` (no endpoint), `listEvidence()` (no endpoint), `downloadEvidence()` (no endpoint).
- [x] Step 6 — `src/api/ai.ts`: complete rewrite. Endpoints: `chat({message, sessionId?})` → `POST /api/v1/ai/chatbot` (returns `{sessionId, reply, timestamp}`), `getChatHistory(sessionId)` → `GET /api/v1/ai/chatbot/history`, `getFraudCheck(claimId)` → `GET /api/v1/ai/fraud-checks/{claimId}`, `analyzeClaim(claimId)` → `POST /api/v1/ai/claims/{claimId}/analyze`, `getRiskProfile(userId)` → `GET /api/v1/ai/risk-profile/{userId}`.
- [x] Step 7 — `src/api/notification.ts`: ROOT changed from `/api/notification/notifications` to `/api/v1/notifications`.
- [x] Step 8 — `src/context/AuthContext.tsx`: role fallback `"MEMBER"` → `"CUSTOMER"`, homePathForRole `"CUSTOMER"→"/dashboard"` / `"STAFF"→"/staff"`. `logout()` is now sync (no backend call).
- [x] Step 9 — `src/utils/format.ts`: label maps updated for `CUSTOMER/STAFF/ADMIN`, `PENDING/ACTIVE/CANCELLED`, `SUBMITTED/UNDER_REVIEW/APPROVED/REJECTED`, `HOSPITALIZATION/MEDICATION/DENTAL/CONSULTATION`. Removed: `planStatusLabels, coverageDecisionLabels, recommendedActionLabels, evidenceTypeLabels, percent()`.
- [x] Step 10 — `src/App.tsx`: role guards updated (`"MEMBER"→"CUSTOMER"`; `STAFF` unchanged).
- [x] Step 11 — Member pages updated:
  - `PlansPage`: filter `p.active === true`, show `premiumAmount/coverageAmount`, removed AI recommend modal and plan compare feature, purchase sends no `startDate`.
  - `PolicyPage`: uses `myPolicies()` array, shows `policyNumber/premiumAmount`, removed history/coverage-check/renew/cancel-reason.
  - `NewClaimPage`: form has `claimType` dropdown + `amount` + `description`; removed `procedureCode/diagnosisCode/providerName/serviceDate`.
  - `ClaimsPage`: filter values are backend `ClaimStatus` values; columns show `claimNumber/claimType/amount/approvedAmount/status/createdAt`.
  - `ClaimDetailPage`: flat `Claim` fields; AI button calls `analyzeClaim()` returning `FraudAiResponse`; evidence list removed (no list endpoint).
  - `ChatPage`: session-based chat (UUID `sessionId`), `res.reply` field, no conversation sidebar.
  - `NotificationsPage`: uses `n.recipient` (not `n.recipientEmail`), `n.type` (not `n.template`).
  - `ProfilePage`: removed `phone` field and `user.status/lastLoginAt`.
  - `DashboardPage`: uses `myPolicies()`, shows `policyNumber/premiumAmount`, claim filters use `SUBMITTED||UNDER_REVIEW`.
- [x] Step 12 — Staff pages updated:
  - `StaffQueuePage`: replaced `claimApi.queue()` with `getAllClaims("UNDER_REVIEW", 0, 50)`.
  - `StaffDashboardPage`: replaced `claimApi.queue()` with `getAllClaims("UNDER_REVIEW", 0, 20)`, fixed `submittedAt→createdAt`, `providerName→claimType`.
  - `StaffClaimReviewPage`: uses `aiApi.analyzeClaim()` → `FraudAiResponse`, review uses `rejectionReason` (not `reason`), shows flat fraud fields.
  - `StaffMembersPage`: removed `adminMemberPolicies()` call, removed `phone/status/lastLoginAt` display.
- [x] Step 13 — Admin pages updated:
  - `AdminUsersPage`: removed role-change UI entirely; lookup by UUID only.
  - `AdminPlansPage`: form has 5 fields matching `CreatePlanRequest`; removed edit/retire/activate (no endpoints).
  - `AdminPoliciesPage`: uses `getAllPolicies(page, size)`; removed status/member filter and "purchase for member" modal.
  - `AdminDashboardPage`: removed `adminStatistics()` and `PolicyStatistics` type (no endpoint); uses `getAllClaims("UNDER_REVIEW")` for queue count.
- [x] Step 14 — `src/components/Layout.tsx`: role guard uses `"STAFF"` (unchanged from V2), logout changed from async to sync.
- [x] Step 15 — `src/pages/public/LandingPage.tsx`: fixed plan filter `p.active === true`, fixed plan fields (`premiumAmount/coverageAmount/durationMonths`), removed `percent()` import.
- [x] Step 16 — TypeScript build: `tsc -b --noEmit` passes with zero errors.

## Known Gaps / Deferred Features (frontend calls with NO matching V3 backend endpoint)
Per project decision: these are **not implemented**. The corresponding frontend UI is hidden or shows an info banner. Revisit only if/when the backend gains these endpoints.

### IAM (`src/api/iam.ts`)
- **`logout()`** — no `POST /api/v1/auth/logout` endpoint. Logout is client-side only (clears localStorage).
- **`changeRole(userId, role)`** — no admin endpoint to change user roles. `AdminUsersPage` shows an info banner instead.
- **List all users** — no `GET /api/v1/users` (admin paginated list). Admin and staff must look up by UUID.

### Policy (`src/api/policy.ts`)
- **`updatePlan(id, ...)`** — no `PUT/PATCH /api/v1/plans/{id}`. `AdminPlansPage` has no edit button; shows info banner.
- **`retirePlan` / `activatePlan`** — no endpoint to toggle `Plan.active` after creation. `AdminPlansPage` shows info banner.
- **`comparePlans`** — no compare endpoint.
- **`myPolicyHistory`** — no policy-history endpoint.
- **`renewPolicy`** — no renewal endpoint.
- **`checkCoverage`** — no `/coverage-check` endpoint. Coverage check UI removed from `ClaimDetailPage`.
- **`adminMemberPolicies(userId)`** — no per-member policy list endpoint for admin/staff. Removed from `StaffMembersPage`.
- **`adminPurchaseForMember`** — no admin endpoint to purchase a policy on behalf of a member.
- **`adminStatistics()`** — no `GET /api/v1/policies/statistics` or equivalent. `AdminDashboardPage` shows only plan count and queue count.
- **Policy status filter** — `GET /api/v1/policies` does not accept a `status` query param. `AdminPoliciesPage` has no status filter.

### Claim (`src/api/claim.ts`)
- **`listEvidence(claimId)`** — no `GET /api/v1/claims/{id}/evidence` endpoint (only `POST` to upload exists). Evidence list is hidden in `ClaimDetailPage`; upload still works.
- **`downloadEvidence(claimId, evidenceId)`** — no download endpoint.

### AI (`src/api/ai.ts`)
- **`recommend(...)` (plan recommendation quiz)** — no `/api/v1/ai/recommend` endpoint. `PlansPage` recommendation UI removed.
- **`extractDocument(...)`** — no document-extraction endpoint.

### Notification (`src/api/notification.ts`)
- **`sendEmail(...)`** — no manual send-email endpoint (notifications are Kafka-event-driven only).

### Auth
- **Refresh token rotation** — `POST /api/v1/auth/refresh` exists and is called, but the IAM service may not implement token revocation; logout is client-side only.

## Planned Additions (incremental — wire after IAM backend is built)

> Depends on the four new IAM endpoints in `docs/e-health-insurance-iam.md` →
> "Planned Additions". Build the backend first, then wire the frontend below.
> Verify with `tsc -b --noEmit` (run from the frontend dir; the binary lives at
> `node_modules/.bin/tsc`). These REPLACE the corresponding gaps noted above
> (the "no role change endpoint" and AdminUsersPage limitations).

### Types (`src/types.ts`)
- Add `active?: boolean` to `UserProfile` (backend `UserDto` will include it; treat
  missing/undefined as active).
- Add request types:
  ```ts
  export interface ChangePasswordRequest { currentPassword: string; newPassword: string; }
  export interface ChangeRoleRequest { role: Role; }
  export interface ChangeStatusRequest { active: boolean; }
  ```

### API client (`src/api/iam.ts`)
- Add to `iamApi` (ROOT is already `/api/v1`):
  ```ts
  changePassword: (body: ChangePasswordRequest) =>
    api.post<void>(`${ROOT}/users/me/password`, body).then((r) => r.data),

  changeRole: (id: string, body: ChangeRoleRequest) =>
    api.patch<UserProfile>(`${ROOT}/users/${id}/role`, body).then((r) => r.data),

  changeStatus: (id: string, body: ChangeStatusRequest) =>
    api.patch<UserProfile>(`${ROOT}/users/${id}/status`, body).then((r) => r.data),

  searchUsers: (query: string, page = 0, size = 20) =>
    api.get<SpringPage<UserProfile>>(
      `${ROOT}/users/search?query=${encodeURIComponent(query)}&page=${page}&size=${size}`
    ).then((r) => r.data),
  ```
  (`SpringPage<T>` is already defined in `types.ts` and used by claim/policy modules.)

### ProfilePage (`src/pages/member/ProfilePage.tsx`)
- Add a "Şifrəni dəyiş" (change password) card/form: `currentPassword`, `newPassword`,
  `newPasswordConfirm` fields. Validate `newPassword.length >= 8` and the confirm match
  client-side, then call `iamApi.changePassword({ currentPassword, newPassword })`.
  Toast success "Şifrə yeniləndi"; on error use `extractError`.

### AdminUsersPage (`src/pages/admin/AdminUsersPage.tsx`)
- **Remove** the info banner that says role change / user list aren't supported.
- Add a **search box** using `iamApi.searchUsers(query)` → render a paginated table
  (reuse the pagination pattern from `AdminPoliciesPage`). Keep the existing
  lookup-by-UUID as a fallback if desired.
- Per row / on the detail card, add:
  - a **role dropdown** (`CUSTOMER` / `STAFF` / `ADMIN` from `roleLabels`) that calls
    `iamApi.changeRole(id, { role })` and refreshes;
  - a **suspend/activate toggle** that calls `iamApi.changeStatus(id, { active: !active })`;
    show an active/suspended badge driven by `user.active` (undefined ⇒ active).
- Do not surface role/status actions for the currently logged-in admin (the backend has
  no self-guard; avoid letting an admin lock themselves out from the UI).

### StaffMembersPage (`src/pages/staff/StaffMembersPage.tsx`)
- Optionally upgrade the UUID-only lookup to use `iamApi.searchUsers(query)` (STAFF is
  allowed on `/users/search`). Read-only — no role/status controls for agents.

## Review Findings (see root `check.md` for full detail)

- 🟢 The gateway returns a bare `401` with an empty body (see gw doc), so `extractError` should keep
  a sensible fallback message for empty-body error responses.
- 🟢 `ApiResponse` unwrap and pagination handling are consistent; no functional issues found.

## Testing (required — not yet implemented)

Stack: **Vitest + React Testing Library + MSW** (mock the gateway at the network layer).
- **API modules** (`src/api/*.ts`): `client.ts` unwraps `ApiResponse<T>` to `T`, decodes the
  `userId` JWT claim, and routes 401s to `onAuthFailure`; `iam.ts`/`policy.ts`/`claim.ts` hit the
  correct `/api/v1/...` paths with the right verbs (e.g. `changeRole` → PATCH `/users/{id}/role`).
- **Auth/role flows**: `AuthContext` login stores tokens and resolves the role;
  `homePathForRole` maps CUSTOMER→/dashboard, STAFF→/staff, ADMIN→/admin; route guards block the
  wrong role.
- **Key pages** (with MSW): `ProfilePage` change-password client validation (min 8, confirm match);
  `AdminUsersPage` search → role dropdown → suspend toggle, and the **self-guard** (no
  role/status controls on the current admin's own row); claim submit and review happy paths.
- `extractError` falls back to a sensible message on an empty-body 401 (the gateway case).
- Keep the existing `tsc -b --noEmit` as the type gate in addition to the test suite.
