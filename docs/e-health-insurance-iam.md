# e-health-insurance-iam

## Status: DONE
## Port: 8081
## Database: ehi_iam

## What's Done
- [x] build.gradle
- [x] Entity: User
- [x] Repository: UserRepository
- [x] Service: AuthService, UserService (interfaces in `service/`, impls in `service/impl/`)
- [x] Controller: AuthController, UserController
- [x] DTO: RegisterRequest, LoginRequest, RefreshRequest, UpdateUserRequest (`dto/request/`), AuthResponse, UserDto (`dto/response/`) + UserMapper
- [x] Security: SecurityConfig, JwtProvider, JwtAuthenticationFilter, JwtProperties
- [x] Kafka producer: UserRegisteredEvent → user.registered
- [x] GlobalExceptionHandler
- [x] application.yml
- [x] Dockerfile

## Entities
### User (`users` table)
| Field | Type | Notes |
|---|---|---|
| id | UUID | PK, generated |
| email | String | unique, not null |
| password | String | hashed, not null |
| firstName | String | not null |
| lastName | String | not null |
| role | UserRole (infra enum) | not null, stored as STRING |
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
| GET | /api/v1/users/{id} | User by ID | ADMIN, AGENT |

## Kafka
- Produces: user.registered (`UserRegisteredEventProducer`, `kafka/`, fires from `AuthServiceImpl.register()` after save, key = userId, value = `UserRegisteredEvent`)
- Consumes: (none)

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
- `UserController` (`/api/v1/users`): `/me` (GET/PUT) uses `Authentication.getName()` (email, set by `JwtAuthenticationFilter`) to resolve the current user; `GET /api/v1/users` requires `ROLE_ADMIN` via `@PreAuthorize`; `GET /api/v1/users/{id}` requires `ROLE_ADMIN` or `ROLE_AGENT`. List endpoint returns `ApiResponse<PagedResponse<UserDto>>` via Spring Data `Pageable`.
- API versioning (project-wide, see CLAUDE.md): all endpoints prefixed with `/api/v1/`.
- `exception/IamErrorEnum implements BaseErrorService`: `FORBIDDEN` → `"IAM-FORBIDDEN-0001"` / 403 — used for Spring Security's `AccessDeniedException` (no equivalent in infra's `BaseErrorEnum`, which tops out at `UNAUTHORIZED`=401). Not thrown via `ServiceException`; only used by `GlobalExceptionHandler` to build a consistent error response.
- `exception/GlobalExceptionHandler` (`@RestControllerAdvice`, `@Slf4j`): handles `BaseException` (uses `errorCode`/`statusCode` from the exception), `MethodArgumentNotValidException` (400, `BaseErrorEnum.VALIDATION_ERROR`, field errors in `details`), `AccessDeniedException` (403, `IamErrorEnum.FORBIDDEN`), and generic `Exception` (500, `BaseErrorEnum.INTERNAL_ERROR`). All responses are `ApiResponse<ErrorResponse>` with `success=false`, `data` = the `ErrorResponse` (status, message, details incl. `errorCode`, timestamp).
- Added `org.springframework.kafka:spring-kafka` and `spring.kafka.*` producer config (StringSerializer key, JsonSerializer value, `localhost:9092`) to `application.yml`.
- Step 10 (Build & verify): `./gradlew build` → BUILD SUCCESSFUL (compile, processResources, classes, bootJar, jar, assemble all pass; no test sources yet so `test`/`check` are NO-SOURCE/UP-TO-DATE). A full `bootRun` smoke test was not run since this environment has no local Postgres (`ehi_iam` on 5432) or Kafka (9092) available — needed once those are provisioned (e.g. via docker-compose).
- Dockerfile (multi-stage): an `infra-build` stage builds+publishes `e-health-insurance-infra` (provided via Compose's `additional_contexts: infra`) to `/root/.m2`, a `build` stage compiles this service's `bootJar` against that local repo, and the runtime stage is `eclipse-temurin:17-jre-jammy`. `gradle.properties` (host-only JDK path) is deleted before building so the container's own JDK 17 is used. `.dockerignore` excludes `.gradle/`, `build/`, `out/`.
- `application-docker.yml` overrides `spring.datasource.url` → `postgres:5432/ehi_iam` and `spring.kafka.bootstrap-servers` → `kafka:29092`, activated via `SPRING_PROFILES_ACTIVE=docker` in docker-compose.
