# Submission Checklist

## Frontend
- [ ] Next.js frontend source is present under `mindbridge/`
- [ ] frontend runs locally on port `3000`
- [ ] authentication flow works in browser
- [ ] session flow works in browser
- [ ] diary flow works in browser
- [ ] progress flow works in browser
- [ ] crisis support flow works in browser

## Backend
- [x] Spring Boot backend source is present
- [x] OpenAPI artifact exists at `docs/api/openapi.json`
- [x] CLI is disabled by default
- [ ] backend runs cleanly on port `8080`
- [ ] actuator health endpoint returns `UP`

## Deployment
- [x] root `Dockerfile` exists
- [x] frontend Dockerfile exists
- [x] `docker-compose.yml` exists
- [ ] `nginx.conf` is present with final correct filename and config
- [ ] `DEPLOYMENT.md` is present with final correct filename and clean content
- [ ] frontend is exposed on the required production port
- [ ] backend deployment paths are verified

## MCP
- [x] MCP code exists in backend source
- [ ] MCP deliverable scope is complete
- [ ] MCP tools/resources/prompts are verified against Assignment 3 requirements

## Workflows
- [ ] `.github/workflows/ci.yml` exists in the correct folder
- [ ] build workflow exists
- [ ] deploy workflow exists
- [ ] workflows are valid and reviewable

## Documentation
- [x] root `README.md` exists
- [x] CLI deprecation is documented
- [ ] architecture docs reflect Assignment 3
- [ ] deployment diagram exists
- [ ] CI/CD pipeline diagram exists
- [ ] deployment guide is final

## Evidence
- [ ] frontend screenshot with deployed public IP visible
- [ ] Swagger screenshot with deployed public IP visible
- [ ] H2 console screenshot with deployed public IP visible
- [ ] actuator health screenshot with deployed public IP visible
- [ ] architecture image artifacts exist if required for submission
- [ ] pipeline screenshots or evidence exist if required

## Final Verification
- [ ] final smoke test completed
- [ ] filenames/paths are cleaned up
- [ ] no stale CLI-first wording remains in submission docs
- [ ] project is ready to push
