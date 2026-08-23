# Workly — Architecture

This document reflects the **actual state of the code**, not an aspiration. It replaces the previous
version of this file, which described a different, already-finished project ("FetchJob Platform" with
a separate React frontend and working OAuth/JWT) that does not match anything in this repository.
See [DECISIONS.md](DECISIONS.md#adr-000-discarding-the-old-fetchjob-docs) for why.

Last verified against the code: 2026-08-22.

---

## 1. What Workly is

Workly is a job-search/job-board backend, built as the capstone for a Spring Boot → microservices
video course, extended with production-grade auth (OAuth2 + JWT + RBAC) so it stands on its own as a
resume project.

Three domains:
- **Job** — job postings (title, description, location, salary range, status, belongs to a Company)
- **Company** — employer profiles (has many Jobs, has many Reviews)
- **Review** — a review of a Company, with a rating

## 2. Current state (as of this commit)

**One Spring Boot monolith.** No microservices split yet, no service discovery, no gateway, no auth.

```text
workly/
  src/main/java/com/aliya/workly/
    WorklyApplication.java
    job/       Job, JobDTO, JobStatus, JobController, JobService(+Impl), JobRepository
    company/   Company, CompanyDTO, CompanyController, CompanyService(+Impl), CompanyRepository
    review/    Review, ReviewDTO, ReviewController, ReviewService(+Impl), ReviewRepository
    exception/ GlobalExceptionHandler, ResourceNotFoundException
    user/      ⚠ dead code — see below, do not build on this
  src/main/resources/
    application.properties        # Postgres config (active), commented-out H2 + JWT/OAuth placeholders
    application-dev.properties    # commented-out H2 block
    application-prod.properties   # commented-out env-driven Postgres block
```

- Java 21, Spring Boot 3.3.5, Spring Data JPA, Bean Validation, Actuator.
- `spring-boot-starter-security`, `spring-boot-starter-oauth2-client`, and `jjwt-*` are declared in
  `pom.xml` but **commented out** — not on the classpath yet.
- Persistence: Postgres is the active `spring.datasource.*` config; H2 is present as a runtime
  dependency but its properties are commented out everywhere. No `spring.security` config exists, so
  there is currently **no authentication on any endpoint**.
- One `Dockerfile` at the repo root, packaging the single jar (`EXPOSE 8080`).
- Tests: `WorklyApplicationTests` (context load) and `JobServiceImplTest` (mocked-repository unit test).

### Entity relationships (verified from code)

```text
Company 1 ──< Job        (Job.company_id FK, @ManyToOne on Job)
Company 1 ──< Review     (Review.company_id FK, @ManyToOne LAZY on Review)
```

`Job.postedBy` and `Review.reviewedBy` are bare `Long` columns — not FKs to anything, because there is
no user table yet. Once auth lands, these should become the acting user's id (see
[Task 2.x in ROADMAP.md](ROADMAP.md)).

### Known code issues (tracked, not yet fixed)

| Issue | File | Why it matters |
|---|---|---|
| `@OneToMany` on `Company.jobs`/`Company.reviews` has no `mappedBy` | [Company.java](../src/main/java/com/aliya/workly/company/Company.java) | Hibernate creates a hidden join table instead of reusing the `company_id` FK that already exists on `Job`/`Review`. Silent schema bug. |
| Non-RESTful paths: `/jobs/post/job`, `/jobs/job/{id}`, `/companies/company/{id}` | `*Controller.java` | Reads as inexperienced REST design to a reviewer; should be `POST /jobs`, `DELETE /jobs/{id}`, `GET /companies/{id}`. |
| `user` package is fully commented out and references classes (`ApiException`, `JobApplicationRepository`) that don't exist in this codebase | [src/main/java/com/aliya/workly/user/](../src/main/java/com/aliya/workly/user/) | Leftover from a different project. Delete it and design the real `user`/auth model from scratch — don't uncomment it. |
| `@Valid` only on `JobController.createJob`, missing on Company/Review create+update | `CompanyController.java`, `ReviewController.java` | Inconsistent input validation. |

These are captured as cleanup tasks in [ROADMAP.md](ROADMAP.md) — Phase 0.

## 3. Target end-state architecture

This is the destination the course + your auth additions are building toward. Build it in the phased
order in [ROADMAP.md](ROADMAP.md) — don't attempt it all at once.

