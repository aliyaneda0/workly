# Progress Log

Append an entry every time you close out meaningful work — don't just tick boxes in ROADMAP.md
silently. Newest entry at the top. Keep entries short: what changed, what you learned/decided, what's
next.

---

## 2026-08-22 — Baseline audit + docs set up

**What happened:** No code changed. Audited the existing repo against the video course summary and
the user's goal of adding OAuth2 + JWT + RBAC. Set up `docs/ARCHITECTURE.md`, `docs/ROADMAP.md`,
`docs/DECISIONS.md`, `docs/AUTH_GUIDE.md`, this file, and rewrote the root `README.md`.

**Findings:**
- Confirmed the project is still a single monolith (Job/Company/Review modules only) — the
  microservices split described in the course hasn't started.
- The previous `README.md`/`docs/ARCHITECTURE.md` described an unrelated, already-finished project
  ("FetchJob Platform" with a separate frontend and working auth) that doesn't match this codebase.
  Replaced — see [DECISIONS.md ADR-000](DECISIONS.md#adr-000-discarding-the-old-fetchjob-docs).
- Found dead, non-compiling scaffold code in `src/main/java/com/aliya/workly/user/` — fully commented
  out, references classes that don't exist here. Flagged for deletion in ROADMAP.md Phase 0.
- Found a real entity-mapping bug: `Company.jobs`/`Company.reviews` `@OneToMany` missing `mappedBy`,
  creates a phantom join table instead of reusing the `company_id` FK. Flagged in ROADMAP.md Phase 0.
- `pom.xml` has Security/OAuth2/JJWT dependencies commented out, not yet in use.

**Decided:** Build JWT + OAuth2 + RBAC into the monolith first, split into microservices after — see
[DECISIONS.md ADR-002](DECISIONS.md#adr-002-sequencing-auth-before-or-after-the-microservices-split).

**Next:** Phase 0 cleanup (delete dead `user` package, fix the mapping bug, normalize REST paths),
then start Phase 2 (real auth design) once cleanup is done.

---

<!--
Template for new entries:

## YYYY-MM-DD — Short title

**What happened:** ...
**Learned/decided:** ...
**Blockers:** ...
**Next:** ...
-->
