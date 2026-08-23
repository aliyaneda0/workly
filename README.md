# Workly

A job-board backend (Job, Company, Review) — a Spring Boot capstone being taken from a single
monolith through a full microservices rebuild: service discovery, API gateway, centralized config,
distributed tracing, resilience patterns, async messaging, containerization, and Kubernetes — plus
OAuth2 + JWT + role-based access control layered on top.

**Current state:** monolith, no auth wired up yet. See [docs/ARCHITECTURE.md](docs/ARCHITECTURE.md)
for exactly what exists today vs. the target design.

## Docs

- [docs/ARCHITECTURE.md](docs/ARCHITECTURE.md) — current state + target architecture
- [docs/ROADMAP.md](docs/ROADMAP.md) — phased task tracker (start here for "what's next")
- [docs/PROGRESS.md](docs/PROGRESS.md) — dated changelog of actual work done
- [docs/DECISIONS.md](docs/DECISIONS.md) — design decisions and why
- [docs/AUTH_GUIDE.md](docs/AUTH_GUIDE.md) — JWT / OAuth2 / RBAC concepts, written for a first pass at auth

## Running locally

```bash
./mvnw spring-boot:run
```

Requires a Postgres connection configured via `spring.datasource.*` in
`src/main/resources/application.properties` (currently blank — fill in local credentials, or switch
to H2 for zero-setup local dev; see ROADMAP.md Phase 1).

Default port: `8080`.

## Stack

Java 21 · Spring Boot 3 · Spring Data JPA · Postgres/H2 · Maven — see
[docs/ARCHITECTURE.md](docs/ARCHITECTURE.md) for the full target stack (Eureka, Spring Cloud Gateway,
Config Server, OpenFeign, RabbitMQ, Resilience4j, Zipkin, Docker, Kubernetes).
