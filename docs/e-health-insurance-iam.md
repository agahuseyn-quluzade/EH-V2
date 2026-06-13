# e-health-insurance-iam

## Status: DONE (tests complete)
## Port: 8081
## Database: ehi_iam

## What's Done
- [x] build.gradle
- [x] Entity: User (includes `active` field)
- [x] Repository: UserRepository (includes `search` query)
- [x] Service: AuthService, UserService (interfaces in `service/`, impls in `service/impl/`)
- [x] Controller: AuthController, UserController
- [x] DTO: RegisterRequest, LoginRequest, RefreshRequest, UpdateUserRequest, ChangePasswordRequest, ChangeRoleRequest, ChangeStatusRequest (`dto/request/`), AuthResponse, UserDto (`dto/response/`) + UserMapper
- [x] Security: SecurityConfig, JwtProvider, JwtAuthenticationFilter, JwtProperties
- [x] Kafka producer: UserRegisteredEvent → user.registered
- [x] GlobalExceptionHandler
- [x] application.yml
- [x] Dockerfile
- [x] Planned Additions implemented: 4 new endpoints (change password, change role, change status, search users)

## Entities

### User (`users` table)

| Field | Type | Notes |
| --- | --- | --- |
| id | UUID | PK, generated |
| email | String | unique, not null |
| password | String | hashed, not null |
| firstName | String | not null |
| lastName | String | not null |
| phone | String | nullable — added via changelog 002-add-phone |
| role | UserRole (infra enum) | not null, stored as STRING |
| active | boolean | not null, default true |
| createdAt | Instant | set on persist |
| updatedAt | Instant | set on persist/update |

## Endpoints
| Method | Path | Description | Auth |
|---|---|---|---|
| POST | /api/v1/auth/register | Register new user | Public |
| POST | /api/v1/auth/login | Login → JWT tokens | Public |
| POST | /api/v1/auth/refresh | Refresh access token | Bearer |
| GET | /api/v1/users/me | Current user profile | Bearer |
| PUT | /api/v1/users/me | Update profile | Bearer |
| GET | /api/v1/users | All users | ADMIN |
| GET | /api/v1/users/{id} | User by ID | ADMIN, STAFF |
| POST | /api/v1/users/me/password | Change own password | Bearer (any role) |
| PATCH | /api/v1/users/{id}/role | Change a user's role | ADMIN |
| PATCH | /api/v1/users/{id}/status | Suspend / re-activate a user | ADMIN |
| GET | /api/v1/users/search?query= | Search users by email/name (paginated) | ADMIN, STAFF |

## Kafka

- Produces: user.registered (`UserRegisteredEventProducer`, `kafka/`, fires from `AuthServiceImpl.register()` after save, key = userId, value = `UserRegisteredEvent` — includes `phone` field)
- Consumes: (none)

## Planned Additions (DONE — implemented)

> All four endpoints below are implemented and tested. They are wired into the existing
> `UserController` / `UserServiceImpl`. Frontend wiring for these is described in
> `docs/e-health-insurance-frontend.md` → "Planned Additions".

### New endpoints (IMPLEMENTED)
| Method | Path | Description | Auth |
|---|---|---|---|
| POST | /api/v1/users/me/password | Change own password | Bearer (any role) |
| PATCH | /api/v1/users/{id}/role | Change a user's role | ADMIN |
| PATCH | /api/v1/users/{id}/status | Suspend / re-activate a user | ADMIN |
| GET | /api/v1/users/search?query= | Search users by email/name (paginated) | ADMIN, STAFF |

### 1. User entity (`entity/User.java`)
- Add field:
  ```java
  @Builder.Default
  @Column
  private Boolean active = true;
  ```
- Use a **nullable** column (plain `@Column`, NOT `nullable=false`): Hibernate
  `ddl-auto=update` cannot add a NOT NULL column to a table that already has rows.
  Treat `null` as active everywhere (only an explicit `false` blocks login).
- After deploying, backfill existing rows once:
  `UPDATE users SET active = true WHERE active IS NULL;`

### 2. UserDto (`dto/response/UserDto.java`)
- Add `Boolean active` as the last record component. MapStruct `UserMapper` needs no
  change (direct field-name match).

### 3. New request DTOs (`dto/request/`, records)
- `ChangePasswordRequest(@NotBlank String currentPassword, @NotBlank String newPassword)`
  — add `@Size(min = 8)` on `newPassword`.
- `ChangeRoleRequest(@NotNull UserRole role)` — `UserRole` is the infra enum
  (`ADMIN`/`STAFF`/`CUSTOMER`); Jackson binds the string value.
