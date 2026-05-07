# SWE 320 — Assignment 3 Work Breakdown
## MindBridge / Digital Therapy Assistant

## Objective
Finish Assignment 3 using the current merged `mindbridge` project state, not the earlier pre-merge assumptions.

The merged project already has:
- a Spring Boot backend
- a real Next.js frontend in `mindbridge/`
- AI/RAG backend code
- Swagger/OpenAPI generation
- backend tests
- frontend API integration work
- Docker-related files
- a basic MCP endpoint
- CLI disabled by default

The merged project is still missing or incomplete on several Assignment 3 requirements:
- final deployment config cleanup
- final AWS EC2 deployment verification
- full MCP deliverable scope
- complete GitHub Actions workflow set
- browser-based functional test evidence
- deployment screenshots and final submission evidence
- deployment and CI/CD reporting artifacts

This breakdown is designed to:
1. match the actual repo gaps
2. assign work by the current repo structure
3. keep ownership clear across frontend, infrastructure, and documentation

---

## Current Repo Reality

### Already present
- Spring Boot backend under `src/main/java/com/digitaltherapy`
- Next.js frontend under `mindbridge/`
- OpenAPI artifacts under `docs/api/` and `mindbridge/docs/`
- architecture docs under `docs/architecture/`
- backend test suite under `src/test/`
- frontend API integration drop appears to be included
- Docker-related files are present
- a basic MCP endpoint exists
- CLI is already disabled by default
- root README exists in the merged project
- submission checklist exists
- evidence folders exist
- architecture doc cleanup has started

### Major gaps still blocking Assignment 3
- deployment config still needs cleanup to match Assignment 3 port and path requirements
- the deployment guide now exists in clean form but still needs to be validated against the final deployment setup
- only a basic CI workflow appears present; build/deploy workflows still appear missing
- MCP support exists at a basic level but still needs to be expanded to match full deliverable scope
- no committed browser-based end-to-end test suite or test evidence is visible
- architecture docs have been partially updated, but the full diagram/reporting set is not yet confirmed complete
- deployment evidence, screenshots, and final report-ready artifacts are still missing

---

## Remaining Work In Original Project Scope

This section maps the remaining tasks back to the original Assignment 3 ownership plan.

### Josh — Frontend and browser-facing flow

#### Likely already completed or mostly completed
- frontend API layer
- core web screens
- live backend workflow integration

#### Josh still needs to verify or finish
1. Confirm every required Assignment 3 browser flow works end-to-end:
   - auth
   - sessions/chat
   - diary
   - progress
   - crisis support
2. Verify there are no remaining mock-only screens in the browser demo path.
3. Verify token handling, refresh handling, and protected-route behavior.
4. Confirm the frontend runs cleanly on port `3000` in local development.
5. Help validate the final production routing once Timmy fixes the Nginx and Docker setup.
6. Confirm the final browser demo path uses real backend data and not fallback mock behavior.

### Timmy — Deployment, MCP, automation, and infrastructure

#### Timmy still needs to finish
1. Fix Docker Compose and Nginx to match Assignment 3 expectations:
   - frontend exposed on port `3000`
   - backend accessible on port `8080`
   - proxy behavior clearly defined and working
2. Validate the deployment guide against the final deployment setup.
3. Add the missing GitHub Actions workflows:
   - CI
   - CD build
   - CD deploy
4. Expand MCP deliverables beyond the current minimal tools listing:
   - tools
   - resources
   - prompts
   - any required launch/config behavior from the assignment
5. Add or commit browser-based functional testing or final test evidence if that is part of the team’s submission plan.
6. Verify the deployed app supports:
   - frontend access
   - Swagger
   - H2 console
   - actuator health
   - MCP endpoint access

### Trevor — Docs, architecture, evidence, and submission readiness

#### Trevor still needs to finish
1. Keep the root README and docs aligned with the final merged project.
2. Confirm CLI deprecation wording remains accurate:
   - frontend is primary
   - CLI remains in repo
   - CLI is disabled by default
   - how to re-enable if needed
3. Finish and verify architecture/report artifacts for Assignment 3:
   - web frontend instead of CLI-first flow
   - backend API
   - Docker / Docker Compose
   - EC2 deployment
   - Nginx proxy relationship
