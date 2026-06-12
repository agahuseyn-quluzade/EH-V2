# e-health-insurance-gw

## Status: DONE
## Port: 8080
## Database: (none)

## What's Done
- [x] build.gradle
- [x] GatewayApplication.java
- [x] JwtAuthFilter (global filter)
- [x] Route configuration (application.yml)
- [x] Public vs protected route separation
- [x] Dockerfile

## Routes
| Route | Target | Public |
|---|---|---|
| /api/v1/auth/** | iam-service:8081 | Yes |
| /api/v1/users/** | iam-service:8081 | No |
| /api/v1/plans/** (GET) | policy-service:8082 | Yes |
| /api/v1/policies/** | policy-service:8082 | No |
| /api/v1/claims/** | claim-service:8083 | No |
| /api/v1/payments/** | payment-service:8084 | No |
| /api/v1/ai/** | ai-service:8085 | No |
| /api/v1/notifications/** | notification-service:8086 | No |

## Kafka
- Produces: (none)
- Consumes: (none)

## Decisions & Notes
- JWT validated at gateway level. Downstream services receive `X-User-Id` and `X-User-Role` headers.
- No database — pure routing + auth filter.
- Routes corrected from the doc's original `/api/...` paths to `/api/v1/...` to match every service's actual `@RequestMapping` prefixes (same correction made for the AI service).
- Package root: `com.ehi.gw`. Built on Spring Cloud Gateway (reactive/WebFlux), Spring Boot 3.2.5 + Spring Cloud 2023.0.3 (`spring-cloud-dependencies` BOM imported via `dependencyManagement`).
- No `e-health-insurance-infra` dependency, per the module dependency graph — gateway is pure routing + JWT filter. `JwtProvider` reads `userId`/`role` claims as plain `String`s (no `UserRole` enum).

### Step 1 — Repo scaffolding
- `settings.gradle`, `build.gradle`, `gradle.properties` (JDK 17 `Contents/Home` path), `gradle/libs.versions.toml`, Gradle wrapper (8.10.2) — copied/adapted from the `ai` service, trimmed to gateway-only deps (no JPA, Postgres, Kafka, MapStruct, infra).
- `application.yml`: port 8080, `spring.cloud.gateway.routes` — one route per downstream service (`auth`, `users` → iam:8081; `plans`, `policies` → policy:8082; `claims` → claim:8083; `payments` → payment:8084; `ai` → ai:8085; `notifications` → notification:8086), each matching `Path=/api/v1/<segment>/**`.
- `jwt.secret` (env `JWT_SECRET`, shared secret with iam — same default as other services).
- `gateway.security.public-paths` (`/api/v1/auth/**`) and `gateway.security.public-get-paths` (`/api/v1/plans/**`) — config-driven public route lists consumed by `JwtAuthFilter` in Step 3.

### Step 2 — Application + JWT validation
- `GatewayApplication` — standard `@SpringBootApplication` main class.
- `JwtProperties` (`@ConfigurationProperties(prefix="jwt")`): single `secret` field.
- `JwtProvider` (validation-only, mirrors other services' `JwtProvider` but without the infra `UserRole` enum dependency): `isTokenValid(token)`, `getUserId(token)` and `getRole(token)` both return `String` (read directly from JWT claims).

### Step 3 — JwtAuthFilter
- `GatewaySecurityProperties` (`@ConfigurationProperties(prefix="gateway.security")`): binds `publicPaths` and `publicGetPaths` lists from `application.yml`.
- `JwtAuthFilter implements GlobalFilter, Ordered` (`order = -1`, runs before routing):
  - Path matched against `publicPaths` (any method) or `publicGetPaths` (GET only) via `AntPathMatcher` — if public, request passes through unauthenticated.
  - Otherwise requires `Authorization: Bearer <token>`; missing/invalid header or invalid token → `401 Unauthorized`, request short-circuited.
  - On valid token, mutates the request to add `X-User-Id` and `X-User-Role` headers (from JWT claims) before forwarding downstream.

### Step 4 — Build & verify
- `./gradlew build` → BUILD SUCCESSFUL. Service complete.

### Docker
- Dockerfile (2-stage, no infra dependency): `build` stage compiles `bootJar` with `gradle.properties` removed (container's own JDK 17 used instead), runtime stage `eclipse-temurin:17-jre-jammy`. `.dockerignore` excludes `.gradle/`, `build/`, `out/`.
- `application-docker.yml` redefines all 8 routes to point at Docker Compose service names (`iam:8081`, `policy:8082`, `claim:8083`, `payment:8084`, `ai:8085`, `notification:8086`) instead of `localhost`, activated via `SPRING_PROFILES_ACTIVE=docker`.

## Review Findings (see root `check.md` and CLAUDE.md cross-cutting for full detail)

- 🟠 **`X-User-Id`/`X-User-Role` injection is dead code** — no downstream service reads them; each
  re-validates the JWT itself (cross-cutting #6). Decide gateway-trust model. If trusting headers,
  strip client-supplied `X-User-*` first (the filter currently `.header(...)` appends, not replaces).
- 🟠 **Services are exposed directly to the host** (cross-cutting #7) — the gateway isn't the sole
  entry point. Expose only `8080` + frontend in non-local environments.
- 🟢 **Bare `401` with empty body** on auth failure, vs the structured `ApiResponse` error shape from
  downstream services. Write a small JSON body for consistency (and so the frontend's `extractError`
  has a message).
- ✅ Correct: POST `/api/v1/plans` is **not** public — `public-get-paths` is GET-only, so admin
  plan-create stays protected.

## Testing (DONE — 14 tests, all green)

Stack: JUnit 5 + Mockito + AssertJ + `reactor-test` (`StepVerifier`) for unit tests;
`@SpringBootTest(webEnvironment=RANDOM_PORT)` + `WebTestClient` + WireMock
(`org.wiremock:wiremock-standalone:3.9.1`) for the routing IT. No Postgres/Kafka needed — the
gateway is pure routing + filter.

### Implemented test classes (all passing, 14 tests total)
- **`JwtProviderTest`** (4): valid token → `isTokenValid() == true` and `userId`/`role` claims
  extracted correctly; token signed with a different secret, expired token, and malformed/empty
  string → `isTokenValid() == false`.
- **`JwtAuthFilterTest`** (7, using `MockServerWebExchange` + a mocked `GatewayFilterChain` +
  `StepVerifier`): public path (`/api/v1/auth/**`) and public GET-only path (`/api/v1/plans/**`)
  pass through without a token; POST to `/api/v1/plans` (GET-only public rule does **not** exempt
  POST) → 401 without invoking the chain; protected path with missing or invalid `Bearer` token →
  401, chain never invoked; protected path with a valid token → chain invoked with the request
  mutated to carry `X-User-Id`/`X-User-Role` from the JWT claims (verified via
  `ArgumentCaptor<ServerWebExchange>`); `getOrder() == -1`.
- **`GatewayRoutingIT`** (3, `@SpringBootTest(webEnvironment=RANDOM_PORT)` + WireMock +
  `WebTestClient`): `/api/v1/auth/login` (public) forwards without a token to the stubbed
  downstream; `/api/v1/claims/123` without a token → 401 and never reaches the downstream; the
  same path with a valid JWT forwards to the downstream carrying `X-User-Id`/`X-User-Role` headers
  matching the token's claims.

### Implementation decisions / deviations
- `GatewayRoutingIT` overrides `spring.cloud.gateway.routes[0]` (auth, public) and `[1]` (claims,
  protected) via `@DynamicPropertySource` to point at a WireMock instance on a dynamic port —
  Spring Boot binds an indexed list property entirely from the highest-priority source that
  defines it, so this 2-route override fully replaces the 8-route list from `application.yml` for
  the test context (the other 6 routes aren't needed for these assertions).
- Test JWTs are built manually with `Jwts.builder()` (the gateway's `JwtProvider` is
  validation-only, with no `generateAccessToken`), signed with the same default secret as
  `application.yml`'s `jwt.secret` (`change-me-to-a-secure-256-bit-secret-key-for-jwt-signing-please`,
  well over the 32-byte HMAC-SHA256 minimum).
- `build.gradle` test deps: added `io.projectreactor:reactor-test` and
  `org.wiremock:wiremock-standalone:3.9.1` (raw coordinates — no version-catalog entries needed).
- `./gradlew test` passes (BUILD SUCCESSFUL, 14/14).
