---
phase: 02-entry-loop-invite-register-login
plan: "02"
subsystem: backend/media-s3
tags: [s3, presign, media, tdd, security]
dependency_graph:
  requires: [02-01]
  provides: [s3-service, media-controller, media-routes]
  affects: [src/app.js, package.json]
tech_stack:
  added: ["@aws-sdk/client-s3@^3.1055.0", "@aws-sdk/s3-request-presigner@^3.1055.0"]
  patterns: [presigned-put-url, server-generated-key, env-safe-defaults, tdd-red-green]
key_files:
  created:
    - src/services/s3.service.js
    - src/controllers/media.controller.js
    - src/routes/media.routes.js
    - src/controllers/media.controller.test.js
  modified:
    - src/config/env.js
    - src/app.js
    - package.json
    - .env.example
decisions:
  - "Server generates the S3 key (avatars/<uuid>.<ext>) and returns it as a bare key — registration never reverse-parses a client-controlled URL (T-02-08b)"
  - "S3 vars (AWS_REGION, S3_BUCKET, credentials, TTL, MAX_UPLOAD_BYTES, FRONTEND_ORIGIN) use safe defaults, NOT added to required[] — npm test boots without AWS"
  - "No auth on POST /api/media/presign — presign happens pre-registration; content-type allowlist + size cap are the guards"
  - "Module._load mock for s3.service in tests — avoids live AWS, satisfies no-network test requirement"
metrics:
  duration: "8 min"
  completed_date: "2026-05-27"
  tasks_completed: 2
  files_changed: 8
---

# Phase 02 Plan 02: S3 Presigned Upload Vertical Slice Summary

S3 presigned PUT URL backend: validates content-type/size then issues a time-limited upload URL; server owns the object key for trust-boundary safety; 8/8 unit tests pass offline.

## Tasks Completed

| # | Task | Commit | Type |
|---|------|--------|------|
| 1 | Install AWS SDK v3 + env config | af0b321 | chore |
| 2 | TDD RED — failing test | 0836da8 | test |
| 3 | TDD GREEN — s3.service + media controller/route + mount | 2046c49 | feat |

## What Was Built

**s3.service.js** (`src/services/s3.service.js`): Constructs one `S3Client` from env config; exports:
- `getPresignedPutUrl({ key, contentType })` — issues a `PutObjectCommand` presigned URL (TTL = S3_PRESIGN_TTL, default 300s); returns `{ uploadUrl, key, objectUrl }` where objectUrl is `https://${S3_BUCKET}.s3.${AWS_REGION}.amazonaws.com/${key}`
- `objectExists(key)` — `HeadObjectCommand` → true; any error → false (registration Plan 03 uses this to confirm the upload happened)

**media.controller.js** (`src/controllers/media.controller.js`): `presign(req, res)` validates `contentType` against the allowlist `{image/jpeg, image/png, image/webp}` and `size <= MAX_UPLOAD_BYTES` (default 5 MB) BEFORE any S3 call; server generates the key as `avatars/<uuid>.<ext>`; returns 200 `{ uploadUrl, key, objectUrl }` or 400/500.

**media.routes.js** (`src/routes/media.routes.js`): `POST /presign` wired to the presign handler with no auth middleware — upload happens during registration before the account exists.

**App mount** (`src/app.js`): `app.use('/api/media', require('./routes/media.routes'))` added.

**Env config** (`src/config/env.js`): Added `AWS_REGION`, `S3_BUCKET`, `S3_ACCESS_KEY_ID`, `S3_SECRET_ACCESS_KEY`, `S3_PRESIGN_TTL`, `MAX_UPLOAD_BYTES`, `FRONTEND_ORIGIN` with safe defaults; none added to `required[]`.

**`.env.example`**: All new vars documented with placeholder values and comments.

## Test Results

8/8 tests passing (`node --test src/controllers/media.controller.test.js`):
- valid image/png: 200 with uploadUrl, key, objectUrl (objectUrl contains key)
- valid image/jpeg: 200
- valid image/webp: 200
- disallowed content-type (application/pdf): 400
- size > MAX_UPLOAD_BYTES: 400
- missing contentType: 400
- missing size: 400
- empty body: 400

No network access required — s3.service mocked via `Module._load`.

## Deviations from Plan

### Auto-fixed Issues

**1. [Rule 1 - Bug] Fixed mock objectUrl to be key-relative**
- **Found during:** Task 2 TDD GREEN first run
- **Issue:** Mock `getPresignedPutUrl` returned a hardcoded `MOCK_OBJECT_URL` with `mock-uuid.png`, but the controller generates a real random UUID key — test assertion `objectUrl.includes(key)` failed
- **Fix:** Mock now constructs `objectUrl` as `https://fademy-dev.s3.us-east-1.amazonaws.com/${key}` using the passed key, so the assertion holds against any server-generated key
- **Files modified:** `src/controllers/media.controller.test.js`
- **Commit:** 2046c49 (included in the GREEN commit)

## TDD Gate Compliance

- RED gate commit: 0836da8 (`test(02-02): add failing test for media presign controller (RED)`) — MODULE_NOT_FOUND confirmed
- GREEN gate commit: 2046c49 (`feat(02-02): implement s3.service, media controller/route, and mount (GREEN)`) — 8/8 passing

## Known Stubs

None — the presign endpoint is fully wired. S3 calls will fail without real AWS credentials (expected; vars have safe defaults). `objectExists` will return false without live AWS, but Plan 03 will use it only in a context where real credentials are configured.

## Threat Flags

No new security surface beyond the plan's threat model. All STRIDE entries mitigated as designed:
- T-02-06: content-type allowlist enforced before any S3 call
- T-02-07: size <= MAX_UPLOAD_BYTES enforced; TTL default 300s
- T-02-08: credentials never returned to client; only signed uploadUrl is exposed
- T-02-08b: server generates key — client never controls object path

## Deferred Items

**Pre-existing vulnerability (out of scope):** `bcrypt` depends on `@mapbox/node-pre-gyp` → `tar` with a high-severity path traversal CVE. This was present before this plan (bcrypt was in dependencies since Phase 01). Tracked for future remediation; `npm audit fix --force` would require breaking changes.

## Self-Check: PASSED

- src/services/s3.service.js: FOUND
- src/controllers/media.controller.js: FOUND
- src/routes/media.routes.js: FOUND
- src/controllers/media.controller.test.js: FOUND
- Commit af0b321: FOUND
- Commit 0836da8: FOUND
- Commit 2046c49: FOUND
