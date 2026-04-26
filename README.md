# it3030-paf-2026-smart-campus

Smart Campus Operations Hub backend built with Spring Boot.

## Current scope

This repository currently covers:

- Module A resource catalogue APIs for managing campus facilities and assets
- Module B booking management APIs for creating, reviewing, filtering, and cancelling resource bookings
- Module C maintenance and incident ticketing APIs for campus issue reporting and technician handling
- Module D notification APIs for booking and ticket event alerts
- Module E authentication, role management, and Google OAuth2-ready backend login flow
- final backend hardening for ownership, technician workflow, audit history, legacy backfill, and API polish

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
- ownership-aware booking and ticket flows linked to local authenticated users
- notification inbox alignment with linked recipients and admin-wide visibility support
- technician role support with user-linked ticket assignment and technician-specific workflow permissions
- audit log visibility for important booking, ticket, role, and notification actions
- admin backfill utility for linking older legacy string-based ownership records to local users
- paginated and sortable booking, ticket, notification, and audit APIs
- consistent bulk-action summary responses for read-all style operations
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

## Response conventions

Paged endpoints return a common envelope:

```json
{
  "content": [],
  "page": 0,
  "size": 20,
  "totalElements": 0,
  "totalPages": 0,
  "first": true,
  "last": true,
  "sort": [
    "createdAt,desc"
  ]
}
```

Current paging rules:

- default page size is `20`
- maximum page size is `100`
- sort fields are whitelisted per endpoint group
- empty list queries still return `200 OK` with an empty `content` array

Error handling stays consistent across modules:

- `400 Bad Request` for validation and request-shape problems
- `403 Forbidden` for ownership and role violations
- `404 Not Found` for missing records
- `409 Conflict` for duplicate or invalid workflow transitions

Bulk endpoints such as `PATCH /api/notifications/read-all` now return a small JSON summary instead of an empty response body.

## Pagination and sorting examples

