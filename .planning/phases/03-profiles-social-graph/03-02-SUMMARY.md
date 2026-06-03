---
phase: 03-profiles-social-graph
plan: "02"
subsystem: backend
tags: [user-profile, search, privacy, block-exclusion, route-ordering]
dependency_graph:
  requires:
    - "03-01 (Follow.js, Block.js, Notification.js, notification.service.js, user.routes.js stub)"
    - "02-03 (User.js model, auth.middleware, connectDB)"
  provides:
    - "GET /api/users/search — institution-scoped, ReDoS-mitigated, block-excluded paginated search"
    - "GET /api/users/me — caller own profile (isOwner path)"
    - "GET /api/users/:id — profile view with private-profile visibility rules and followStatus"
    - "PATCH /api/users/me — isPrivate toggle"
  affects:
    - "src/routes/user.routes.js (extended with GET/PATCH routes at correct position)"
tech_stack:
  added: []
  patterns:
    - "getMe delegates to getProfile with params.id override — eliminates code duplication"
    - "Promise.all for parallel followersCount/followingCount queries"
    - "ReDoS mitigation: q.slice(0,100).replace(/[.*+?^${}()|[\\]\\\\]/g, '\\\\$&') + new RegExp(safeQ,'i')"
    - "institutionId from JWT only (req.user.institutionId) — never from query param"
    - "Block query covers both directions before any profile data returned"
key_files:
  created:
    - path: src/controllers/user.controller.js
      role: "getProfile, getMe, updateMe, searchUsers handlers with privacy rules and block exclusion"
    - path: src/controllers/user.controller.test.js
      role: "9 tests covering PROF-01, PROF-02, PROF-03, SOCL-05"
  modified:
    - path: src/routes/user.routes.js
      role: "Added GET /search, GET /me, GET /:id, PATCH /me — correctly ordered before /:id/follow"
decisions:
  - "getMe implemented as a one-liner delegating to getProfile with params.id = req.user.id, avoiding duplication"
  - "Route ordering: /search and /me registered before /:id to prevent Express first-match swallowing"
  - "app.js and package.json already correct from Plan 03-01 — zero changes needed"
  - "Only isPrivate validated as boolean in updateMe; no other User fields accepted (T-03-10)"
metrics:
  duration: "~10 min"
  completed: "2026-06-03T23:47:00Z"
  tasks_completed: 2
  files_created: 2
  files_modified: 1
---

# Phase 03 Plan 02: User Profile and Search Backend Summary

**One-liner:** User profile API with private-visibility rules, block-exclusion search, and ReDoS-safe regex delivered in 136-line controller and 9 passing tests.

## Tasks Completed

| # | Name | Commit | Files |
|---|------|--------|-------|
| 1 | user.controller.js — getProfile, getMe, updateMe, searchUsers | 7471f8c | src/controllers/user.controller.js (created) |
| 2 | user.routes.js + user.controller.test.js + app.js + package.json | cee630e | src/routes/user.routes.js (modified), src/controllers/user.controller.test.js (created) |

## What Was Built

### user.controller.js

Four handlers implementing the full profile and search API surface:

**getProfile:** Block check covers both directions before any data is returned (T-03-08). Private profile non-followers receive a reduced payload (id, name, avatarUrl, role, isPrivate, followStatus) without followersCount, followingCount, or other data (T-03-09). Full-profile path runs two Follow.countDocuments queries in parallel via Promise.all.

**getMe:** Single-line delegation — `return getProfile({ ...req, params: { id: req.user.id } }, res)` — avoids duplication without introducing a separate code path.

**updateMe:** Validates isPrivate as typeof boolean before any DB write. Only this field is accepted — no other User fields writable through this endpoint (T-03-10).

**searchUsers:** Empty q check returns early with `{ users: [], total: 0 }` (Pitfall 6). ReDoS mitigation: `q.slice(0, 100).replace(/[.*+?^${}()|[\]\\]/g, '\\$&')` before `new RegExp(safeQ, 'i')` (T-03-06). institutionId sourced from JWT only (T-03-07). Block exclusion fetches all blocks in both directions for the viewer and builds excludedIds list (SOCL-05). Results paginated with skip/limit.

### user.routes.js (extended)

Added GET /search, GET /me, GET /:id, PATCH /me at the top of the file, before the /:id/follow and /:id/block routes from Plan 03-01. Critical route ordering ensures Express matches /search and /me before /:id (Pitfall 1 from RESEARCH.md).

### user.controller.test.js

9 tests, all passing:
- PROF-01: isPrivate toggle (happy path + 400 for non-boolean)
- PROF-02: public profile has followersCount/followingCount; private non-follower has reduced payload; blocked user sees 404
- PROF-02 isOwner: getMe returns caller's own profile
- PROF-03: search with q='User' returns matching users; empty q returns empty array
- SOCL-05: user who blocked the viewer is excluded from search results

## Deviations from Plan

None — plan executed exactly as written.

app.js already had `app.use('/api/users', ...)` from Plan 03-01 — no change made (surgical).
package.json already had `user.controller.test.js` in scripts.test from Plan 03-01 — no change made (surgical).

## Threat Model Coverage

All threats in the plan's threat register were mitigated during implementation:

| Threat | Mitigation Applied |
|--------|-------------------|
| T-03-06 ReDoS in searchUsers | q.slice(0,100) + metachar escape before RegExp construction |
| T-03-07 Cross-tenant search leak | institutionId from req.user (JWT), never from query param |
| T-03-08 Block bypass in getProfile | Block.findOne with $or both directions before any user data returned |
| T-03-09 Private profile info disclosure | Reduced payload (no counters) for non-followers of private profiles |
| T-03-10 updateMe field injection | typeof isPrivate === 'boolean' guard; only that field updated |
| T-03-11 Package installs | No new packages — pre-existing dependencies only |

## Known Stubs

None — all handlers are fully implemented and wired.

## Self-Check

- [x] src/controllers/user.controller.js created and exports { getProfile, getMe, updateMe, searchUsers }
- [x] src/controllers/user.controller.test.js created with 9 passing tests
- [x] src/routes/user.routes.js has /search (line 13) before /:id (line 19)
- [x] app.js has exactly one app.use('/api/users', ...) mount
- [x] package.json scripts.test includes user.controller.test.js
- [x] Full test suite: 48 tests, 0 failures
- [x] Commits 7471f8c and cee630e exist in git log

## Self-Check: PASSED
