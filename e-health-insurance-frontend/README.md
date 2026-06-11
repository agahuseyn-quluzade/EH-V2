# HealthAssure — E-Health Insurance Frontend

Member, staff, and admin web UI for the E-Health Insurance microservices platform.
Built with Vite + React + TypeScript. Single-page app that talks to the API
gateway (`e-health-insurance-gw`, port 8080) and routes through to the
underlying services (IAM, Policy, Claim, AI, Notification).

## What's inside

```
src/
├─ api/                  Axios client + one module per microservice
│  ├─ client.ts          JWT injection, refresh-on-401, error helpers
│  ├─ iam.ts             auth/register/login/refresh, users
│  ├─ policy.ts          plans, member policies, coverage checks
│  ├─ claim.ts           claims, evidence upload, staff review
│  ├─ ai.ts              chat, recommendations, claim scoring
│  └─ notification.ts    member notification history
├─ auth/                 (reserved)
├─ components/           Loader, Modal, Badge, Empty — reusable bits
├─ context/              AuthContext + ToastContext
├─ layouts/              AppLayout (sidebar shell), AuthLayout
├─ pages/
│  ├─ public/            Login, Register
│  ├─ member/            Dashboard, Plans, Policy, Claims (list, new,
│  │                     detail), Chat, Notifications, Profile
│  ├─ staff/             Review queue, all claims, member lookup
│  └─ admin/             Overview, plan catalog CRUD, user role mgmt
├─ routes/               ProtectedRoute, RoleHome
├─ styles/               globals.css with design tokens
├─ types/                Shared TS types for every API payload
└─ utils/                format helpers (money, dates, initials)
```

## Quick start

### 1. Install Node.js (only if you don't have it)

This project needs Node 18+. Pick one:

```bash
# via Homebrew (macOS)
brew install node

# via nvm
curl -o- https://raw.githubusercontent.com/nvm-sh/nvm/v0.39.7/install.sh | bash
nvm install 20 && nvm use 20
```

### 2. Install dependencies

```bash
cd e-health-insurance-frontend
npm install
```

### 3. Pick a mode

The frontend ships with two modes, controlled by `VITE_USE_MOCKS` in `.env`:

| Mode             | When to use                          | What runs                  |
| ---------------- | ------------------------------------ | -------------------------- |
| **Mock** (default) | Demo the UI without the backend.     | All API calls served from an in-memory store. |
| **Live**         | Integrate against the real services. | Calls go through the API gateway on port 8080. |

The `.env` file is pre-configured for **mock mode**. To switch to live:

```bash
# Edit .env
VITE_USE_MOCKS=false
VITE_GATEWAY_URL=http://localhost:8080
```

### 4. (Live mode only) Start the backend

```bash
cd ../e-health-insurance-infra
docker-compose up -d
```

This brings up PostgreSQL, MailHog, and all six services. Then visit
http://localhost:8080/actuator/health to confirm the gateway is up.

### 5. Run the frontend

```bash
npm run dev
```

Open http://localhost:5173.

### Mock mode demo accounts

In mock mode, any email/password combo works — the **role is derived from the email**:

| Email starts with | Role assigned | What you'll see                               |
| ----------------- | ------------- | --------------------------------------------- |
| `admin…`          | ADMIN         | Plan catalog CRUD, user role management       |
| `staff…`          | STAFF         | Flagged-claims review queue, member lookup    |
| anything else     | MEMBER        | Member dashboard, plans, claims, AI chat      |

Pre-seeded accounts:
- `alex.morgan@test.com` — MEMBER with active Standard Plus policy + sample claims
- `jordan.kim@test.com` — STAFF with access to the review queue
- `sam.patel@test.com` — MEMBER with active Premium Shield policy + flagged claims

A small "MOCK MODE" tag appears in the bottom-left corner so you always know which mode you're in.

### 6. Build for production

```bash
npm run build
npm run preview   # serve the built bundle on http://localhost:4173
```

## How the frontend talks to the backend

All HTTP requests go through the **API gateway** (`localhost:8080`).
The gateway strips the first two path segments — so `/api/iam/api/v1/auth/login`
becomes `/api/v1/auth/login` once it reaches the IAM service.

