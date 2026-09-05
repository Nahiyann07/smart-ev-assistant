# Architecture

## Overview

Smart EV Assistant is a layered, server-rendered Spring application. The browser receives Thymeleaf pages and uses small JavaScript modules for dynamic JSON operations. Spring Security authenticates a server-side session and validates CSRF before requests reach controllers.

```text
Browser (Thymeleaf + fetch)
        |
Spring Security filter chain
        |
Page controllers / REST controllers
        |
Application services and transactions
        |
Spring Data repositories
        |
Hibernate / JDBC / MySQL
```

## Responsibilities

- `controller/page`: route-to-view mapping and view models only.
- `controller/api`: HTTP parameters, validated request records, status codes, and response records.
- `service`: ownership, workflow rules, transactions, search, aggregate calculations, and recommendation scoring.
- `repository`: JPA persistence and focused aggregate queries.
- `entity`: private persistent state, UTC timestamps, and narrow state changes.
- `dto`: immutable records that prevent entity exposure, recursion, over-posting, and password leakage.
- `security`: database-backed principals, role-aware login destinations, BCrypt, session rules, CSRF, and route authorization.
- `exception`: one stable API error contract and status-specific page recovery.

## Request flow

```text
User action
 -> form or fetch
 -> authentication / authorization / CSRF
 -> DTO binding and Bean Validation
 -> service business rules
 -> repository operation
 -> transaction commit or rollback
 -> response DTO or view
 -> success, empty, validation, or recovery state
```

Mutations use `@Transactional`; query services use `@Transactional(readOnly = true)`. Entities are not serialized. Collections remain unidirectional so user and station reads do not accidentally fetch unbounded histories.

## Security model

Credentials are verified through Spring Security and BCrypt. Login rotates/creates the HTTP session. Cookies are HTTP-only and SameSite=Lax; secure-cookie mode is controlled by deployment configuration. Browser mutations provide the CSRF header embedded by Thymeleaf. `/admin/**` and `/api/admin/**` require `ADMIN`; services enforce author ownership and self-lockout prevention. API 401/403 responses are JSON while page access uses login redirection or an error page.

## Recommendation algorithm

Eligible stations exclude `UNDER_MAINTENANCE`, `OUT_OF_SERVICE`, and soft-deleted rows. Four normalized values contribute to a 0–100 score:

```text
availability 40 + rating 30 + speed 20 + distance 10
```

Distance falls linearly to zero contribution at 25 km. When coordinates are omitted, availability/rating/speed weights are divided by 0.9 so their relative importance is preserved. Results use score-descending and station-id tie-breaking, and include numeric breakdowns and plain-language reasons.

## Operational boundaries

This is a single deployable application and a relational database. It does not claim live charger integration, background polling, navigation, payments, or machine learning. These boundaries keep the behavior testable and explainable for the project scope.
