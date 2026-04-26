# Smart Campus Frontend

React + Vite frontend client for the Smart Campus Operations Hub backend.

## Prerequisites

1. Run the Spring Boot backend from the repo root:

```powershell
.\mvnw spring-boot:run
```

2. Make sure the backend is available at `http://localhost:8080`.
3. If the login page says `Failed to fetch`, it usually means one of these:

- the backend is not running on `http://localhost:8080`
- the frontend dev server started on another local port and backend CORS did not allow it yet
- the browser is blocking the request because the backend process stopped or restarted

4. If you want Google browser login, configure:

- `GOOGLE_CLIENT_ID`
- `GOOGLE_CLIENT_SECRET`
- optional fallback redirect: `FRONTEND_BASE_URL`

Otherwise, the local Basic Auth demo users are enough for manual demos.

## Frontend setup

From the `frontend/` folder:

```powershell
npm install
npm run dev:5173
```

Recommended Google OAuth demo flow:

- run the frontend on `http://127.0.0.1:5173`
- open the login page and click `Continue with Google`
- finish Google sign-in
- expect the browser to return to the React dashboard automatically

If port `5173` is busy, use:

```powershell
npm run dev:5174
```

If you start the frontend on another local origin and want a fixed backend fallback redirect, start the backend with:

```powershell
$env:FRONTEND_BASE_URL="http://127.0.0.1:5174/"
.\mvnw spring-boot:run
```

If you prefer the generic Vite command:

```powershell
npm run dev -- --host 127.0.0.1 --port 5173 --strictPort
```

Production build:

```powershell
npm run build
```

Optional API base URL override:

```powershell
$env:VITE_API_BASE_URL="http://localhost:8080"
```

If you run the frontend on `127.0.0.1`, you can also use:

```powershell
$env:VITE_API_BASE_URL="http://127.0.0.1:8080"
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

- Google login now starts through the backend `/login` endpoint and stores the current frontend origin for the OAuth success redirect.
- After successful Google sign-in, the browser should return to the React dashboard automatically instead of stopping on raw `/api/users/me` JSON.
- If you trigger OAuth directly from a backend URL instead of the frontend login page, the backend falls back to `FRONTEND_BASE_URL` or `http://127.0.0.1:5173/`.
- Basic Auth mode is the fastest way to demo the full assignment workflow across all modules.
- The frontend expects the backend paged response format used by bookings, tickets, notifications, and audit logs.
- Local backend CORS is configured for:
  - `http://localhost:5173`
  - `http://127.0.0.1:5173`
  - `http://localhost:5174`
  - `http://127.0.0.1:5174`
- For Google Cloud Console redirect URIs, add both if you use both hostnames:
  - `http://localhost:8080/login/oauth2/code/google`
  - `http://127.0.0.1:8080/login/oauth2/code/google`
- Backend start command from repo root:

```powershell
.\mvnw spring-boot:run
```