```http
GET /api/bookings?page=0&size=10&sort=startTime,asc
GET /api/tickets?page=0&size=10&sort=createdAt,desc
GET /api/notifications?unreadOnly=true&page=0&size=20&sort=createdAt,desc
GET /api/audit-logs?entityType=TICKET&page=0&size=20&sort=createdAt,desc
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
- normal users are limited to their own bookings, tickets, and notification inbox
- technicians can see tickets assigned to them and progress their workflow
- admins can manage all bookings and tickets, and can inspect any notification inbox when needed
- audit and backfill endpoints are admin-only

Operational notes:

- `spring.jpa.open-in-view=false` is enabled so entity loading stays inside the service layer
- `spring.data.web.pageable.max-page-size=100` is enabled for safer paging defaults
- local MySQL development still points to `smart_campus_db_v2`

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

- `app.security.oauth2.success-redirect-uri=${FRONTEND_BASE_URL:http://127.0.0.1:5173/}`

PowerShell example:

```powershell
$env:GOOGLE_CLIENT_ID="your-google-client-id"
$env:GOOGLE_CLIENT_SECRET="your-google-client-secret"
.\mvnw spring-boot:run
```

Optional fallback frontend redirect:

```powershell
$env:FRONTEND_BASE_URL="http://127.0.0.1:5173/"
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

On successful login, the Google user is created or updated in the local `users` table with a safe default role of `USER`.
When OAuth is started from the React login screen, the backend now remembers the current frontend origin and redirects back to the dashboard automatically after Google sign-in. The configured `success-redirect-uri` only acts as the fallback target when OAuth is started directly from the backend.

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

If you also run the frontend/backend flow through `127.0.0.1`, add this too:

```text
http://127.0.0.1:8080/login/oauth2/code/google
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
- `smart-campus-backend-demo.postman_collection.json`

The backend demo collection is organized for final manual walkthroughs across public resource reads, authenticated booking and ticket flows, notification inbox checks, and admin audit/backfill actions.

## Booking API

| Method | URL | Purpose |
| --- | --- | --- |
| `GET` | `/api/bookings` | Get paged bookings with optional filters |
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

Paging example:

```http
GET /api/bookings?status=PENDING&page=0&size=5&sort=createdAt,desc
```

## Sample booking create request

```json
{
  "resourceId": 1,
  "purpose": "Database practical session",
  "expectedAttendees": 30,
  "startTime": "2026-04-25T09:00:00",
  "endTime": "2026-04-25T11:00:00"
}
```

## Booking rules

- authenticated `USER` requests default to the logged-in user as booking owner
- `ADMIN` users can still create or update bookings on behalf of another identifier when needed
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
| `GET` | `/api/tickets` | Get paged tickets with optional filters |
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

Paging example:

```http
GET /api/tickets?assignedTechnician=dev-tech@smartcampus.local&page=0&size=10&sort=priority,asc
```

## Sample ticket create request

```json
{
  "title": "Water leak in Block A",
  "description": "Water leaking near the entrance and making the floor slippery.",
  "category": "PLUMBING",
  "priority": "HIGH",
  "location": "Block A Entrance",
  "imageUrls": [
    "https://img.example.com/leak-1.jpg",
    "https://img.example.com/leak-2.jpg"
  ]
}
```

## Ticket rules

- title, description, and location are required
- authenticated `USER` requests default to the logged-in user as ticket reporter
- `ADMIN` users can still register or edit a ticket on behalf of another identifier when needed
- technician assignment now links to a real active `TECHNICIAN` user whenever an assignment is made
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

Technician workflow:

- `ADMIN` assigns tickets to a real `TECHNICIAN` user through `PATCH /api/tickets/{id}/assign`
- assigned technicians can view tickets assigned to them
- assigned technicians can move tickets through:
  - `OPEN -> IN_PROGRESS`
  - `IN_PROGRESS -> RESOLVED`
  - `RESOLVED -> IN_PROGRESS`
- reporting users can still edit or cancel their own tickets where allowed, but they cannot resolve or close tickets unless they are also the assigned technician or an admin

## Auth And Role Model

Role model:

- `USER`
- `TECHNICIAN`
- `ADMIN`

Local development users from [src/main/resources/application.properties](src/main/resources/application.properties):

- `dev-user@smartcampus.local` / `dev-user-pass`
- `dev-tech@smartcampus.local` / `dev-tech-pass`
- `dev-admin@smartcampus.local` / `dev-admin-pass`

Google OAuth2 configuration keys supported by the app:

- `app.security.oauth2.google.client-id`
- `app.security.oauth2.google.client-secret`
- `app.security.oauth2.google.scope`
- `app.security.oauth2.google.redirect-uri`
- `app.security.oauth2.google.client-name`
- `app.security.oauth2.success-redirect-uri`

Frontend demo flow:

- start the backend with `.\mvnw spring-boot:run`
- start the frontend with `npm run dev:5173` from `frontend/`
- open the React login page
- click `Continue with Google`
- complete Google sign-in
- expect the browser to return to the React dashboard automatically

## User API

| Method | URL | Purpose |
| --- | --- | --- |
| `GET` | `/api/users/me` | Get the currently authenticated user |
| `GET` | `/api/users` | Get all users (`ADMIN` only) |
| `PATCH` | `/api/users/{id}/role` | Update a user role (`ADMIN` only) |

## Notification API

| Method | URL | Purpose |
| --- | --- | --- |
| `GET` | `/api/notifications` | Get paged notifications for the current user, or all notifications for `ADMIN` |
| `GET` | `/api/notifications/unread` | Get paged unread notifications for the current user, or unread notifications for a selected inbox as `ADMIN` |
| `PATCH` | `/api/notifications/{id}/read` | Mark one notification as read |
| `PATCH` | `/api/notifications/read-all` | Mark all current-user notifications as read and return a summary payload |
| `DELETE` | `/api/notifications/{id}` | Delete one notification |

### Notification filters

- `unreadOnly`
- `type`
- `recipient` (`ADMIN` only, optional)

Example:

```http
GET /api/notifications?unreadOnly=true&type=BOOKING_APPROVED
```

Paging example:

```http
GET /api/notifications?unreadOnly=true&page=0&size=10&sort=createdAt,desc
```

Sample bulk read response:

```json
{
  "message": "Notifications marked as read.",
  "updatedCount": 3
}
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
- `GET /api/audit-logs`
- `GET /api/audit-logs/{entityType}/{entityId}`
- `POST /api/admin/backfill/user-links`
- `GET /api/users`
- `PATCH /api/users/{id}/role`

Technician-specific behavior:

- technicians can read tickets assigned to them
- technicians receive assignment and status-change notifications for their assigned tickets
- technicians can update ticket status only for tickets assigned to them and only through the technician workflow above

## Ownership model

- bookings now keep both a legacy `requesterId` string and an optional linked `ownerUser`
- tickets now keep both a legacy `reportedBy` string and an optional linked `reportedByUser`
- tickets now also keep both a legacy `assignedTechnician` string and an optional linked `assignedTechnicianUser`
- when an authenticated user creates a booking or ticket, the linked local `User` becomes the primary owner by default
- normal users can only list, view, update, or cancel their own bookings and tickets
- normal users can only read and manage their own notification inbox
- admins can view and manage all bookings and tickets
- technicians can view tickets assigned to them and progress assigned ticket statuses
- admins can also inspect any notification inbox by using the optional `recipient` query parameter, or view all notifications when no recipient filter is supplied

## Sample role update request

```json
{
  "role": "ADMIN"
}
```

## Compatibility notes

- legacy bookings and tickets still preserve the old string fields (`requesterId`, `reportedBy`) so existing MySQL records are not broken
- ticket assignments still preserve the old `assignedTechnician` string field so existing records remain readable during migration
- if an older record only has a legacy identifier that does not match any local user email, ownership fallback stays string-based and that record is best managed by an admin during migration
- older ticket assignments that point to a plain string and not a real `TECHNICIAN` user can still be read, but new assignments now require a real active technician account
- the backfill endpoint links legacy string ownership or technician fields only when they match an existing local user email
- legacy technician links are only backfilled to active `TECHNICIAN` users; non-technician matches are skipped safely
- backfill summary counts each legacy link attempt that was scanned, linked, or skipped
- browser-based OAuth login can be tested manually once real Google credentials are added

## Audit API

| Method | URL | Purpose |
| --- | --- | --- |
| `GET` | `/api/audit-logs` | Get paged audit logs with optional admin filters |
| `GET` | `/api/audit-logs/{entityType}/{entityId}` | Get paged audit history for one entity (`ADMIN` only) |

### Audit filters

- `entityType`
- `action`
- `performedBy`
- `from`
- `to`

Example:

```http
GET /api/audit-logs?entityType=TICKET&action=TICKET_STATUS_CHANGED&performedBy=admin@example.com
```

Paging example:

```http
GET /api/audit-logs?entityType=BOOKING&page=0&size=20&sort=createdAt,desc
```

## Audit events currently tracked

- booking created
- booking approved
- booking rejected
- booking cancelled
- ticket created
- ticket assigned
- ticket status changed
- user role changed
- notification read-all
- admin backfill execution

## Admin backfill API

| Method | URL | Purpose |
| --- | --- | --- |
| `POST` | `/api/admin/backfill/user-links` | Scan legacy booking/ticket string identifiers and link them to matching local users (`ADMIN` only) |

Backfill behavior:

- links `Booking.requesterId -> ownerUser` when the string matches an existing user email
- links `Ticket.reportedBy -> reportedByUser` when the string matches an existing user email
- links `Ticket.assignedTechnician -> assignedTechnicianUser` only when the string matches an active `TECHNICIAN` user
- does not overwrite or remove the legacy string fields
- returns a summary with scanned, linked, and skipped counts plus per-area linked totals

## Known remaining limitations

- the React frontend now lives under `frontend/`, but production deployment setup is still out of scope
- resource listing is intentionally kept as a simple public list/search API and is not paginated yet
- ticket attachments are URL-based fields, not uploaded file storage
- audit logs are queryable but do not yet support export, retention policies, or field-by-field diffs
- legacy backfill is admin-triggered on demand, not scheduled automatically
- full production OAuth rollout still depends on real Google Cloud credentials and consent-screen setup outside this repo
