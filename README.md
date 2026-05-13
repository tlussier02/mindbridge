# MindBridge

MindBridge is the Assignment 3 evolution of the Digital Therapy Assistant project. It combines:

- a Next.js frontend
- a Spring Boot backend
- JWT-based authentication
- AI-assisted CBT workflows
- H2 persistence
- Docker-based deployment support

## Primary Interface

For Assignment 3, the web frontend is the primary user interface.

- frontend: browser application under `mindbridge/`
- backend: Spring Boot API on port `8080`
- CLI: still present in the codebase for fallback/testing, but disabled by default

The CLI can be re-enabled by setting:

```properties
app.cli.enabled=true
```

## Main Project Areas

- frontend: `mindbridge/`
- backend: `src/main/java/com/digitaltherapy`
- docs: `docs/`
- deployment docs: `docs/deployment/`

## Local Environment Setup

Create a local `.env` file in the project root and paste in the required secrets.

Tracked template:

- `.env.example`

Local secret file:

- `.env`

Required values:

```env
ANTHROPIC_API_KEY=your_real_anthropic_key
JWT_SECRET=your_secure_random_secret
```

Notes:

- `.env` is gitignored and should not be committed
- `docker-compose.yml` reads these values for container startup
- GitHub Actions uses the repository secret named `ANTHROPIC_API_KEY`

For full teammate build/run steps, see:

- `docs/TEAM_SETUP.md`

## Expected Assignment 3 Deliverables

- working frontend on port `3000`
- backend API on port `8080`
- Docker and Docker Compose configuration
- deployment documentation
- updated architecture diagrams
- CI/CD/CD workflow files
- submission evidence and screenshots

## Running the Application with Docker

### Prerequisites

Make sure the following are installed on your machine:

 Docker Desktop
 Docker Compose

Verify Docker is running before starting the application.

---

## Start the Application

From the project root directory, run:

docker compose up --build

This will:

 Build the backend Spring Boot container
 Build the frontend Next.js container
 Start both services

---

## Access the Application

Frontend:

http://localhost:3000

Backend Health Endpoint:

http://localhost:8080/actuator/health

Swagger UI:

http://localhost:8080/swagger-ui/index.html

H2 Console:

http://localhost:8080/h2-console

---

## Stop the Application

To stop the containers:

docker compose down

To fully remove containers and volumes:

docker compose down -v

---

## Rebuild Containers After Changes

If configuration or Dockerfiles change, rebuild using:

docker compose up --build

For a completely fresh rebuild:

docker compose build --no-cache
docker compose up


