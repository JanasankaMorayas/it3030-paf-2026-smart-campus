# it3030-paf-2026-smart-campus

Smart Campus Operations Hub backend built with Spring Boot.

## Current scope

This repository currently covers:

- Module A resource catalogue APIs for managing campus facilities and assets
- Module B booking management APIs for creating, reviewing, filtering, and cancelling resource bookings
- Module C maintenance and incident ticketing APIs for campus issue reporting and technician handling
- Module D notification APIs for booking and ticket event alerts
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
- notification inbox APIs with read, unread, read-all, and delete flows
- booking and ticket event-driven notifications
- persisted user model with `USER` and `ADMIN` roles
- local development basic-auth users plus Google OAuth2 login support through external configuration
- admin role management endpoints and current-user endpoint
- H2-backed automated tests for Modules A, B, C, D, and E foundation

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

## Auth and security

Module E now supports both local development auth and Google OAuth2 login when credentials are supplied externally. Current backend security supports:

- local development basic-auth users for protected endpoints
- a persisted user model with `USER` and `ADMIN` roles
- Google OAuth2 login with local user sync when Google client credentials are configured

Current route strategy:

- `GET /api/resources/**` is public
- resource write endpoints require `ADMIN`
- booking and ticket APIs require authentication
- notification APIs require authentication
- booking status updates and technician assignment have admin protection
- user management endpoints are protected, with admin-only access where appropriate

### Google OAuth2 configuration

The app now reads Google OAuth2 settings from `app.security.oauth2.google.*`, with environment-variable-friendly defaults already wired in [src/main/resources/application.properties](src/main/resources/application.properties).

Required values:

- `GOOGLE_CLIENT_ID`
- `GOOGLE_CLIENT_SECRET`

Mapped app properties:

- `app.security.oauth2.google.client-id=${GOOGLE_CLIENT_ID:}`
- `app.security.oauth2.google.client-secret=${GOOGLE_CLIENT_SECRET:}`
- `app.security.oauth2.google.scope=openid,profile,email`
- `app.security.oauth2.google.redirect-uri={baseUrl}/login/oauth2/code/{registrationId}`
- `app.security.oauth2.google.client-name=Google`

Optional app-level setting:

- `app.security.oauth2.success-redirect-uri=/api/users/me`

PowerShell example:

```powershell
$env:GOOGLE_CLIENT_ID="your-google-client-id"
$env:GOOGLE_CLIENT_SECRET="your-google-client-secret"
.\mvnw spring-boot:run
```

After startup, open this URL in a browser to begin login:

```text
http://localhost:8080/oauth2/authorization/google
```

You can also open:

```text
http://localhost:8080/login
```

to use Spring Security's generated OAuth login page when Google client registration is present.

On successful login, Spring Security redirects to `/api/users/me`, and the Google user is created or updated in the local `users` table with a safe default role of `USER`.

If Google credentials are not configured, local basic auth still works for protected endpoints during development.

### Google Cloud Console setup

1. Open Google Cloud Console.
2. Create or select the project you want to use for this app.
3. Go to `APIs & Services` -> `OAuth consent screen`.
4. Configure the consent screen first.
5. Choose `External` for normal student/local testing unless you specifically need internal organization-only access.
6. Fill the required app details:
   - App name
   - User support email
   - Developer contact email
7. Add your Google account under test users if the app is still in testing mode.
8. Then go to `APIs & Services` -> `Credentials`.
9. Click `Create Credentials` -> `OAuth client ID`.
10. Choose application type `Web application`.
11. Give it a name like `Smart Campus Local`.
12. Under `Authorized redirect URIs`, add this exact value:

```text
http://localhost:8080/login/oauth2/code/google
```

13. Save the client and copy:
   - Client ID
   - Client secret
14. Set them in your shell as `GOOGLE_CLIENT_ID` and `GOOGLE_CLIENT_SECRET`.

### How to diagnose common Google OAuth browser errors

- `invalid_client`
  Usually means the client ID is wrong, the client secret is wrong, the OAuth client was deleted, or the credentials come from a different Google Cloud project/client than the one you expect.
- `redirect_uri_mismatch`
  Means Google Cloud Console does not contain `http://localhost:8080/login/oauth2/code/google` exactly as an authorized redirect URI.
- consent screen or access blocked errors
  Usually means the OAuth consent screen is incomplete, publishing status is limited, or your Google account is not added as a test user.

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

Google OAuth2 configuration keys supported by the app:

- `app.security.oauth2.google.client-id`
- `app.security.oauth2.google.client-secret`
- `app.security.oauth2.google.scope`
- `app.security.oauth2.google.redirect-uri`
- `app.security.oauth2.google.client-name`
- `app.security.oauth2.success-redirect-uri`

## User API

| Method | URL | Purpose |
| --- | --- | --- |
| `GET` | `/api/users/me` | Get the currently authenticated user |
| `GET` | `/api/users` | Get all users (`ADMIN` only) |
| `PATCH` | `/api/users/{id}/role` | Update a user role (`ADMIN` only) |

## Notification API

| Method | URL | Purpose |
| --- | --- | --- |
| `GET` | `/api/notifications` | Get the current user's notifications with optional filters |
| `GET` | `/api/notifications/unread` | Get unread notifications for the current user |
| `PATCH` | `/api/notifications/{id}/read` | Mark one notification as read |
| `PATCH` | `/api/notifications/read-all` | Mark all current-user notifications as read |
| `DELETE` | `/api/notifications/{id}` | Delete one notification |

### Notification filters

- `unreadOnly`
- `type`

Example:

```http
GET /api/notifications?unreadOnly=true&type=BOOKING_APPROVED
```

## Notification triggers

Current backend notification events:

- booking created
- booking approved
- booking rejected
- booking cancelled
- ticket created
- technician assigned to a ticket
- ticket status updated
- ticket resolved

The notification type enum currently includes:

- `BOOKING_CREATED`
- `BOOKING_APPROVED`
- `BOOKING_REJECTED`
- `BOOKING_CANCELLED`
- `TICKET_CREATED`
- `TICKET_ASSIGNED`
- `TICKET_STATUS_UPDATED`
- `TICKET_RESOLVED`
- `GENERAL`

## Public vs protected endpoints

Public:

- `GET /api/resources`
- `GET /api/resources/search`
- `GET /api/resources/{id}`
- `/oauth2/authorization/google` once Google OAuth2 credentials are configured

Authenticated:

- all booking APIs
- all ticket APIs
- all notification APIs
- `GET /api/users/me`

Admin only:

- resource create, update, delete APIs
- `PATCH /api/bookings/{id}/status`
- `PATCH /api/tickets/{id}/assign`
- `GET /api/users`
- `PATCH /api/users/{id}/role`

## Sample role update request

```json
{
  "role": "ADMIN"
}
```

## Remaining auth work

- browser-based OAuth login can be tested manually once real Google credentials are added
- stricter ownership enforcement for bookings and tickets can now be built on top of the authenticated user identity

## Notification limitations

- notifications are currently linked using the best available identifier strategy from existing modules
- bookings and tickets still store requester/reporter identifiers as strings, so perfect ownership mapping is not fully enforced yet
- if a booking requester or ticket reporter is not using the same identifier as the authenticated email, that notification may not appear in the authenticated inbox until ownership rules are tightened further
