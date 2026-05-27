---
phase: 02-entry-loop-invite-register-login
plan: "01"
subsystem: backend/invites
tags: [invites, tdd, multi-tenant, auth, roles]
dependency_graph:
  requires: [01-01, 01-02]
  provides: [invite-model, invite-controller, invite-routes]
  affects: [src/app.js, src/models/User.js]
tech_stack:
  added: []
  patterns: [multi-tenant-scoping, requireRole-gate, node-core-crypto, tdd-red-green]
key_files:
  created:
    - src/models/Invite.js
    - src/controllers/invite.controller.js
    - src/controllers/invite.controller.test.js
    - src/routes/invite.routes.js
  modified:
    - src/models/User.js
    - src/app.js
    - package.json
decisions:
  - "crypto.randomUUID() used for token generation — node core, no extra dependency, 122-bit entropy (T-02-04)"
  - "D-08 compound unique index applied to User.email+institutionId — same email allowed across institutions"
  - "D-04 slot caps enforced in controller (aluno→50, all others→10) with 400 on exceed"
  - "D-07 admin-target restriction enforced in createInvite controller, not the route middleware"
  - "revokeInvite scopes findOneAndUpdate to req.user.institutionId — prevents cross-tenant revoke (T-02-03)"
metrics:
  duration: "3 min"
  completed_date: "2026-05-27"
  tasks_completed: 3
  files_changed: 7
---

# Phase 02 Plan 01: Invite Generation Vertical Slice Summary

Multi-use invite link backend: non-aluno users can generate slot-capped, expiring, revocable invite tokens scoped to their institution.

## Tasks Completed

| # | Task | Commit | Type |
|---|------|--------|------|
| 1 | Failing integration test (TDD RED) | 4e2da7d | test |
| 2 | Invite model + User compound index | cad5a05 | feat |
| 3 | Invite controller, routes, app mount (GREEN) | 9ff6f60 | feat |

## What Was Built

**Invite model** (`src/models/Invite.js`): Mongoose schema with all D-09 fields — institutionId, invitedById, roleTarget (role enum), token (unique UUID), maxUses, usesCount (default 0), status (active/revoked/expired, default active), expiresAt (required), createdAt (default Date.now).

**User model fix** (`src/models/User.js`): Removed global `unique: true` on email field; added compound index `{ institutionId: 1, email: 1 }` so the same email address may appear in different institutions but is unique within one (D-08).

**Invite controller** (`src/controllers/invite.controller.js`): Three handlers —
- `createInvite`: enforces D-07 (only admin targets admin), D-04 (slot caps per roleTarget), generates UUID token, sets expiresAt (7-day default), scopes institutionId/invitedById from req.user only (T-02-03)
- `getInvite`: public lookup by token, computes valid flag (status=active AND not expired AND not full), returns roleTarget, institution name, remainingSlots, reason
- `revokeInvite`: scoped to req.user.institutionId, flips status to revoked

**Invite routes** (`src/routes/invite.routes.js`): POST `/` (authenticate + requireRole non-aluno → createInvite enforcing INVT-04), GET `/:token` (public → getInvite), POST `/:token/revoke` (authenticate + requireRole non-aluno → revokeInvite).

**App mount** (`src/app.js`): Added `app.use('/api/invites', require('./routes/invite.routes'))`.

## Test Results

9/9 tests passing (`node --test src/controllers/invite.controller.test.js`):
- createInvite: 4 tests (valid creation, aluno cap exceeded, professor cap exceeded, admin-target 403)
- getInvite: 4 tests (valid lookup, unknown token 404, expired invite, revoked invite)
- revokeInvite: 1 test (revoke + subsequent getInvite reflects invalid)

## Deviations from Plan

None — plan executed exactly as written.

## TDD Gate Compliance

- RED gate commit: 4e2da7d (`test(02-01): add failing integration test for invite controller (RED)`) — MODULE_NOT_FOUND confirmed
- GREEN gate commit: 9ff6f60 (`feat(02-01): implement invite controller, routes, and app mount (GREEN)`) — 9/9 passing

## Known Stubs

None.

## Threat Flags

No new security surface beyond the plan's threat model (T-02-01 through T-02-05 all mitigated as designed).

## Self-Check: PASSED

- src/models/Invite.js: FOUND
- src/controllers/invite.controller.js: FOUND
- src/controllers/invite.controller.test.js: FOUND
- src/routes/invite.routes.js: FOUND
- Commit 4e2da7d: FOUND
- Commit cad5a05: FOUND
- Commit 9ff6f60: FOUND
