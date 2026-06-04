# booker-worker-service

Background job runner for the Booker football court booking platform. It has no HTTP endpoints and no public API — it runs `@Scheduled` jobs against the shared PostgreSQL database, handling time-sensitive booking state transitions that no user-facing service should own.

Built with Spring Boot 3.5. Headless (no web server).

## Architecture

The service follows a minimal layered structure — no controllers, no DTOs, no security config. Each layer has one responsibility:

- **Jobs** — scheduled task classes annotated with `@Scheduled`. Each job runs a single bulk SQL operation on a fixed cadence. No complex orchestration — each is essentially one UPDATE statement on a timer.
- **Repositories** — data access. Spring Data JPA interfaces with custom `@Modifying` queries for the bulk state transitions.
- **Entities** — the JPA/ORM models mapped to the shared PostgreSQL schema with `jpa.ddl-auto: validate`. Only the entities the worker touches (bookings, booking_participants).
- **Config** — scheduler configuration and datasource wiring.

The service connects directly to the shared PostgreSQL database (no Liquibase — schema is owned by `booker-database`). It reads and writes bookings. It deletes expired idempotency_records.

## Jobs

**Hold expiration** — every 5 seconds. Bookings still in `HELD` state past their `hold_expires_at` deadline are flipped to `CANCELLED`. Single UPDATE, no application logic.

```sql
UPDATE bookings SET state = 'CANCELLED', updated_at = NOW()
WHERE state = 'HELD' AND hold_expires_at < NOW();
```

**Quorum check** — every 30 seconds. Public bookings approaching their slot start time are evaluated: those with enough players (`slots_filled >= min_players`) are confirmed, the rest are cancelled. Two UPDATE passes, both idempotent.

**No-show detection** — every 60 seconds. `CONFIRMED` bookings that are 15 minutes past their `slot_start` with no check-in (never transitioned to `SEATED`) are marked `NO_SHOW`.

**Idempotency cleanup** — daily. Expired rows in `idempotency_records` are deleted to keep the table from growing indefinitely.

## Tech stack

Java 21, Spring Boot 3.5, Spring Data JPA, Hibernate, PostgreSQL 16, Lombok, Maven, Docker.

~200 lines of code total. The smallest service in the platform — it has no users, no permissions, no input validation, and no error responses to design. Just scheduled SQL on a timer.

Part of a multi-service system — see the [platform overview](https://github.com/maleksabbah/booker-deploy) for the full architecture, booking flow, and the other services.