- `ChangeStatusRequest(@NotNull Boolean active)`.

### 4. UserRepository (`repository/UserRepository.java`)
- Add a search query (email OR first/last name, case-insensitive):
  ```java
  @Query("""
      SELECT u FROM User u
      WHERE LOWER(u.email) LIKE LOWER(CONCAT('%', :q, '%'))
         OR LOWER(u.firstName) LIKE LOWER(CONCAT('%', :q, '%'))
         OR LOWER(u.lastName) LIKE LOWER(CONCAT('%', :q, '%'))
      """)
  Page<User> search(@Param("q") String q, Pageable pageable);
  ```

### 5. UserService / UserServiceImpl
- Inject `PasswordEncoder` into `UserServiceImpl` (the `BCryptPasswordEncoder` bean
  already exists in `SecurityConfig`).
- Add methods:
  - `UserDto changePassword(String email, ChangePasswordRequest req)` — load by email
    (`authentication.getName()` is the **email** in IAM, unlike other services where it
    is the userId); if `!passwordEncoder.matches(req.currentPassword(), user.getPassword())`
    throw `BadRequestException("Current password is incorrect")`; else set
    `passwordEncoder.encode(req.newPassword())`, save, return dto.
  - `UserDto changeRole(UUID id, UserRole role)` — load by id (`NotFoundException` if
    missing), set role, save, return dto.
  - `UserDto changeStatus(UUID id, Boolean active)` — load by id, set active, save,
    return dto.
  - `PagedResponse<UserDto> searchUsers(String query, Pageable pageable)` — mirror the
    existing `getAllUsers` paging code but call `userRepository.search(query, pageable)`.

### 6. UserController (`controller/UserController.java`)
- `POST /me/password` — `Authentication` + `@Valid ChangePasswordRequest` →
  `userService.changePassword(authentication.getName(), req)`. No `@PreAuthorize`
  (any authenticated user).
- `PATCH /{id}/role` — `@PreAuthorize("hasRole('ADMIN')")`, `@PathVariable UUID id` +
  `@Valid ChangeRoleRequest` → `userService.changeRole(id, req.role())`.
- `PATCH /{id}/status` — `@PreAuthorize("hasRole('ADMIN')")` →
  `userService.changeStatus(id, req.active())`.
- `GET /search` — `@PreAuthorize("hasRole('ADMIN') or hasRole('STAFF')")`,
  `@RequestParam String query`, `Pageable` → `ApiResponse<PagedResponse<UserDto>>`.

### 7. AuthServiceImpl (`service/impl/AuthServiceImpl.java`)
- In `login(...)`, after the password check passes, reject suspended users:
  ```java
  if (Boolean.FALSE.equals(user.getActive())) {
      throw new UnauthorizedException("Account is suspended");
  }
  ```
- In `register(...)`, the `@Builder.Default` already sets `active = true`; no change
  needed beyond confirming the builder keeps it.

### Notes / deliberately skipped
- **No server-side logout.** Tokens are stateless JWTs; logout stays client-side
  (frontend clears localStorage). Adding token revocation would need a refresh-token
  store — over-engineering for the MVP.
- **No email password-reset** (`reset-request`/`reset-confirm`) — needs SMTP infra.
- **No self-guard** on role/status changes (admin demoting/suspending themselves) — keep
  it simple; the frontend simply won't surface those actions for the current user.

