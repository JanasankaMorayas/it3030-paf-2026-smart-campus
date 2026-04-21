# it3030-paf-2026-smart-campus

Smart Campus Operations Hub backend built with Spring Boot.

## Current scope

This repository currently covers Module A resource catalogue APIs for managing campus facilities and assets.

Implemented in this sprint:

- resource catalogue CRUD
- `resourceCode` uniqueness handling
- filtering/search by `type`, `location`, `minCapacity`, and `status`
- DTO-based controller responses
- validation and JSON error handling
- H2-backed automated tests for Module A

## Tech stack

- Java 25
- Spring Boot 4
- Spring Data JPA
- MySQL for local development
- H2 for automated tests

## Setup

1. Create a MySQL database named `smart_campus_db`.
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
