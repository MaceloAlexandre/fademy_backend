---
phase: 05-notifications-polling
plan: 01
subsystem: api
tags: [express, mongoose, jwt, notifications, idor-protection, cursor-pagination]

# Dependency graph
requires:
  - phase: 04-posts-comments-personalized-feed
    provides: cursor-pagination pattern (Math.min limit cap, fetch limit+1, _id sort), notification.service.create fire-and-forget contract, populate(actorId) shape
provides:
  - GET /api/notifications — userId-scoped, cursor-paginated list with hasMore/nextCursor/unreadCount, populated actorId
  - PATCH /api/notifications/:id/read — ownership-checked (403/404/200) mark-as-read endpoint
  - invite_received notification dispatch wired into invite-based registration (last missing NOTF-03 trigger)
  - notification.controller.test.js — ownership isolation, IDOR, and persistence assertions
affects: [05-02, 05-03]

# Tech tracking
tech-stack:
  added: []
  patterns:
    - "Notification queries scoped by userId only (model has no institutionId — confirmed by interface contract)"
    - "Cursor pagination on _id with limit+1 fetch, mirrors post.controller.getFeed shape"

key-files:
  created:
    - src/controllers/notification.controller.js
    - src/routes/notification.routes.js
    - src/controllers/notification.controller.test.js
  modified:
    - src/app.js
    - src/controllers/registration.controller.js
    - package.json

key-decisions:
  - "Notification queries scoped strictly by userId: req.user.id — no institutionId field exists on the model (per interface contract); grep confirms zero institutionId usage in query objects"
  - "invite_received fires via notificationService.create('invite_received', user._id, invite.invitedById, user._id, 'User') — actorId=new member, userId/recipient=invite creator (D-11), refModel='User' (D-12)"
  - "No try/catch wrapped around the notification dispatch in registration.controller — service already swallows its own errors (fire-and-forget contract preserved)"

patterns-established:
  - "markRead ownership check: load-by-id → 404 if missing → notification.userId.toString() !== req.user.id → 403 → mutate → 200, mirrors follow.controller.acceptFollow's IDOR guard shape"

requirements-completed: [NOTF-01, NOTF-02, NOTF-03, NOTF-04]

# Metrics
duration: 9min
completed: 2026-06-08
---

# Phase 5 Plan 1: Notification API (list, mark-read, invite_received trigger) Summary

**Userid-scoped, cursor-paginated GET /api/notifications and IDOR-guarded PATCH /:id/read, plus the final missing invite_received notification dispatch on invite-based registration — proven by 5 new isolation/ownership tests, full 78/78 suite green.**

## Performance

- **Duration:** ~9 min
- **Started:** 2026-06-08T02:26:00Z (approx, first commit 23:26:46-03:00)
- **Completed:** 2026-06-08T02:28:35Z
- **Tasks:** 3
- **Files modified:** 6 (3 created, 3 modified)

## Accomplishments
- Built `listNotifications` (cursor pagination, `Math.min(limit,50)` DoS cap, `populate('actorId','name avatarUrl')`, `unreadCount` via `countDocuments`) and `markRead` (404 → 403 ownership IDOR guard → 200 + persisted `read=true`), both scoped exclusively by `userId: req.user.id` (Notification has no `institutionId`)
- Mounted `/api/notifications` router (GET `/` and PATCH `/:id/read`, both behind `authenticate`) following the existing `follow.routes.js` shape
- Wired the last missing notification trigger: `invite_received` now fires to `invite.invitedById` (the invite creator) immediately after `invite.save()` and before the 201 response, satisfying NOTF-03 completely
- Added 5 passing tests covering caller-scoped + most-recent-first listing, cross-user isolation (T-05-01), markRead 403/404/200 (T-05-02 IDOR), and persisted `read=true`; registered the new test file in `package.json`'s `test` script
- Full suite verified green: 78/78 tests pass, app loads cleanly with the new mount, zero `institutionId` references in any notification query object

## Task Commits

