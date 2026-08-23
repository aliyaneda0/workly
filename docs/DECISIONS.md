# Decisions Log

Lightweight ADR (architecture decision record) log. Newest at the bottom of its section. Each entry:
what was decided, why, and what the alternative was.

---

## ADR-000: Discarding the old FetchJob docs

**Date:** 2026-08-22
**Status:** Done

The pre-existing `README.md` and `docs/ARCHITECTURE.md` described a finished project called
"FetchJob Platform": a separate React+Vite frontend, working JWT auth, working Google/GitHub OAuth,
`/api/auth/**` endpoints, `app.jwt.secret` actually wired up. None of that exists in this repository —
the `user` package that would back it is fully commented out and references classes that were never
created here (`ApiException`, `JobApplicationRepository`, an `application` package).

Best guess: this was carried over from an earlier/parallel personal project and never adapted after
`fetchjobapp` was renamed to `workly` (see commit `6a51ee3`).

**Decision:** Treat the old docs as non-authoritative and rewrite them from the real code. Keep the
`user` package's dead code around only until Phase 0 cleanup (see ROADMAP.md) explicitly removes it —
don't uncomment or build on it, it doesn't compile against this codebase's actual classes.

**Alternative considered:** Try to make the old FetchJob design fit this repo. Rejected — it assumes a
separate frontend project and an application/apply-to-job domain that isn't part of Workly's scope per
the course.

---

## ADR-001: Auth service topology — dedicated `auth-service`, validated at every hop

**Date:** 2026-08-22
**Status:** Proposed

**Decision:** Authentication/authorization becomes its own microservice (`auth-service`, :8084) that
owns the user table and is the only issuer of JWTs (local login + OAuth2 login via Google/GitHub).
Every other service (job, company, review) independently validates the JWT signature, expiry, and role
claims on incoming requests — they do not just trust a header set by the gateway.

**Why:**
- Matches the course's own pattern (each domain is an independent service with its own DB) — auth is
  a domain too.
- "Gateway validates, services trust the network" is a common shortcut in tutorials, but it means a
  misrouted or internal request bypasses auth entirely. Validating at every service is the standard
  zero-trust posture and is a stronger thing to say in an interview than "I put a filter on the
  gateway."
- Services can each enforce their own role rules (`@PreAuthorize("hasRole('COMPANY')")` on job-service
  write endpoints, for example) without needing to trust gateway-injected headers that could be spoofed
  if a service is ever reachable directly (e.g., during local dev, or a gateway misconfiguration).

**Alternative considered:** Gateway does all JWT validation, forwards trusted `X-User-Id`/`X-User-Role`
headers downstream, services never see the token. Rejected as the primary design — simpler, but weaker
resume story and weaker actual security. Worth *mentioning* as a pattern you understand, not the one you built.

**Signing:** Start with HS256 + a shared secret distributed through Config Server (simple, works for a
solo capstone). Track upgrading to RS256 + JWKS as a stretch item (Phase 4) — that's the version real
multi-team systems use, because it lets services verify tokens without ever holding the signing secret.

---

## ADR-002: Sequencing — auth before or after the microservices split?

**Date:** 2026-08-22
**Status:** Proposed

**Decision:** Implement JWT + OAuth2 + RBAC **inside the current monolith first**, fully working and
tested, *before* splitting into job/company/review microservices. Only after that, extract auth into
its own service in the same pass where the monolith gets split (Phase 3 in ROADMAP.md).

