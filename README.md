# it3030-paf-2026-smart-campus

Smart Campus Operations Hub backend built with Spring Boot.

## Current scope

This repository currently covers:

- Module A resource catalogue APIs for managing campus facilities and assets
- Module B booking management APIs for creating, reviewing, filtering, and cancelling resource bookings

Implemented in this sprint:

- resource catalogue CRUD
- `resourceCode` uniqueness handling
- filtering/search by `type`, `location`, `minCapacity`, and `status`
- DTO-based controller responses
- validation and JSON error handling
- booking lifecycle with `PENDING`, `APPROVED`, `REJECTED`, and `CANCELLED`
- booking overlap prevention for the same resource
- booking filters by `resourceId`, `requesterId`, and `status`
- H2-backed automated tests for Modules A and B

## Tech stack

- Java 25
- Spring Boot 4
- Spring Data JPA
- MySQL for local development
- H2 for automated tests

## Setup

1. Create a MySQL database named `smart_campus_db_v2`.
2. Update the database credentials in [src/main/resources/application.properties](src/main/resources/application.properties) if needed.
3. Start the API:

```bash
./mvnw spring-boot:run
```

PowerShell command:

```powershell
.\mvnw spring-boot:run
```

4. Run tests:

```bash
./mvnw test
```

PowerShell command:

```powershell
.\mvnw test
```

## Temporary security note

Module E OAuth and role-based access control are not implemented yet. For local Module A API testing, the current security config temporarily allows `/api/**` and `/error` only. This must be replaced with proper OAuth/RBAC during Module E.

## Resource API

| Method | URL | Purpose |
| --- | --- | --- |
| `GET` | `/api/resources` | Get all resources with optional filters |
| `GET` | `/api/resources/search` | Search resources using query parameters |
| `GET` | `/api/resources/{id}` | Get a single resource by id |
| `POST` | `/api/resources` | Create a new resource |
| `PUT` | `/api/resources/{id}` | Update an existing resource |
| `DELETE` | `/api/resources/{id}` | Delete a resource |

### Supported filters

- `type`
- `location`
- `minCapacity`
- `status`

Example:

```http
GET /api/resources/search?type=LAB&location=Block A&minCapacity=40
```

## Sample create request

```json
{
  "resourceCode": "LAB-001",
  "name": "Advanced Networks Lab",
  "description": "Lab with 40 workstations and projector support.",
  "type": "LAB",
  "capacity": 40,
  "location": "Block A",
  "availabilityStart": "2026-04-22T08:00:00",
  "availabilityEnd": "2026-04-22T18:00:00",
  "status": "ACTIVE"
}
```

## Validation and errors

- `400 Bad Request` for validation failures
- `404 Not Found` when a resource id does not exist
- `409 Conflict` when a duplicate `resourceCode` is submitted

Validation and error responses are returned as JSON through global exception handling.

## Postman

A ready-to-import Postman collection is available in:

- `smart-campus-module-a.postman_collection.json`
- `postman/collections/smart-campus-module-a.postman_collection.json`

## Booking API

| Method | URL | Purpose |
| --- | --- | --- |
| `GET` | `/api/bookings` | Get all bookings with optional filters |
| `GET` | `/api/bookings/{id}` | Get a single booking by id |
| `POST` | `/api/bookings` | Create a new booking request |
| `PUT` | `/api/bookings/{id}` | Update a pending booking |
| `PATCH` | `/api/bookings/{id}/status` | Approve, reject, or cancel a booking |
| `DELETE` | `/api/bookings/{id}` | Cancel an existing booking |

### Booking filters

- `resourceId`
- `requesterId`
- `status`

Example:

```http
GET /api/bookings?resourceId=1&requesterId=student-1&status=PENDING
```

## Sample booking create request

```json
{
  "resourceId": 1,
  "requesterId": "student-1",
  "purpose": "Database practical session",
  "expectedAttendees": 30,
  "startTime": "2026-04-25T09:00:00",
  "endTime": "2026-04-25T11:00:00"
}
```

## Booking rules

- the referenced resource must exist
- new and updated bookings cannot overlap for the same resource
- only `PENDING` bookings can be edited
- valid status transitions:
  - `PENDING -> APPROVED`
  - `PENDING -> REJECTED`
  - `PENDING -> CANCELLED`
  - `APPROVED -> CANCELLED`
