# E-Sağlamlıq Sığortası — Frontend (Azərbaycan dili)

E-Health Insurance platformasının backend mikroservislərinə tam uyğun, Azərbaycan dilində hazırlanmış React SPA.

## Texnologiyalar

- React 18 + TypeScript (strict)
- Vite 5 (dev server port **5174**)
- React Router DOM 6
- Axios (JWT + avtomatik refresh interceptor)
- Əl ilə yazılmış CSS (dizayn tokenləri, UI kitabxanasız)

## İşə salma

```bash
cd e-health-insurance-frontend-az
npm install
npm run dev          # http://localhost:5174
```

Bütün `/api/*` sorğuları Vite proxy ilə API Gateway-ə (`VITE_GATEWAY_URL`, default `http://localhost:8080`) yönləndirilir. Backend-i işə salmaq üçün:

```bash
cd ../e-health-insurance-infra
docker-compose up --build
```

## Səhifələr

### Açıq
| Yol | Təsvir |
|---|---|
| `/` | Ana səhifə (landing) — üstünlüklər, plan kataloqu, qeydiyyata dəvət |
| `/login` | Daxil olma |
| `/register` | Qeydiyyat |

### Üzv (MEMBER)
| Yol | Təsvir |
|---|---|
| `/dashboard` | İdarə paneli — sığorta, iddia və bildiriş icmalı |
| `/plans` | Plan kataloqu, müqayisə (3-ə qədər), AI tövsiyəsi, satınalma |
| `/policy` | Aktiv sığorta, tarixçə, ləğv, yeniləmə, əhatə yoxlaması |
| `/claims` | İddia siyahısı (status filtri ilə) |
| `/claims/new` | Yeni iddia + sübut sənədi yükləmə |
| `/claims/:id` | İddia detalları, qərar, AI izahı, sənədlər (yüklə/endir) |
| `/chat` | AI köməkçi çatı (söhbət tarixçəsi ilə) |
| `/notifications` | E-poçt bildiriş tarixçəsi |
| `/profile` | Profil redaktəsi |

### Əməkdaş (STAFF / ADMIN)
| Yol | Təsvir |
|---|---|
| `/staff` | Baxış növbəsinin icmal paneli |
| `/staff/queue` | Manual baxış növbəsi (FIFO) |
| `/staff/claims/:id` | İddia baxışı — təsdiq/rədd, AI fraud analizi, sənəd endirmə |
| `/staff/members` | Üzv axtarışı (ID ilə) + sığorta tarixçəsi |

### Admin (ADMIN)
| Yol | Təsvir |
|---|---|
| `/admin` | Statistika paneli (aktiv sığortalar, gəlir, top planlar) |
| `/admin/plans` | Plan CRUD + prosedur qaydaları + arxivlə/aktivləşdir |
| `/admin/policies` | Bütün müqavilələrin axtarışı (filtr + səhifələmə) + üzv üçün satınalma |
| `/admin/users` | İstifadəçi axtarışı (ID ilə) + rol dəyişikliyi |

## Arxitektura qeydləri

- **Auth:** giriş/qeydiyyat token cütü qaytarır; rol JWT `role` claim-indən oxunur, profil `GET /api/iam/users/me` ilə yüklənir. 401 zamanı refresh-token avtomatik fırlanır, paralel sorğular növbəyə alınır.
- **API kontraktları** backend DTO-ları ilə birəbir uyğundur (`src/types.ts`): `coveragePercent` 0–1 əmsaldır (UI-da % göstərilir), `ClaimResponse.decision` iç-içə obyektdir, çat `conversationId` rəqəmdir.
- **Rol əsaslı marşrutlar:** `ProtectedRoute` + rola görə yönləndirmə (MEMBER → `/`, STAFF → `/staff`, ADMIN → `/admin`).
- **Sübut sənədləri:** yükləmə (multipart, maks 10 MB, PDF/JPEG/PNG/WebP) və endirmə (`.../evidence/{id}/download`) dəstəklənir.

## Məlum backend məhdudiyyətləri

- İstifadəçi siyahısı endpoint-i yoxdur — admin/staff axtarışı yalnız UUID ilə işləyir.
- Çat mesaj tarixçəsi endpoint-i yoxdur — köhnə söhbət seçildikdə yalnız yeni mesajlar göstərilir (kontekst backend-də saxlanılır).
- SMS bildirişləri backend-də stub-dur (503).
