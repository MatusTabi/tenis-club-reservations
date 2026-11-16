# BET-001 design: court and surface management

## Scope and decisions

Implement the acceptance criteria in [BET-001](BET-001.md), following the class and use-case diagrams except for access control, which is deferred as described in [ADR-001](../adr/ADR-001.md). Spring Security dependencies will be commented out with a reminder; all application endpoints will be unprotected until security is implemented.

## API

- `/api/courts` and `/api/surfaces` support POST and GET; `/{id}` supports GET, PUT, PATCH, and DELETE.
- POST returns 201 with the created representation and Location. GET and updates return 200; successful DELETE returns 204. Missing or deleted resources return 404; invalid requests return 400; duplicate active values and attempts to delete an assigned surface return 409. Referencing a missing or deleted surface is a 404.
- Court input contains `courtNumber` and `surfaceId`; responses contain `id`, `courtNumber`, and `surface` with `id`, `name`, and `pricePerMinute`. Surface input contains `name` and `pricePerMinute`; responses include these fields and `id`.
- PUT requires all editable fields. PATCH accepts a nonempty JSON object with only editable fields, distinguishes absent from null, and validates the resulting complete record. Unknown fields are rejected for all input. Numeric inputs must be JSON numbers with appropriate integer or decimal values; prices with more than two decimal places are rejected.
- Lists exclude deleted records and sort ascending by court number or by normalized surface name. Names are trimmed before storage and normalized for case-insensitive comparison. API errors include a clear message without exposing internals.

## Implementation approach

- Keep court and surface code in vertical feature packages, with thin controllers, DTOs, services, and manual `EntityManager` DAOs. Use constructor injection and transaction boundaries in services.
- Use a shared mapped base entity with ID, creation/update timestamps, and nullable removal timestamp, following the diagram. Soft-deleted rows remain in the database; all ordinary reads and references filter them out.
- Add Liquibase changes for the entities and their relationship, preserving Hibernate `ddl-auto: validate`. Enforce active uniqueness with nullable unique active keys: court number for courts and normalized name for surfaces; clear these keys on deletion so values can be reused. Keep original values for history.
- Serialize court and surface mutations using a pessimistic lock on a dedicated database guard row within each transaction. This keeps duplicate checks and the check against deleting assigned surfaces safe under concurrent writes, including assignment versus deletion. Database unique constraints provide an additional backstop; translate constraint conflicts to 409. Active references are checked while holding the guard.
- Reject invalid numbers, blank or overlong trimmed names, nonnumeric or over-precision prices, and values outside 0.00–1000.00. Do not round invalid prices silently.

## Verification

- Unit tests cover service rules, normalization, validation, PATCH presence/null semantics, and conflict handling.
- Integration tests exercise controllers, JPA mappings, Liquibase constraints, soft deletion, sorting, uniqueness and reuse, and concurrent mutations against H2.
- End-to-end tests start the application and exercise a court/surface lifecycle over HTTP, including deletion conflict and reassignment. Tests run as part of `./mvnw clean verify` with Java 25; verify discovery and report counts.
- Confirm endpoints operate without authentication, document the exposure, and run the full Maven verification command. Report any test level not run and why.
