# Smart EV Assistant — Project Report

## Abstract

Smart EV Assistant reduces uncertainty when choosing an electric-vehicle charging stop. Drivers can search a managed station directory, understand simulated port availability, save favourites, contribute reviews, report faults, and receive a transparent recommendation. Administrators maintain the station network and resolve reports with an audit trail. The system demonstrates a secure layered Java web application without claiming live charger or artificial-intelligence integration.

## Problem and objectives

EV drivers need more than a pin on a map: connector profile, charging speed, likely availability, operating hours, community evidence, and a recovery path when infrastructure fails. The project objectives are to centralize that information, preserve trustworthy historical data, enforce user ownership, expose explainable rankings, and provide a responsive accessible interface.

## Modules

1. Account registration and session authentication with normalized email and BCrypt.
2. Station management with soft deletion and strict port/status invariants.
3. Indexed search, filters, stable pagination, aggregate ratings, and optional Haversine distance.
4. Detail pages with derived occupied ports and explicit simulation disclosure.
5. One-review-per-driver lifecycle with author-only modification.
6. Fault reports with pending/resolved/rejected transitions and resolver auditing.
7. Unique favourites and driver activity summaries.
8. Deterministic recommendation scoring and readable reasons.
9. Admin statistics, user access, station editor, and report queue.

## Engineering design

The application uses controller/service/repository layers. Controllers bind immutable records and return DTOs; services own rules and transaction boundaries; repositories abstract JPA. Entities encapsulate state and never cross the HTTP boundary. Flyway creates the relational schema, while Hibernate validates mappings. This division keeps transport, business, and persistence concerns independently testable.

Security is defense in depth: session authentication, generic login failures, BCrypt, session invalidation, CSRF, HTTP-only SameSite cookies, route roles, service ownership checks, restricted admin operations, DTOs, server validation, and database constraints. Error payloads are stable and hide implementation details.

## Java concepts demonstrated

Classes and encapsulation appear in entities/services; constructor injection establishes required dependencies. Spring Data and `UserDetailsService` demonstrate interface-driven abstraction and runtime polymorphism. Domain exceptions inherit from `ApiException`. Records model immutable requests/responses. Generics power `PageResponse<T>`; enums model finite workflows. Collections, streams, lambdas, `Instant`, validation annotations, JPA relationships, transactions, and exception translation all solve real application concerns rather than acting as demonstrations alone.

## Recommendation method

Availability, average rating, charging speed, and distance are normalized to 0–1. Their weights are 40%, 30%, 20%, and 10%. Distance reaches zero at 25 km. Without location, the remaining weights are proportionally normalized. Maintenance, out-of-service, and deleted stations are excluded. Stable tie-breaking makes the result reproducible. This is a rule-based decision aid, not AI.

## User and admin flow

A driver registers, signs in, searches/filter stations, optionally shares coordinates, opens details, saves a favourite, reviews a visit, reports an issue, and inspects recommendation reasons. Location denial affects only distance features. An administrator signs in to view counts, maintain station profiles and port state atomically, resolve/reject reports, and enable/disable other users. Ordinary users receive 403 for admin operations.

## Interface and accessibility

The visual language derives from an automotive instrument cluster: graphite/navy surfaces, steel structure, warm amber action signals, restrained semantic state colors, Bahnschrift/Segoe UI typography, and one circular hero instrument. It avoids generic gradient-heavy SaaS styling. Semantic landmarks, labels, skip links, visible focus, 44px controls, native form elements, textual status labels, skeleton/empty/error states, keyboard actions, responsive breakpoints, and `prefers-reduced-motion` support are included.

## Testing evidence

The Maven suite contains 44 passing tests. It covers startup/health, Flyway tables and SQL constraints, registration validation and duplicates, email normalization, password hashing, login/logout/disabled accounts, role routing, CSRF, admin denial, station CRUD/soft deletion/invariants, search/filter/pagination/distance, status updates, review uniqueness/ownership/rating/empty state, report ownership/transitions/audit, favourites conflicts, recommendation ranking/fallback, page rendering, malformed parameters, sanitized database outages, and JSON security errors.

A live isolated H2 demo was also exercised in a real browser: driver login, city filtering, station details, favourite toggle, review creation, fault report, logout, administrator login, dashboard counts, report queue, and resolution. Desktop and 375px responsive layouts were inspected. The initial mobile horizontal filter strip was found and replaced with a single-column form.

The Flyway migration was separately verified against a local isolated MySQL server. Production acceptance should use MySQL 8.4 LTS with deployment credentials and secure cookies enabled.

## Limitations

- Availability is manually maintained simulated state; it can become stale.
- Each station has one charger profile rather than per-connector hardware.
- Coordinates support distance calculation but no map or routing UI.
- No reservation, payment, charger protocol, email, or push integration.
- The demo seed is ephemeral and must not be enabled in production.

## Future enhancements

Introduce per-connector entities, OCPP ingestion with freshness timestamps, map/routing integration, accessible notifications, verified charging-session reviews, database-native geospatial indexes, audit-event storage, containerized MySQL integration tests, and deployment monitoring. Each enhancement should preserve the DTO, ownership, transaction, and explainability boundaries established here.

## Conclusion

Smart EV Assistant meets its scoped goal as a functional, secure, testable full-stack Java application. Its strongest qualities are explicit data integrity, transparent decisions, preserved history, role/ownership enforcement, and an interface that communicates operational state without pretending to be connected to real charging hardware.
