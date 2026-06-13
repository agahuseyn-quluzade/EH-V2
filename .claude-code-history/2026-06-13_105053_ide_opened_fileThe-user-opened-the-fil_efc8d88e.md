# Claude Code Session

> **Resume in CLI:** `claude --resume efc8d88e-324b-4679-91db-ae20f91f114b`

| Field | Value |
|---|---|
| **Project** | `d:\ehfinal` |
| **Session ID** | `efc8d88e-324b-4679-91db-ae20f91f114b` |
| **Working Dir** | `d:\ehfinal` |
| **Started** | 6/13/2026, 10:48:49 AM |
| **Last Updated** | 6/13/2026, 10:50:53 AM |
| **Messages** | 50 |

---

## User <sup>6/13/2026, 10:48:49 AM</sup>

<ide_opened_file>The user opened the file d:\ehfinal\e-health-insurance-claim\src\main\java\com\ehi\claim\service\impl\ClaimServiceImpl.java in the IDE. This may or may not be related to the current task.</ide_opened_file>

bu  proyekti digitalocean-da deploy etmək istəyirəm proyektdə nə etmək lazımdı?


---

## Assistant <sup>6/13/2026, 10:48:54 AM</sup>


---

## Assistant <sup>6/13/2026, 10:48:58 AM</sup>

Sualınızı düzgün cavablandırmaq üçün əvvəlcə proyektin hazırkı vəziyyətini, xüsusən docker-compose və infra konfiqurasiyasını nəzərdən keçirim.


---

## Assistant <sup>6/13/2026, 10:48:58 AM</sup>

<details>
<summary>Tool: <code>Glob</code></summary>

```json
{
  "pattern": "**/docker-compose*.yml"
}
```

</details>


---

## Assistant <sup>6/13/2026, 10:48:58 AM</sup>

<details>
<summary>Tool: <code>Glob</code></summary>

```json
{
  "pattern": "**/Dockerfile*"
}
```

</details>


---

## User <sup>6/13/2026, 10:48:59 AM</sup>

<details>
<summary>Tool Result</summary>

```
e-health-insurance-infra\docker-compose.yml
```

</details>


---

## User <sup>6/13/2026, 10:48:59 AM</sup>

<details>
<summary>Tool Result</summary>

```
e-health-insurance-ai\Dockerfile
e-health-insurance-claim\Dockerfile
e-health-insurance-gw\Dockerfile
e-health-insurance-iam\Dockerfile
e-health-insurance-notification\Dockerfile
e-health-insurance-payment\Dockerfile
e-health-insurance-policy\Dockerfile
e-health-insurance-frontend\Dockerfile
```

</details>


---

## Assistant <sup>6/13/2026, 10:48:59 AM</sup>

<details>
<summary>Tool: <code>Read</code></summary>

```json
{
  "file_path": "d:\\ehfinal\\docs\\e-health-insurance-infra.md"
}
```

</details>


---

## User <sup>6/13/2026, 10:48:59 AM</sup>

<details>
<summary>Tool Result</summary>

```
1	# e-health-insurance-infra
2	
3	## Status: DONE
4	## Type: Shared Library (no Spring Boot plugin, plain jar)
5	
6	## What's Done
7	- [x] build.gradle
8	- [x] Enums (8 files)
9	- [x] KafkaTopics constants
10	- [x] Event DTOs (8 files)
11	- [x] Response DTOs (ApiResponse, PagedResponse, ErrorResponse)
12	- [x] Exceptions (BaseErrorService, BaseErrorEnum, BaseException + 4 subclasses, ServiceException)
13	- [x] Build & publishToMavenLocal verified
14	- [x] DateUtil
15	
16	## Enums
17	| Enum | Values |
18	|---|---|
19	| UserRole | ADMIN, STAFF, CUSTOMER | <!-- was AGENT in v3-3; renamed back to STAFF -->
20	| PolicyStatus | PENDING, ACTIVE, EXPIRED, CANCELLED |
21	| ClaimStatus | SUBMITTED, UNDER_REVIEW, APPROVED, REJECTED |
22	| ClaimType | HOSPITALIZATION, MEDICATION, DENTAL, CONSULTATION |
23	| PaymentStatus | PENDING, COMPLETED, FAILED, REFUNDED |
24	| PaymentReferenceType | POLICY_PREMIUM, CLAIM_PAYOUT |
25	| NotificationType | WELCOME, POLICY_ACTIVATED, POLICY_PENDING, CLAIM_SUBMIT
...(truncated)
```

