# Tennis club reservations

A REST API for managing tennis courts, surfaces, customers, reservations, and user accounts. Reservations record the court, time, game type, and price.

## Tech stack

- Java 25, Spring Boot, Spring MVC and Spring Security
- JPA/Hibernate with an in-memory H2 database and Liquibase migrations
- Maven Wrapper, Lombok, springdoc OpenAPI, JUnit, Mockito, and JaCoCo

## Getting started

Install JDK 25; Maven is provided by the wrapper. Set a JWT secret of at least 32 UTF-8 bytes and start the application:

```sh
export JWT_SECRET='replace-with-a-secret-at-least-32-bytes-long'
./mvnw spring-boot:run
```

The API runs at `http://localhost:8080/api`. OpenAPI is available at `/api/openapi` and Swagger UI at `/api/swagger-ui.html`. Configuration is in [`application.yaml`](src/main/resources/application.yaml). Environment variables must be exported by your shell or deployment tooling; the application does not automatically load a `.env` file.

## Authentication

A migration always creates the development account `admin` / `admin` with the Admin role. Accounts are separate from reservation customers; admins manage them at `/api/accounts`, and the last Admin cannot be deleted or demoted.

Send Basic credentials to `POST /api/auth/login`, then use the returned `Authorization: Bearer ...` header for subsequent requests. `POST /api/auth/refresh` accepts a valid bearer token within its refresh window and returns a new token in the same header. `JWT_LIFETIME` defaults to `1h` and `JWT_REFRESH_WINDOW` to `15m`; both accept Spring duration syntax. The refresh window must be positive and no longer than the lifetime.

## Database seeding

Liquibase always creates the schema. To also seed courts and surfaces, set `SEED_DATA_ENABLED=true` before starting the application. This adds Clay (3.60 CZK/minute) for courts 1 and 2 and Hard (5.00 CZK/minute) for courts 3 and 4. The seed runs once per database; disabling it later does not remove data. Conflicting active names or court numbers cause the migration to fail; resolve the conflict and restart. The default in-memory H2 data is lost when the database is recreated.

## Architecture and project structure

The application is a monolith organized into vertical feature slices under [`src/main/java`](src/main/java/com/example/tenisclubreservations/). Each feature groups its controllers, services, DAOs, entities, and DTOs: controllers handle HTTP, services contain business logic, and DAOs use JPA `EntityManager`. Shared code and authentication live in their own packages. Liquibase changes are in [`src/main/resources/db/changelog`](src/main/resources/db/changelog/), tests in [`src/test/java`](src/test/java/), and UML diagrams in [`diagrams`](diagrams/).

## Agentic workflow

The [development pipeline](docs/orchestration.md) coordinates human decisions, agent work, and independent review:

1. **Scope and clarification:** The agent assesses the request and asks the human questions to agree on the scope and acceptance criteria.
2. **BET, when warranted:** A [BET](docs/bets/) records a substantial desired outcome, especially when scope is broad or uncertain. Small, bounded changes can proceed without one.
3. **Design and ADR:** The agent proposes a design and at least one [Architectural Decision Record (ADR)](docs/adr/) documenting the change and chosen approach. An independent Critic reviews them until approved.
4. **Human Gate 1:** The human explicitly approves the reviewed design, scope, and ADRs before implementation. Approved ADRs become accepted.
5. **Implementation and review:** The agent implements the approved scope, runs relevant unit, integration, and end-to-end tests, and submits the changes and verification evidence to the Critic.
6. **Human Gate 2:** The human reviews the implementation and explicitly approves completion. The session is then exported to [`docs/sessions`](docs/sessions/).

The Critic can request revisions but cannot approve a human gate. If implementation requires a change to the approved scope or design, the workflow returns to design review and Gate 1.

## Testing and coverage

Run unit, integration, and end-to-end tests and the coverage check with the CI command:

```sh
./mvnw clean verify
```

Run a focused test with `./mvnw -Dtest=ClassName test`. JaCoCo generates a report at `target/site/jacoco/index.html` and enforces at least 90% line coverage for the project.