## Decisions & Notes
- Package root: `com.ehi.iam`.
- Spring Boot 3.2.5's Gradle plugin is incompatible with Gradle 9 (`bootJar` uses a removed `CopyProcessingSpec` API). Uses Gradle 8.10.2 instead (other repos may need this too if they hit the same issue).
- Gradle 8.10.2 doesn't support running on JDK 25, so `gradle.properties` sets `org.gradle.java.home=/opt/homebrew/opt/openjdk@17/libexec/openjdk.jdk/Contents/Home` to run the Gradle daemon itself on JDK 17 (separate from infra's Java 17 toolchain config in `~/.gradle/gradle.properties`). Must be the full `Contents/Home` path — the bare homebrew prefix works on the CLI but IntelliJ rejects it as an invalid JDK. IntelliJ's Gradle JVM (`.idea/gradle.xml`) is set to the registered JDK named `17` (same JDK).
- `io.spring.dependency-management` plugin bumped to 1.1.7 for Gradle 8/9 compatibility.
- jjwt 0.12.5 (api/impl/jackson), spring-boot-starter-security, spring-boot-starter-validation, spring-boot-starter-data-jpa, postgresql driver.
- Security setup: `JwtProperties` (`config/`, `@ConfigurationProperties(prefix = "jwt")`) binds `application.yml`'s `jwt.*` settings. `JwtProvider` (`security/`) issues/validates access & refresh tokens with claims `userId` and `role`. `JwtAuthenticationFilter` (`security/`) reads the `Authorization: Bearer` header and populates `SecurityContextHolder`. `SecurityConfig` (`config/`) is stateless, permits `/api/v1/auth/**`, requires auth elsewhere, registers `BCryptPasswordEncoder`.
- No `UserDetailsService`/`AuthenticationManager` — AuthService (Step 6) authenticates directly against `UserRepository` + `PasswordEncoder` to keep it simple.
- DTOs added beyond the original list: `RefreshRequest` (for `/api/auth/refresh`) and `UpdateUserRequest` (for `PUT /api/users/me`) — both small (≤2 fields), no `@Builder`.
- DTO package convention (project-wide, see CLAUDE.md): request DTOs in `dto/request/` (`RegisterRequest`, `LoginRequest`, `RefreshRequest`, `UpdateUserRequest`), response DTOs in `dto/response/` (`AuthResponse`, `UserDto`).
- `AuthResponse` (userId, email, role, accessToken, refreshToken — 5 fields, mixed types) and `UserDto` (id, email, firstName, lastName, role, createdAt — 6 fields) both get `@Builder` per the 5+ field rule.
- `UserMapper` (MapStruct, `mapper/`): `User` → `UserDto`, direct field-name matches, no custom mappings needed.
- `AuthService`: register (default role `CUSTOMER`, throws `DuplicateResourceException` on existing email), login (`UnauthorizedException` on bad credentials), refresh (validates refresh token via `JwtProvider`, `UnauthorizedException`/`NotFoundException`). All use infra's generic exceptions — no `IamErrorEnum` needed so far.
- `UserService`: getCurrentUser/updateProfile by email (from JWT subject), getAllUsers (paginated via `PagedResponse`), getUserById — all `NotFoundException` on missing user.
- Service layer convention (project-wide, see CLAUDE.md): interface `XxxService` in `service/`, implementation `XxxServiceImpl` in `service/impl/` (`@Service`). Controllers depend on the interface. `AuthServiceImpl`/`UserServiceImpl` are the first to follow this.
- `AuthController` (`/api/v1/auth`): register/login/refresh, all public (permitted in `SecurityConfig`), wrapped in `ApiResponse<AuthResponse>`.
- `UserController` (`/api/v1/users`): `/me` (GET/PUT) uses `Authentication.getName()` (email, set by `JwtAuthenticationFilter`) to resolve the current user; `GET /api/v1/users` requires `ROLE_ADMIN` via `@PreAuthorize`; `GET /api/v1/users/{id}` requires `ROLE_ADMIN` or `ROLE_STAFF`. List endpoint returns `ApiResponse<PagedResponse<UserDto>>` via Spring Data `Pageable`.
- API versioning (project-wide, see CLAUDE.md): all endpoints prefixed with `/api/v1/`.
- `exception/IamErrorEnum implements BaseErrorService`: `FORBIDDEN` → `"IAM-FORBIDDEN-0001"` / 403 — used for Spring Security's `AccessDeniedException` (no equivalent in infra's `BaseErrorEnum`, which tops out at `UNAUTHORIZED`=401). Not thrown via `ServiceException`; only used by `GlobalExceptionHandler` to build a consistent error response.
- `exception/GlobalExceptionHandler` (`@RestControllerAdvice`, `@Slf4j`): handles `BaseException` (uses `errorCode`/`statusCode` from the exception), `MethodArgumentNotValidException` (400, `BaseErrorEnum.VALIDATION_ERROR`, field errors in `details`), `AccessDeniedException` (403, `IamErrorEnum.FORBIDDEN`), and generic `Exception` (500, `BaseErrorEnum.INTERNAL_ERROR`). All responses are `ApiResponse<ErrorResponse>` with `success=false`, `data` = the `ErrorResponse` (status, message, details incl. `errorCode`, timestamp).
- Added `org.springframework.kafka:spring-kafka` and `spring.kafka.*` producer config (StringSerializer key, JsonSerializer value, `localhost:9092`) to `application.yml`.
- Step 10 (Build & verify): `./gradlew build` → BUILD SUCCESSFUL. `application.yml` uses `ddl-auto: validate` and `show-sql: false` (hardened from the original `update`/`true` defaults).
- Dockerfile (multi-stage): an `infra-build` stage builds+publishes `e-health-insurance-infra` (provided via Compose's `additional_contexts: infra`) to `/root/.m2`, a `build` stage compiles this service's `bootJar` against that local repo, and the runtime stage is `eclipse-temurin:17-jre-jammy`. `gradle.properties` (host-only JDK path) is deleted before building so the container's own JDK 17 is used. `.dockerignore` excludes `.gradle/`, `build/`, `out/`.
- `application-docker.yml` overrides `spring.datasource.url` → `postgres:5432/ehi_iam` and `spring.kafka.bootstrap-servers` → `kafka:29092`, activated via `SPRING_PROFILES_ACTIVE=docker` in docker-compose.

## Logging (SLF4J) — DONE

> Add `@Slf4j` only to the classes below and only the lines listed — these are security/audit
> events and failure paths, not blanket tracing. Follow the existing convention: parameterized
> `{}` placeholders, `info` for successful state changes, `warn` for rejected/suspicious actions.
> Never log passwords, password hashes, or raw JWTs.

- **`AuthServiceImpl`** (`@Slf4j`):
  - `register`: `info` after save — `"Registered new user userId={}, email={}"` (no password).
  - `login`: `warn` on bad credentials — `"Failed login attempt for email={}"`; `warn` on suspended
    account — `"Login blocked: account suspended, userId={}"`. (Security audit trail.)
  - `refresh`: `warn` on invalid/expired refresh token — `"Refresh rejected: invalid token"`.
- **`UserServiceImpl`** (`@Slf4j`) — admin privilege changes must be auditable:
  - `changeRole`: `info` — `"Role changed for userId={} -> {}"`.
  - `changeStatus`: `info` (or `warn` when suspending) — `"Status changed for userId={}, active={}"`.
  - `changePassword`: `info` on success — `"Password changed for userId={}"` (never the password);
    `warn` on wrong current password — `"Password change rejected (wrong current) for email={}"`.

## Review Findings (see root `check.md` for full detail)

- 🟡 **No self-guard on role/status change.** `changeRole`/`changeStatus` let an ADMIN demote or
  suspend themselves (frontend hides it, API allows it). Add `if (id.equals(currentUserId)) throw ...`
  so it can't be done by hand. (Documented as deliberately skipped in "Planned Additions"; revisit.)
- 🟢 **`search` Pageable uncapped** — `?size=100000` is allowed. Add `@PageableDefault(size=20)` + a
  max (applies to `getAllUsers` too; cross-cutting).
- 🟢 MapStruct `UserMapper` maps the new `active` field by name (compiles, so confirmed); `UserDto`
  correctly omits `password`.

## Testing (DONE — 30 tests, all green)

Stack: JUnit 5 + Mockito + AssertJ (unit); `@SpringBootTest` + running Compose Postgres (IT).

### Implemented test classes (all passing, 30 tests total)

- **`AuthServiceImplTest`** (8 tests): `register` hashes password + defaults CUSTOMER + throws
  `DuplicateResourceException` on dupe email + publishes `UserRegisteredEvent`; `login` throws
  `UnauthorizedException` on bad credentials and on suspended user; `refresh` validates token.
- **`UserServiceImplTest`** (6 tests): `changePassword` rejects wrong current password
  (`BadRequestException`) and encodes the new one; `changeRole`/`changeStatus` 404 on missing id;
  `searchUsers` paging shape; `getCurrentUser` resolves by email.
- **`JwtProviderTest`** (4 tests): access/refresh tokens carry `userId`/`role` claims and validate;
  token signed with wrong secret rejected; malformed token rejected.
- **`UserControllerTest`** (`@WebMvcTest`, 9 tests): ADMIN-only on `/{id}/role` and `/{id}/status`;
  ADMIN/STAFF on `/search`; any authenticated user on `/me/password`; unauthenticated blocked.
- **`IamAuthIT`** (`@SpringBootTest` + `ehi_iam_test` DB, 2 tests): register → login → `/users/me`
  full HTTP flow; suspended user → 401 on login.

### Implementation decisions / deviations
- `@WebMvcTest` doesn't auto-scan `SecurityConfig` so uses inline `@TestConfiguration
  @EnableMethodSecurity` + `@MockBean JwtProvider` to wire method security without the real JWT
  filter chain.
- IT test uses the already-running Compose PostgreSQL (`ehi_iam_test` DB, `ddl-auto: create-drop`)
  rather than Testcontainers — docker-java 3.4.1 has a known HTTP/2 incompatibility with Docker
  Engine 29.x that returns 400 from the Unix socket. `ext['testcontainers.version'] = '1.20.6'`
  is set for future use when the socket issue is resolved.
- Added `org.apache.httpcomponents.client5:httpclient5` to testImplementation so `TestRestTemplate`
  uses Apache HttpClient instead of Java's URLConnection (avoids `HttpRetryException` on 401 POST).