**Why:**
- Security config is the hardest new concept here (per your own note that you're new to JWT/OAuth). One
  app, one DB, no network hops to debug means you can see the whole request/response/token cycle in one
  place — far easier to reason about than chasing a 401 across three services and a gateway.
- Once `SecurityFilterChain`, the JWT filter, `@PreAuthorize` roles, and OAuth2 login all work and are
  covered by tests in the monolith, extracting them into `auth-service` is mostly "move this config,
  point the JWT filter in the other two services at the shared secret" — a much smaller jump than
  building auth and the split simultaneously.
- Produces two independently demoable milestones for your resume/portfolio instead of one big bang:
  "Job platform with JWT + OAuth2 + RBAC" (monolith), then "...now as a secured microservices system."

**Alternative considered:** Follow the course's exact order (split first, add auth later as its own
video/phase). Rejected for *your* path specifically, since the course already assumes auth knowledge
you're still building — not because the course order is wrong in general.

---

## ADR-003: JWT storage on the client

**Date:** 2026-08-22
**Status:** Proposed — revisit once a frontend or Postman-only demo path is chosen

**Decision:** Default to `Authorization: Bearer <token>` header, access token short-lived (~15 min),
refresh token longer-lived (~7 days) issued alongside it. Document but don't default to httpOnly-cookie
storage.

**Why:** This project's primary client for now is Postman/curl/JMeter (per the course), not a browser
frontend, so the XSS-vs-localStorage tradeoff that drives the cookie-vs-header debate doesn't fully
apply yet. Bearer header is simpler to demo and test. If a frontend is added later, revisit — see
[AUTH_GUIDE.md](AUTH_GUIDE.md#where-should-the-frontend-store-the-token) for the tradeoff writeup to
use at that point.

---

## ADR-004: Flyway over Hibernate `ddl-auto`

**Date:** 2026-08-22
**Status:** Proposed

**Decision:** Replace `spring.jpa.hibernate.ddl-auto=create-drop` with Flyway-managed SQL migrations
(`db/migration/V1__init.sql`, etc.) per service. **Timing:** not urgent in isolation, but do it right when
Phase 2 (auth) starts, before the `User`/`Role` entities are added — so `V1` captures today's schema and
`V2` captures the auth schema, keeping migration history aligned with feature history.

**Why:** `create-drop` regenerates the schema from your entities on every restart and destroys all
data — it works for a tutorial precisely because nothing in a tutorial needs to survive a restart. A
real service can't do this: schemas change over time without wiping production data, and "how do you
handle a schema migration" is a question that comes up in almost every backend interview. Flyway gives
you a real, versioned answer to that question instead of "I let Hibernate figure it out."

**Alternative considered:** Liquibase. Equivalent in capability; Flyway's plain-SQL migrations are
simpler to read in a portfolio review than Liquibase's XML/YAML changelogs, so Flyway wins here on
readability, not technical merit.

---

## ADR-005: Transactional outbox for review-service events

**Date:** 2026-08-22
**Status:** Proposed

**Decision:** When `review-service` publishes a rating-changed event to RabbitMQ (Phase 6), write the
event to an `outbox` table in the **same database transaction** as the review write, and have a
separate poller (or CDC tool like Debezium, as a further stretch) read from the outbox and publish to
RabbitMQ — rather than publishing to RabbitMQ directly inside the request handler.

**Why:** "Save to DB, then publish to the queue" has a gap: if the DB commit succeeds but the publish
call fails (RabbitMQ blip, network partition, pod restart between the two lines), the review exists but
the event is lost forever — `company-service`'s aggregate rating silently drifts out of sync with no
error anywhere. Writing the event to the outbox *in the same transaction* as the review means either
both happen or neither does; the poller's job (publish-then-mark-published, retry on failure) is then
allowed to fail and retry without ever losing an event or double-committing inconsistent state.

This is the highest-leverage single addition in the whole roadmap for demonstrating distributed-systems
understanding — it's a widely recognized pattern (search "transactional outbox pattern" for canonical
writeups), and being able to explain *why* the naive version is broken is a strong, concrete interview
answer.

**Alternative considered:** Publish directly, accept eventual inconsistency as "good enough for a
capstone." Rejected — it's not meaningfully less work to do it right, and the naive version is the kind
of shortcut an interviewer will specifically probe for ("what if the publish fails?").