</details>


---

## Assistant <sup>6/13/2026, 10:49:02 AM</sup>


---

## Assistant <sup>6/13/2026, 10:49:03 AM</sup>

<details>
<summary>Tool: <code>Read</code></summary>

```json
{
  "file_path": "d:\\ehfinal\\e-health-insurance-infra\\docker-compose.yml"
}
```

</details>


---

## User <sup>6/13/2026, 10:49:03 AM</sup>

<details>
<summary>Tool Result</summary>

```
1	name: e-health-insurance
2	
3	x-jwt-secret: &jwt-secret
4	  JWT_SECRET: ${JWT_SECRET:?JWT_SECRET must be set (see .env / .env.example)}
5	
6	networks:
7	  ehi-network:
8	    driver: bridge
9	
10	volumes:
11	  postgres-data:
12	  claim-uploads:
13	
14	services:
15	  postgres:
16	    image: postgres:16
17	    environment:
18	      POSTGRES_USER: postgres
19	      POSTGRES_PASSWORD: postgres
20	      POSTGRES_DB: postgres
21	    ports:
22	      - "5432:5432"
23	    volumes:
24	      - postgres-data:/var/lib/postgresql/data
25	      - ./docker/init-db.sql:/docker-entrypoint-initdb.d/init-db.sql
26	    healthcheck:
27	      test: ["CMD-SHELL", "pg_isready -U postgres"]
28	      interval: 5s
29	      timeout: 5s
30	      retries: 10
31	    networks:
32	      - ehi-network
33	
34	  zookeeper:
35	    image: confluentinc/cp-zookeeper:7.5.0
36	    environment:
37	      ZOOKEEPER_CLIENT_PORT: 2181
38	      ZOOKEEPER_TICK_TIME: 2000
39	    networks:
40	      - ehi-network
41	
42	  kafka:
43	    
...(truncated)
```

</details>


---

## Assistant <sup>6/13/2026, 10:49:03 AM</sup>

<details>
<summary>Tool: <code>Read</code></summary>

```json
{
  "file_path": "d:\\ehfinal\\e-health-insurance-iam\\Dockerfile"
}
```

</details>


---

## Assistant <sup>6/13/2026, 10:49:03 AM</sup>

<details>
<summary>Tool: <code>Read</code></summary>

```json
{
  "file_path": "d:\\ehfinal\\e-health-insurance-frontend\\Dockerfile"
}
```

</details>


---

## User <sup>6/13/2026, 10:49:03 AM</sup>

<details>
<summary>Tool Result</summary>

```
1	FROM eclipse-temurin:17-jdk-jammy AS infra-build
2	WORKDIR /infra
3	COPY --from=infra . .
4	RUN rm -f gradle.properties && chmod +x gradlew && ./gradlew publishToMavenLocal --no-daemon --console=plain
5	
6	FROM eclipse-temurin:17-jdk-jammy AS build
7	WORKDIR /app
8	COPY --from=infra-build /root/.m2 /root/.m2
9	COPY . .
10	RUN rm -f gradle.properties && chmod +x gradlew && ./gradlew bootJar -x test --no-daemon --console=plain
11	
12	FROM eclipse-temurin:17-jre-jammy
13	WORKDIR /app
14	COPY --from=build /app/build/libs/*.jar app.jar
15	EXPOSE 8081
16	ENTRYPOINT ["java", "-jar", "app.jar"]
17
```

</details>


---

## User <sup>6/13/2026, 10:49:04 AM</sup>

<details>
<summary>Tool Result</summary>

