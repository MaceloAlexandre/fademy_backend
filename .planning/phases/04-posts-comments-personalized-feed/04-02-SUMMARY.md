---
phase: 04-posts-comments-personalized-feed
plan: "02"
subsystem: backend-comments
tags: [comments, notifications, multi-tenant, tdd, mongodb]
dependency_graph:
  requires: [Post model, notification.service, auth.middleware, Follow model, Block model]
  provides: [Comment model, createComment endpoint, listComments endpoint, commentCount increment, post_comment notification]
  affects: [src/app.js routes, npm test suite]
tech_stack:
  added: []
  patterns: [fire-and-forget notification, self-comment guard, $inc denormalized counter, Module._load spy for notification testing]
key_files:
  created:
    - src/models/Comment.js
    - src/controllers/comment.controller.js
    - src/routes/comment.routes.js
    - src/controllers/comment.controller.test.js
  modified:
    - src/app.js
    - package.json
decisions:
  - "Comment institutionId taken from req.user.institutionId (JWT), never from request body (T-04C-02 spoofing prevention)"
  - "Self-comment guard uses String() comparison on post.authorId vs req.user.id to handle ObjectId/string mixed types"
  - "Module._load interception used to create a call-counting spy on notification.service before controller loads"
  - "notifSpy.calls reset to 0 before each spy-dependent test to avoid cross-test contamination"
metrics:
  duration: "~8 min"
  completed: "2026-06-07T23:00:00Z"
  tasks_completed: 2
  files_changed: 6
---

# Phase 04 Plan 02: Comments Backend Summary

**One-liner:** Comment model with institutionId scoping, $inc commentCount, fire-and-forget post_comment notification with self-comment guard — all covered by 6 passing tests.

## Tasks Completed

| Task | Name | Commit | Files |
|------|------|--------|-------|
| 1 | Comment model + controller + routes + app.js mount | b64a299 | Comment.js, comment.controller.js, comment.routes.js, app.js |
| 2 | comment.controller tests | 3755b88 | comment.controller.test.js, package.json |

## What Was Built

### Comment Model (`src/models/Comment.js`)
Mongoose model with fields: `postId` (ObjectId ref Post, required), `authorId` (ObjectId ref User, required), `institutionId` (ObjectId ref Institution, required), `text` (String, required, maxlength 500), `createdAt` (Date, default Date.now). Index: `{ postId: 1, createdAt: 1 }` for efficient oldest-first listing.

### Comment Controller (`src/controllers/comment.controller.js`)
- **createComment**: Validates text non-empty (400 if missing/blank). Loads Post by postId and 404s if not found OR if `post.institutionId` mismatches `req.user.institutionId` (cross-tenant write prevention, T-04C-01). Creates Comment with authorId/institutionId from JWT only (T-04C-02). Increments `post.commentCount` via `$inc`. Fires `post_comment` notification fire-and-forget ONLY when commenter !== post author (T-04C-04). Returns 201 `{ comment }`.
- **listComments**: Returns all comments for postId sorted `createdAt: 1` (oldest-first) with `authorId` populated (name, avatarUrl). Returns 200 `{ comments }`.

### Routes (`src/routes/comment.routes.js`)
`POST /:postId/comments` and `GET /:postId/comments` — both behind `authenticate` middleware.

### App Mount (`src/app.js`)
`app.use('/api/posts', require('./routes/comment.routes'))` added after the post.routes mount. Both mount under `/api/posts`; paths don't collide (`/:id` vs `/:postId/comments` differ in depth).

### Tests (`src/controllers/comment.controller.test.js`)
6 tests covering:
- createComment → 201 + commentCount increments to 1
- createComment cross-institution → 404
- self-comment → notification service NOT called (spy assertion)
- cross-author comment → notification service called once (spy assertion)
- missing text → 400
- listComments → oldest-first + authorId populated

## Deviations from Plan

None — plan executed exactly as written.

## Success Criteria Verification

| Criterion | Status | Evidence |
|-----------|--------|---------|
| CONT-05: POST a comment returns 201 with created comment | PASS | test "returns 201, creates comment, and increments commentCount to 1" |
| CONT-05: commentCount increments | PASS | assert.equal(updatedPost.commentCount, 1) |
| CONT-05: post_comment notification for non-self comments | PASS | test "calls notificationService.create for a cross-author comment" (notifSpy.calls === 1) |
| CONT-05: self-comment skips notification | PASS | test "does NOT call notificationService.create on a self-comment" (notifSpy.calls === 0) |
| CONT-05: GET comments returns oldest-first with author info | PASS | test "returns comments oldest-first with populated authorId" |
| Cross-institution returns 404 | PASS | test "returns 404 when post belongs to a different institution" |
| npm test exits 0 | PASS | 73 tests, 0 failures |

## Threat Model Coverage

All STRIDE threats from the plan's threat register are mitigated:
- T-04C-01: Post loaded by postId + institutionId mismatch → 404 (cross-tenant write blocked)
- T-04C-02: authorId and institutionId set from req.user (JWT) only, never from request body
- T-04C-03: text maxlength 500 enforced by schema; empty/whitespace-only text rejected with 400
- T-04C-04: notificationService.create skipped when String(post.authorId) === String(req.user.id)
- T-04C-SC: No new packages installed

## Known Stubs

None — all fields produce real data from the database. No placeholder values in code paths.

## Threat Flags

None — no new network endpoints beyond those planned. Comment routes follow established auth patterns (authenticate middleware on all routes).

## Self-Check: PASSED

Files created:
- src/models/Comment.js: FOUND
- src/controllers/comment.controller.js: FOUND
- src/routes/comment.routes.js: FOUND
- src/controllers/comment.controller.test.js: FOUND

Files modified:
- src/app.js (comment.routes mount added): FOUND
- package.json (comment.controller.test.js in npm test): FOUND

Commits verified:
- b64a299 (Task 1): FOUND
- 3755b88 (Task 2): FOUND

npm test: 73 tests, 0 failures.
