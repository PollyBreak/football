# Deployment Notes

## Recommended production shape

Recommended all-in-one option:

- Railway project with three services: `football-client`, `football-core`, and `Postgres`
- See `RAILWAY.md` for the exact setup

Alternative split option:

- Frontend: Netlify static deploy from `football-client`
- Backend: Docker-capable Java hosting, for example Render, Railway, Fly.io, or a VPS
- Database: managed PostgreSQL close to the backend, for example Neon, Supabase, Railway Postgres, Render Postgres, or a managed VPS database

Keep the frontend and backend as separate deploy units unless you intentionally put a reverse proxy in front of both. Netlify serves the Telegram Mini App over HTTPS, and the backend exposes the public `/api/**` endpoints over HTTPS.

## Netlify frontend

The root `netlify.toml` is configured for this monorepo:

```toml
[build]
  base = "football-client"
  command = "npm ci && npm run build"
  publish = "dist"
```

Netlify environment variables:

```text
VITE_API_BASE_URL=https://your-backend-domain.example
```

Use the public backend origin only, without a trailing `/api`. The frontend already calls paths like `/api/auth/telegram`, so a value such as `https://api.example.com` becomes `https://api.example.com/api/auth/telegram`.

If you later configure a Netlify proxy for `/api/*`, keep `VITE_API_BASE_URL` empty. The current Docker setup already uses that same-origin pattern through nginx.

The SPA fallback is also configured in `netlify.toml`, so direct opens of routes such as `/sessions` or `/profile` should serve `index.html`.

## Backend and PostgreSQL

Deploy `football-core` with Java 21, or use the existing Dockerfile:

```text
football-core/Dockerfile
```

Required backend environment variables:

```text
DB_URL=jdbc:postgresql://your-postgres-host:5432/your_database
DB_USERNAME=your_database_user
DB_PASSWORD=your_database_password
TELEGRAM_BOT_TOKEN=your_bot_token
CORS_ALLOWED_ORIGIN_PATTERNS=https://your-netlify-site.netlify.app,https://your-custom-domain.example
```

Do not hardcode `TELEGRAM_BOT_TOKEN` or database credentials. For production, use a strong database password and managed platform secrets.

Flyway runs on backend startup, so the production database user must be allowed to create and alter the application schema.

## Telegram Mini App production checklist

1. Deploy the backend and confirm it is reachable over HTTPS.
2. Deploy the Netlify frontend with `VITE_API_BASE_URL` pointing to that backend origin.
3. Set backend `CORS_ALLOWED_ORIGIN_PATTERNS` to the exact Netlify/custom frontend HTTPS origin.
4. Set `TELEGRAM_BOT_TOKEN` on the backend to the same bot token used by BotFather.
5. In BotFather, set the Main Mini App URL to the Netlify/custom frontend HTTPS URL.
6. Open the Mini App from Telegram, not directly in a normal browser, when testing real `initData`.

## Local Docker stack

From `D:\FootballApp`:

```powershell
Copy-Item .env.example .env
# Edit .env and set TELEGRAM_BOT_TOKEN.
# For production set a strong POSTGRES_PASSWORD.
# For production also set CORS_ALLOWED_ORIGIN_PATTERNS to the public HTTPS frontend domain.
docker compose up --build
```

Services:

- frontend: `http://localhost:5173`
- backend: `http://localhost:8080`
- postgres: `localhost:5432`

## What is already prepared

- `football-client` is mobile-first and Telegram Mini App ready
- `football-client` can run with a direct backend URL in dev via `.env`
- in Docker, frontend proxies `/api` to `football-core` through nginx
- `football-core` reads DB, Telegram, and CORS settings from environment variables

## First-run flow

1. User opens Mini App in Telegram
2. Frontend sends `initData` to `/api/auth/telegram`
3. Backend validates Telegram signature
4. Backend finds or creates `app_user`
5. If there is no linked `player`, frontend redirects to `/onboarding`
6. Until the player profile is created, the frontend hides sessions, players, and profile navigation
7. User fills profile and becomes a registered player
8. On later Telegram opens, backend returns the linked player and the frontend opens the app normally
