---
phase: 04-posts-comments-personalized-feed
plan: "01"
subsystem: backend-posts-feed
tags: [posts, feed, s3, cursor-pagination, role-gate, multi-tenant]
dependency_graph:
  requires: [Follow model, Block model, Category model, s3.service.uploadAvatar pattern, auth.middleware]
  provides: [Post model, uploadPostImage, listCategories endpoint, createPost endpoint, getFeed endpoint, getPost endpoint]
  affects: [src/app.js routes, npm test suite]
tech_stack:
  added: []
  patterns: [cursor-based _id pagination, multer.array(4) multipart, module-level require for populate registration]
key_files:
  created:
    - src/models/Post.js
    - src/controllers/category.controller.js
    - src/routes/category.routes.js
    - src/controllers/post.controller.js
    - src/controllers/post.controller.test.js
    - src/routes/post.routes.js
  modified:
    - src/services/s3.service.js
    - src/app.js
    - package.json
decisions:
  - "Category model required at controller module level to register mongoose model before populate calls"
  - "limit capped at 50 via Math.min to prevent DoS (T-04-06)"
  - "feed excludes blocked users in BOTH directions via $nin union of blockerId/blockedId"
  - "/feed route declared before /:id in post.routes.js to prevent 'feed' captured as ObjectId param"
metrics:
  duration: "~12 min"
  completed: "2026-06-07T23:53:00Z"
  tasks_completed: 2
  files_changed: 9
---

# Phase 04 Plan 01: Post Model, Feed & Categories Backend Summary

**One-liner:** Cursor-paginated personalized feed backend with role-gated link posts, 4-image S3 upload, and block/follow intersection — all covered by 10 passing tests.

## Tasks Completed

| Task | Name | Commit | Files |
|------|------|--------|-------|
| 1 | Post model + uploadPostImage + category endpoint | 51134de | Post.js, s3.service.js, category.controller.js, category.routes.js |
| 2 (RED) | Post controller failing tests | db22cbb | post.controller.test.js |
| 2 (GREEN) | Post controller + routes + app.js mounts | 996d54c | post.controller.js, post.routes.js, app.js |
| - | Add post test to npm script | 1e2192a | package.json |

## What Was Built

### Post Model (`src/models/Post.js`)
Mongoose model with fields: `authorId` (ObjectId ref User, required), `institutionId` (ObjectId ref Institution, required), `text` (String, required, maxlength 1000), `imageUrls` ([String], default []), `linkUrl` (String), `hasLink` (Boolean, default false), `categoryIds` ([ObjectId] ref Category, default []), `commentCount` (Number, default 0), `createdAt` (Date, default Date.now). Three indexes: `{authorId,createdAt}`, `{institutionId,createdAt}`, `{authorId,_id}` (cursor feed support).

### S3 Extension (`src/services/s3.service.js`)
Added `uploadPostImage({ buffer, contentType })` — identical to `uploadAvatar` except key prefix is `posts/`. Exported alongside existing exports.

### Category Endpoint
`GET /api/categories` — returns all seeded Category docs (global, no institutionId filter). Protected by `authenticate`.

### Post Controller (`src/controllers/post.controller.js`)
- **createPost**: text validation, aluno+linkUrl→403 (T-04-01), mimetype allowlist per file, S3 upload per image (max 4 via multer), categoryIds normalization (string or array), hasLink derived server-side, authorId/institutionId from JWT never from body (T-04-05).
- **getFeed**: Follow.find(accepted) → followingIds, Block.find(both directions) → blockedIds, Post.find with $in/$nin + institutionId scoping (T-04-02), cursor via `_id < cursor`, limit capped at 50 (T-04-06), populate author+categories, hasMore/nextCursor via limit+1 trick.
- **getPost**: Post.findById + populate, institutionId mismatch → 404 (T-04-02).

