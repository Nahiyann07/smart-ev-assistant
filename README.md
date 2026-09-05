# Smart EV Assistant

Smart EV Assistant is a complete Java full-stack college project for discovering EV charging stations, viewing simulated port availability, saving favourites, publishing reviews, reporting faults, and receiving transparent rule-based recommendations. Administrators manage stations, reports, and user access from a protected control room.

The repository is organized as a presentation-friendly monorepo: `frontend/` owns all templates and browser assets, while `backend/` owns the Spring Boot application, API, security, database, and tests. Maven combines them into one executable JAR.

## Technology

- Java 21, Spring Boot 4.1.1, Spring MVC, Spring Data JPA, Hibernate, Spring Security, Bean Validation, Thymeleaf
- MySQL 8.4-compatible schema managed only by Flyway; H2 in MySQL mode for tests and the optional demo profile
- Semantic HTML, modular CSS, inline SVG, local GSAP 3.15/ScrollTrigger, and small vanilla JavaScript modules; no Node frontend toolchain
- Maven Wrapper, JUnit Jupiter, MockMvc, Spring Security Test, Mockito

## Prerequisites

Install Java 21 and MySQL 8.4 LTS. Maven does not need to be installed globally. Confirm Java with `java -version` or set `JAVA_HOME` to the JDK directory.

Create the production-style local database and restricted user:

```sql
CREATE DATABASE smart_ev_assistant CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci;
CREATE USER 'smart_ev_app'@'localhost' IDENTIFIED BY 'choose-a-strong-password';
GRANT ALL PRIVILEGES ON smart_ev_assistant.* TO 'smart_ev_app'@'localhost';
```

Copy the values from `.env.example` into environment variables. Do not commit actual credentials.

## Run with MySQL

PowerShell:

```powershell
$env:JAVA_HOME = 'C:\path\to\jdk-21'
$env:DB_URL = 'jdbc:mysql://localhost:3306/smart_ev_assistant?useUnicode=true&characterEncoding=UTF-8&serverTimezone=UTC'
$env:DB_USERNAME = 'smart_ev_app'
$env:DB_PASSWORD = 'your-password'
cd backend
.\mvnw.cmd spring-boot:run
```

Open `http://localhost:8080`. Flyway applies `V1__create_schema.sql`; Hibernate validates it and never mutates it.

## Run an isolated demo

The demo uses H2 and six sample Kerala stations. H2 has test scope, so enable the test classpath:

```powershell
$env:JAVA_HOME = 'C:\path\to\jdk-21'
$env:SPRING_PROFILES_ACTIVE = 'demo'
$env:DEMO_SEED_ENABLED = 'true'
$env:DEMO_DRIVER_EMAIL = 'choose-a-driver-email'
$env:DEMO_DRIVER_PASSWORD = 'choose-a-driver-password'
$env:DEMO_ADMIN_EMAIL = 'choose-an-admin-email'
$env:DEMO_ADMIN_PASSWORD = 'choose-an-admin-password'
cd backend
.\mvnw.cmd spring-boot:run '-Dspring-boot.run.useTestClasspath=true'
```

Demo accounts are created only from those environment variables; no reusable passwords are compiled into the application. Passwords must contain at least eight characters and the two email addresses must differ. The in-memory database is erased when the process stops. Never enable the `demo` profile in production.

## Optional Google Maps and routing

The station list, details, reviews, favourites, reports, and recommendations work without Google credentials. When credentials are absent, the map panels present a useful setup state instead of breaking the page.

1. In one billing-enabled Google Cloud project, enable **Maps JavaScript API** and **Routes API**.
2. Create a Map ID for the web platform. Advanced Markers require this value.
3. Create `GOOGLE_MAPS_BROWSER_KEY`; restrict it to your website HTTP referrers (for local work, `http://localhost:8080/*`) and allow only Maps JavaScript API.
4. Create a separate `GOOGLE_ROUTES_SERVER_KEY`; restrict it by the deployment server IP and allow only Routes API. Never expose this key in a template or commit it.
5. Set `GOOGLE_MAP_ID` to the Map ID, then restart the application.

PowerShell example (use your own restricted values):

```powershell
$env:GOOGLE_MAPS_BROWSER_KEY = 'browser-key'
$env:GOOGLE_MAP_ID = 'map-id'
$env:GOOGLE_ROUTES_SERVER_KEY = 'server-key'
```

Discovery markers include only stations returned by this application's database API. `POST /api/routes` resolves its destination from that same database and asks Google Routes only for drive duration, road distance, and an encoded polyline. The browser also offers an external “Open in Google Maps” handoff for turn-by-turn navigation.

## Test and package

```powershell
$env:JAVA_HOME = 'C:\path\to\jdk-21'
cd backend
.\mvnw.cmd test
.\mvnw.cmd clean package
```

The tests use an isolated H2 database in MySQL compatibility mode and cover migrations, constraints, registration, authentication, session revocation, CSRF, authorization, rate limiting, security headers, numeric edge cases, CRUD, filters, port invariants, review ownership, reports, favourites, recommendations, route validation/failures, errors, and all page templates. Google is mocked in automated tests; perform a live route smoke test only after supplying restricted, billing-enabled credentials. Final database verification should also be run against MySQL 8.4 using the same migration.

For the complete static and dependency audit, first set an NVD API key and then run:

```powershell
$env:NVD_API_KEY = 'read-from-your-secret-manager'
cd backend
.\mvnw.cmd -Psecurity-audit verify
```

The production profile requires `DB_URL`, `DB_USERNAME`, and `DB_PASSWORD`, redirects HTTP to HTTPS, honors forwarded headers, and sets secure session cookies. Deploy it behind a trusted TLS reverse proxy. The ready-to-ship executable and operator instructions are in `deploy/`.

## Important behavior

- Status is simulated database state, not charger telemetry or IoT connectivity.
- Recommendation is deterministic: availability 40%, rating 30%, speed 20%, optional distance 10%. Without coordinates, the first three weights are proportionally normalized.
- Station deletion is a soft delete. User history stays referentially intact.
- Users can change only their own reviews/favourites; admin routes require `ADMIN`. An administrator cannot disable their own account.
- All state-changing browser requests use the session CSRF token.
- Authenticated accounts are revalidated on protected requests, so disabling an account or changing its role revokes an existing session.
- Login failures, registration, and paid route requests use bounded single-JVM rate limits. Override the `RATE_LIMIT_*`, `LOGIN_RATE_LIMIT_*`, `REGISTRATION_RATE_LIMIT_*`, and `ROUTE_RATE_LIMIT_*` environment variables when needed, and enforce equivalent shared limits at the edge for clustered deployments.
- HTML responses use per-response CSP nonces together with explicit referrer and browser-permissions policies.

## Documentation

- `docs/architecture.md` — system boundaries and request flow
- `docs/database-design.md` — ER design, constraints, indexes, and migrations
- `docs/api-documentation.md` — endpoints, contracts, and error format
- `docs/design-system.md` — visual tokens, responsive wireframes, motion, accessibility
- `docs/verification.md` — completed redesign checks and remaining live verification
- `docs/project-report.md` — viva-ready report, Java concepts, testing, limitations, future work
- `postman/Smart-EV-Assistant.postman_collection.json` — executable session/CSRF-aware API workflow
- `PROJECT_OVERVIEW.md` — full technology and deployment overview
- `PRE_DEPLOYMENT_AUDIT.md` — evidence, findings, fixes, and remaining external checks
