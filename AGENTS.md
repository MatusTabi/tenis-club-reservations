# Repository guidance

## Build and tests
- Use Java 25 and the Maven Wrapper. Validate changes with `./mvnw clean verify` (also the CI command).
- Run a focused test with `./mvnw -Dtest=ClassName test` or `./mvnw -Dtest=ClassName#methodName test`.
- Write unit and integration tests targeting at least 90% code coverage. The current build does not configure a coverage tool or enforce this threshold.

## Application conventions
- Use Spring Boot, JPA/Hibernate and the in-memory H2 database. Implement DAOs manually with JPA `EntityManager`; do not use Spring Data `JpaRepository` or `CrudRepository` for DAOs, even though the JPA starter is present.
- Manage schema changes with Liquibase via `src/main/resources/db/changelog/db.changelog-master.yaml`. Keep Hibernate's existing `ddl-auto: validate` configuration unchanged.
- Prefix all REST endpoints with `/api`. Use request and response DTOs, thin controllers, service-layer business logic and constructor injection.
- Apply SOLID pragmatically: favor simple, explicit, readable and testable code over overengineering.
- Keep UML diagrams up to date as the implementation evolves.
