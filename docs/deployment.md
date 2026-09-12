# Deployment

Everything in this repository is ready to deploy. What remains needs accounts and
credentials, and is written out here step by step.

Platform plans, free tiers and limits change often — check the current terms on each
platform rather than trusting any numbers quoted around the internet, this document
included.

## Shape of it

| Piece | Where | Declared in |
|---|---|---|
| API + PostgreSQL | Render | `render.yaml` |
| Frontend | Vercel | `frontend/vercel.json` |
| RabbitMQ | An external broker | wired by environment variables |
| Deploys | GitHub Actions, only after CI passes | `.github/workflows/deploy.yml` |

**RabbitMQ is not on Render**, which has no managed broker. Rather than drop the
messaging architecture the project exists to demonstrate (ADR-0007) to fit one
platform's catalogue, point `RABBITMQ_*` at a managed broker elsewhere — CloudAMQP is
the usual choice — or run one yourself.

Without a broker the application still starts and check-ins still save: publishing is
best-effort and a failure is logged, never propagated to the person mid-check-in. What
stops is the recommendation engine, so the dashboard's suggestion feed stays empty.

## 1. Generate the two secrets

```bash
openssl rand -base64 48   # JWT_SECRET
openssl rand -base64 32   # ENCRYPTION_KEY — must decode to exactly 32 bytes
```

Never reuse the development values committed in `application.yml`. They are in this
repository's history permanently, so a deployment using them signs tokens anybody can
forge and encrypts emotional content to a key anybody can read. The application refuses
to start if it finds either of them, or an `ENCRYPTION_KEY` that is not a usable AES-256
key — it will not boot and then fail on the first note somebody writes.

## 2. Render: the API and its database

1. **New → Blueprint**, pointed at this repository. `render.yaml` creates the web service
   and the managed PostgreSQL, and wires the database credentials into the API by itself.
2. Render will ask for the values marked `sync: false`:
   - `JWT_SECRET`, `ENCRYPTION_KEY` — from step 1
   - `CORS_ALLOWED_ORIGINS` — the Vercel URL from step 3; come back and set it once you
     have it. A mismatch here breaks the WebSocket handshake, not only fetches, so the
     live dashboard goes quiet rather than erroring visibly
   - `RABBITMQ_HOST`, `RABBITMQ_PORT`, `RABBITMQ_USERNAME`, `RABBITMQ_PASSWORD`
   - `ANTHROPIC_API_KEY` — optional; leave `LLM_PROVIDER=mock` and the companion answers
     from the canned client for free, with every guardrail still in place
3. Copy the service's **deploy hook URL** (Settings → Deploy Hook) for step 4.

The blueprint sets `SPRING_PROFILES_ACTIVE=prod,demo`: production hardening, and the
synthetic history that makes a public instance worth opening. Drop `demo` for a real
deployment with real people in it.

## 3. Vercel: the frontend

1. Import the repository, root directory `frontend`. `vercel.json` supplies the build,
   the SPA rewrite and the cache and security headers.
2. Set **`VITE_API_BASE_URL`** to the Render URL.
   This is a *build-time* variable. Vite inlines it into the bundle, so changing it later
   means rebuilding the image, not restarting it.
3. Copy the project's **deploy hook URL** (Settings → Git → Deploy Hooks).

## 4. GitHub: deploy only what passed

Add both hook URLs as repository secrets (Settings → Secrets and variables → Actions):

- `RENDER_DEPLOY_HOOK_URL`
- `VERCEL_DEPLOY_HOOK_URL`

Then turn **off** auto-deploy on both platforms. `deploy.yml` runs after CI completes on
`main` and fires the hooks only when it passed; leaving auto-deploy on would ship a red
build to whoever is using the app. With the secrets absent the workflow skips rather than
fails, so a fork stays green.

## Afterwards

- `GET /actuator/health` is public and returns status only.
- `/actuator/health/liveness` and `/readiness` are public because the platform has to
  reach them; a platform that cannot restarts the container forever.
- `/actuator/prometheus` requires an ADMIN account. Health says up or down; metrics
  describe when the crisis flow is firing, which in this domain is a picture of when
  people are in trouble.
- The demo account is `demo@lumen.dev` / `Demo1234!`. On a public instance those
  credentials are public too: anyone can log in, change that account's data, or delete it
  through the erasure endpoint. That is acceptable for a demonstration where every person
  in the database is invented, and is a reason not to run `demo` anywhere else.

## Cold starts

On a free instance the API sleeps when idle and the first request after that pays for
waking it — tens of seconds, during which the frontend looks broken rather than slow.
Worth a line in the README so a visitor knows, and worth keeping in mind before reading
anything into a first-load measurement.
