# e-health-insurance-frontend

> The web client — a React single-page app branded **SaglamOl**. Talks to the backend exclusively
> through the API Gateway, with role-based areas for customers, staff, and admins.

| | |
|---|---|
| Stack | React 18 + TypeScript + Vite, React Router, Axios |
| Dev server | http://localhost:5174 (Vite, `/api` proxied to the gateway) |
| Backend | API Gateway at `VITE_GATEWAY_URL` (default `http://localhost:8080`) |
| State | React context (auth, toasts) + local component state; no Redux |

## Responsibilities
- Public marketing/landing page and auth (login/register).
- Customer area: browse plans, buy/cancel a policy, submit/track claims, chat with the AI assistant,
  view notifications and profile.
- Staff area: claim review queue and member search.
- Admin area: plan management, user management, and oversight of policies.

## Project structure
```
src/
  api/            axios client + per-service modules (iam, policy, claim, ai, notification)
  components/     Layout, ProtectedRoute, shared UI (Badge, Field, PasswordInput, Modal, …)
  context/        AuthContext (JWT + role), ToastContext
  pages/
    public/       LandingPage
    auth/         LoginPage, RegisterPage
    member/       Dashboard, Plans, Policy, Claims, NewClaim, ClaimDetail, Chat, Notifications, Profile
    staff/        StaffDashboard, StaffQueue, StaffClaimReview, StaffMembers
    admin/        AdminDashboard, AdminPlans, AdminPolicies, AdminUsers
  styles/         global.css
  types.ts        shared TypeScript types
public/           logo.png (brand logo, also favicon)
```

## Routing & access control
Routes are guarded by `<ProtectedRoute roles={…}>`; unauthenticated users are redirected to login,
and a wrong-role user is sent to their home page.

| Area | Routes | Roles |
|---|---|---|
| Public | `/`, `/login`, `/register` | anyone |
| Member | `/dashboard`, `/plans`, `/policy`, `/claims`, `/claims/new`, `/claims/:id`, `/chat`, `/notifications`, `/profile` | CUSTOMER |
| Staff | `/staff`, `/staff/queue`, `/staff/claims/:id`, `/staff/members` | STAFF (some ADMIN) |
| Admin | `/admin`, `/admin/plans`, `/admin/policies`, `/admin/users` | ADMIN |

## API layer (`src/api`)
- **`client.ts`** — a single Axios instance:
  - Request interceptor attaches `Authorization: Bearer <accessToken>`.
  - Response interceptor **unwraps** `ApiResponse<T>` → `T`.
  - On `401`, transparently refreshes the token (once) and retries; on failure, logs out.
- **`iam.ts`, `policy.ts`, `claim.ts`, `ai.ts`, `notification.ts`** — typed wrappers per service.

## Auth flow
- `AuthContext` stores the JWT (localStorage) and decodes the role.
- Login/Register call IAM, store the token pair, and route to the role's home page.
- `PasswordInput` (shared component) renders password fields with a show/hide **eye toggle**.

## Key behaviours
- **Policy view** shows the user's `ACTIVE` (else `PENDING`) policy; cancelled/expired policies are
  not shown as "current".
- **Plans** purchase surfaces backend errors (e.g. "already have an active policy") as toasts.
- **Claims** submit then poll for the AI auto-decision; uncertain claims show as under review.
- **Chat** keeps the session id to preserve conversation context; the assistant's `**`/`*` markdown
  markers are stripped for clean display.
- After purchase, the policy/dashboard re-query a few seconds later to reflect async activation.

## Configuration
| Env var | Purpose |
|---|---|
| `VITE_GATEWAY_URL` | Gateway base URL the dev proxy / build targets (default `http://localhost:8080`) |

`.env` is git-ignored; `.env.example` documents the variable.

## Running
- **Dev (recommended for frontend work):** `npm install && npm run dev` → http://localhost:5174 with
  hot reload; `/api/*` is proxied to the gateway.
- **Docker:** a containerized build is possible but the current compose setup runs the frontend via
  the dev server / direct port; use the dev server for day-to-day work.

## Notes & limitations
- Branding is **SaglamOl** (logo at `public/logo.png`, used in the sidebar, landing page, and favicon).
- Automated tests (Vitest + React Testing Library + MSW) are planned but not yet implemented.