| Frontend path                              | Service       | Backend path                |
| ------------------------------------------ | ------------- | --------------------------- |
| `/api/iam/api/v1/auth/*`                   | IAM           | `/api/v1/auth/*`            |
| `/api/iam/api/v1/users/*`                  | IAM           | `/api/v1/users/*`           |
| `/api/policy/policies/plans`               | Policy (TBD)  | `/policies/plans`           |
| `/api/policy/policies/me`                  | Policy (TBD)  | `/policies/me`              |
| `/api/policy/policies/purchase`            | Policy (TBD)  | `/policies/purchase`        |
| `/api/policy/policies/{id}/coverage`       | Policy (TBD)  | `/policies/{id}/coverage`   |
| `/api/claim/claims`                        | Claim (TBD)   | `/claims`                   |
| `/api/claim/claims/me`                     | Claim (TBD)   | `/claims/me`                |
| `/api/claim/claims/{id}/evidence`          | Claim (TBD)   | `/claims/{id}/evidence`     |
| `/api/claim/claims/{id}/review`            | Claim (TBD)   | `/claims/{id}/review`       |
| `/api/claim/claims/flagged`                | Claim (TBD)   | `/claims/flagged`           |
| `/api/ai/chat/message`                     | AI (TBD)      | `/chat/message`             |
| `/api/ai/policies/recommend`               | AI (TBD)      | `/policies/recommend`       |
| `/api/notification/notifications/me`       | Notification  | `/notifications/me`         |

**Status today:** the IAM service is fully implemented and ships working
endpoints. Policy / Claim / AI / Notification have entities but no controllers
yet — those pages will surface friendly empty/error states in the UI until the
backend ships, but every endpoint is already wired up on the client side and
will start working as soon as the controllers are added.

**Mock mode parity:** when `VITE_USE_MOCKS=true`, every one of the endpoints
above is intercepted by `src/api/mocks/handlers.ts` and served from an
in-memory store. The page code is identical between mock and live mode —
only the axios adapter differs.

## Authentication

- On login/register the IAM service returns an `accessToken` + `refreshToken`.
  Both are stored in `localStorage` (`ehi.accessToken`, `ehi.refreshToken`).
- Every outgoing request automatically gets `Authorization: Bearer <token>`.
- On a `401` the client tries the refresh endpoint once. If that also fails,
  the user is redirected to `/login`.
- The user's role (MEMBER / STAFF / ADMIN) drives both the sidebar and the
  default home page via the `RoleHome` and `ProtectedRoute` components.

## Role-based screens

| Role   | Default home    | Pages                                                                   |
| ------ | --------------- | ----------------------------------------------------------------------- |
| MEMBER | `/`             | Dashboard · Plans · My Policy · My Claims · New Claim · Chat · Notifs   |
| STAFF  | `/staff`        | Review queue · All claims · Member lookup                               |
| ADMIN  | `/admin`        | Overview · Plan catalog (CRUD) · User role management                   |

To test a different role, sign in with that account or use the admin
"User role" screen to promote/demote a user (calls IAM
`PATCH /api/v1/users/{id}/role`).

## Common scripts

| Command           | What it does                              |
| ----------------- | ----------------------------------------- |
| `npm run dev`     | Vite dev server with HMR on :5173         |
| `npm run build`   | Type-check (`tsc -b`) then production build |
| `npm run preview` | Serve the built bundle locally            |
| `npm run lint`    | TypeScript noEmit check                   |

## Project conventions

- **No remapping**: field names in `src/types/index.ts` match backend DTO
  field names exactly. The client passes payloads straight through.
- **All HTTP goes through `src/api/client.ts`** so JWT handling and error
  extraction stay in one place.
- **Error messages**: every catch site uses `extractError(err)` to pull the
  friendliest available message from a Spring error response.
- **Independent failures**: dashboards use `Promise.allSettled` so a single
  service being down doesn't break the whole page.

## Troubleshooting

- **CORS errors** in the browser — the gateway must allow the dev origin.
  Add `http://localhost:5173` to its CORS allowed-origins.
- **All requests 502 / 504** — the backend isn't running. Check
  `docker ps` and `docker logs e-health-insurance-gw`.
- **Login works but `/users/me` returns 403** — your JWT secret in IAM has
  rotated since the token was issued. Sign in again.
- **Plan / claim screens stay empty** — those services aren't implemented
  yet. The IAM-only flows (login, register, profile) will still work.
