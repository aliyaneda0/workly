# FetchJob Architecture, Tech Choices, and Configuration Guide

## 1) What is built

You now have a split full-stack setup:
- **Backend API** (`fetchjobapp`) using Spring Boot
- **Frontend Web App** (`fetchjob-frontend`) using React + Vite

This separation makes VS Code workflow clean:
- Open backend and frontend in different terminals
- Independent dependency management (`maven` vs `npm`)
- Independent deployment pipelines

## 2) Why these tools are used

### Spring Boot (Backend)
- **Why**: Fast API development with built-in security, validation, and database integration.
- **Used for**:
  - REST APIs for jobs, applications, auth
  - Role-based authorization (`APPLICANT`, `COMPANY`, `ADMIN`)
  - OAuth2 login handling for applicants
  - JPA ORM for database entities/repositories

### React + Vite (Frontend)
- **Why**: Vite gives fast local startup and hot reload; React is modular and easy for portal-style apps.
- **Used for**:
  - Applicant pages (OAuth login + jobs)
  - Company pages (login/register + post jobs)
  - Admin overview page
  - Client-side routing and token-based session handling

### JWT Authentication
- **Why**: Stateless auth for APIs; frontend can call backend securely with Bearer token.
- **Used for**:
  - Company/admin local login flow
  - Applicant OAuth success flow (backend creates token and redirects with token)

### OAuth2 (Google + GitHub)
- **Why**: Applicant onboarding without password management.
- **Used for**:
  - Applicant clicks provider login
  - Backend receives callback, upserts user, issues JWT
  - Redirects to frontend callback route with token

### Local Auth for Company/Admin
- **Why**: Better control for business/admin accounts and role assignment.
- **Used for**:
  - `POST /api/auth/register/company`
  - `POST /api/auth/login`

## 3) Project architecture

```text
Desktop/
  fetchjobapp/                      # Backend API repo (Spring Boot)
    src/main/java/com/aliya/fetchjobapp/
      auth/ security/ user/ company/ job/ application/ common/ config/
    src/main/resources/
      application.properties
      application-dev.properties
      application-prod.properties
    fetchjob.code-workspace         # Opens backend + sibling frontend in VS Code
    docs/ARCHITECTURE.md

  fetchjob-frontend/                # Separate frontend repo (React + Vite)
    package.json
    vite.config.js
    .env.example
    src/
      api/                          # API client and auth header injection
      auth/                         # Auth context (token store/profile fetch/logout)
      components/                   # Shared UI/route guards (e.g., ProtectedRoute)
      layouts/                      # App layout wrapper
      pages/                        # Applicant, company, admin pages
      styles/
```

## 4) API contracts used by frontend

- `POST /api/auth/login`
- `POST /api/auth/register/company`
- `GET /api/auth/me`
- `GET /api/jobs`
- `POST /api/company/jobs`
- `POST /api/applicant/jobs/{jobId}/apply`
- `GET /api/admin/overview`

OAuth callback contract:
- Backend redirects to frontend callback URL with query params including `token`.

## 5) Configuration matrix (what, where, why)

| Config Key | Project | Local Example | Production Example | Why it is needed |
|---|---|---|---|---|
| `server.port` | Backend | `8080` | `8080/443 behind proxy` | API listening port |
| `spring.profiles.active` | Backend | `dev` | `prod` | Chooses dev/prod datasource behavior |
| `app.jwt.secret` | Backend | Base64 secret | Strong rotated Base64 secret | Signs JWT tokens |
| `app.jwt.expiration-seconds` | Backend | `86400` | `900` to `3600` typical | Token lifetime policy |
| `app.cors.allowed-origins` | Backend | `http://localhost:5173` | `https://app.yourdomain.com` | Restricts cross-origin browser access |
| `app.oauth2.frontend-callback-url` | Backend | `http://localhost:5173/oauth/callback` | `https://app.yourdomain.com/oauth/callback` | Where OAuth success redirects |
| `GOOGLE_CLIENT_ID` / `GOOGLE_CLIENT_SECRET` | Backend env | Provider app creds | Provider app creds | Enables Google OAuth login |
| `GITHUB_CLIENT_ID` / `GITHUB_CLIENT_SECRET` | Backend env | Provider app creds | Provider app creds | Enables GitHub OAuth login |
| `DB_URL` / `DB_USERNAME` / `DB_PASSWORD` | Backend env | Optional in dev | Required | Production DB connection |
| `VITE_API_BASE_URL` | Frontend | `http://localhost:8080` | `https://api.yourdomain.com` | Base URL for all frontend API calls |
| `VITE_OAUTH_GOOGLE_URL` | Frontend | `http://localhost:8080/oauth2/authorization/google` | `https://api.yourdomain.com/oauth2/authorization/google` | Starts Google OAuth flow |
| `VITE_OAUTH_GITHUB_URL` | Frontend | `http://localhost:8080/oauth2/authorization/github` | `https://api.yourdomain.com/oauth2/authorization/github` | Starts GitHub OAuth flow |

## 6) Backend security behavior

- Endpoint access:
  - Public: `GET /api/jobs/**`, auth endpoints, OAuth endpoints
  - Company/Admin: `/api/companies/**`
  - Applicant/Admin: `/api/applicant/**`
  - Admin only: `/api/admin/**`
- JWT filter reads `Authorization: Bearer <token>`.
- CORS allowed origins are now **config-driven** via `app.cors.allowed-origins`.

## 7) Runbook (local)

### A) Backend
1. Set OAuth env vars in terminal (or system env):
   - `GOOGLE_CLIENT_ID`, `GOOGLE_CLIENT_SECRET`
   - `GITHUB_CLIENT_ID`, `GITHUB_CLIENT_SECRET`
2. Run backend:
   - PowerShell: `./mvnw spring-boot:run`
3. Backend URL:
   - `http://localhost:8080`

### B) Frontend
1. Go to separate frontend project:
   - `cd C:\Users\nedaa\OneDrive\Desktop\fetchjob-frontend`
2. Create `.env` from `.env.example`.
3. Install and run:
   - `npm install`
   - `npm run dev`
4. Frontend URL:
   - `http://localhost:5173`

### C) OAuth provider setup
For each provider (Google/GitHub), set callback/redirect URI to backend callback endpoint:
- Google: `http://localhost:8080/login/oauth2/code/google`
- GitHub: `http://localhost:8080/login/oauth2/code/github`

Then ensure backend redirects to frontend callback (`app.oauth2.frontend-callback-url`).

## 8) Production checklist

- Set `spring.profiles.active=prod`.
- Provide managed Postgres credentials via env vars.
- Replace default JWT secret with a long rotated key.
- Set strict CORS to only your frontend domain.
- Use HTTPS for frontend and backend URLs.
- Register production OAuth redirect URIs at providers.
