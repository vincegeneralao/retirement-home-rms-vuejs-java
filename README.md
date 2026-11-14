# Retirement Home RMS (Vue.js + Java 25 / Spring Boot)

A retirement home roster management system with a Vue 3 frontend and a **Java 25 / Spring Boot 4** backend. It reproduces the business and functional requirements, the non-functional requirements (role-based auth, atomic open-shift claiming, ≥80% test coverage, containerised deployment) and the REST contract of the reference project — the backend is a from-scratch Java rewrite (Lombok, Spring Data JPA, Spring MVC) of the original Node/TypeScript API.

### Open Shifts

Admins publish open (unassigned) date/time slots for a client. Every open shift appears on each worker's dashboard, where it can be claimed first-come, first-served — claiming is **atomic** (a conditional `UPDATE … WHERE status = 'OPEN'` inside a transaction), so a shift already taken by another worker returns `409 SHIFT_ALREADY_TAKEN`, and a claim that would overlap the worker's existing roster is rolled back with `409 SHIFT_OVERLAP`. Once taken, the shift becomes a scheduled assignment on that worker's roster and disappears from the open list for everyone else. Admins can also directly assign a worker to an open shift or cancel an open shift.

## Tech Stack

| Layer     | Technology                                       |
|-----------|--------------------------------------------------|
| Frontend  | Vue 3, Vite, Pinia, Vue Router, Tailwind CSS v4, shadcn-vue-style UI components (cva + clsx + tailwind-merge), Lucide icons |
| Backend   | **Java 25, Spring Boot 4 (Spring MVC), Spring Data JPA / Hibernate, Maven, Lombok** |
| Database  | PostgreSQL 18 (Alpine) — H2 in-memory for tests  |
| Auth      | JWT (HS256, jjwt), BCrypt password hashing       |
| Reports   | Apache POI (daily schedule XLSX export)          |
| API Docs  | springdoc-openapi (Swagger UI at `/api/docs`)    |
| Testing   | JUnit 5 + Spring Boot Test + JaCoCo (backend), Vitest + jsdom (frontend) |
| Container | Docker Compose (db + backend + frontend/nginx)   |

## Quick Start (Docker)

```bash
cd retirement-home-rms-vuejs-java
docker compose up -d --build
```

Demo data is **seeded automatically** on first startup (only when the users table is empty), so no separate seed step is required.

- Frontend: **http://localhost:5175**
- Backend API + Swagger UI (via the nginx proxy): http://localhost:5175/api/docs
- Backend directly: **http://localhost:3001/api/docs**

### Demo Accounts

| Username     | Password      | Role  |
|--------------|---------------|-------|
| `admin`      | `Admin#2026`  | Admin |
| `grace.loke` | `Worker#2026` | Worker|
| `priya.raman`| `Worker#2026` | Worker|

## Local Development

### Backend (Java)

Requires JDK 25 and Maven 3.9+.

```bash
cd backend
# Bring up PostgreSQL (host port 5434, matches the app's defaults):
#   docker compose up -d db
mvn spring-boot:run        # http://localhost:8080  (context loads & seeds demo data)
mvn test                   # run the JUnit 5 suite
mvn verify                 # full build + JaCoCo coverage gate (>=80%)
```

The API and the frontend's dev proxy (`/api`) both target `http://localhost:8080` by default, so the two run side-by-side with no extra configuration.

### Frontend

```bash
cd frontend
npm install
npm run dev                # http://localhost:5175 (proxies /api → localhost:8080)
npm test                   # run tests
npm run test:coverage      # coverage (80% threshold enforced)
```

### Database (local dev)

```bash
docker compose up -d db
# Connection: localhost:5434, user rms, password rms_secret_2026, db rms_java
```

## Environment Variables (Backend)

| Variable         | Default                        | Description                   |
|------------------|--------------------------------|-------------------------------|
| PORT             | 8080                           | HTTP listen port              |
| DB_HOST          | localhost                      | PostgreSQL host               |
| DB_PORT          | 5434                           | PostgreSQL port               |
| DB_NAME          | rms_java                       | Database name                 |
| DB_USER          | rms                            | Database user                 |
| DB_PASSWORD      | rms_secret_2026                | Database password             |
| JWT_SECRET       | (dev fallback, **≥32 bytes required in prod**) | HS256 signing secret |
| JWT_TTL          | 8h                             | Token lifetime (e.g. 8h, 30m) |
| CORS_ORIGINS     | http://localhost:5175          | Allowed browser origins |
| SITE_TIMEZONE    | Australia/Brisbane             | Timezone for schedules ("today", rosters) |
| LOGIN_MAX_FAILED_ATTEMPTS | 5                      | Failed sign-ins tolerated per window |
| LOGIN_FAIL_WINDOW | 10m                           | Rolling window that counts failed sign-ins |
| LOGIN_LOCKOUT    | 15m                            | Sign-in lockout once the allowance is used |

The JWT issuer claim is fixed to `retirement-home-rms`. `docker-compose.yml` overrides the secret/origins and points the datasource at the `db` service.

## API Endpoints

Interactive Swagger UI (with "Try it out") is served by the backend:

