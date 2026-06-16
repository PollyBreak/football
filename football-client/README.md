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

## Local auth without Telegram

For local browser testing outside Telegram, disable the Telegram requirement in `football-client/.env`:

```text
VITE_TELEGRAM_AUTH_REQUIRED=false
VITE_DEV_AUTH_TELEGRAM_ID=1001
VITE_DEV_AUTH_DISPLAY_NAME=Local Dev Player
```

If you already have a player in the local backend database, set its id to skip onboarding:

```text
VITE_DEV_AUTH_PLAYER_ID=1
```

When `VITE_DEV_AUTH_PLAYER_ID` is set, the app loads that player profile from the backend. If `VITE_DEV_AUTH_PLAYER_ID` is empty, the app signs in as a mock Telegram user and opens onboarding. For production deploys, leave `VITE_TELEGRAM_AUTH_REQUIRED` unset or set it to `true`; Vite embeds these variables at build time.

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
- Production builds must not set `VITE_TELEGRAM_AUTH_REQUIRED=false`.
