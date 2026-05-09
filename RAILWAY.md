# Railway Deployment

This project can be deployed as three Railway services in one project:

- `football-core` - Spring Boot backend from `football-core/Dockerfile`
- `football-client` - Vite static frontend served by nginx from `football-client/Dockerfile`
- `Postgres` - Railway PostgreSQL plugin/service

## 1. Create the Railway project

1. Create a new Railway project from the Git repository.
2. Add a PostgreSQL service.
3. Add two app services from the same repository:
   - backend service root directory: `/football-core`
   - frontend service root directory: `/football-client`

Railway reads each service's `railway.json` from its root directory.

## 2. Backend service variables

Set these on the `football-core` service:

```text
DB_URL=jdbc:postgresql://${{Postgres.PGHOST}}:${{Postgres.PGPORT}}/${{Postgres.PGDATABASE}}
DB_USERNAME=${{Postgres.PGUSER}}
DB_PASSWORD=${{Postgres.PGPASSWORD}}
TELEGRAM_BOT_TOKEN=your_bot_token
CORS_ALLOWED_ORIGIN_PATTERNS=https://your-frontend-domain.up.railway.app
```

If your PostgreSQL service is named differently, replace `Postgres` in the reference variables with the exact Railway service name.

The backend listens on Railway's injected `PORT` variable and falls back to `8080` locally.

## 3. Backend public domain

Generate a public Railway domain for `football-core`, then copy that HTTPS origin.

Example:

```text
https://football-core-production.up.railway.app
```

Check health after deploy:

```text
https://football-core-production.up.railway.app/api/health
```

## 4. Frontend service variables

Set this on the `football-client` service:

```text
VITE_API_BASE_URL=https://your-backend-domain.up.railway.app
```

Use the backend origin only. Do not add `/api`, because the frontend already calls `/api/...` paths.

Generate a public Railway domain for `football-client`. This is the Telegram Mini App URL unless you attach a custom domain.

After the frontend domain exists, update the backend variable:

```text
CORS_ALLOWED_ORIGIN_PATTERNS=https://your-frontend-domain.up.railway.app
```

Then redeploy `football-core`.

## 5. Telegram BotFather

Set the BotFather Main Mini App URL to the public frontend HTTPS URL:

```text
https://your-frontend-domain.up.railway.app
```

Test real authentication from Telegram. A normal browser will not provide real `window.Telegram.WebApp.initData`.

## Local behavior

The existing Docker Compose flow remains unchanged:

- frontend: `http://localhost:5173`
- backend: `http://localhost:8080`
- postgres: `localhost:5432`

In Docker Compose the frontend build keeps `VITE_API_BASE_URL` empty, so browser requests go to `/api/...` on the frontend origin and nginx proxies them to `football-core`.

The Railway frontend image uses `nginx.static.conf.template` and does not proxy `/api` internally. In Railway, frontend API calls must use the public backend URL from `VITE_API_BASE_URL`.
