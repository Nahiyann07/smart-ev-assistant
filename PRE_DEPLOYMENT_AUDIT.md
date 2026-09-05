# Smart EV Assistant — Pre-Deployment Audit

Audit date: 2026-09-05  
Target: generic Java 21 host, HTTPS reverse proxy, external MySQL

## Executive result

The application passed its functional regression suite and the deployment blockers found during this audit were remediated. The code and generated JAR are suitable for deployment after the operator supplies a production database, TLS-capable proxy, and restricted Google credentials if interactive maps/routes are required. Google Cloud restrictions and a live MySQL 8.4 run cannot be verified from this workstation.

## Checks performed and findings

### Secrets and leakage

- Searched application source, templates, JavaScript, configuration, documentation, Postman, and filenames for Google key signatures, bearer/private tokens, passwords, `.env` files, PEM material, PKCS/JKS keystores, and common credential assignments.
- No real API key, access token, private key, keystore, or committed `.env` was found.
- Predictable demo passwords were present in Java, README, and Postman. **Fixed:** demo seeding is now disabled by default and requires environment-supplied credentials; Postman uses empty collection variables.
- `.gitignore` did not cover environment files or key material. **Fixed:** `.env`, `.env.*`, PEM/key, PKCS, and Java keystore patterns are ignored while `.env.example` remains trackable.
- The public Maps browser key is rendered to configured pages as required by the JavaScript API. It is not a secret. The server Routes key is never added to a page model; an integration test guards this boundary.
- Provider-console referrer, API, billing, quota, and server-IP restrictions cannot be inspected without Google Cloud access. They remain an operator action.
- Git-history scanning was not possible because this directory has no `.git` repository or commit history.

### Web and application security

- CSRF is enabled by Spring Security. Forms receive Thymeleaf tokens and JavaScript mutations send the CSRF header.
- Session authentication rotates/creates a server session; logout invalidates it. Cookies are HttpOnly and SameSite=Lax in all profiles and Secure in production.
- Production now redirects HTTP to HTTPS and understands forwarded headers. HSTS remains Spring Security's HTTPS default.
- Admin routes require `ADMIN`; unauthenticated APIs return JSON `401`; forbidden API operations return JSON `403`; ownership is enforced in services.
- No permissive CORS configuration or `@CrossOrigin` was found, so browser access remains same-origin.
- No `th:utext`, `eval`, `document.write`, or raw user-controlled HTML insertion was found. Thymeleaf escapes server content and dynamic review/station markup uses explicit HTML escaping.
- Database access uses JPA/Criteria or bound JPQL parameters. Sort choices are allow-listed and request DTOs use Bean Validation. Page size is capped at 50.
- API exceptions and security-filter failures are serialized to a standard response. Database/unexpected failures are logged by request method, path, and exception type without returning or logging exception messages, request bodies, passwords, SQL details, or keys.
- Protected requests revalidate the authenticated user's enabled/role security state. Missing, disabled, or role-changed accounts have their authentication cleared and session invalidated before the request proceeds.
- Login failures, registration, and paid route requests now have configurable, bounded, thread-safe fixed-window rate limits. API throttling returns standard JSON with `Retry-After`; form login uses a generic message. These are single-JVM controls and require an equivalent shared edge control when clustered.
- HTML responses now receive a cryptographically random script nonce and a strict Content Security Policy compatible with the optional Google Maps loader. Referrer and Permissions policies are explicit; Spring's CSRF, HSTS, frame, and MIME-sniffing protections remain enabled.
- Non-finite numeric inputs are rejected and pagination/port totals use `long` arithmetic to prevent validation bypasses and integer-overflow HTTP 500 responses.

### Dependency audit