```
1	# ---- Build stage ----
2	FROM node:18-alpine AS build
3	
4	WORKDIR /app
5	
6	COPY package*.json ./
7	RUN npm ci
8	
9	COPY . .
10	RUN npm run build
11	
12	# ---- Serve stage ----
13	FROM nginx:1.27-alpine
14	
15	COPY nginx.conf /etc/nginx/conf.d/default.conf
16	COPY --from=build /app/dist /usr/share/nginx/html
17	
18	EXPOSE 80
19	
20	CMD ["nginx", "-g", "daemon off;"]
21
```

</details>


---

## Assistant <sup>6/13/2026, 10:49:13 AM</sup>


---

## Assistant <sup>6/13/2026, 10:49:13 AM</sup>

<details>
<summary>Tool: <code>Glob</code></summary>

```json
{
  "pattern": "**/.env*"
}
```

</details>


---

## User <sup>6/13/2026, 10:49:13 AM</sup>

<details>
<summary>Tool Result</summary>

```
e-health-insurance-frontend\.env.example
e-health-insurance-infra\.env.example
e-health-insurance-infra\.env
```

</details>


---

## Assistant <sup>6/13/2026, 10:49:14 AM</sup>

<details>
<summary>Tool: <code>Glob</code></summary>

```json
{
  "pattern": "e-health-insurance-frontend/nginx.conf"
}
```

</details>


---

## User <sup>6/13/2026, 10:49:14 AM</sup>

<details>
<summary>Tool Result</summary>

```
e-health-insurance-frontend\nginx.conf
```

</details>


---

## Assistant <sup>6/13/2026, 10:49:14 AM</sup>

<details>
<summary>Tool: <code>Glob</code></summary>

```json
{
  "pattern": "**/application-docker.yml"
}
```

</details>


---

## User <sup>6/13/2026, 10:49:14 AM</sup>

<details>
<summary>Tool Result</summary>

```
e-health-insurance-ai\src\main\resources\application-docker.yml
e-health-insurance-claim\build\resources\main\application-docker.yml
e-health-insurance-claim\src\main\resources\application-docker.yml
e-health-insurance-gw\src\main\resources\application-docker.yml
e-health-insurance-iam\src\main\resources\application-docker.yml
e-health-insurance-notification\src\main\resources\application-docker.yml
e-health-insurance-policy\src\main\resources\application-docker.yml
e-health-insurance-ai\build\resources\main\application-docker.yml
e-health-insurance-gw\build\resources\main\application-docker.yml
e-health-insurance-policy\build\resources\main\application-docker.yml
e-health-insurance-payment\src\main\resources\application-docker.yml
e-health-insurance-payment\build\resources\main\application-docker.yml
e-health-insurance-iam\build\resources\main\application-docker.yml
e-health-insurance-notification\build\resources\main\application-docker.yml
```

</details>


---

## Assistant <sup>6/13/2026, 10:49:14 AM</sup>

<details>
<summary>Tool: <code>Read</code></summary>

```json
{
  "file_path": "C:\\Users\\Aga\\.claude\\projects\\d--ehfinal\\memory\\project-deployment.md"
}
```

</details>


---

## User <sup>6/13/2026, 10:49:14 AM</sup>

<details>
<summary>Tool Result</summary>

```
File does not exist. Note: your current working directory is d:\ehfinal.
```

</details>


---

## Assistant <sup>6/13/2026, 10:49:17 AM</sup>

<details>
<summary>Tool: <code>Read</code></summary>

```json
{
  "file_path": "d:\\ehfinal\\e-health-insurance-infra\\.env.example"
}
```

</details>


---

## User <sup>6/13/2026, 10:49:17 AM</sup>

<details>
<summary>Tool Result</summary>

