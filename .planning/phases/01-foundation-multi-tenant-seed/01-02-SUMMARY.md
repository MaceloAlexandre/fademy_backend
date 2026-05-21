---
phase: 01-foundation-multi-tenant-seed
plan: 02
subsystem: auth
tags: [express, jsonwebtoken, bcrypt, jwt, middleware, auth, role-guard, multi-tenant]

# Dependency graph
requires: [01-01]
provides:
  - JWT token service (signToken/verifyToken) with HS256 algorithm pinned
  - auth.middleware: Bearer token verification, attaches req.user = {id, role, institutionId}
  - role.middleware: requireRole factory — rejects unauthorized roles with 403 before controller
  - POST /api/auth/login: mints JWT for valid credentials (no user enumeration)
  - GET /api/admin/overview: institution-scoped admin route exercising full auth→role→controller chain
affects: [02-auth-invite, 03-profile-follow, 04-posts-feed, 05-notifications, 06-chat, 07-frontend]

# Tech tracking
tech-stack:
  added: []
  patterns:
    - Algorithm-pinned JWT: algorithms: ['HS256'] on both sign and verify (defeats alg:none / alg-confusion)
    - Fail-closed auth: any verification failure → 401, never pass-through
    - AUTH-03 pattern: requireRole called in route definition before controller — role rejection fires before controller runs
    - PLAT-01 query pattern: every controller query filtered by req.user.institutionId from verified token
    - Timing-safe login: dummy-hash bcrypt.compare for unknown emails prevents timing-based user enumeration

key-files:
  created:
    - src/services/token.service.js
    - src/middlewares/auth.middleware.js
    - src/middlewares/role.middleware.js
    - src/middlewares/auth.middleware.test.js
    - src/middlewares/role.middleware.test.js
    - src/controllers/auth.controller.js
    - src/routes/auth.routes.js
    - src/controllers/admin.controller.js
    - src/routes/admin.routes.js
  modified:
    - src/app.js (mount point only — surgical)
    - package.json (test script extended)

key-decisions:
  - "HS256 pinned on both sign AND verify — jsonwebtoken algorithms option is an array; passing ['HS256'] prevents alg:none and alg-confusion attacks (T-02-01)"
  - "Dummy-hash bcrypt.compare for unknown emails — timing-safe login prevents user enumeration even on missed DB lookups (T-02-04)"
  - "requireRole called inline in route definition, not as a separate step — keeps the auth→role→controller chain visible and explicit per AUTH-03"
  - "institutionId sourced from verified JWT, never from request body — prevents IDOR cross-tenant data access (T-02-03)"
  - "No new npm packages — jsonwebtoken and bcrypt were already installed in Plan 01 (T-02-SC)"

# Metrics
duration: 8min
completed: 2026-05-21
---

# Phase 1 Plan 02: Auth Middleware + JWT Summary

**JWT token service (HS256-pinned), fail-closed auth middleware, requireRole factory with 403-before-controller enforcement, and institution-scoped admin route completing the Walking Skeleton end-to-end**

## Performance

- **Duration:** 8 min
- **Started:** 2026-05-21T18:32:00Z
- **Completed:** 2026-05-21T18:40:01Z
- **Tasks:** 2 (TDD RED + GREEN for Task 1; standard for Task 2)
- **Files modified:** 9 created, 2 modified

## Accomplishments

- TDD RED/GREEN cycle for Task 1: 11 failing tests committed first (RED), then all 16 tests passing after implementation (GREEN)
- token.service.js: signToken/verifyToken with algorithm pinned to HS256 on both sign and verify — defeats alg:none and algorithm-confusion attacks (T-02-01)
- auth.middleware: reads Bearer token, verifies via token.service, attaches req.user = {id, role, institutionId}; fails closed with 401 on any error path
- role.middleware: requireRole(...roles) factory — 401 when req.user absent, 403 when role disallowed, rejects before controller per AUTH-03
- auth.controller.login: bcrypt-compare, dummy-hash timing safety for unknown emails (T-02-04), generic 401 for both bad email and bad password
- admin.controller.getInstitutionOverview: institution document + user count, both queries scoped strictly to req.user.institutionId (PLAT-01 / T-02-03)
- app.js surgically updated at the Plan 01 commented mount point — no other lines touched
- All 16 tests pass (5 seed + 5 auth.middleware + 4 role.middleware + 2 describe-level)