- `http://localhost:5175/api/docs` — through the frontend proxy
- `http://localhost:3001/api/docs` — backend directly (Docker) / `http://localhost:8080/api/docs` (local)
- `…/api/docs.json` — raw OpenAPI 3 spec

Click **Authorize** and paste the token returned by `POST /api/auth/login` to try authenticated endpoints.

### Auth
- `POST /api/auth/login` — returns JWT + user info (`mustChangePassword` is `true` right after an admin reset)

### Admin (requires `ROLE_ADMIN`)
- `GET /api/admin/workers` — list workers with shift counts
- `POST /api/admin/workers` — create worker
- `DELETE /api/admin/workers/{workerId}` — soft-deactivate worker
- `POST /api/admin/workers/{workerId}/reset-password` — set the worker's password to blank, sign them out everywhere and flag them to change it at next sign-in (`204`)
- `GET /api/admin/workers/{workerId}/availability` — list availability windows
- `POST /api/admin/workers/{workerId}/availability` — add availability
- `DELETE /api/admin/workers/{workerId}/availability/{availabilityId}` — remove window
- `GET /api/admin/clients` — list active clients
- `POST /api/admin/clients` — create client (fullName, roomNumber, address)
- `GET /api/admin/assignments/today?date=` — assignments for a day
- `POST /api/admin/open-shifts` — publish an open shift (client, date, start/end time, notes)
- `GET /api/admin/open-shifts` — list open and taken shifts
- `POST /api/admin/open-shifts/{openShiftId}/assign` — assign a worker to an open shift
- `DELETE /api/admin/open-shifts/{openShiftId}` — cancel an open shift
- `GET /api/admin/reports/daily/download?date=` — download XLSX report

### Worker (requires `ROLE_WORKER`)
- `GET /api/worker/assignments/today?date=` — own shifts for a day
- `GET /api/worker/assignments/upcoming` — own future shifts, including claimed open shifts
- `GET /api/worker/open-shifts` — upcoming open shifts available to claim
- `POST /api/worker/open-shifts/{openShiftId}/claim` — take an open shift (`409 SHIFT_ALREADY_TAKEN` / `409 SHIFT_OVERLAP`)
- `PATCH /api/worker/assignments/{assignmentId}/status` — update own shift status (`SCHEDULED`, `IN_PROGRESS`, `COMPLETED`; `403` for other workers' shifts)
- `POST /api/worker/password/change` — set a new password (`403 INVALID_CURRENT_PASSWORD`, `400 PASSWORD_UNCHANGED`); returns a fresh JWT because the token version rotates
- `POST /api/worker/password/keep` — acknowledge the reset and keep the current (blank) password (`409 PASSWORD_RESET_NOT_PENDING` once acknowledged)

### Password reset flow

An admin can reset a worker's password to blank. The worker then signs in with an empty password and the SPA shows a modal that either sets a new password or, after a two-click warning that a blank password is not best practice, keeps it. The `password_reset_required` flag on `users` drives this, and the flag is mirrored in `localStorage.rms_password_reset_required` so the modal survives a refresh. `BCryptPasswordEncoder` rejects an empty raw secret, so `SecurityBeans` supplies a blank-tolerant encoder that verifies `""` with `BCrypt.checkpw`.

Error responses use RFC 9457–style bodies (`type`, `title`, `status`, `detail`, plus a machine-readable `code` for domain errors or an `errors` field-map for validation failures), identical to the reference API. Security hardening adds to that contract: repeated sign-in failures return `429 TOO_MANY_ATTEMPTS` with a `Retry-After` header, tokens issued before a worker is deactivated return `401`, and every API response carries baseline security headers (`Content-Security-Policy`, `X-Frame-Options`, `X-Content-Type-Options`, `Referrer-Policy`, `Cache-Control: no-store`) while the nginx-served SPA ships a strict CSP.

## Project Structure

```
retirement-home-rms-vuejs-java/
├── docker-compose.yml
├── backend/
│   ├── Dockerfile
│   ├── pom.xml
│   └── src/
│       ├── main/java/com/rms/
│       │   ├── config/        # App properties, Web/CORS, security beans, OpenAPI, seeder
│       │   ├── entity/        # JPA entities (Lombok)
│       │   ├── repository/    # Spring Data JPA repositories
│       │   ├── dto/           # Request/response records
│       │   ├── mapper/        # Entity → wire view (date/location formatting)
│       │   ├── error/         # ApiException + factories, global handler
│       │   ├── security/      # JWT service, auth filter, role interceptor
│       │   ├── service/       # Business logic (auth, directory, shifts, report)
│       │   ├── web/           # REST controllers
│       │   └── util/          # Day-bounds / date-parsing helpers
│       ├── main/resources/application.yml
│       └── test/              # JUnit 5 API + unit tests (H2)
├── frontend/
│   ├── Dockerfile
│   ├── nginx.conf
│   └── src/                   # Vue 3 SPA (api, components, pages, stores, router)
└── README.md
```

## Testing & Coverage

- **Backend:** `mvn verify` runs the JUnit 5 suite (Spring Boot Test against H2, plus unit tests) and enforces **≥80%** JaCoCo coverage on **instructions, branches and lines**.
- **Frontend:** `npm run test:coverage` enforces **≥80%** coverage (Vitest + V8).