```
1	# Copy to .env and fill in real values. .env is git-ignored — never commit secrets.
2	OPENROUTER_API_KEY=
3	
4	# Payment provider: mock (default) or epoint
5	PAYMENT_PROVIDER=mock
6	EPOINT_PUBLIC_KEY=
7	EPOINT_PRIVATE_KEY=
8	EPOINT_SUCCESS_REDIRECT_URL=http://localhost:5173/payments/success
9	EPOINT_ERROR_REDIRECT_URL=http://localhost:5173/payments/error
10	
11	# AWS notifications (SES email + SNS SMS)
12	AWS_ENABLED=false
13	AWS_REGION=eu-central-1
14	AWS_SES_FROM=
15	AWS_ACCESS_KEY_ID=
16	AWS_SECRET_ACCESS_KEY=
17	
18	# JWT signing secret — REQUIRED in production, must be a strong 256-bit value.
19	JWT_SECRET=
20
```

</details>


---

## Assistant <sup>6/13/2026, 10:49:17 AM</sup>

<details>
<summary>Tool: <code>Read</code></summary>

```json
{
  "file_path": "d:\\ehfinal\\e-health-insurance-frontend\\nginx.conf"
}
```

</details>


---

## User <sup>6/13/2026, 10:49:18 AM</sup>

<details>
<summary>Tool Result</summary>

```
1	server {
2	    listen 80;
3	    server_name _;
4	
5	    root /usr/share/nginx/html;
6	    index index.html;
7	
8	    # Proxy API calls to the gateway (service name on the compose network).
9	    location /api/ {
10	        proxy_pass http://gw:8080;
11	        proxy_http_version 1.1;
12	        proxy_set_header Host $host;
13	        proxy_set_header X-Real-IP $remote_addr;
14	        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
15	        proxy_set_header X-Forwarded-Proto $scheme;
16	    }
17	
18	    # SPA fallback — let the React router handle client-side routes.
19	    location / {
20	        try_files $uri $uri/ /index.html;
21	    }
22	}
23
```

</details>


---

## Assistant <sup>6/13/2026, 10:49:18 AM</sup>

<details>
<summary>Tool: <code>Read</code></summary>

```json
{
  "file_path": "d:\\ehfinal\\e-health-insurance-frontend\\.env.example"
}
```

</details>


---

## User <sup>6/13/2026, 10:49:18 AM</sup>

<details>
<summary>Tool Result</summary>

```
1	# API Gateway ünvanı (dev proxy bu ünvana yönləndirir)
2	VITE_GATEWAY_URL=http://localhost:8080
3
```

</details>


---

## Assistant <sup>6/13/2026, 10:49:18 AM</sup>

<details>
<summary>Tool: <code>Read</code></summary>

```json
{
  "file_path": "d:\\ehfinal\\e-health-insurance-iam\\src\\main\\resources\\application-docker.yml"
}
```

</details>


---

## User <sup>6/13/2026, 10:49:18 AM</sup>

<details>
<summary>Tool Result</summary>

```
1	spring:
2	  datasource:
3	    url: jdbc:postgresql://postgres:5432/ehi_iam
4	  kafka:
5	    bootstrap-servers: kafka:29092
6
```

</details>


---

## Assistant <sup>6/13/2026, 10:49:35 AM</sup>


---

## Assistant <sup>6/13/2026, 10:49:38 AM</sup>

Proyektin əksər hissəsi artıq Docker-ə hazırdır. İki kritik məqamı yoxlayım — frontend API çağırışlarını necə qurur (prod-da `localhost` problemi olmasın) və gateway CORS konfiqurasiyası.


---

## Assistant <sup>6/13/2026, 10:49:39 AM</sup>

<details>
<summary>Tool: <code>Read</code></summary>

```json
{
  "file_path": "d:\\ehfinal\\e-health-insurance-frontend\\src\\api\\ai.ts",
  "limit": 30
}
```

</details>


---

## User <sup>6/13/2026, 10:49:39 AM</sup>

<details>
<summary>Tool Result</summary>

