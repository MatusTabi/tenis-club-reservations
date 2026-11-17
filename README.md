# Tennis club reservations

Liquibase always creates the database schema. Initial courts and surfaces are disabled by default. To seed a fresh database, set `SEED_DATA_ENABLED=true` in the environment before starting the application (for example, `SEED_DATA_ENABLED=true ./mvnw spring-boot:run`). A `.env` file must be loaded into the environment by your shell or deployment tooling; the application does not load it automatically.

Seeding adds Clay (3.60 CZK/minute) for courts 1 and 2 and Hard (5.00 CZK/minute) for courts 3 and 4. Liquibase runs the seed only once per database. Disabling it later does not remove data. Conflicting active surface names or court numbers cause the migration to fail; resolve the conflict and restart to retry. The default H2 database is in-memory, so its data does not survive recreation.
