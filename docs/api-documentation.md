# API Documentation

Base URL: `http://localhost:8080`. JSON uses UTF-8. Authentication is a `JSESSIONID` cookie; non-GET browser/API operations require the session CSRF token in `X-CSRF-TOKEN`. Import the Postman collection for an executable flow that extracts the token from `/login`.

## Error contract

```json
{
  "timestamp": "2026-09-04T10:15:30Z",
  "status": 400,
  "code": "VALIDATION_FAILED",
  "message": "Please correct the highlighted fields",
  "fieldErrors": { "email": "Enter a valid email address" },
  "path": "/api/auth/register"
}
```

Common statuses: 400 invalid input/state, 401 session required, 403 role/ownership denied or missing CSRF, 404 missing/soft-deleted resource, 409 duplicate/conflict, 503 database unavailable, 500 unexpected failure.

## Endpoints

| Method and path | Access | Purpose |
|---|---|---|
| `GET /api/health` | Public | Application health |
| `POST /api/auth/register` | Public + CSRF | Register normalized `USER` account |
| `GET /api/auth/me` | Authenticated | Current identity without password data |
| `GET /api/stations` | Authenticated | Search, filter, sort, paginate stations; summaries include coordinates |
| `GET /api/stations/{id}` | Authenticated | Station details and computed occupied ports |
| `POST /api/routes` | Authenticated + CSRF | Compute a driving route to a database station |
| `GET /api/stations/{id}/reviews` | Authenticated | Reviews, newest first |
| `POST /api/stations/{id}/reviews` | User | Create the user's single review |
| `PUT /api/reviews/{id}` | Author | Edit own review |
| `DELETE /api/reviews/{id}` | Author | Delete own review |
| `POST /api/stations/{id}/reports` | User | Submit pending issue |
| `GET /api/users/me/reports` | User | Own report history |
| `GET /api/favourites` | User | Own favourites |
| `POST /api/favourites/{stationId}` | User | Add favourite; duplicate is 409 |
| `DELETE /api/favourites/{stationId}` | User | Remove favourite; missing is 404 |
| `GET /api/recommendations` | Authenticated | Ranked explainable recommendations |
| `GET /api/users/me/profile` | User | Identity and activity counts |
| `GET /api/admin/dashboard` | Admin | Operational counts |
| `GET /api/admin/users` | Admin | User access list |
| `PATCH /api/admin/users/{id}/enabled` | Admin | Enable/disable; self-disable rejected |
| `GET/POST /api/admin/stations` | Admin | List/create stations |
| `GET/PUT/DELETE /api/admin/stations/{id}` | Admin | Read/update/soft-delete station |
| `PATCH /api/admin/stations/{id}/status` | Admin | Atomically update status and port counts |
| `GET /api/admin/reports?status=PENDING` | Admin | Filter report queue |
| `PATCH /api/admin/reports/{id}` | Admin | Resolve/reject with audit data |

## Station search

`GET /api/stations` accepts `query`, `city`, `chargerType=AC|DC_FAST`, `availableOnly`, `minSpeedKw`, `minRating`, `sort=name|rating|speed|distance`, `latitude`, `longitude`, `page`, and `size`. Page starts at 0; size is 1–50. Distance sort requires both valid coordinates. Response:

```json
{
  "content": [], "page": 0, "size": 12, "totalElements": 0,
  "totalPages": 0, "first": true, "last": true
}
```

## Key request examples

Registration: `{"name":"Asha Driver","email":"asha@example.com","password":"Charge123"}`. Review: `{"rating":5,"comment":"Clearly marked bays."}`. Report: `{"issueType":"CHARGER_NOT_WORKING","description":"Connector two would not start."}`. Status update: `{"status":"AVAILABLE","availablePorts":2,"outOfServicePorts":0}`. Resolution: `{"status":"RESOLVED"}`.

## Route request

`POST /api/routes` accepts a station ID and the user-approved browser origin. The server looks up the destination coordinates; clients cannot inject a different destination.

```json
{"stationId":1,"originLatitude":8.52,"originLongitude":76.93}
```

```json
{"stationId":1,"distanceMeters":4200,"durationSeconds":720,"encodedPolyline":"..."}
```

Invalid coordinates return 400, an unknown or deleted station returns 404, and missing configuration, timeouts, provider errors, or malformed provider data return 503. The Google Routes server key never appears in the response or rendered HTML.
