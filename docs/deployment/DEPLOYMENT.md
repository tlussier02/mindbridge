# Deployment Guide
## MindBridge

## Purpose
This document captures the expected Assignment 3 deployment flow for the merged MindBridge project.

It is intended to be a clean submission-facing deployment guide. If Timmy changes Docker, Nginx, workflow, or EC2 details, update this file to match the final deployed configuration before submission.

## Deployment Target

The intended deployment model is:

- frontend: browser-based web application
- backend: Spring Boot API
- reverse proxy: Nginx
- hosting target: AWS EC2
- container orchestration: Docker Compose

## Expected Runtime Surfaces

The final deployed system should provide:

- frontend on port `3000`
- Swagger UI proxied through port `3000`
- H2 console proxied through port `3000`
- actuator health endpoint proxied through port `3000`
- MCP endpoint exposure as required by the Assignment 3 deliverables

## Prerequisites

- AWS EC2 instance with SSH access
- Docker installed
- Docker Compose available
- Git installed
- required environment variables prepared
- GitHub repository secrets configured for deployment

## Required GitHub Secrets

The deploy workflow expects these repository secrets:

- `EC2_HOST`: the EC2 public DNS or IP
- `EC2_USER`: the SSH user, typically `ec2-user`
- `EC2_SSH_KEY`: the full PEM private key contents for the EC2 instance

## Required Environment Variables

Create a `.env` file in the project root with the values required by the backend and deployment stack.

Use the tracked template:

```bash
cp .env.example .env
```

Minimum expected values:

```env
ANTHROPIC_API_KEY=your_api_key_here
JWT_SECRET=your_secure_random_secret
```

If additional deployment variables are introduced later, document them here before submission.

## Repository Setup On EC2

1. SSH into the EC2 instance.
2. Clone the project repository.
3. Change into the project root.
4. Create the `.env` file with the required values.
5. Confirm Docker and Docker Compose are available.

## Launch Process

Run the deployment stack from the project root:

```bash
docker compose up -d --build
```

The current deployment branch uses `docker compose` syntax and a git-based deploy flow on EC2.

## Verification Checklist

After deployment, verify:

1. frontend loads in a browser
2. Swagger UI loads through `:3000/swagger-ui`
3. H2 console loads through `:3000/h2-console/`
4. actuator health returns `UP` through `:3000/actuator/health`
5. MCP endpoint is reachable if exposed over HTTP in the final submission

## Evidence To Capture

Store screenshots under `docs/deployment/screenshots/` for:

- frontend home page with public IP visible
- Swagger UI with public IP visible
- H2 console with public IP visible
- actuator health endpoint with public IP visible

Store any supporting notes or deployment logs under `docs/deployment/evidence/`.

## Troubleshooting

- check container logs with:

```bash
docker compose logs -f
```

- confirm the EC2 security group exposes the required ports
- confirm `.env` variables are populated
- confirm frontend and backend containers both started successfully
- confirm the reverse proxy file matches the final deployment layout

## Final Note

This file must match the actual submitted deployment configuration. Before submission, re-check it against:

- `docker-compose.yml`
- `nginx.conf`
- Dockerfiles
- workflow files
- final EC2 deployment behavior
