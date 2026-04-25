# it3030-paf-2026-smart-campus

Smart Campus Operations Hub backend built with Spring Boot.

## Current scope

This repository currently covers:

- Module A resource catalogue APIs for managing campus facilities and assets
- Module B booking management APIs for creating, reviewing, filtering, and cancelling resource bookings
- Module C maintenance and incident ticketing APIs for campus issue reporting and technician handling
- Module E authentication and role-management groundwork for future OAuth2 and RBAC enforcement

Implemented in this sprint:

- resource catalogue CRUD
- `resourceCode` uniqueness handling
- filtering/search by `type`, `location`, `minCapacity`, and `status`
- DTO-based controller responses
- validation and JSON error handling
- booking lifecycle with `PENDING`, `APPROVED`, `REJECTED`, and `CANCELLED`
- booking overlap prevention for the same resource
- booking filters by `resourceId`, `requesterId`, and `status`
- ticket lifecycle with `OPEN`, `IN_PROGRESS`, `RESOLVED`, `CLOSED`, and `CANCELLED`
- ticket filters by `status`, `priority`, `category`, `reportedBy`, and `assignedTechnician`
- technician assignment and resolution flow for maintenance incidents
- persisted user model with `USER` and `ADMIN` roles
- local development basic-auth users plus OAuth2-ready security configuration
- admin role management endpoints and current-user endpoint
- H2-backed automated tests for Modules A, B, C, and E foundation

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

Module E groundwork is now added, but full Google OAuth login is not complete yet. Current backend security supports:

- local development basic-auth users for protected endpoints
- a persisted user model with `USER` and `ADMIN` roles
- an OAuth2-ready structure so Google login can be wired later through configuration

Current route strategy:

- `GET /api/resources/**` is public
- resource write endpoints require `ADMIN`
- booking and ticket APIs require authentication
- booking status updates and technician assignment have admin protection
- user management endpoints are protected, with admin-only access where appropriate

Real Google OAuth client credentials must be supplied later through external configuration.

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

## Ticket API

| Method | URL | Purpose |
| --- | --- | --- |
| `GET` | `/api/tickets` | Get all tickets with optional filters |
| `GET` | `/api/tickets/{id}` | Get a single ticket by id |
| `POST` | `/api/tickets` | Create a new maintenance/incident ticket |
| `PUT` | `/api/tickets/{id}` | Update an open or in-progress ticket |
| `PATCH` | `/api/tickets/{id}/status` | Move a ticket through its workflow |
| `PATCH` | `/api/tickets/{id}/assign` | Assign a technician to a ticket |
| `DELETE` | `/api/tickets/{id}` | Cancel an open or in-progress ticket |

### Ticket filters

- `status`
- `priority`
- `category`
- `reportedBy`
- `assignedTechnician`

Example:

```http
GET /api/tickets?status=IN_PROGRESS&priority=HIGH&category=NETWORK&reportedBy=student-1
```

## Sample ticket create request

```json
{
  "title": "Water leak in Block A",
  "description": "Water leaking near the entrance and making the floor slippery.",
  "category": "PLUMBING",
  "priority": "HIGH",
  "location": "Block A Entrance",
  "reportedBy": "staff-1",
  "imageUrls": [
    "https://img.example.com/leak-1.jpg",
    "https://img.example.com/leak-2.jpg"
  ]
}
```

## Ticket rules

- title, description, location, and reportedBy are required
- a maximum of 3 image URLs is allowed
- only `OPEN` or `IN_PROGRESS` tickets can be updated
- only `OPEN` or `IN_PROGRESS` tickets can be assigned or cancelled
- valid status transitions:
  - `OPEN -> IN_PROGRESS`
  - `OPEN -> RESOLVED`
  - `OPEN -> CANCELLED`
  - `IN_PROGRESS -> RESOLVED`
  - `IN_PROGRESS -> CANCELLED`
- `RESOLVED -> CLOSED`
- `RESOLVED -> IN_PROGRESS`
- resolution notes are required when resolving a ticket

## Auth And Role Model

Role model:

- `USER`
- `ADMIN`

Local development users from [src/main/resources/application.properties](src/main/resources/application.properties):

- `dev-user@smartcampus.local` / `dev-user-pass`
- `dev-admin@smartcampus.local` / `dev-admin-pass`

OAuth2 placeholders are also prepared in configuration comments for future Google setup:

- `spring.security.oauth2.client.registration.google.client-id`
- `spring.security.oauth2.client.registration.google.client-secret`
- `spring.security.oauth2.client.registration.google.scope`

## User API

| Method | URL | Purpose |
| --- | --- | --- |
| `GET` | `/api/users/me` | Get the currently authenticated user |
| `GET` | `/api/users` | Get all users (`ADMIN` only) |
| `PATCH` | `/api/users/{id}/role` | Update a user role (`ADMIN` only) |

## Sample role update request

```json
{
  "role": "ADMIN"
}
```