4. Track whether deployment and CI/CD diagrams are still missing, and keep placeholder notes until they are delivered.
5. Keep the final submission checklist current.
6. Keep evidence folders organized for:
   - deployment screenshots
   - architecture images
   - pipeline screenshots if needed
7. After Josh and Timmy finish, run the final smoke pass and collect:
   - frontend screenshot
   - Swagger screenshot
   - H2 screenshot
   - actuator health screenshot

---

## Ordered Finish Plan

### Step 1
- Timmy locks the final deployment config and workflow set.

### Step 2
- Josh validates that the browser app is fully using the backend and is demo-ready.

### Step 3
- Timmy finalizes MCP scope, workflows, and deployment verification.

### Step 4
- Trevor updates and verifies architecture docs, README, and submission checklist against the stabilized project.

### Step 5
- Team performs one final deployment + smoke-test pass.

### Step 6
- Trevor gathers screenshots and submission evidence.

---

## Workload Strategy

### Josh ownership area
- frontend conversion from mock UI to real app behavior
- browser-facing workflows and API integration

### Timmy ownership area
- infrastructure, deployment, automation, and MCP/server integration

### Trevor ownership area
- configuration cleanup, documentation, evidence capture, and submission preparation

---

## Team Roles and Ownership

### 1) Josh — Frontend Integration + UX + Browser Functionality
**Primary focus:** make the existing Next.js frontend actually function against the backend

#### Why Josh gets this
- the repo already has a frontend shell, but it is not wired to real backend data
- this is the single biggest Assignment 3 gap
- it is more work than Trevor’s support/documentation scope

#### Core ownership
- convert the frontend from mock-state driven to API-driven
- authentication flow in the frontend
- session/chat UI integration
- diary UI integration
- progress and crisis UI integration
- loading states, error states, and user feedback
- responsive cleanup and route/view flow

#### Main files Josh should expect to own
- `mindbridge/app/**`
- `mindbridge/components/web/**`
- `mindbridge/components/flows/**`
- `mindbridge/lib/**`
- any new `mindbridge/services/**` or `mindbridge/lib/api/**`
- frontend auth/session storage utilities

#### Remaining frontend work
- verify login/register/logout/refresh against the real backend
- verify diary/session/progress/crisis state is backed by live API calls
- confirm protected-route or protected-screen behavior
- clean up any remaining error, loading, or empty-state issues
- confirm the required Assignment 3 browser workflows are stable:
  - Authentication
  - CBT Sessions
  - Thought Diary
  - Progress Dashboard
  - Crisis Support

#### Josh deliverables
- working frontend on port `3000`
- frontend talking to backend on port `8080`
- proxy/dev setup for local development
- real user flow instead of static mock behavior
- browser-usable demo path across all major features

---

### 2) Timmy — Backend Assignment 3 Infrastructure + MCP + Deployment
**Primary focus:** everything needed to make the system deployable, automatable, and Assignment 3-complete on the backend/infrastructure side

#### Why Timmy gets this
- Assignment 3 is not just frontend work
- the biggest missing backend-side deliverables are deployment, automation, and MCP work
- this is heavy but more infrastructure-oriented than Josh’s frontend integration load

#### Core ownership
- MCP server implementation
- Dockerization
- Docker Compose
- Nginx production proxying
- EC2 deployment setup
- GitHub Actions CI/CD/CD
- actuator/health and deployment verification
- Selenium or browser automation evidence path

#### Main files Timmy should expect to own
- root `Dockerfile`
- `mindbridge/Dockerfile` or `frontend/Dockerfile`
- `docker-compose.yml`
- `nginx.conf`
- `.github/workflows/**`
- `docs/deployment/DEPLOYMENT.md`
- MCP server config/classes under backend source tree
- health/deploy-related config files

#### Remaining backend/infrastructure work
- expand the required MCP server deliverables
- add the required tools/resources/prompts on top of existing services
- finalize Docker Compose and Nginx for the required ports and routes
- validate EC2 deployment instructions against the final config
- keep CI pipeline in the correct repo path
- add CD build pipeline
- add CD deploy pipeline
- ensure public verification works for:
  - frontend
  - Swagger
  - H2 console
  - actuator health endpoint

#### Testing/verification Timmy should own
- deployment verification checklist
- browser automation strategy
- pipeline pass criteria
- post-deploy smoke validation

