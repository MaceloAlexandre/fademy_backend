---
phase: 06-chat-groups-real-time
plan: 01
subsystem: backend-chat-foundation
tags: [socket.io, chat, models, tdd, security]
dependency_graph:
  requires: [05-notifications-polling]
  provides: [Conversation model, Message model, conversation REST API, Socket.io auth service]
  affects: [src/server.js, src/app.js, package.json]
tech_stack:
  added: [socket.io@^4.8.3, socket.io-client@^4.8.3 (devDep)]
  patterns: [TDD RED/GREEN, find-or-create, bilateral mutual-follow gate, JWT handshake auth, per-user Socket.io rooms]
key_files:
  created:
    - src/models/Conversation.js
    - src/models/Message.js
    - src/controllers/conversation.controller.js
    - src/routes/conversation.routes.js
    - src/services/socket.service.js
    - src/controllers/conversation.controller.test.js
    - src/services/socket.service.test.js
  modified:
    - package.json (socket.io dep, socket.io-client devDep, Wave 0 test paths)
    - package-lock.json
    - src/app.js (mount /api/conversations)
    - src/server.js (http.createServer + initSocket; removes app.listen)
decisions:
  - "Conversation/Message carry no institutionId — scope flows through memberIds/senderId (User is institution-scoped), mirrors Notification precedent (Phase 5)"
  - "createConversation derives caller id from req.user.id only, never body (T-06-01 spoofing prevention)"
  - "Cross-institution gate (T-06-06) is an explicit String() equality check before any follow/block queries — not deferred"
  - "Socket.io CORS set on Server constructor, NOT inherited from Express cors() (Pitfall 2)"
  - "server.js uses http.createServer + httpServer.listen; app.listen removed (Pitfall 1)"
  - "socket.io-client installed as devDependency for socket.service.test.js — not in production code"
  - "socket.service.test.js uses async/await for before/after (node:test callback-done not supported at suite level)"
metrics:
  duration: "4 min"
  completed: "2026-06-08"
  tasks_completed: 3
  files_created: 7
  files_modified: 4
  tests_added: 11
  full_suite_tests: 89
---

# Phase 6 Plan 1: Backend Chat Foundation — Socket.io + Conversation/Message Models + Gated Controller Summary

**One-liner:** JWT-authenticated Socket.io service with per-user rooms + gated 1:1 conversation REST API (mutual-follow + block + same-institution gate, find-or-create) backed by Conversation/Message Mongoose models.

## Tasks Completed

| Task | Name | Commit | Files |
|------|------|--------|-------|
| 1 | Install Socket.io, Conversation + Message models, register Wave 0 test paths | 570611f | package.json, package-lock.json, src/models/Conversation.js, src/models/Message.js |
| 2 (TDD) | Gated conversation controller (create + list) and routes | e528d17 (RED), e91bea5 (GREEN) | conversation.controller.test.js, conversation.controller.js, conversation.routes.js, app.js |
| 3 (TDD) | Socket.io service (handshake auth + per-user rooms), wire into server.js | f364bb0 (RED), bc37689 (GREEN) | socket.service.test.js, socket.service.js, server.js, package.json |

## Verification

- `node --test src/controllers/conversation.controller.test.js` — 7 tests pass (self 400, not-found 404, one-direction 403, block 403, cross-institution 403, create 201+200, list with lastMessage null)
- `node --test src/services/socket.service.test.js` — 4 tests pass (valid JWT connects, no token rejects, garbage token rejects, room delivery only to targeted user)
- `npm test` (full backend suite) — 89 tests, 0 failures

## Deviations from Plan

### Auto-fixed Issues

**1. [Rule 1 - Bug] Fixed `before`/`after` callback-style in socket.service.test.js**
- **Found during:** Task 3 GREEN — test failed immediately with `TypeError: done is not a function`
- **Issue:** `node:test` `before()` at suite level does not support callback-style `done` parameter; it only supports `async/await`
- **Fix:** Converted `before((done) => { ... done() })` and `after((done) => { ... done() })` to `async () => { await new Promise(resolve => { ... resolve() }) }` pattern
- **Files modified:** src/services/socket.service.test.js
- **Commit:** bc37689 (included in GREEN commit alongside implementation)

## TDD Gate Compliance

| Gate | Status | Commit |
|------|--------|--------|
| Task 2 RED (test) | PASS — `MODULE_NOT_FOUND` error confirmed failing state | e528d17 |
| Task 2 GREEN (impl) | PASS — all 7 tests green | e91bea5 |
| Task 3 RED (test) | PASS — `MODULE_NOT_FOUND` error confirmed failing state | f364bb0 |
| Task 3 GREEN (impl) | PASS — all 4 tests green, full suite 89/89 | bc37689 |

## Known Stubs

None — all functionality is fully implemented and wired. The `listConversations` lastMessage N+1 pattern is intentional per RESEARCH.md Option A recommendation for the current scale.

## Threat Flags

No new threat surface beyond what is covered by the plan's threat model. All T-06-0x mitigations implemented as specified.

## Self-Check: PASSED

Files created/modified:
- FOUND: src/models/Conversation.js
- FOUND: src/models/Message.js
- FOUND: src/controllers/conversation.controller.js
- FOUND: src/routes/conversation.routes.js
- FOUND: src/services/socket.service.js
- FOUND: src/controllers/conversation.controller.test.js
- FOUND: src/services/socket.service.test.js

Commits:
- FOUND: 570611f (chore - models + socket.io install)
- FOUND: e528d17 (test - RED conversation controller)
- FOUND: e91bea5 (feat - GREEN conversation controller)
- FOUND: f364bb0 (test - RED socket service)
- FOUND: bc37689 (feat - GREEN socket service)
