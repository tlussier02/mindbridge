# Assignment Scope Recovery Plan

## Purpose

This plan documents the recovery strategy for the current MindBridge branch with one constraint in mind:

- the frontend and backend may be treated as teacher-supplied foundations

Because of that, the correct goal is not to redesign the product. The correct goal is to make the supplied stack:

- secure enough for demonstration
- integrated enough to behave coherently
- deployable enough to satisfy the Assignment 3 submission scope

## Scope Baseline

The assignment-critical success condition is:

- a working web frontend
- a working Spring Boot backend
- coherent frontend-to-backend integration
- Docker-based deployment support
- AWS EC2 deployment readiness
- CI/CD workflow coverage
- submission evidence and documentation

The plan explicitly avoids large cosmetic rewrites or deep product changes unless they block one of the items above.

## What Was Fixed

### 1. User-owned data is now scoped to the authenticated user

Reason:
- raw UUID-based access to diary entries, live sessions, and safety plans was a real completion risk
- it could break demos, invalidate review, and expose user data across accounts

Changes made:
- diary detail and delete now use authenticated user scope
- session chat and end now use authenticated user scope
- crisis safety-plan read/write no longer trusts a raw `userId` query parameter

Why this is in scope:
- this is not feature expansion
- this is correcting incorrect behavior in required backend flows

### 2. Public crisis access was narrowed without breaking the assignment path

Reason:
- some crisis support content can reasonably stay public
- personalized safety-plan data should not

Decision:
- keep general crisis hub, coping strategies, and crisis detection publicly reachable where useful
- require authentication for personal safety-plan operations

Why this is the right tradeoff:
- it preserves likely teacher/demo intent for general crisis support
- it closes the most serious user-data exposure

### 3. Live session flow now carries the correct session identifier

Reason:
- the frontend was starting a live session but then reusing the CBT catalog session ID instead of the created `userSessionId`
- that breaks the real chat/end flow against the backend

Changes made:
- the frontend now stores the returned live session ID from `start`
- the live session end path no longer drops users into static teacher/mock history screens

Why this is in scope:
- this is a direct integration bug in the live assignment path
- it is more important than polishing non-essential teacher-supplied screens

### 4. Deployment automation now matches the actual server model

Reason:
- the previous workflow design mixed Docker Hub pushes with an EC2 server that was still rebuilding from local git state
- that creates false confidence and stale deploys

Changes made:
- CD build now validates Docker Compose builds
- CD deploy now updates the EC2 git checkout to the exact workflow branch and rebuilds there

Why this is in scope:
- deployment correctness is explicitly part of the assignment deliverable

### 5. Local-only state is being removed from source control expectations

Reason:
- H2 database files should not create fake branch churn
- Docker builds should not depend on untracked local clutter

Changes made:
- H2 local artifacts are now ignored
- Docker ignore files are part of the intended tracked deployment setup

Why this is in scope:
- this reduces teammate friction and deployment instability

## What Was Intentionally Not Expanded

### 1. Teacher-supplied static screens were not rewritten wholesale

Reason:
- a full replacement of all static session-history or detailed UI screens would be larger than the assignment-critical fix set
- the current priority is that the real integrated path works and does not route users into broken states

Decision:
- fix the live path
- do not turn the branch into a frontend redesign project

### 2. Font/network fragility in local sandbox builds was not treated as a product rewrite trigger

Reason:
- the frontend production build still depends on Google Fonts access
- that failed in a restricted environment, but it is not evidence of a logic regression in the assignment path

Decision:
- record it as an environment limitation unless it fails again in CI or EC2 under normal network conditions

## Remaining Assignment-Critical Actions

### 1. Commit and push the current scoped recovery changes

This includes:

- backend access-control fixes
- live session integration fix
- workflow alignment
- ignore/hygiene cleanup
- updated teammate/deployment docs

### 2. Confirm EC2 security group behavior for backend surfaces

Current expectation:

- frontend should remain reachable on `3000`
- backend tooling such as Swagger, H2, and health need explicit `8080` access if they must be shown publicly

Decision rule:
- if submission evidence requires public direct access to `8080`, keep the rule open
- if not, keep exposure tighter and gather evidence from the server or proxy path

### 3. Re-run workflow verification after push

Needed checks:

- CI still passes
- CD build validation runs on the active branch
- CD deploy targets the correct branch on EC2

### 4. Capture final evidence

Required evidence should include:

- public frontend
- Swagger
- H2 console if required by submission
- actuator health
- any MCP endpoint evidence required by the rubric

## Verification Status

Completed:

- backend compile pass succeeded
- targeted controller/service tests for the scoped changes passed
- the live session path bug was corrected in code

Known environment limitation:

- frontend production build failed in a restricted environment because Google Fonts could not be fetched

Interpretation:

- this is a network-dependent build issue, not direct evidence that the scoped frontend changes are broken
- it should still be checked again in CI and/or on EC2

## Decision Summary

The governing principle for this recovery branch is:

- fix what threatens the assignment submission
- do not overbuild teacher-supplied surfaces

That means the branch should prioritize:

- real integration
- data ownership
- deployment truth
- documentation truth

over:

- speculative feature work
- redesigning static screens that are outside the critical live path