## Task Commits

1. **Task 1 RED: test(01-02)** — `68d6e5e` — failing tests + stubs
2. **Task 1 GREEN: feat(01-02)** — `815a9a0` — token service + auth/role middleware
3. **Task 2: feat(01-02)** — `c6a3057` — login endpoint + admin route + app.js wiring

## TDD Gate Compliance

- RED gate: commit `68d6e5e` — `test(01-02): add failing tests for auth + role middleware (RED)`
- GREEN gate: commit `815a9a0` — `feat(01-02): implement token service + auth/role middleware (GREEN)`
- 11 tests failed in RED, 0 tests failed in GREEN — gate sequence valid

## Files Created/Modified

- `src/services/token.service.js` — signToken/verifyToken, HS256 pinned, uses JWT_SECRET + JWT_EXPIRES_IN
- `src/middlewares/auth.middleware.js` — Bearer token verifier, sets req.user, fails closed
- `src/middlewares/role.middleware.js` — requireRole factory, 401/403 before controller
- `src/middlewares/auth.middleware.test.js` — 5 behaviors: valid token, no header, malformed, expired, alg:none
- `src/middlewares/role.middleware.test.js` — 4 behaviors: allowed, disallowed, no req.user, missing role
- `src/controllers/auth.controller.js` — login handler with bcrypt-compare and timing-safe dummy hash
- `src/routes/auth.routes.js` — POST /login
- `src/controllers/admin.controller.js` — getInstitutionOverview scoped by institutionId
- `src/routes/admin.routes.js` — GET /overview guarded by authenticate + requireRole('admin')
- `src/app.js` — mount point updated (surgical — only the 2 commented lines replaced)
- `package.json` — test script extended to include middleware test files

## Decisions Made

- **HS256 pinned on both sign and verify:** Using `algorithms: ['HS256']` in jwt.verify prevents alg:none and alg-confusion attacks where an attacker crafts a token with a different algorithm header.
- **Dummy-hash bcrypt.compare for unknown emails:** Calling bcrypt.compare against a fixed dummy hash when the user is not found makes the timing indistinguishable from a real failed comparison, preventing user enumeration via response timing.
- **requireRole inline in route definition:** Placing `requireRole('admin')` directly in the router chain (not as a per-controller guard) keeps the auth→role→controller sequence explicit and auditable — consistent with "Sempre validar role no middleware antes de chegar no controller" from CONTEXT.md.
- **institutionId from verified JWT only:** The admin controller reads institutionId from req.user (set by auth.middleware from the verified token), never from query params or request body — prevents cross-institution IDOR.

## Deviations from Plan

None — plan executed exactly as written.

## Known Stubs

None — all routes are functional. Login returns a real JWT signed against JWT_SECRET; admin overview queries real Mongoose models scoped by institutionId.

## Threat Flags

None — all T-02-xx threats from the plan's threat register were addressed:
- T-02-01: HS256 pinned on sign AND verify; invalid/tampered/alg:none tokens rejected 401
- T-02-02: requireRole rejects 403 before controller; fails closed when req.user absent
- T-02-03: admin controller filters every query by req.user.institutionId from verified token
- T-02-04: Generic 401 for both unknown email and wrong password; dummy-hash for timing safety
- T-02-05: JWT_EXPIRES_IN expiry enforced; expired tokens rejected 401
- T-02-06: bcrypt.compare against passwordHash; plaintext never stored or logged
- T-02-SC: No new npm packages installed

## Next Phase Readiness

- Phase 01 Plan 02 is complete. Phase 01 is now fully done.
- Auth middleware chain (authenticate + requireRole) is the established pattern for all future protected routes.
- PLAT-01 institutionId query scoping is demonstrated in admin.controller — all future controllers must mirror this.
- POST /api/auth/login is the token-minting endpoint for the full Walking Skeleton.

---
*Phase: 01-foundation-multi-tenant-seed*
*Completed: 2026-05-21*
