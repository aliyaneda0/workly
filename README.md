# Workly

A job-board backend (Job, Company, Review) — a Spring Boot capstone being taken from a single
monolith through a full microservices rebuild: service discovery, API gateway, centralized config,
distributed tracing, resilience patterns, async messaging, containerization, and Kubernetes — plus
OAuth2 + JWT + role-based access control layered on top.


## Docs

- [ARCHITECTURE.md](docs/ARCHITECTURE.md) — current state of the code vs. the target end-state
- [ROADMAP.md](docs/ROADMAP.md) — phased task list, current status
- [PROGRESS.md](docs/PROGRESS.md) — dated changelog of what's actually been done
- [AUTH_GUIDE.md](docs/AUTH_GUIDE.md) — JWT/OAuth2/RBAC concepts, specific to this project
- [DECISIONS.md](docs/DECISIONS.md) — design decisions and why
- [GIT_WORKFLOW.md](docs/GIT_WORKFLOW.md) — the fetch/pull/merge/PR loop used on this project

(These docs live locally under `docs/` and aren't tracked in git — see `.gitignore`.)

## Running locally

```bash
./mvnw spring-boot:run
```

Requires a Postgres connection configured via three environment variables: `DATABASE_URL`,
`DATABASE_USERNAME`, `DATABASE_PASSWORD` (see `src/main/resources/application.properties`).

Default port: `8080`.

## Stack

Java 21 · Spring Boot 3 · Spring Data JPA · Postgres/H2 · Maven — see
[ROADMAP.md](docs/ROADMAP.md) for the full target stack (Eureka, Spring Cloud Gateway,
Config Server, OpenFeign, RabbitMQ, Resilience4j, Zipkin, Docker, Kubernetes).
