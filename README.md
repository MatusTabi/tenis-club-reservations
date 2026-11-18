# Tennis club reservations

Liquibase always creates the database schema. Initial courts and surfaces are disabled by default. To seed a fresh database, set `SEED_DATA_ENABLED=true` in the environment before starting the application (for example, `SEED_DATA_ENABLED=true ./mvnw spring-boot:run`). A `.env` file must be loaded into the environment by your shell or deployment tooling; the application does not load it automatically.

Seeding adds Clay (3.60 CZK/minute) for courts 1 and 2 and Hard (5.00 CZK/minute) for courts 3 and 4. Liquibase runs the seed only once per database. Disabling it later does not remove data. Conflicting active surface names or court numbers cause the migration to fail; resolve the conflict and restart to retry. The default H2 database is in-memory, so its data does not survive recreation.

## Authentication

Set `JWT_SECRET` to a secret of at least 32 UTF-8 bytes before starting. `JWT_LIFETIME` defaults to `1h` and `JWT_REFRESH_WINDOW` to `15m`; both accept Spring duration syntax. The refresh window must be positive and no longer than the lifetime.

A separate unconditional migration creates the development account `admin` / `admin` with the Admin role, regardless of `SEED_DATA_ENABLED`. These credentials are insecure and are intended only for development. Accounts are separate from reservation customers. Admins manage accounts at `/api/accounts`; the last Admin cannot be deleted or demoted.

Send Basic credentials to `POST /api/auth/login` and use the returned `Authorization: Bearer ...` header on subsequent API requests. `POST /api/auth/refresh` accepts a valid bearer token in its configured refresh window and returns a new token in the same header. OpenAPI is public at `/api/openapi` and Swagger UI at `/api/swagger-ui.html`.
