# Workly

A job-board backend (Job, Company, Review) — a Spring Boot capstone being taken from a single
monolith through a full microservices rebuild: service discovery, API gateway, centralized config,
distributed tracing, resilience patterns, async messaging, containerization, and Kubernetes.

The monolith already has production-grade auth layered in: local JWT login + OAuth2 (Google/GitHub),
rotating/revocable refresh tokens, and role-based access control (`APPLICANT`/`COMPANY`/`ADMIN`) —
built and proven here first, before the microservices split, on purpose (see
[DECISIONS.md ADR-002](docs/DECISIONS.md#adr-002-sequencing-auth-before-or-after-the-microservices-split)).

## Docs

(These docs live locally under `docs/` and aren't tracked in git — see `.gitignore`. If you're
reading this from a fresh clone, they won't be there.)

-
## Running locally

```bash
docker compose up --build -d
```

Brings up Postgres + the app on one Docker network. Needs a `.env` file first (copy
`.env.example` → `.env` and fill in real values — `.env` is gitignored). The app listens on
`localhost:8080`; Postgres is reachable at `localhost:5433` (not the Postgres default `5432` —
this project maps it there deliberately, see the `ports:` comment in `docker-compose.yml`).

To run outside Docker instead (`./mvnw spring-boot:run`), export `DATABASE_URL`,
`DATABASE_USERNAME`, `DATABASE_PASSWORD`, and `JWT_SECRET` yourself (`.env`'s values, pointed at
`localhost:5433` if Postgres is still running via `docker compose`).

## Stack

Java 21 · Spring Boot 3 · Spring Data JPA · Postgres (via Docker) · Flyway · Spring Security +
JWT (`jjwt`) + OAuth2 client · Maven — see [ROADMAP.md](docs/ROADMAP.md) for the full target stack
(Eureka, Spring Cloud Gateway, Config Server, OpenFeign, RabbitMQ, Resilience4j, Zipkin,
Kubernetes).
