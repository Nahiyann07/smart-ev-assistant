# Database Design

## ER relationships

```text
USERS 1 ----< REVIEWS >---- 1 STATIONS
  |                             |
  +------< REPORTS >------------+
  |          |
  |          +-- resolved_by_user_id -> USERS (administrator)
  +------< FAVOURITES >---------+
```

## Tables

`users` stores normalized unique email, BCrypt hash, `USER|ADMIN`, enabled state, and audit timestamps. `stations` stores one charger profile, coordinates, port counts, speed, status, copy, audit timestamps, and nullable `deleted_at`. `reviews` has one row per user/station. `reports` records reporter, issue, workflow state, resolver, and resolution time. `favourites` is a unique user/station association.

All identifiers are `BIGINT AUTO_INCREMENT`. Timestamps are `DATETIME(6)` and Hibernate uses UTC. Foreign keys use `RESTRICT`, because users are disabled and stations are soft-deleted rather than erasing referenced history.

## Enforced rules

- Coordinates are nullable but bounded to valid latitude/longitude ranges.
- `total_ports > 0`; available/offline counts are non-negative and cannot exceed total together.
- Occupied is derived as `total - available - out_of_service`.
- `AVAILABLE` has at least one available port.
- `OCCUPIED` has none available and at least one operational port.
- `UNDER_MAINTENANCE` has none available and one or more offline ports.
- `OUT_OF_SERVICE` has every port offline.
- Rating is 1–5; `(user_id, station_id)` is unique for reviews and favourites.
- Pending reports have no resolver/time; resolved or rejected reports require both.

These checks exist both in service validation for useful errors and in SQL constraints for final integrity.

## Index strategy

Search indexes cover station `name`, `city`, `(status, deleted_at)`, `(charger_type, status)`, and `(city, status)`. Review and report indexes begin with the columns used by station/user/status queries. The user role/enabled pair supports access administration. Unique indexes enforce identities and one-per-user relations.

## Migration ownership

Flyway owns all DDL in `backend/src/main/resources/db/migration`. Hibernate uses `ddl-auto=validate`. A new schema change must be an append-only versioned migration; an applied migration must not be edited in a deployed environment. The initial migration is verified on H2/MySQL mode and has also been executed against an isolated local MySQL server.

## Future migration

Multi-connector stations would introduce `chargers` or `connectors` with station foreign keys and per-connector speed/status. It is intentionally absent today because one station has one charger profile in the requirements.
