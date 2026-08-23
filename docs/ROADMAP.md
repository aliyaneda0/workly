# Roadmap / Task Tracker

Check items off as you finish them (`[x]`). Log the actual date + what happened in
[PROGRESS.md](PROGRESS.md) when you close out a phase — don't just check the box silently.

Status legend: 🔲 not started · 🔶 in progress · ✅ done

---

## Phase 0 — Cleanup (do this before anything else) 🔲

- [ ] Delete the dead `src/main/java/com/aliya/workly/user/` package (fully commented out, references
      nonexistent classes — see [ARCHITECTURE.md §2](ARCHITECTURE.md#known-code-issues-tracked-not-yet-fixed))
- [ ] Fix `Company.jobs` / `Company.reviews` — add `mappedBy = "company"` to both `@OneToMany`
      relations so they use the existing `company_id` FK instead of a phantom join table
- [ ] Normalize REST paths: `POST /jobs` (not `/jobs/post/job`), `DELETE /jobs/{id}` (not
      `/jobs/job/{id}`), `GET/PUT/DELETE /companies/{id}` (not `/companies/company/{id}`)
- [ ] Add `@Valid` to `CompanyController` and `ReviewController` create/update methods (Job already has it)
- [ ] Rewrite root `README.md` to describe Workly accurately (in progress alongside this roadmap)

## Phase 1 — Monolith hardening 🔲

- [ ] Add a service-layer unit test for `CompanyServiceImpl` and `ReviewServiceImpl` (only Job has one today)
- [ ] Add `@ControllerAdvice` coverage check — confirm `GlobalExceptionHandler` handles validation
      errors (`MethodArgumentNotValidException`) with a clean 400 body, not just `ResourceNotFoundException`
- [ ] Decide and document one persistence path for local dev: Postgres via Docker, or H2 — right now
      both are half-configured (H2 properties commented out in `application.properties`/`-dev`). Pick one,
      delete the other's leftover comments.
- [ ] Replace `spring.jpa.hibernate.ddl-auto=create-drop` with **Flyway migrations**
      (`db/migration/V1__init.sql`). Not urgent this exact week — but do it **right when Phase 2 starts**,
      before adding the `User`/`Role` entities, not after. That way `V1` = today's job/company/review
      schema and `V2__add_users_and_roles.sql` lands exactly when auth does — your migration history ends
      up documenting your feature history. Waiting until after the Phase 3 microservices split means
      retrofitting migrations into three separate service repos instead of one. See
      [DECISIONS.md ADR-004](DECISIONS.md#adr-004-flyway-over-hibernate-ddl-auto).

## Phase 2 — Auth in the monolith (JWT + OAuth2 + RBAC) 🔲

Built and tested here **before** the microservices split — see
[DECISIONS.md ADR-002](DECISIONS.md#adr-002-sequencing-auth-before-or-after-the-microservices-split) for why.
Read [AUTH_GUIDE.md](AUTH_GUIDE.md) first if any of these terms are new.

- [ ] Uncomment/add `spring-boot-starter-security`, `spring-boot-starter-oauth2-client`, `jjwt-*` in `pom.xml`
- [ ] (Tier 2 add-on, do alongside refresh tokens below) Refresh token **rotation + revocation**: on
      `POST /auth/refresh`, invalidate the old refresh token and issue a new one; keep a revocation
      list (Redis or a DB table) so logout actually revokes access instead of just deleting the client's
      copy — a plain JWT can't be un-issued on its own, this is how you solve that. See AUTH_GUIDE.md.
- [ ] Design the real `User` entity (id, email, passwordHash nullable, fullName, role, authProvider, createdAt)
      and `Role` enum — decide roles for *this* project (suggest: `APPLICANT`, `COMPANY`, `ADMIN`)
- [ ] `SecurityFilterChain` config: which endpoints are public (`GET /jobs/**`, `GET /companies/**`),
      which need any authenticated user, which need a specific role
- [ ] Local auth: `POST /auth/register`, `POST /auth/login` (password hashed with `BCryptPasswordEncoder`)
- [ ] JWT issuing + a `JwtAuthenticationFilter` that reads `Authorization: Bearer <token>` and populates
      the `SecurityContext`
- [ ] Refresh token flow: `POST /auth/refresh`
- [ ] OAuth2 login (Google first, then GitHub) — on successful OAuth2 login, upsert a `User` row and
      issue the same JWT format as local login, so downstream code doesn't care how the user logged in
- [ ] Role-based authorization: `@PreAuthorize` on write endpoints — e.g. only `COMPANY`/`ADMIN` can
      `POST /jobs`, only the review's author or `ADMIN` can update/delete a `Review`
- [ ] Wire `postedBy` (Job) and `reviewedBy` (Review) to the authenticated user's id instead of an
      unvalidated request field
- [ ] Tests: `@WithMockUser` tests per role for at least one protected endpoint per module; a real
      login→use-token integration test

## Phase 3 — Split into microservices 🔲

- [ ] Extract `job`, `company`, `review` into standalone Spring Boot apps (own `pom.xml`, own port:
      8081/8082/8083), each with its own database
- [ ] Extract `auth-service` (:8084) from the monolith's security code — same JWT format, same shared
      secret, now the *only* issuer
- [ ] Stand up Eureka Server (:8761); all four services + gateway register with it
- [ ] Inter-service calls: start with `RestTemplate` + DTOs + streams (matches course), then refactor
      to OpenFeign — carry the caller's `Authorization` header through with a Feign `RequestInterceptor`
- [ ] Every service (not just the gateway) validates incoming JWTs — see
      [DECISIONS.md ADR-001](DECISIONS.md#adr-001-auth-service-topology--dedicated-auth-service-validated-at-every-hop)

## Phase 4 — Config Server, Gateway, RBAC hardening 🔲

- [ ] Spring Cloud Config Server backed by a Git repo, `dev`/`prod` profiles per service
- [ ] Spring Cloud Gateway as single entry point, load-balanced routes to each service via Eureka
- [ ] Gateway-level JWT pre-check (fast fail) — defense in depth, not the only check
- [ ] Stretch: upgrade JWT signing from HS256/shared-secret to RS256 with a JWKS endpoint on
      `auth-service`, so services verify with a public key instead of holding the signing secret

## Phase 5 — Observability & resilience 🔲

- [ ] Zipkin + Micrometer Tracing across all services and the gateway
- [ ] Resilience4j on every Feign call: circuit breaker + fallback, retry, rate limiter
- [ ] JMeter script that actually trips the rate limiter, to demo it

## Phase 6 — Async messaging 🔲

- [ ] RabbitMQ: `review-service` publishes a rating-changed event on review create/update/delete;
      `company-service` consumes it and updates the company's aggregate rating
- [ ] **Transactional outbox pattern** for the above, instead of publishing directly: write the event
      to an `outbox` table in the *same DB transaction* as the review write, then a separate poller (or
      Debezium, if you want to go further) publishes from the outbox to RabbitMQ. Naive "save then
      publish" loses events silently if the publish fails after the DB commit — the outbox closes that
      gap. This is the single highest-leverage distributed-systems item in this project: it's a genuine
      senior-level pattern, and being able to explain *why* naive pub/sub is broken is a strong interview
      moment. See [DECISIONS.md ADR-005](DECISIONS.md#adr-005-transactional-outbox-for-review-service-events).
- [ ] Stretch: `auth-service` publishes a "user registered" event; a consumer sends a welcome email
      (or just logs it) — shows async messaging isn't only for the review→company path

## Phase 7 — Packaging & orchestration 🔲

- [ ] Each service: Dockerfile via Paketo Buildpacks (`mvn spring-boot:build-image`)
- [ ] `docker-compose.yml` bringing up all services + Postgres (one DB per service or one instance,
      multiple schemas — decide and document) + RabbitMQ + Eureka + Config Server + Gateway + Zipkin
- [ ] Deploy the whole stack to Minikube: Deployments + Services + ConfigMaps/Secrets for each piece

## Phase 8 — Resume differentiation (pick a few per tier, don't try all of them) 🔲

Full reasoning for why each tier matters for 2026 hiring is in the chat/PROGRESS.md 2026-08-22 entry.
Short version: frameworks alone don't differentiate anymore — everyone's tutorial project has them.
What differentiates is proof you understand *consequences* (what breaks, what you did about it, why).

**Tier 1 — table stakes (a reviewer notices immediately if these are missing):**
- [ ] Flyway migrations (see Phase 1 — do this one early, not last)
- [ ] OpenAPI/Swagger docs on every service (`springdoc-openapi`)
- [ ] GitHub Actions CI: build + test on every push, matrix across services

**Tier 2 — engineering maturity (separates you from tutorial-followers):**
- [ ] Refresh token rotation + revocation (see Phase 2)
- [ ] Testcontainers-based integration tests (real Postgres/RabbitMQ in CI, not just mocked repos)
- [ ] Pagination + filtering + search on `GET /jobs` (by location, salary range, status)
- [ ] Structured JSON logging + correlation/trace id in every log line

**Tier 3 — distributed-systems depth (impressive if you can explain it, don't add what you can't defend):**
- [ ] Transactional outbox pattern (see Phase 6 — this is the standout item)
- [ ] Redis: cache hot `GET /companies/{id}` reads, and/or the refresh-token revocation list
- [ ] Prometheus + Grafana dashboard (Actuator already exposes the metrics)
- [ ] Deploy one environment to AWS: ECR for images, EKS instead of Minikube — a live URL beats a
      local `docker-compose.yml` every time

**Tier 4 — presentation (recruiters skim for ~20 seconds; sell it visually):**
- [ ] A real architecture diagram (not ASCII) in the README
- [ ] A 60–90 second demo GIF/screen recording of the Postman flows
- [ ] Published Postman collection anyone can import and run

---

## Right now

**You are here:** Phase 0 not started. Recommended next action: work through Phase 0 (cleanup) then
Phase 2 (auth in the monolith) — Phase 1 items can interleave as you touch each module. Skip straight
to Phase 3 only once login, JWT validation, refresh, OAuth2, and role checks all work locally against
the monolith.
