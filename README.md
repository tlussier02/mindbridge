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

## Expected Assignment 3 Deliverables

- working frontend on port `3000`
- backend API on port `8080`
- Docker and Docker Compose configuration
- deployment documentation
- updated architecture diagrams
- CI/CD/CD workflow files
- submission evidence and screenshots
