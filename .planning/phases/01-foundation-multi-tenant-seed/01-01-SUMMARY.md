---
phase: 01-foundation-multi-tenant-seed
plan: 01
subsystem: database
tags: [express, mongoose, mongodb, bcrypt, jsonwebtoken, node-test, mongodb-memory-server]

# Dependency graph
requires: []
provides:
  - Express/MongoDB walking skeleton with health check
  - Institution Mongoose model (name, slug, logoUrl, active, createdAt)
  - User Mongoose model with required institutionId ref (PLAT-01 multi-tenant scoping pattern)
  - Category Mongoose model (name, slug, type enum content|area)
  - Idempotent seed: catolica-pb institution + 10 fixed categories + bootstrap admin (PLAT-02)
  - GET /health endpoint reporting Mongo connection state
  - src/config/env.js: fail-fast on missing MONGODB_URI or JWT_SECRET
  - src/config/db.js: connectDB + getReadyState helpers
affects: [02-auth-invite, 03-profile-follow, 04-posts-feed, 05-notifications, 06-chat, 07-frontend]

# Tech tracking
tech-stack:
  added:
    - express 4.x
    - mongoose 8.x
    - bcrypt 5.x
    - jsonwebtoken 9.x
    - dotenv 16.x
    - mongodb-memory-server 10.x (devDependency — self-contained test DB)
    - node:test (built-in, no extra framework)
  patterns:
    - CommonJS (require/module.exports) throughout — consistent with existing Node.js ecosystem
    - institutionId: ObjectId ref Institution (required) on all tenant-owned models — PLAT-01 pattern
    - Seed idempotency via findOneAndUpdate upsert + $setOnInsert for password hash
    - connectDB(uri) takes URI as arg so tests pass their own URI without mutating process.env
    - getReadyState() in db.js shields health route from direct mongoose dependency

key-files:
  created:
    - package.json
    - .gitignore
    - .env.example
    - src/config/env.js
    - src/config/db.js
    - src/models/Institution.js
    - src/models/User.js
    - src/models/Category.js
    - src/seed/seed.js
    - src/seed/seed.test.js
    - src/routes/health.routes.js
    - src/app.js
    - src/server.js
  modified: []

key-decisions:
  - "CommonJS (not ESM) — keeps require/module.exports consistent across Node.js backend; avoids ESM/CJS interop issues with dotenv and mongoose"
  - "mongodb-memory-server as devDependency — test suite is self-contained, no external MongoDB needed for CI"
  - "node:test built-in — zero extra test-runner dependencies, aligned with Simplicity First"
  - "bcrypt (not bcryptjs) — native bindings, better performance in production"
  - "$setOnInsert for passwordHash in seed upsert — prevents re-hashing on every seed run (idempotency + T-01-02 threat mitigation)"
  - "connectDB returns the mongoose instance; getReadyState() abstraction keeps health route decoupled from raw mongoose"

patterns-established:
  - "PLAT-01 pattern: every tenant-owned model carries institutionId: { type: ObjectId, ref: 'Institution', required: true }"
  - "Seed idempotency: upsert by natural unique key (slug/email), $set for mutable fields, $setOnInsert for immutable fields (passwordHash)"
  - "app.js exports app, server.js calls listen — keeps app testable without a live port"
  - "env.js throws on missing required vars — server never starts misconfigured"

requirements-completed: [PLAT-01, PLAT-02]

# Metrics
duration: 3min
completed: 2026-05-21
---

# Phase 1 Plan 01: Foundation & Multi-Tenant Seed Summary

**Express/MongoDB walking skeleton with Institution/User/Category models, idempotent Católica PB seed, and GET /health endpoint — multi-tenant institutionId scoping pattern established**

## Performance

- **Duration:** 3 min
- **Started:** 2026-05-21T18:31:15Z
- **Completed:** 2026-05-21T18:34:16Z
- **Tasks:** 3
- **Files modified:** 13 created

## Accomplishments

- TDD RED/GREEN cycle: failing test committed first, then models + seed implemented to make all 5 assertions pass
- Three Mongoose models (Institution, User, Category) with correct schema shapes from CONTEXT.md
- Idempotent runSeed(): upserts catolica-pb institution, 4 content + 6 area categories, bootstrap admin with bcrypt hash only on insert
- GET /health returns 200 when Mongoose readyState=1, 503 otherwise — decoupled via getReadyState()
- server.js exits non-zero on DB connect failure (T-01-05 mitigation), app.js does not call listen (testable)
- env.js throws on missing MONGODB_URI or JWT_SECRET (T-01-05 mitigation)

## Task Commits

Each task was committed atomically:

1. **Task 1: Scaffold project and write failing seed test** - `452c7b0` (test — RED)
2. **Task 2: Implement models and idempotent seed** - `85dea99` (feat — GREEN)
3. **Task 3: Express app with health check** - `7ef81ea` (feat)

## Files Created/Modified

- `package.json` - Node.js project manifest with start/dev/seed/test scripts
- `.gitignore` - Excludes node_modules, .env, coverage
- `.env.example` - Documents all required env vars with safe placeholder values
- `src/config/env.js` - Fail-fast config loader; throws if MONGODB_URI or JWT_SECRET absent
- `src/config/db.js` - connectDB(uri) and getReadyState() helpers
- `src/models/Institution.js` - Institution schema: name, slug (unique), logoUrl, active, createdAt
- `src/models/User.js` - User schema with required institutionId (PLAT-01), role enum, isPrivate default false
- `src/models/Category.js` - Category schema with type enum [content, area]
- `src/seed/seed.js` - runSeed(): idempotent upserts, $setOnInsert for passwordHash
- `src/seed/seed.test.js` - 5 end-to-end assertions using node:test + mongodb-memory-server
- `src/routes/health.routes.js` - GET /health: 200 ok or 503 degraded based on Mongo readyState
- `src/app.js` - Express app with JSON parsing, health router mounted, no listen call
- `src/server.js` - Connects to MongoDB then listens; exits non-zero on connection failure

## Decisions Made

- **CommonJS over ESM:** Keeps require/module.exports consistent; avoids ESM/CJS interop friction with dotenv and mongoose in this Node.js backend.
- **mongodb-memory-server:** Tests are fully self-contained — no external MongoDB required. Aligns with Simplicity First (no test infrastructure to manage).
- **node:test built-in:** Zero additional test-runner dependencies.
- **bcrypt (not bcryptjs):** Native bindings for production performance.
- **$setOnInsert for passwordHash:** Prevents re-hashing on every seed run — addresses T-01-02 (seed idempotency threat) and T-01-01 (password hash security).

## Deviations from Plan

None — plan executed exactly as written.

## Issues Encountered

None.

## Known Stubs

None — all data flows are wired. The seed creates real persisted records; health check reads real connection state.

## Threat Flags

None — all T-01-0x threats from the plan's threat register were addressed:
- T-01-01: bcrypt hash stored, plaintext never persisted or logged
- T-01-02: $setOnInsert for passwordHash, upsert-keyed by natural unique fields
- T-01-03: institutionId required on User (PLAT-01 pattern established)
- T-01-04: /health reports only boolean connection state — accepted
- T-01-05: env.js throws on missing vars; server.js exits non-zero on DB failure

## Next Phase Readiness

- Foundation is solid. Phase 01 Plan 02 (auth middleware + JWT) can build directly on User model and env.js config.
- connectDB and getReadyState helpers are ready for test reuse.
- The PLAT-01 institutionId pattern is established — all future tenant-owned models should mirror User.js.

---
*Phase: 01-foundation-multi-tenant-seed*
*Completed: 2026-05-21*
