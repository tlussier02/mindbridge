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
- `docs/TIMMY_TESTING_CHECKLIST.md`

## Expected Assignment 3 Deliverables

- working frontend on port `3000`
- backend API on port `8080`
- Docker and Docker Compose configuration
- deployment documentation
- updated architecture diagrams
- CI/CD workflow files
- submission evidence and screenshots
