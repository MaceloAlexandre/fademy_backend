---
phase: quick-260610-fjc
plan: 01
subsystem: api
tags: [express, mongoose, users, follow, block, suggestions]

# Dependency graph
requires:
  - phase: 03 (social graph / search)
    provides: searchUsers block-exclusion pattern and Follow/Block models reused here
provides:
  - GET /api/users/suggestions endpoint returning institution-scoped, exclusion-filtered, staff-first user suggestions
affects: [quick-260610-fjc-02 (SearchPage suggestions list), quick-260610-fjc-03 (FeedPage carousel)]

# Tech tracking
tech-stack:
  added: []
  patterns:
    - "suggestUsers reuses searchUsers' bidirectional Block exclusion pattern"
    - "Staff-before-aluno ordering via JS partition after over-fetching limit*2 candidates"

key-files:
  created: []
  modified:
    - src/controllers/user.controller.js
    - src/controllers/user.controller.test.js
    - src/routes/user.routes.js

key-decisions:
  - "institutionId and viewerId sourced only from req.user (JWT) — never query/body (T-fjc-01)"
  - "Excluded IDs built from Block (both directions) + Follow (any status) + self, mirroring searchUsers"
  - "Over-fetch limit*2 candidates then partition staff-first in JS, slice to final limit, so staff aren't starved by DB ordering"
  - "Result capped via Math.min(Number(limit) || 15, 20) mirroring feed's T-04-06 DoS cap"

patterns-established:
  - "Suggestion-style endpoints should reuse the searchUsers exclusion-building pattern verbatim for consistency"

requirements-completed: [FJC-04]

# Metrics
duration: 6min
completed: 2026-06-10
---

# Phase quick-260610-fjc Plan 01: Suggested Users Endpoint Summary

**New `GET /api/users/suggestions` endpoint returns institution-scoped, exclusion-filtered "people you might want to follow" with staff roles surfaced before alunos.**

## Performance

- **Duration:** ~6 min
- **Tasks:** 3 completed
- **Files modified:** 3

## Accomplishments
- Added `suggestUsers` controller excluding self, all followed users (pending/accepted), and bidirectional blocks
- Added 4 new test cases (13 total in user.controller.test.js, all passing)
- Registered `GET /api/users/suggestions` before `/:id` to avoid route param capture

## Task Commits

Each task was committed atomically:

1. **Task 1: Add suggestUsers controller** - `fdc6a69` (feat)
2. **Task 2: Tests for suggestUsers** - `7d560d1` (test)
3. **Task 3: Register the route** - `34bf723` (feat)

_Note: Task 1 and Task 2 were not split into RED/GREEN TDD commits — the controller was implemented and verified together with its tests in sequence per the plan's `<verify>` step pointing at the same test file for both tasks._

## Files Created/Modified
- `src/controllers/user.controller.js` - Added `suggestUsers(req, res)` async controller, exported alongside existing controllers
- `src/controllers/user.controller.test.js` - Added `suggestUsers (FJC-04)` describe block with seed helper covering staff/aluno/follow/block/cross-institution scenarios
- `src/routes/user.routes.js` - Imported `suggestUsers`, registered `GET /suggestions` above `/:id`

## Decisions Made
- Reused the exact `searchUsers` block-exclusion snippet (Block.find with $or on blockerId/blockedId) for consistency and to satisfy T-fjc-02
- Built the followed-user exclusion set from `Follow.find({ followerId: viewerId })` regardless of status (pending or accepted both excluded)
- Fetched `limit * 2` candidates before partitioning staff-first in JS, then sliced to `limit`, ensuring staff aren't starved when alunos dominate the candidate pool

## Deviations from Plan

None - plan executed exactly as written.

## Issues Encountered

None. All 13 tests in `src/controllers/user.controller.test.js` pass (9 pre-existing + 4 new).

## User Setup Required

None - no external service configuration required.

## Next Phase Readiness
- `GET /api/users/suggestions` is ready to be consumed by Plan 02 (SearchPage suggestions list) and Plan 03 (FeedPage carousel)
- Response shape `{ users: [{ _id, name, avatarUrl, role, isPrivate }] }` matches the field set both frontend consumers expect

---
*Phase: quick-260610-fjc*
*Completed: 2026-06-10*

## Self-Check: PASSED

All created/modified files exist on disk and all 3 task commits (fdc6a69, 7d560d1, 34bf723) verified present in git log.
