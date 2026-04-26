# Smart Campus Frontend

React + Vite frontend client for the Smart Campus Operations Hub backend.

## Prerequisites

1. Run the Spring Boot backend from the repo root:

```powershell
.\mvnw spring-boot:run
```

2. Make sure the backend is available at `http://localhost:8080`.

3. If you want Google browser login, configure:

- `GOOGLE_CLIENT_ID`
- `GOOGLE_CLIENT_SECRET`

Otherwise, the local Basic Auth demo users are enough for manual demos.

## Frontend setup

From the `frontend/` folder:

```powershell
npm install
npm run dev
```

Production build:

```powershell
npm run build
```

Optional API base URL override:

```powershell
$env:VITE_API_BASE_URL="http://localhost:8080"
```

## Demo login credentials

- User: `dev-user@smartcampus.local` / `dev-user-pass`
- Technician: `dev-tech@smartcampus.local` / `dev-tech-pass`
- Admin: `dev-admin@smartcampus.local` / `dev-admin-pass`

## Main screens

- Login / auth landing
- Dashboard
- Resources
- Bookings
- Tickets
- Notifications
- Users / role management
- Audit logs / legacy backfill

## Notes

- Google login opens the backend OAuth flow in a browser tab and the frontend can then refresh the backend session.
- Basic Auth mode is the fastest way to demo the full assignment workflow across all modules.
- The frontend expects the backend paged response format used by bookings, tickets, notifications, and audit logs.
