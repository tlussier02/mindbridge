# Partner Testing Guide

Use `master`. That is the current team branch.

## 1. Pull the latest code

```bash
git fetch origin
git checkout master
git pull origin master
```

## 2. Create local secrets

From the repo root:

```bash
cp .env.example .env
```

Fill in:

```env
ANTHROPIC_API_KEY=your_real_anthropic_key
JWT_SECRET=your_secure_random_secret
```

## 3. Start the stack with Docker

From the repo root:

```bash
docker compose up -d --build --remove-orphans
docker compose ps
```

You should see:

- `mindbridge-backend-1`
- `mindbridge-frontend-1`
- `mindbridge-nginx-1`

## 4. Verify local endpoints

```bash
curl -sS http://localhost:8080/actuator/health
curl -I http://localhost:3000
curl -I http://localhost:3000/swagger-ui
curl -I http://localhost:3000/h2-console/
```

Expected result:

- actuator returns `UP`
- frontend returns `200`
- Swagger responds through nginx on `3000`
- H2 responds through nginx on `3000`

## 5. If pages do not load

Check container state:

```bash
docker compose ps
docker compose logs --tail=100 backend
docker compose logs --tail=100 frontend
```

Most common cases:

- `502` on `3000`: backend is still starting, wait 20-30 seconds and retry
- backend not healthy: `.env` is missing or stale
- build failure: confirm Java 21 for any local Maven usage and rerun the Docker build

## 6. Public EC2 checks

Use the active EC2 public IP or DNS after deployment:

```bash
curl -I http://<host>:3000
curl http://<host>:3000/actuator/health
curl -I http://<host>:3000/swagger-ui
```

## 7. Done criteria

Testing is good when:

- local Docker stack builds
- frontend loads on `3000`
- backend health is `UP`
- deployed app loads on EC2
