# Josh Frontend Integration Notes

## What was implemented

- Added `mindbridge/lib/api.ts` as the frontend API service layer.
- Added JWT access/refresh token persistence in localStorage with automatic refresh retry on `401`.
- Connected login, register, logout, CBT sessions, AI chat, thought diary CRUD, distortion suggestions, progress, and crisis views to backend REST endpoints.
- Added `mindbridge/components/web/real-screens.tsx` for API-driven web screens.
- Updated `mindbridge/components/app-screen.tsx` so the primary web routes use the real backend-connected screens.
- Updated `mindbridge/next.config.mjs` with development rewrites from the Next.js dev server to the Spring Boot backend on port `8080`.
- Added the required package-level proxy entry in `mindbridge/package.json`.

## Local run path

From the repository root, start the backend:

```bash
mvn spring-boot:run
```

Then start the frontend:

```bash
cd mindbridge
pnpm install
pnpm dev -- --port 3000
```

Open:

```text
http://localhost:3000
```

## Backend endpoints used

- `POST /auth/register`
- `POST /auth/login`
- `POST /auth/refresh`
- `POST /auth/logout`
- `GET /sessions`
- `GET /sessions/history`
- `POST /sessions/{sessionId}/start`
- `POST /sessions/{sessionId}/chat`
- `POST /sessions/{sessionId}/end`
- `GET /diary/entries`
- `POST /diary/entries`
- `GET /diary/entries/{entryId}`
- `DELETE /diary/entries/{entryId}`
- `POST /diary/distortions/suggest`
- `GET /progress/weekly`
- `GET /progress/monthly`
- `GET /progress/burnout`
- `GET /progress/achievements`
- `GET /crisis`
- `POST /crisis/detect`
- `GET /crisis/safety-plan`

## Notes for final QA

- The frontend now uses live API calls instead of relying only on the mocked `app-context` data.
- The mobile mock flow was left intact; the web flow is the Assignment 3 browser demo path.
- Final browser evidence should show register/login, session chat, diary creation/deletion, progress loading, and crisis support loading from backend responses.
