# Team Build and Run Instructions

This project has two main parts:

- Spring Boot backend in the repo root
- Next.js frontend in `mindbridge/`

For local development, run them separately first. That gives clearer errors than starting with Docker.

## Prerequisites

- Java `21`
- Node.js `20+`
- npm
- Git

## 1. Pull the latest code

From your local clone:

```bash
git fetch origin
git checkout master
git pull origin master
```

## 2. Configure Java 21

Check your active Java version:

```bash
java -version
```

If it is not Java 21, set it in your shell before building:

```bash
export JAVA_HOME=/Library/Java/JavaVirtualMachines/temurin-21.jdk/Contents/Home
export PATH="$JAVA_HOME/bin:$PATH"
java -version
```

The backend will fail to compile on Java 17 because the project targets release 21.

## 3. Create local environment variables

From the repo root:

```bash
cp .env.example .env
```

Open `.env` and fill in:

```env
ANTHROPIC_API_KEY=your_real_anthropic_key
JWT_SECRET=your_secure_random_secret
```

Notes:

- `.env` is local only and must not be committed
- GitHub Actions also needs a repository secret named `ANTHROPIC_API_KEY`

## 4. Prepare the Maven wrapper

If `./mvnw` fails with `permission denied`, run:

```bash
chmod +x mvnw
```

## 5. Start the backend

From the repo root:

```bash
./mvnw spring-boot:run
```

If the wrapper still gives trouble, this fallback also works:

```bash
sh mvnw spring-boot:run
```

Expected backend verification URLs:

- Swagger UI: `http://localhost:8080/swagger-ui`
- H2 Console: `http://localhost:8080/h2-console`

## 6. Start the frontend

Open a second terminal:

```bash
cd mindbridge
npm install
npm run dev
```

Expected frontend URL:

- Frontend: `http://localhost:3000`

## 7. Local smoke test

Verify all of these before claiming your local setup works:

- frontend loads on `3000`
- backend loads on `8080`
- Swagger UI opens
- H2 console opens
- authentication flow responds
- at least one AI-backed action completes using the Anthropic key

## 8. Docker note

There is a `docker-compose.yml` and `nginx.conf`, but local debugging should start with separate backend and frontend processes first.

Current Docker/Nginx behavior:

- Nginx listens on port `3000`
- `/` proxies to the frontend container
- `/api/` proxies to the backend container

That means Docker is better for deployment validation than for first-time setup debugging.

## 9. Common failure points

### `release version 21 not supported`

Your shell is using the wrong JDK. Switch to Java 21 and retry.

### `zsh: permission denied: ./mvnw`

The Maven wrapper is not executable. Run `chmod +x mvnw`.

### AI tests or AI features return `401`

The Anthropic key is missing, invalid, or not loaded into `.env`.

### Frontend build complains about Google Fonts

That is a network/build-environment issue, not necessarily an app logic failure.

## 10. What to test before pushing

- backend starts cleanly
- frontend starts cleanly
- no obvious console errors
- any dependency changes are intentional
- `.env` is not staged
- local H2 database files are not committed by accident

## 11. If the pages do not load

Start from the repo root:

```bash
docker compose up -d --build --remove-orphans
docker compose ps
curl -sS http://localhost:8080/actuator/health
curl -I http://localhost:3000
```

Expected result:

- backend health returns `UP`
- frontend on `3000` returns `200`

If `3000` returns `502`, wait 20-30 seconds and retry. The backend can take time to finish Spring Boot startup and AI model initialization on a fresh run.
