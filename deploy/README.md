# Smart EV Assistant — Deployment

This folder is the production delivery unit. `smart-ev-assistant.jar` contains the Spring Boot backend, Flyway migration, Thymeleaf pages, CSS, JavaScript, fonts, and optimized media.

## Requirements

- Java 21
- MySQL 8.4-compatible database reachable from the application host
- An HTTPS reverse proxy/load balancer that forwards the original scheme
- Optional restricted Google Maps credentials

## 1. Database

Create a dedicated database and least-privilege application user. Example for MySQL 8.4:

```sql
CREATE DATABASE smart_ev_assistant CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci;
CREATE USER 'smart_ev_app'@'application-host' IDENTIFIED BY 'replace-with-a-strong-password';
GRANT SELECT, INSERT, UPDATE, DELETE, CREATE, ALTER, INDEX, REFERENCES ON smart_ev_assistant.* TO 'smart_ev_app'@'application-host';
```

Flyway creates/validates the tables when the application starts.

## 2. Environment

Use `.env.example` as a list of variable names. Do not rename it to `.env` inside a public artifact and never commit real values.

Required: `SPRING_PROFILES_ACTIVE=prod`, `DB_URL`, `DB_USERNAME`, `DB_PASSWORD`. `SERVER_PORT` defaults to 8080.

Optional: `GOOGLE_MAPS_BROWSER_KEY`, `GOOGLE_MAP_ID`, and `GOOGLE_ROUTES_SERVER_KEY`. Without them, station pages show a clickable demo location preview and all non-route features remain available. Restrict the browser key by HTTPS referrer and Maps JavaScript API. Restrict the Routes key by server IP and Routes API.

## 3. Start

Linux/macOS shell:

```bash
export SPRING_PROFILES_ACTIVE=prod
export DB_URL='jdbc:mysql://database-host:3306/smart_ev_assistant?useUnicode=true&characterEncoding=UTF-8&serverTimezone=UTC'
export DB_USERNAME='smart_ev_app'
export DB_PASSWORD='use-your-secret-manager'
java -jar smart-ev-assistant.jar
```

PowerShell:

```powershell
$env:SPRING_PROFILES_ACTIVE = 'prod'
$env:DB_URL = 'jdbc:mysql://database-host:3306/smart_ev_assistant?useUnicode=true&characterEncoding=UTF-8&serverTimezone=UTC'
$env:DB_USERNAME = 'smart_ev_app'
$env:DB_PASSWORD = 'use-your-secret-manager'
java -jar .\smart-ev-assistant.jar
```

The production profile redirects HTTP to HTTPS. Terminate TLS at the proxy and forward `X-Forwarded-Proto: https`; do not expose the application port directly to the internet.

## 4. Verify

```bash
sha256sum -c SHA256SUMS.txt
curl --fail https://your-host.example/api/health
```

Sign in with an account created through `/register`, then test search, station details, favourites, reviews, reports, recommendations, and the map preview. Administrator accounts must be promoted through a controlled database/admin provisioning process; the production profile creates no default credentials.

## Rollback

Keep the previous JAR and database backup. Stop the service, restore the prior JAR and environment, and restart it. Flyway migrations are forward-only; if a future release includes a migration, review its release notes and database rollback procedure before deployment.
