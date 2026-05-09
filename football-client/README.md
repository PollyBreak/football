# football-client

Vue-based mobile-first client for the `football-core` backend, intended to run as a Telegram Mini App.

## Run

1. Install dependencies:

```powershell
npm.cmd install
```

2. Start dev server:

```powershell
npm.cmd run dev
```

3. Open:

```text
http://localhost:5173
```

For direct local backend calls, create a local `.env`:

```text
VITE_API_BASE_URL=http://localhost:8080
```

When the app is served behind a same-origin `/api` proxy, leave `VITE_API_BASE_URL` empty.

## Current scope

- Telegram Mini App auth bootstrap with `initData`
- First-run player onboarding
- Players list and profile editing
- Sessions list, creation, join, leave, and waitlist
- Session details with tabs:
  - session players
  - bulk team assignment
  - matches
  - standings
- Match event creation and deletion

## Notes

- In Docker, nginx proxies `/api` to `football-core`.
- In Railway/Netlify production, set `VITE_API_BASE_URL` to the public backend origin.
- Real authentication requires opening the app inside Telegram so `window.Telegram.WebApp.initData` is present.