```
1	import { api } from "./client";
2	import { ChatMessageDto, ChatResponse, FraudAiResponse, RiskAiResponse } from "../types";
3	
4	const ROOT = "/api/v1/ai";
5	
6	export const aiApi = {
7	  // POST /api/v1/ai/chatbot — send a message, optionally continuing a session
8	  chat: (body: { message: string; sessionId?: string | null }) =>
9	    api.post<ChatResponse>(`${ROOT}/chatbot`, body).then((r) => r.data),
10	
11	  // GET /api/v1/ai/chatbot/history?sessionId= — chat history for a session (CUSTOMER)
12	  getChatHistory: (sessionId: string) =>
13	    api
14	      .get<ChatMessageDto[]>(`${ROOT}/chatbot/history?sessionId=${sessionId}`)
15	      .then((r) => r.data),
16	
17	  // GET /api/v1/ai/fraud-checks/{claimId} — fraud check result (STAFF, ADMIN)
18	  getFraudCheck: (claimId: string) =>
19	    api.get<FraudAiResponse>(`${ROOT}/fraud-checks/${claimId}`).then((r) => r.data),
20	
21	  // POST /api/v1/ai/claims/{claimId}/analyze — manual re-analysis (STAFF, ADMIN)
22	  analyzeClaim: (clai
...(truncated)
```

</details>


---

## Assistant <sup>6/13/2026, 10:49:39 AM</sup>

<details>
<summary>Tool: <code>Glob</code></summary>

```json
{
  "pattern": "e-health-insurance-frontend/src/api/*.ts"
}
```

</details>


---

## User <sup>6/13/2026, 10:49:40 AM</sup>

<details>
<summary>Tool Result</summary>

```
e-health-insurance-frontend\src\api\claim.ts
e-health-insurance-frontend\src\api\client.ts
e-health-insurance-frontend\src\api\iam.ts
e-health-insurance-frontend\src\api\notification.ts
e-health-insurance-frontend\src\api\policy.ts
e-health-insurance-frontend\src\api\ai.ts
```

</details>


---

## Assistant <sup>6/13/2026, 10:49:40 AM</sup>

<details>
<summary>Tool: <code>Glob</code></summary>

```json
{
  "pattern": "e-health-insurance-frontend/vite.config.*"
}
```

</details>


---

## User <sup>6/13/2026, 10:49:40 AM</sup>

<details>
<summary>Tool Result</summary>

```
e-health-insurance-frontend\vite.config.ts
```

</details>


---

## Assistant <sup>6/13/2026, 10:49:41 AM</sup>

<details>
<summary>Tool: <code>Grep</code></summary>

```json
{
  "pattern": "cors|allowedOrigin|CorsConfig|setAllowedOrigin",
  "path": "d:\\ehfinal\\e-health-insurance-gw",
  "output_mode": "files_with_matches",
  "-i": true
}
```

</details>


---

## User <sup>6/13/2026, 10:49:42 AM</sup>

<details>
<summary>Tool Result</summary>

```
No files found
```

</details>


---

## Assistant <sup>6/13/2026, 10:49:44 AM</sup>

<details>
<summary>Tool: <code>Read</code></summary>

```json
{
  "file_path": "d:\\ehfinal\\e-health-insurance-frontend\\src\\api\\client.ts"
}
```

</details>


---

## User <sup>6/13/2026, 10:49:44 AM</sup>

<details>
<summary>Tool Result</summary>

```
1	import axios, {
2	  AxiosError,
3	  AxiosInstance,
4	  AxiosRequestConfig,
5	  InternalAxiosRequestConfig,
6	} from "axios";
7	
8	const TOKEN_KEY = "ehi.az.accessToken";
9	const REFRESH_KEY = "ehi.az.refreshToken";
10	const USER_ID_KEY = "ehi.az.userId";
11	const USER_ROLE_KEY = "ehi.az.userRole";
12	
13	export const tokenStore = {
14	  getAccess: () => localStorage.getItem(TOKEN_KEY),
15	  getRefresh: () => localStorage.getItem(REFRESH_KEY),
16	  set(access: string, refresh: string) {
17	    localStorage.setItem(TOKEN_KEY, access);
18	    localStorage.setItem(REFRESH_KEY, refresh);
19	  },
20	  clear() {
21	    localStorage.removeItem(TOKEN_KEY);
22	    localStorage.removeItem(REFRESH_KEY);
23	  },
24	};
25	
26	export const userInfoStore = {
27	  getId: () => localStorage.getItem(USER_ID_KEY),
28	  getRole: () => localStorage.getItem(USER_ROLE_KEY),
29	  set(id: string, role: string) {
30	    localStorage.setItem(USER_ID_KEY, id);
31	    localStorage.setItem(USER_ROLE_KEY, role);
32
...(truncated)
```

