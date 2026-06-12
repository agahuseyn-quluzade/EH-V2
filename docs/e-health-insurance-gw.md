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