- Baseline OSV review found three critical advisories in `tomcat-embed-core` 11.0.24, including a FORM-auth authorization issue. **Fixed:** the managed Tomcat family is overridden to 11.0.25.
- GSAP 3.15.0 returned no OSV vulnerability records.
- OWASP Dependency-Check 13.0.0 was invoked but could not initialize its NVD database without an NVD API key. A reproducible `security-audit` Maven profile now reads `NVD_API_KEY` and fails at CVSS 7 or greater. This limitation is explicit rather than reported as a clean NVD scan.
- The post-remediation OSV batch checked 94 runtime dependencies and returned zero vulnerability records.
- Official Spring and Apache Tomcat advisory pages were reviewed for the packaged Spring Boot 4.1.1, Spring Security 7.1.1, Spring Data JPA 4.1.1, and Tomcat 11.0.25 releases; no additional confirmed issue affecting this application was identified.
- SpotBugs 4.10.4.0 completed at maximum effort with zero unsuppressed findings. Audit output remains under `target/` and is not shipped.

### Code quality and performance

- No application `console.log`, `debugger`, TODO, or FIXME statements were found.
- SpotBugs initially reported 28 items: two authority null-safety warnings, mutable collection exposure, and framework/JPA reference false positives. Null comparisons and immutable record copies were fixed; only narrow documented framework/JPA exclusions remain.
- Review, report, and favourite response mapping could cause lazy-association N+1 queries. **Fixed:** targeted entity graphs fetch only the associations each response needs.
- Station search currently materializes matching stations before aggregate-rating filtering and final pagination. This is acceptable for the small demonstration dataset but should become a database projection before operating at large scale.
- The landing hero previously shipped a 9.8 MB 1080p/60fps video and an 898 KB poster. **Fixed:** the web rendition is 720p/30fps H.264 with fast-start metadata (~2.2 MB) and the WebP poster is ~210 KB.
- Production enables Thymeleaf caching and one-hour public static-resource caching; development remains uncached.
- The compiler reports one existing deprecated Java HTTP-client API use in `GoogleRoutesClient`; route success, timeouts, and malformed responses remain covered. This is non-blocking maintenance for a future framework-compatible refactor.

### Final build verification

- `mvnw clean package`: **passed**.
- Tests: **64 run, 0 failures, 0 errors, 0 skipped**.
- Artifact: executable `smart-ev-assistant.jar`, 63,216,363 bytes.
- SHA-256: `692a8522591dd19b20978bb96a844c5d22ed380aace3cce9064b57064158a944`.
- JAR inspection found no `.env`, source map, test class, Java source, private-key/keystore file, obsolete poster, or raw source-video filename. It contains the production/demo profiles, optimized media, Spring Boot/Data JPA 4.1.1, Spring Security 7.1.1, and Tomcat 11.0.25 modules.
- Local demo startup succeeded on `127.0.0.1:8080` with embedded Tomcat 11.0.25, environment-supplied accounts, and six H2 stations.
- Live HTTP smoke checks confirmed health, CSRF-protected registration, driver login/pages, admin rejection for the driver (`403`), admin login/dashboard, the no-Google-key fallback, per-response CSP nonce matching, and explicit Referrer/Permissions headers.

## Environment separation

- `dev`: MySQL defaults for local work, debug application logging, non-secure cookie permitted for localhost.
- `demo`: H2 and optional environment-driven sample seeding; must never be enabled in production.
- `test`: isolated H2 in MySQL mode.
- `prod`: required DB variables, HTTPS redirect, forwarded headers, secure cookies, cached templates/assets, and INFO logging.

## Remaining operator actions

1. Deploy behind HTTPS and pass trustworthy `Forwarded`/`X-Forwarded-*` headers only from the reverse proxy.
2. Create a least-privilege MySQL database/user and set `DB_URL`, `DB_USERNAME`, and `DB_PASSWORD` outside the artifact.
3. If maps are enabled, restrict the browser key by production referrer and Maps JavaScript API; restrict the server key by host IP and Routes API; verify billing and the Map ID.
4. Supply `NVD_API_KEY` and run the full Maven security profile before a high-assurance release.
5. Initialize version control before team deployment and run a history-aware secret scanner after commits exist.
6. Run the final schema/application smoke test on MySQL 8.4. This workstation exposes MySQL 9.7 and no usable database credentials were supplied.
