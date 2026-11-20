# Workly

A job-board backend (Job, Company, Review) — a Spring Boot capstone being taken from a single
monolith through a full microservices rebuild: service discovery, API gateway, centralized config,
distributed tracing, resilience patterns, async messaging, containerization, and Kubernetes.

The monolith already has production-grade auth layered in: local JWT login + OAuth2 (Google/GitHub),
rotating/revocable refresh tokens, and role-based access control (`APPLICANT`/`COMPANY`/`ADMIN`) —
built and proven here first, before the microservices split, on purpose (see
[DECISIONS.md ADR-002](docs/DECISIONS.md#adr-002-sequencing-auth-before-or-after-the-microservices-split)).

## Docs

These docs live locally under `docs/` and aren't tracked in git (see `.gitignore`) — if you're
reading this from a fresh clone, they won't be there. [ROADMAP.md](docs/ROADMAP.md) (what's done,
what's next), [PROGRESS.md](docs/PROGRESS.md) (dated changelog), [ARCHITECTURE.md](docs/ARCHITECTURE.md),
and [DECISIONS.md](docs/DECISIONS.md) are the ones worth reading first.

## Running locally

```bash
docker compose up --build -d
```

Brings up the monolith + its Postgres, **and** the two extracted microservices so far —
[`company-service`](company-service/) and [`job-service`](job-service/), each with its own
Postgres — all on one Docker network. Needs a `.env` file first (copy `.env.example` → `.env` and
fill in real values — `.env` is gitignored).

| | Monolith | company-service | job-service |
|---|---|---|---|
| App port | `localhost:8080` | `localhost:8082` | `localhost:8081` |
| Postgres port | `localhost:5433` | `localhost:5434` | `localhost:5435` |

(None of these use the Postgres default `5432` — this machine has an unrelated native Postgres
service already bound to it; see the `ports:` comments in `docker-compose.yml`.) Neither extracted
service is called by the monolith yet — it keeps serving all `/jobs`/`/companies` traffic from its
own local tables. `job-service` *does* call `company-service` (its `CompanyClient`, over HTTP) to
validate a `companyId` — the project's first real inter-service call. See
[ARCHITECTURE.md](docs/ARCHITECTURE.md) and [DECISIONS.md ADR-010](docs/DECISIONS.md#adr-010-job-service-extraction--the-first-real-inter-service-call)
for why.

To run a service outside Docker instead (`./mvnw spring-boot:run`), export its env vars yourself —
`DATABASE_URL`/`DATABASE_USERNAME`/`DATABASE_PASSWORD`/`JWT_SECRET` for the monolith,
`COMPANY_DATABASE_URL`/`COMPANY_DATABASE_USERNAME`/`COMPANY_DATABASE_PASSWORD`/`JWT_SECRET` for
`company-service`, or `JOB_DATABASE_URL`/`JOB_DATABASE_USERNAME`/`JOB_DATABASE_PASSWORD`/
`JWT_SECRET`/`COMPANY_SERVICE_URL` for `job-service` (`.env`'s values, pointed at the matching
port above if Postgres is still running via `docker compose`).

## Stack

Java 21 · Spring Boot 3 · Spring Data JPA · Postgres (via Docker) · Flyway · Spring Security +
JWT (`jjwt`) + OAuth2 client · Maven — see [ROADMAP.md](docs/ROADMAP.md) for the full target stack
(Eureka, Spring Cloud Gateway, Config Server, OpenFeign, RabbitMQ, Resilience4j, Zipkin,
Kubernetes).
