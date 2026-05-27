---
phase: 02-entry-loop-invite-register-login
plan: "03"
subsystem: backend/registration
tags: [registration, tdd, cors, s3, invite, multi-tenant, auth]
dependency_graph:
  requires: [02-01, 02-02]
  provides: [registration-controller, cors-middleware, register-route]
  affects: [src/app.js, src/routes/auth.routes.js, package.json]
tech_stack:
  added: ["cors@^2.8.6"]
  patterns: [invite-bound-registration, server-issued-avatar-key, tdd-red-green, cors-env-origin]
key_files:
  created:
    - src/controllers/registration.controller.js
    - src/controllers/registration.controller.test.js
  modified:
    - src/routes/auth.routes.js
    - src/app.js
    - package.json
decisions:
  - "avatarKey (server-issued bare key) used directly for objectExists — never reverse-parsed from client-supplied avatarUrl (T-02-08b)"
  - "role and institutionId sourced exclusively from the invite document, never from req.body (T-02-09, D-12)"
  - "409 returned on Mongo duplicate-key error code 11000 (compound index: institutionId + email) — confirms D-08"
  - "invite.status flipped to expired when usesCount reaches maxUses after incrementing (D-06)"
  - "201 returns user object without JWT token — UI-SPEC redirects to /login?registered=1 (no auto-login)"
  - "cors middleware placed after express.json(), before all routers — FRONTEND_ORIGIN env var controls allowed origin"
  - "Module._load mock for s3.service installed before registration.controller require — ensures mock is captured at module load time"
metrics:
  duration: "7 min"
  completed_date: "2026-05-27"
  tasks_completed: 2
  files_changed: 5
---

# Phase 02 Plan 03: Registration Vertical Slice Summary

Invite-bound registration with mandatory S3 photo: a valid invite becomes a tenant/role-scoped bcrypt-hashed user only after objectExists confirms the server-issued avatarKey; CORS enabled via FRONTEND_ORIGIN; 9/9 TDD tests pass with S3 mocked.

## Tasks Completed

| # | Task | Commit | Type |
|---|------|--------|------|
| SC | cors supply-chain gate (pre-approved by orchestrator) | — | chore |
| 1 | TDD RED — failing integration test for registration controller | e6b9cab | test |
| 2 | Registration controller + route + CORS — GREEN | 3fe6262 | feat |

## What Was Built

**registration.controller.js** (`src/controllers/registration.controller.js`): `register(req, res)` handler:
- Validates all required fields: token, name, email, password, avatarUrl, avatarKey
- Loads Invite by token; rejects 400 if not found, status !== 'active', expiresAt < now, or usesCount >= maxUses (D-06)
- Calls `s3Service.objectExists(avatarKey)` using the server-issued key directly (T-02-08b — never reverse-parses avatarUrl)
- Hashes password with `bcrypt.hash(password, env.BCRYPT_ROUNDS)`
- Creates User with institutionId and role sourced from invite (never req.body — T-02-09, D-12)
- Returns 409 on Mongo duplicate-key error 11000 (compound index {institutionId, email})
- Increments invite.usesCount; flips invite.status to 'expired' when usesCount reaches maxUses
- Returns 201 with `{ id, name, email, role, institutionId, avatarUrl }` — no JWT token (UI-SPEC)

**auth.routes.js** (`src/routes/auth.routes.js`): Added `router.post('/register', register)` alongside the existing login route (public, no auth middleware).

**app.js** (`src/app.js`): Added `cors` middleware using `env.FRONTEND_ORIGIN` (default `http://localhost:5173`), placed after `express.json()` and before all routers.

**registration.controller.test.js** (`src/controllers/registration.controller.test.js`): 9 integration tests using mongodb-memory-server:
- Happy path: 201 with correct user scoping, bcrypt hash, incremented usesCount, no JWT
- Response shape: id, name, email, role, institutionId, avatarUrl — no passwordHash exposed
- Expired invite → 400, no user
- Revoked invite → 400, no user
- Full invite (usesCount === maxUses) → 400, no user
- Missing avatarKey → 400, no user
- objectExists returns false → 400, no user
- Duplicate email in same institution → 409, only one user
- Last slot consumed → invite.status flips to expired; next attempt → 400

## Test Results

9/9 registration tests passing. Full suite: 42/42 across all 6 test files (seed, auth.middleware, role.middleware, invite.controller, media.controller, registration.controller).

## Deviations from Plan

### Auto-fixed Issues

**1. [Rule 1 - Bug] Restructured Module._load mock to install before controller require**
- **Found during:** Task 2 first GREEN run — 4 of 9 tests returning 400 instead of 201
- **Issue:** The test file originally required `registration.controller` at line 10, then set up the `Module._load` mock at line 22. Node.js already executed `require('../services/s3.service')` inside the controller before the mock was in place, so the controller held a reference to the real AWS-backed s3Service. The `objectExists` calls hit the real (uncredentialed) implementation which returned false, causing all avatar-dependent paths to 400.
- **Fix:** Moved `Module._load` override to lines 12-24 (immediately after env vars), then `require('./registration.controller')` at line 28. The controller's `require('../services/s3.service')` now intercepts the mock.
- **Files modified:** `src/controllers/registration.controller.test.js`
- **Commit:** 3fe6262 (included in GREEN commit)

## TDD Gate Compliance

- RED gate commit: e6b9cab (`test(02-03): add failing integration test for registration controller (RED)`) — MODULE_NOT_FOUND confirmed
- GREEN gate commit: 3fe6262 (`feat(02-03): implement registration controller, route, and CORS (GREEN)`) — 9/9 passing

## Known Stubs

None — registration is fully wired. S3 `objectExists` will return false without live AWS credentials in production (expected; configure S3 env vars on VPS deployment).

## Threat Flags

No new security surface beyond the plan's threat model. All STRIDE entries mitigated as designed:
- T-02-09: role/institutionId from invite only, never req.body
- T-02-10: objectExists(avatarKey) required before user creation; avatarKey is server-issued (T-02-08b)
- T-02-11: status/expiry/slot checks before any user creation; usesCount incremented after create
- T-02-12: bcrypt.hash with BCRYPT_ROUNDS; passwordHash never returned or logged
- T-02-13: 409 accepted (invite-only UX, UI-SPEC copy)

## Self-Check: PASSED

- src/controllers/registration.controller.js: FOUND
- src/controllers/registration.controller.test.js: FOUND
- src/routes/auth.routes.js (register route added): FOUND
- src/app.js (cors middleware added): FOUND
- Commit e6b9cab: FOUND
- Commit 3fe6262: FOUND
