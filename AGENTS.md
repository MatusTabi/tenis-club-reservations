# Repository guidance

## Build and tests
- Use Java 25 and the Maven Wrapper. Validate changes with `./mvnw clean verify` (also the CI command).
- Run a focused test with `./mvnw -Dtest=ClassName test` or `./mvnw -Dtest=ClassName#methodName test`.
- Write unit and integration tests targeting at least 90% code coverage. The current build does not configure a coverage tool or enforce this threshold.

## Application conventions
- Use a simple monolith organized into vertical feature slices. Keep each feature's controllers, services, DAOs, entities and DTOs together in one package.
- Use Spring Boot, JPA/Hibernate and the in-memory H2 database. Implement DAOs manually with JPA `EntityManager`; do not use Spring Data `JpaRepository` or `CrudRepository` for DAOs, even though the JPA starter is present.
- Manage schema changes with Liquibase via `src/main/resources/db/changelog/db.changelog-master.yaml`. Keep Hibernate's existing `ddl-auto: validate` configuration unchanged.
- Prefix all REST endpoints with `/api`. Use request and response DTOs, thin controllers, service-layer business logic and constructor injection.
- Apply SOLID pragmatically: favor simple, explicit, readable and testable code over overengineering.

## Coding style
- Follow the [Google Java Style Guide](https://google.github.io/styleguide/javaguide.html) and [Spring Framework code style](https://github.com/spring-projects/spring-framework/blob/main/CONTRIBUTING.md); prefer Spring's style where they conflict.

## Agentic workflow
- Follow `docs/orchestration.md` for the development pipeline.
- Use the `create-bet` skill when creating a new BET.
- Never assume human approval or bypass human gates.