### Routes (`src/routes/post.routes.js`)
`/feed` registered before `/:id` (prevents 'feed' captured as id param). multer.array('images', 4) with MAX_UPLOAD_BYTES limit. Upload errors surfaced as 400.

### Tests (`src/controllers/post.controller.test.js`)
10 tests covering: aluno+linkUrl→403, professor+linkUrl→201+hasLink:true, missing text→400, image upload→imageUrls, feed shows only accepted-follow authors, feed excludes blocker→blocked AND blocked→blocker directions, cursor pagination hasMore+nextCursor, getPost same institution→200, getPost cross-institution→404.

## Deviations from Plan

### Auto-fixed Issues

**1. [Rule 1 - Bug] Category model not registered for mongoose populate**
- **Found during:** Task 2 GREEN phase (test run)
- **Issue:** `Post.findById().populate('categoryIds', ...)` threw "Schema hasn't been registered for model Category" because Category.js was never required in the controller module chain.
- **Fix:** Added `require('../models/Category')` at the top of `post.controller.js` to register the model before any populate calls execute.
- **Files modified:** `src/controllers/post.controller.js`
- **Commit:** 996d54c (included in GREEN commit)

**2. [Rule 2 - Missing integration] post.controller.test.js not in npm test script**
- **Found during:** Post-implementation verification
- **Issue:** The test file existed and passed standalone but was not included in `package.json` `test` script, so `npm test` would not run it.
- **Fix:** Appended `src/controllers/post.controller.test.js` to the test script.
- **Files modified:** `package.json`
- **Commit:** 1e2192a

## Success Criteria Verification

| Criterion | Status | Evidence |
|-----------|--------|---------|
| CONT-01: POST /api/posts with text returns 201 | PASS | test "returns 201 with hasLink:true..." |
| CONT-02: images attach and upload to S3 (imageUrls populated) | PASS | test "populates imageUrls when files are attached" |
| CONT-03: aluno linkUrl→403; permitted roles→hasLink true | PASS | 2 dedicated tests asserting 403 and 201+hasLink |
| CONT-04: categoryIds stored on post | PASS | controller normalizes and persists categoryIds |
| FEED-01: feed shows accepted-follow authors only | PASS | test "returns only posts from accepted-follow authors" |
| FEED-02: feed excludes blocked users in either direction | PASS | 2 tests covering blocker→blocked and blocked→blocker |
| FEED-03: feed is cursor-paginated with hasMore/nextCursor | PASS | test "returns hasMore:true and nextCursor" |

## Threat Model Coverage

All STRIDE threats from the plan's threat register are mitigated in the implementation:
- T-04-01: aluno+linkUrl→403 enforced in controller, hasLink server-derived
- T-04-02: Every query filters by `req.user.institutionId`; getPost 404s on mismatch
- T-04-03: Block.find covers both directions via $or, excluded via $nin
- T-04-04: multer limits.fileSize=MAX_UPLOAD_BYTES, array cap=4, mimetype allowlist
- T-04-05: authorId/institutionId from JWT only, never from request body
- T-04-06: limit=Math.min(parseInt,50), cursor in safe `_id < cursor` Mongoose comparison

## Known Stubs

None — all fields produce real data from the database. No placeholder values in code paths.

## Threat Flags

None — no new network endpoints beyond those planned. All new endpoints follow established auth patterns.

## Self-Check: PASSED

Files created:
- src/models/Post.js: FOUND
- src/services/s3.service.js (uploadPostImage added): FOUND
- src/controllers/category.controller.js: FOUND
- src/routes/category.routes.js: FOUND
- src/controllers/post.controller.js: FOUND
- src/controllers/post.controller.test.js: FOUND
- src/routes/post.routes.js: FOUND

Commits verified:
- 51134de (Task 1): FOUND
- db22cbb (Task 2 RED): FOUND
- 996d54c (Task 2 GREEN): FOUND
- 1e2192a (package.json): FOUND

npm test: 67 tests, 0 failures.
