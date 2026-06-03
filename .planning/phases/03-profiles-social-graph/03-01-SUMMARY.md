---
phase: 03-profiles-social-graph
plan: "01"
subsystem: backend/social-graph
tags: [follow, block, notification, social-graph, IDOR]
dependency_graph:
  requires: []
  provides:
    - Follow Mongoose model with compound unique index and status state machine
    - Block Mongoose model with bilateral follow removal on block
    - Notification Mongoose model stub for Phase 5
    - notification.service.js fire-and-forget stub
    - follow.controller.js (follow/unfollow/acceptFollow/rejectFollow)
    - block.controller.js (block)
    - follow.routes.js (PATCH /api/follows/:id/accept|reject)
    - user.routes.js (POST/DELETE /api/users/:id/follow, POST /api/users/:id/block)
  affects:
    - src/app.js (new route mounts for /api/users and /api/follows)
    - package.json (test script extended)
tech_stack:
  added: []
  patterns:
    - Mongoose model with compound unique index (Follow, Block)
    - Fire-and-forget notification stub (notification.service.js)
    - IDOR protection via followingId ownership check in acceptFollow/rejectFollow
    - Bilateral Follow.deleteMany on block
    - Module._load mock pattern for notification.service in tests
key_files:
  created:
    - src/models/Follow.js
    - src/models/Block.js
    - src/models/Notification.js
    - src/services/notification.service.js
    - src/controllers/follow.controller.js
    - src/controllers/follow.controller.test.js
    - src/controllers/block.controller.js
    - src/controllers/block.controller.test.js
    - src/routes/follow.routes.js
    - src/routes/user.routes.js
  modified:
    - src/app.js
    - package.json
decisions:
  - "SOCL-03 frontend UI deferred to Phase 5 per D-07: backend accept/reject API fully implemented with IDOR protection"
  - "user.routes.js created as minimal file in Plan 03-01 so follow/block routes are available immediately; Plan 03-02 extends it with profile/search"
  - "notification.service.create() awaited inside follow controller try/catch — safe because service wraps its own try/catch and never throws"
metrics:
  duration: "4 minutes"
  completed: "2026-06-03"
  tasks_completed: 2
  files_created: 10
  files_modified: 2
---

# Phase 03 Plan 01: Social Graph Backend Summary

Follow/block/accept/reject backend API with IDOR-protected state machine and fire-and-forget notification stub.

## What Was Built

Implemented the complete social graph backend (SOCL-01 through SOCL-04):

- **Follow model** (`src/models/Follow.js`): compound unique index `{followerId,followingId}`, status enum `['pending','accepted']`, three indexes for efficient state machine queries
- **Block model** (`src/models/Block.js`): unique index `{blockerId,blockedId}`, secondary index on `blockedId` for reverse-lookup
- **Notification model** (`src/models/Notification.js`): 6-type enum, composite index `{userId,read,createdAt:-1}` — stub for Phase 5
- **notification.service.js**: fire-and-forget `create()` wraps `Notification.create()` in try/catch and swallows errors silently
- **follow.controller.js**: `follow` (public→accepted, private→pending, self-follow 400, block check 403), `unfollow` (idempotent 200), `acceptFollow` (IDOR check → 403 if not target), `rejectFollow` (IDOR check + delete)
- **block.controller.js**: `block` (self-block 400, bilateral `Follow.deleteMany`, `Block.findOneAndUpdate` upsert)
- **follow.routes.js**: `PATCH /api/follows/:id/accept|reject`
- **user.routes.js**: `POST /api/users/:id/follow`, `DELETE /api/users/:id/follow`, `POST /api/users/:id/block`
- **app.js**: mounted `/api/users` and `/api/follows`
- **package.json**: test script extended with follow/block/user controller test files

## Test Coverage

8 tests passing across 2 new test files:

- `follow.controller.test.js` (6 tests): public profile → accepted (SOCL-01), private profile → pending (SOCL-02), self-follow 400, IDOR 403 on acceptFollow (SOCL-03), acceptFollow 200 by target, unfollow 200
- `block.controller.test.js` (2 tests): bilateral follow removal + block creation (SOCL-04), self-block 400

## Deviations from Plan

None — plan executed exactly as written.

## Known Stubs

- `src/models/Notification.js` and `src/services/notification.service.js` are intentional stubs. Notification.create() is called but the model has no UI consumer yet — Phase 5 (notifications centre) wires the frontend. This is a planned stub, not a defect.
- `src/controllers/user.controller.test.js` is referenced in package.json scripts.test but does not exist yet — created by Plan 03-02.

## Self-Check: PASSED

Files exist:
- src/models/Follow.js: FOUND
- src/models/Block.js: FOUND
- src/models/Notification.js: FOUND
- src/services/notification.service.js: FOUND
- src/controllers/follow.controller.js: FOUND
- src/controllers/follow.controller.test.js: FOUND
- src/controllers/block.controller.js: FOUND
- src/controllers/block.controller.test.js: FOUND
- src/routes/follow.routes.js: FOUND
- src/routes/user.routes.js: FOUND

Commits exist:
- 9badaef: feat(03-01): add Follow, Block, Notification models and notification.service stub — FOUND
- b061ca2: feat(03-01): follow/block controllers, routes, tests, app.js mounts — FOUND

Tests: 8/8 pass (node --test src/controllers/follow.controller.test.js src/controllers/block.controller.test.js)