</details>


---

## Assistant <sup>6/13/2026, 10:49:44 AM</sup>

<details>
<summary>Tool: <code>Read</code></summary>

```json
{
  "file_path": "d:\\ehfinal\\e-health-insurance-gw\\src\\main\\resources\\application.yml"
}
```

</details>


---

## User <sup>6/13/2026, 10:49:44 AM</sup>

<details>
<summary>Tool Result</summary>

```
1	server:
2	  port: 8080
3	
4	spring:
5	  application:
6	    name: e-health-insurance-gw
7	  cloud:
8	    gateway:
9	      routes:
10	        - id: auth
11	          uri: http://localhost:8081
12	          predicates:
13	            - Path=/api/v1/auth/**
14	        - id: users
15	          uri: http://localhost:8081
16	          predicates:
17	            - Path=/api/v1/users/**
18	        - id: plans
19	          uri: http://localhost:8082
20	          predicates:
21	            - Path=/api/v1/plans/**
22	        - id: policies
23	          uri: http://localhost:8082
24	          predicates:
25	            - Path=/api/v1/policies/**
26	        - id: claims
27	          uri: http://localhost:8083
28	          predicates:
29	            - Path=/api/v1/claims/**
30	        - id: payments
31	          uri: http://localhost:8084
32	          predicates:
33	            - Path=/api/v1/payments/**
34	        - id: ai
35	          uri: http://localhost:8085
36	          predicates:
37	       
...(truncated)
```

</details>


---

## Assistant <sup>6/13/2026, 10:50:07 AM</sup>


---

## Assistant <sup>6/13/2026, 10:50:42 AM</sup>

Hər şeyi yoxladım. Yaxşı xəbər: **proyekt artıq demək olar tam Docker-ə hazırdır** — `docker-compose.yml` bütün servisləri qurur, frontend nginx `/api/` sorğularını daxili olaraq `gw:8080`-ə yönləndirir, frontend `baseURL: ""` (nisbi yollar) işlədir, yəni prod-da `localhost` problemi yoxdur və eyni-origin olduğu üçün CORS problemi də yoxdur. Yəni böyük kod dəyişikliyi lazım deyil — əsas iş infrastruktur və konfiqurasiyadır.

## Tövsiyə etdiyim model: tək Droplet + docker-compose

App Platform və ya Kubernetes bu proyektə uyğun gəlmir, çünki:
- Multi-repo qurulumdur və compose `additional_contexts` (BuildKit) ilə qonşu `infra` repo-suna istinad edir — App Platform bunu dəstəkləmir.
- Kafka + Zookeeper özün-host edirsən (App Platform-da əziyyətdir).

Ona görə **tək Droplet üzərində `docker compose up`** ən təbii yoldur.

**Droplet ölçüsü:** 8 Spring servisi + Kafka + Zookeeper + Postgres bir maşında. **Minimum 8 GB RAM / 4 vCPU** (məsələn `s-4vcpu-8gb`). 4 GB ilə OOM/swap olacaq. Üstəlik 7 Java servisini droplet üzərində build etmək ağırdır — kiçik droplet build zamanı çökə bilər, ona görə ya build üçün müvəqqəti böyük droplet, ya da DO Container Registry-yə image push etmək daha rahatdır.

## Proyekt tərəfində edilməli işlər (checklist)

**1. `.env` faylı (real dəyərlərlə) — kod yox, sirlər:**
- `JWT_SECRET` — güclü 256-bit dəyər (hazırda compose məcbur edir, boşdursa fail olur ✅)
- `OPENROUTER_API_KEY`
- `PAYMENT_PROVIDER=epoint` + `EPOINT_PUBLIC_KEY` / `EPOINT_PRIVATE_KEY`
- `AWS_ENABLED=true` + `AWS_REGION` / `AWS_SES_FROM` / `AWS_ACCESS_KEY_ID` / `AWS_SECRET_ACCESS_KEY`