#### Timmy deliverables
- Dockerized full stack
- MCP server capability
- EC2 deployment docs and workflow
- CI/CD/CD workflows
- health/deploy verification path

---

### 3) Trevor — Configuration + Docs + Submission Assembly
**Primary focus:** configuration cleanup, architecture docs, evidence capture, and final submission readiness

#### Core ownership
- CLI deprecation config cleanup
- README and setup docs
- architecture doc updates
- deployment screenshots and submission evidence
- final packaging and checklist tracking
- light smoke testing and issue reporting

#### Main files Trevor should expect to own
- `src/main/resources/application.properties`
- `README.md` if added or created
- `docs/architecture/**`
- `docs/deployment/**`
- screenshot/evidence folders if created
- final checklist documents

#### Trevor required tasks
- keep CLI deprecation docs accurate
- keep architecture docs aligned with the final project state
- track missing reporting artifacts
- keep the final submission checklist current
- gather evidence/screenshots once Josh and Timmy finish their sections
- verify required URLs and deliverables exist
- compare final repo contents against assignment checklist

#### Trevor deliverables
- corrected docs and diagrams
- CLI deprecation note
- final evidence collection
- final submission readiness checklist
- final sanity pass before submit

## Exact Task Split

### Josh must do
1. Verify auth flows against the real backend.
2. Verify session screens and AI chat against the real backend.
3. Verify diary CRUD and distortion suggestions against the real backend.
4. Verify progress and crisis screens against the real backend.
5. Fix any remaining loading, error, and success feedback issues.
6. Confirm the frontend is usable on desktop and mobile widths.

### Timmy must do
1. Expand the MCP server layer to full deliverable scope.
2. Finalize `docker-compose.yml`.
3. Finalize `nginx.conf`.
4. Validate EC2 deployment documentation.
5. Add GitHub Actions workflows for build and deploy.
6. Add health/deployment validation flow.
7. Add browser automation or equivalent functional-test evidence.

### Trevor must do
1. Keep the merged-project docs aligned with the final implementation.
2. Track missing diagrams and reporting artifacts.
3. Keep the final submission checklist current.
4. Collect screenshots and proof artifacts after implementation is done.
5. Run the final manual smoke pass and note any gaps.

---

## Suggested Timeline

### Phase 1 — First 1 to 2 days
- Josh:
  - define frontend API client structure
  - wire auth and dashboard first
- Timmy:
  - scaffold Docker, Compose, MCP structure, and workflow files
  - define deployment file layout
- Trevor:
  - update planning docs
  - fix CLI deprecation setting
  - draft architecture changes list

### Phase 2 — Middle implementation block
- Josh:
  - finish session, diary, progress, and crisis frontend flows
- Timmy:
  - finish MCP endpoints/tools/resources/prompts
  - finish Docker/Compose/Nginx
  - finish workflow automation
- Trevor:
  - update diagrams and docs using the now-stable system shape
  - prepare evidence folders/checklists

### Phase 3 — Final integration block
- Josh:
  - frontend polish, bug fixes, and browser behavior cleanup
- Timmy:
  - deployment validation and pipeline fixes
- Trevor:
  - screenshots, final checklist, README/doc sync, final QA notes

---

## Priority Order

### Highest priority
1. Frontend must actually call the backend
2. CLI must be disabled by default
3. Docker + Compose must exist
4. EC2 deployment path must exist
5. MCP server deliverables must exist

### Medium priority
1. GitHub Actions workflows
2. functional browser testing/evidence
3. architecture updates

### Final polish
1. screenshots
2. README cleanup
3. grading checklist alignment

---

## Risk Notes

### Josh risk
- if frontend remains mock-state based, Assignment 3 is not complete

### Timmy risk
- MCP and deployment work can consume time quickly if delayed

### Trevor risk
- docs and evidence still need to track the final implemented system accurately

---

## Final Deliverable Checklist

### Josh area complete when
- frontend is no longer hardcoded
- auth/session/diary/progress/crisis flows hit real backend endpoints
- user-facing states are polished enough for demo

### Timmy area complete when
- MCP server works
- Docker and Compose work
- deployment docs are reproducible
- CI/CD/CD files are present and meaningful

### Trevor area complete when
- CLI is disabled by default
- docs reflect web + cloud architecture
- screenshots and evidence are captured
- final checklist confirms all required files exist

---
