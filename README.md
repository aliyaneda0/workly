# Workly

A job-board backend (Job, Company, Review) — a Spring Boot capstone being taken from a single
monolith through a full microservices rebuild: service discovery, API gateway, centralized config,
distributed tracing, resilience patterns, async messaging, containerization, and Kubernetes — plus
OAuth2 + JWT + role-based access control layered on top.


## Docs

- 

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