**2. EPOINT redirect URL-ləri** — `docker-compose.yml`-də hazırda `localhost:5173`-ə baxır. Real domenə dəyişməli:
```
EPOINT_SUCCESS_REDIRECT_URL=https://senin-domen.az/payments/success
EPOINT_ERROR_REDIRECT_URL=https://senin-domen.az/payments/error
```

**3. HTTPS / reverse proxy** — bu ən vacib əlavədir. Hazırda frontend `:80`, gateway `:8080` host-a açıqdır, TLS yoxdur. Prod üçün qarşıya bir reverse proxy (məsələn **Caddy** — Let's Encrypt sertifikatını avtomatik alır) qoyub yalnız `80/443` açmaq lazımdır. Caddy → frontend container, frontend nginx isə artıq `/api/`-ni gw-yə proxy edir.

**4. Postgres parolu** — compose-da `postgres/postgres` hardcoded-dur. İki variant: ya compose-da güclü parol + `.env`, ya da **DO Managed PostgreSQL** (avtomatik backup, daha təhlükəsiz; bu halda servislərin `application-docker.yml`-dəki `jdbc:postgresql://postgres:5432/...` ünvanını managed DB host-una dəyişmək lazımdır).

**5. Liquibase migrasiyaları** — servislər `ddl- to: validate` işlədir, yəni cədvəlləri Liquibase yaratmalıdır. İlk deploy-dan əvvəl migrasiyaların düzgün işlədiyini təsdiqləmək lazımdır (CLAUDE.md-də notification hələ `update`-dədir — onu da yoxlamaq lazım).

**6. Firewall** — DO Cloud Firewall ilə yalnız `22, 80, 443` açıq; Postgres/Kafka/servis portları bağlı.

## Qərar verməli olduğun məqamlar

Bunları seçsən, dərhal konkret faylları (Caddy konfiqurasiyası, `.env.example` yeniləməsi, deploy script) hazırlaya bilərəm:


---

## Assistant <sup>6/13/2026, 10:50:53 AM</sup>

<details>
<summary>Tool: <code>AskUserQuestion</code></summary>

```json
{
  "questions": [
    {
      "question": "PostgreSQL-i necə host edək?",
      "header": "Verilənlər bazası",
      "multiSelect": false,
      "options": [
        {
          "label": "Compose-da self-hosted",
          "description": "Mövcud quruluş saxlanılır, sadəcə güclü parol qoyulur. Ən sadə, pulsuz, amma backup-ı özün idarə edirsən."
        },
        {
          "label": "DO Managed PostgreSQL",
          "description": "Avtomatik backup, daha təhlükəsiz, ayrıca ödənişli. Servislərin DB ünvanını dəyişmək lazımdır."
        }
      ]
    },
    {
      "question": "HTTPS/TLS üçün hansı yanaşma?",
      "header": "TLS",
      "multiSelect": false,
      "options": [
        {
          "label": "Caddy reverse proxy",
          "description": "Let's Encrypt sertifikatını avtomatik alır və yeniləyir. Compose-a bir container əlavə olunur. Ən az əziyyət."
        },
        {
          "label": "Nginx + Certbot",
          "description": "Daha çox əl ilə konfiqurasiya, amma daha çox nəzarət."
        },
        {
          "label": "DO Load Balancer",
          "description": "DO-nun idarəolunan LB-si TLS-i həll edir, ayrıca ödənişli."
        }
      ]
    },
    {
      "question": "Image-ləri harada build edək?",
      "header": "Build",
      "multiSelect": false,
      "options": [
        {
          "label": "Droplet üzərində",
          "description": "Repolar droplet-ə klonlanır, orada build olunur. Sadə, amma böyük droplet/RAM tələb edir."
        },
        {
          "label": "DO Container Registry",
          "description": "Image-lər lokal/CI-də build edilib registry-yə push olunur, droplet sadəcə pull edir. Daha təmiz, kiçik droplet kifayətdir."
        }
      ]
    }
  ]
}
```

</details>


---
