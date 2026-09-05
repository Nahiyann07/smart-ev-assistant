# Smart EV Assistant — Project Overview

## Product

Smart EV Assistant is a server-rendered EV charging-station discovery and administration application. Drivers can search the database-managed network, inspect simulated port availability, save favourites, review stations, report faults, and receive deterministic recommendations. Administrators manage stations, reports, and user access.

## Backend stack

- Java 21 LTS (verified with Eclipse Temurin 21.0.12.1)
- Spring Boot 4.1.1, Spring MVC, embedded Tomcat 11.0.25
- Spring Security session authentication, BCrypt, CSRF, route/method authorization
- Spring Data JPA and Hibernate 7.4 with DTO-only web responses
- MySQL Connector/J and MySQL-compatible Flyway migrations
- MySQL for deployed runtime; H2 in MySQL compatibility mode for tests and the opt-in local demo
- Bean Validation, Java HTTP Client, Jackson, JUnit Jupiter, MockMvc, Mockito, and Spring Security Test
- Maven 3.9.16 through the checked-in Maven Wrapper

## Frontend stack

- Server-rendered Thymeleaf templates
- Semantic HTML, modular CSS, locally hosted fonts, and responsive layouts
- Vanilla ES modules and the browser `fetch`, Geolocation, Intersection Observer, and Web Animations APIs
- Locally vendored GSAP 3.15 and ScrollTrigger for restrained marketing motion
- Google Maps JavaScript API with Advanced Markers when configured; no frontend package manager or Node build
- A keyless branded location preview opens the selected station through a supported Google Maps URL when the interactive API is unavailable

The complete frontend is packaged inside the executable Spring Boot JAR; there is no separate frontend deployment.

## Database and application flow

Flyway owns the schema for `users`, `stations`, `reviews`, `reports`, and `favourites`. Stations are soft-deleted and availability is explicitly simulated database state. Controllers validate request records, services enforce transactions and ownership, repositories perform parameterized JPA queries, and response records prevent entities or password hashes from reaching the browser.

## APIs and external services

- Page controllers render Thymeleaf views; `/api/**` supplies JSON for dynamic UI actions and Postman.
- `POST /api/routes` accepts a station ID and origin coordinates. The server looks up the destination in its own database before calling Google Routes `computeRoutes`.
- `GOOGLE_MAPS_BROWSER_KEY` and `GOOGLE_MAP_ID` enable browser maps. The browser key is necessarily public and must be restricted by website referrer and API.
- `GOOGLE_ROUTES_SERVER_KEY` is backend-only and must be restricted by server IP and to Routes API.
- No external AI, charger-network, IoT, analytics, payment, or Google Places integration is used.

## Infrastructure and deployment

The target is a generic Java host behind an HTTPS reverse proxy with an external MySQL database. The production artifact is one executable JAR. Configuration is supplied through environment variables, Flyway migrates the database at startup, and `/api/health` is the health endpoint. No CI/CD pipeline is currently present; build, security audit, package verification, and deployment are documented as explicit commands.

## Repository structure

```text
smart-ev-assistant/
├── frontend/                Thymeleaf templates and all browser CSS, JS, fonts, and media
├── backend/                 Spring Boot API, security, database, tests, Maven wrapper, and Dockerfile
├── docs/                    Architecture, database, API, design, and report documents
├── postman/                 Session/CSRF-aware API collection
├── deploy/                  Clean production delivery folder
├── .github/workflows/       GitHub build and test verification
└── .env.example             Variable names and non-secret examples only
```

## Deployment folder

```text
deploy/
├── smart-ev-assistant.jar   Executable backend plus compiled frontend assets
├── .env.example             Production configuration template without secrets
├── README.md                Exact database, proxy, startup, health, and rollback steps
├── PROJECT_OVERVIEW.md      This stack and architecture reference
└── SHA256SUMS.txt           Integrity checksum for the shipped JAR
```

Tests, source maps, source code, Maven caches, IDE settings, real `.env` files, and credentials are intentionally excluded from `deploy/`.
