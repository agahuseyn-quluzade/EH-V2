# e-health-insurance-iam (Identity & Access)

> Owns users, authentication (JWT), and user administration. The source of truth for identity;
> every other service trusts the JWT it issues.

| | |
|---|---|
| Port | 8081 |
| Database | `ehi_iam` (PostgreSQL) |
| Swagger | http://localhost:8081/swagger-ui.html |
| Produces | `user.registered` |
| Consumes | — |

## Responsibilities
- Register and authenticate users; issue and refresh JWT access/refresh tokens.
- Store the user profile and role; let users update their own profile and password.
- Provide admin user management: list/search users, change role, activate/suspend.
- Announce new users to the rest of the platform via `user.registered` (drives the welcome notification).

## Domain model

### `User` (`users`)
| Field | Type | Notes |
|---|---|---|
| id | UUID | PK |
| email | String | unique, not null |
| password | String | bcrypt hash |
| firstName / lastName | String | not null |
| phone | String | nullable — forwarded to notification for SMS |
| role | `UserRole` | `ADMIN` / `STAFF` / `CUSTOMER` (default CUSTOMER) |
| active | Boolean | suspend/activate flag (default true) |
| createdAt / updatedAt | Instant | |

## API

### Auth — `/api/v1/auth` (public)
| Method | Path | Body | Description |
|---|---|---|---|
| POST | `/register` | `{ email, password(min 8), firstName, lastName, phone? }` | Create CUSTOMER; returns tokens; publishes `user.registered` |
| POST | `/login` | `{ email, password }` | Returns tokens |
| POST | `/refresh` | `{ refreshToken }` | New token pair |

All return `AuthResponse { userId, email, role, accessToken, refreshToken }`.

### Users — `/api/v1/users`
| Method | Path | Auth | Description |
|---|---|---|---|
| GET | `/me` | authenticated | Own profile |
| PUT | `/me` | authenticated | Update own first/last name |
| POST | `/me/password` | authenticated | Change own password `{ currentPassword, newPassword(min 8) }` |
| GET | `/` | ADMIN | List all users (paginated) |
| GET | `/search?query=` | ADMIN, STAFF | Search users (paginated) |
| GET | `/{id}` | ADMIN, STAFF | Get user by id |
| PATCH | `/{id}/role` | ADMIN | `{ role }` — change role |
| PATCH | `/{id}/status` | ADMIN | `{ active }` — suspend/activate |

Response (`UserDto`): `{ id, email, firstName, lastName, role, createdAt, active }`.

## Events
- **Produces `user.registered`** on successful registration, carrying the user's id, name, email,
  and phone. Keyed by userId.

## Business logic
- Passwords hashed with **BCrypt**; login compares against the stored hash.
- JWT carries `sub` (email), `userId`, and `role`; signed with the shared `JWT_SECRET`.
- `register()` is `@Transactional` — if publishing `user.registered` fails, the user insert rolls
  back so a retry can re-run cleanly (no orphaned user without a welcome notification).
- Method-level security (`@PreAuthorize`) enforces ADMIN/STAFF on the management endpoints.

## Configuration
| Env var | Purpose |
|---|---|
| `JWT_SECRET` | Token signing secret (shared with the gateway) |
| `SPRING_PROFILES_ACTIVE=docker` | Use in-cluster DB/Kafka hosts |

## Testing
Unit tests for `AuthServiceImpl` / `UserServiceImpl` (registration, login, password change, role/status,
search), `@WebMvcTest` for the controllers, and a Testcontainers/embedded-Kafka integration test for
the registration → `user.registered` flow.

## Notes & limitations
- A default admin is not seeded automatically — promote a user to `ADMIN` in the DB (or via another
  admin) to bootstrap. Current admin: `admin@saglamol.com`.