```text
                                   ┌─────────────────────┐
                                   │   Eureka Registry     │  :8761
                                   │  (service discovery)  │
                                   └──────────▲───────────┘
                                              │ register/discover
                    ┌─────────────────────────┼─────────────────────────┐
                    │                         │                         │
            ┌───────┴───────┐        ┌────────┴────────┐       ┌────────┴────────┐
 client ──▶ │  API Gateway   │──────▶ │  auth-service    │      │  config-server   │ :8888
 (Postman/  │  :8080         │        │  :8084           │      │  (Git-backed)    │
  browser)  │  - routes      │        │  - local login    │      └────────▲────────┘
            │  - load balance│        │  - OAuth2 login   │               │ pulled by every service at boot
            │  - JWT check   │        │    (Google/GitHub)│
            │    on gateway  │        │  - issues JWT      │
            │    (defense-   │        │  - owns user DB    │
            │    in-depth,   │        └────────────────────┘
            │    not the     │
            │    only check) │
            └───────┬────────┘
                     │ Feign calls carry the caller's JWT in the Authorization header
       ┌─────────────┼─────────────────┐
       ▼             ▼                 ▼
┌────────────┐ ┌────────────┐  ┌────────────┐
│ job-service │ │company-svc │  │ review-svc │
│  :8081      │ │  :8082     │  │  :8083     │
│ - own DB    │ │ - own DB   │  │ - own DB   │
│ - validates │ │ - validates│  │ - validates│
│   JWT itself│ │   JWT itself│ │   JWT itself│
│ - @PreAuthorize role checks in each service, not just at the gateway
└─────┬───────┘ └─────┬──────┘  └─────┬──────┘
      │ publish/consume rating events via RabbitMQ
      └─────────────────┴────────────────┘
                         │
                 ┌───────┴────────┐
                 │   RabbitMQ      │
                 └────────────────┘

Cross-cutting: Zipkin (tracing, every hop above), Resilience4j (circuit breaker / retry / rate
limiter on every Feign call), Actuator + Micrometer on every service.
```

### Key design decisions (see [DECISIONS.md](DECISIONS.md) for full rationale)

1. **Auth gets its own microservice (`auth-service`)**, not bolted onto the gateway or duplicated in
   every service. It owns the user table, handles local login + OAuth2 login (Google/GitHub), and is
   the only thing that *issues* tokens.
2. **Every downstream service validates the JWT itself** (signature + expiry + role claim), not just
   the gateway. The gateway checks too, as a fast-fail layer, but a compromised/misconfigured route
   should never mean an unauthenticated request reaches `job-service`. This "don't trust the network,
   verify at every hop" posture is exactly what interviewers probe for — it's the difference between
   "I called `.oauth2Login()` in a tutorial" and "I understand zero-trust service-to-service auth."
3. **Start with a shared HMAC secret (HS256)** distributed via Config Server so every service can
   verify tokens without calling `auth-service` synchronously. Upgrade to RS256 + a JWKS endpoint on
   `auth-service` later (Phase 4 stretch) — that's a genuinely resume-worthy upgrade to point to.
4. **Build and prove auth in the monolith first**, before splitting into microservices. See
   [DECISIONS.md ADR-002](DECISIONS.md#adr-002-sequencing-auth-before-or-after-the-microservices-split).

## 4. Config keys this project will use (target state)

| Key | Service | Purpose |
|---|---|---|
| `app.jwt.secret` | auth-service (source), all services (verify) | HMAC signing/verification key, pulled from Config Server |
| `app.jwt.access-token-ttl-seconds` | auth-service | Access token lifetime (short — 15 min typical) |
| `app.jwt.refresh-token-ttl-seconds` | auth-service | Refresh token lifetime (longer — 7 days typical) |
| `spring.security.oauth2.client.registration.google.client-id/secret` | auth-service | Google OAuth2 login |
| `spring.security.oauth2.client.registration.github.client-id/secret` | auth-service | GitHub OAuth2 login |
| `app.cors.allowed-origins` | gateway | Restrict browser origins |
| `spring.rabbitmq.host/port` | review-service, company-service | Rating event pub/sub |
| `spring.zipkin.base-url` | all services | Trace export |

## 5. Related docs

- [ROADMAP.md](ROADMAP.md) — phased task list, current status
- [PROGRESS.md](PROGRESS.md) — dated changelog of what's actually been done
- [AUTH_GUIDE.md](AUTH_GUIDE.md) — JWT/OAuth2/RBAC concepts, written for a first-timer, specific to this project
- [DECISIONS.md](DECISIONS.md) — design decisions and why