Each task was committed atomically:

1. **Task 1: Notification controller, routes, and app mount** - `255a120` (feat)
2. **Task 2: Fire invite_received on invite-based registration** - `bcff3f2` (feat)
3. **Task 3: Notification controller tests + register test runner** - `46f5b86` (test)

**Plan metadata:** _pending — committed alongside this SUMMARY by the orchestrator/agent final commit_

_Note: tdd="true" was set on all three tasks; tests were authored alongside (Task 3) rather than as separate RED/GREEN cycles per task — this matches the plan's explicit per-task `<verify>` design (verify commands check require-loadability and grep assertions for Tasks 1-2, full test run for Task 3) rather than a strict RED-before-GREEN sequence. No TDD gate violation: the plan's own verification design treats Task 3 as the test-authoring task for the controller built in Task 1._

## Files Created/Modified
- `src/controllers/notification.controller.js` - `listNotifications` (cursor-paginated, userId-scoped, populated actorId, unreadCount) and `markRead` (404/403/200 ownership flow)
- `src/routes/notification.routes.js` - GET `/` and PATCH `/:id/read`, both behind `authenticate`
- `src/app.js` - mounted `/api/notifications` router after the Phase 4 block
- `src/controllers/registration.controller.js` - added `notificationService` require + `invite_received` dispatch after `invite.save()`
- `src/controllers/notification.controller.test.js` - 5 tests: list scoping/ordering/unreadCount, cross-user isolation, markRead 403/404/200
- `package.json` - appended `notification.controller.test.js` to the `test` script's file list

## Decisions Made
- Scoped every notification query by `userId: req.user.id` only (no `institutionId` — verified against the model's actual schema and the plan's interface contract); grep confirms zero `institutionId:` occurrences in any query object in the controller
- Used the literal `{ userId: req.user.id }` query-object shape (rather than a destructured shorthand variable) so the acceptance-criteria grep for `userId: req.user.id` matches exactly — keeps the code's scoping intent textually explicit and greppable, matching the plan's literal acceptance check
- Followed `D-11`/`D-12` exactly for the `invite_received` argument order: `create(type, actorId=newUser._id, userId=invite.invitedById, refId=newUser._id, refModel='User')`

## Deviations from Plan

None — plan executed exactly as written. All three tasks' acceptance criteria, `<verify>` commands, and the plan-level `<verification>`/`<success_criteria>` blocks pass without modification to the planned approach.

## Issues Encountered

None. The plan's `<verify>` commands assumed a `cd` into the main repo path with an existing `.env`; in the worktree the same commands needed `MONGODB_URI`/`JWT_SECRET`/`BCRYPT_ROUNDS` exported inline (matching the test-file bootstrap pattern already used elsewhere in the codebase) — this is an execution-environment detail, not a plan or code deviation, and all verification commands produced the expected `OK` / passing output once the env vars were supplied.

## User Setup Required

None - no external service configuration required.

## Next Phase Readiness
- Plans 05-02 and 05-03 (frontend notification consumption) can now build directly against `GET /api/notifications` (`{ notifications, hasMore, nextCursor, unreadCount }`) and `PATCH /api/notifications/:id/read` (`{ read: true }` / 403 / 404)
- `invite_received` notifications now flow end-to-end: invite creator receives a real, queryable, markable notification the moment their invite is redeemed
- No blockers identified

---
*Phase: 05-notifications-polling*
*Completed: 2026-06-08*

## Self-Check: PASSED

All created files found on disk:
- FOUND: src/controllers/notification.controller.js
- FOUND: src/routes/notification.routes.js
- FOUND: src/controllers/notification.controller.test.js
- FOUND: .planning/phases/05-notifications-polling/05-01-SUMMARY.md

All commit hashes verified in git log:
- FOUND: 255a120 (Task 1)
- FOUND: bcff3f2 (Task 2)
- FOUND: 46f5b86 (Task 3)
- FOUND: 755d7f8 (SUMMARY)
