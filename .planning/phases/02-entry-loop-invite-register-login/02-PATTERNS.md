# Phase 2: Entry Loop — Invite, Register, Login - Pattern Map

**Mapped:** 2026-05-21
**Files analyzed:** 17 (8 backend, 9 frontend)
**Analogs found:** 8 / 17 (all 8 backend have analogs; all 9 frontend are greenfield)

## File Classification

### Backend (analogs exist — Phase 1 foundation)

| New/Modified File | Role | Data Flow | Closest Analog | Match Quality |
|-------------------|------|-----------|----------------|---------------|
| `src/models/Invite.js` (new) | model | CRUD | `src/models/User.js` | role-match (mongoose schema) |
| `src/models/User.js` (modify — make schema accept registration) | model | CRUD | itself (already correct; only `avatarUrl` already exists) | exact |
| `src/controllers/invite.controller.js` (new) | controller | request-response / CRUD | `src/controllers/admin.controller.js` + `auth.controller.js` | role-match |
| `src/controllers/registration.controller.js` (new) | controller | request-response / CRUD | `src/controllers/auth.controller.js` | exact (auth + bcrypt + token) |
| `src/controllers/media.controller.js` (new — presign) | controller | request-response / file-I/O | `src/controllers/auth.controller.js` (handler shape) | role-match (no S3 analog) |
| `src/routes/invite.routes.js` (new) | route | request-response | `src/routes/admin.routes.js` (protected) + `auth.routes.js` (public) | exact |
| `src/routes/media.routes.js` (new) | route | request-response | `src/routes/admin.routes.js` | exact |
| `src/services/s3.service.js` (new — presign URL) | service | file-I/O | `src/services/token.service.js` (service module shape) | role-match (no I/O analog) |
| `src/config/env.js` (modify — add S3 vars) | config | — | itself | exact |
| `src/app.js` (modify — mount new routers) | config/route | — | itself | exact |
| Tests: `*.test.js` for invite + registration | test | — | `src/seed/seed.test.js` (DB) + `role.middleware.test.js` (unit) | exact |

### Frontend (greenfield — `MINIREDESOCIAL_FRONT` scaffolded this phase)

| New File | Role | Data Flow | Analog | Match Quality |
|----------|------|-----------|--------|---------------|
| `MINIREDESOCIAL_FRONT/` (Vite scaffold, package.json, vite.config) | config | — | none | greenfield |
| `src/styles/tokens.css` | config | — | none (spec is the contract — UI-SPEC lines 358-398) | greenfield |
| `src/lib/api.js` (axios instance + interceptor) | service | request-response | none | greenfield |
| `src/pages/AcceptInvite.jsx` (`/invite/:token`) | component | request-response | none | greenfield |
| `src/pages/Register.jsx` (`/register?token=`) | component | request-response / file-I/O | none | greenfield |
| `src/pages/Login.jsx` (`/login`) | component | request-response | none | greenfield |
| `src/components/{AuthShell,FormField,PrimaryButton,PhotoUpload,InviteStatus,ErrorBanner,Spinner}.jsx` | component | — | none (UI-SPEC component inventory, lines 124-138) | greenfield |
| `src/App.jsx` (React Router + auth guard) | component | — | none | greenfield |
| `src/main.jsx` (entry, imports tokens.css) | config | — | none | greenfield |

## Pattern Assignments — Backend

### `src/models/Invite.js` (model, CRUD) — NEW

**Analog:** `src/models/User.js` (read in full, 25 lines)

Copy the schema module shape exactly: `'use strict'`, `require('mongoose')`, `new mongoose.Schema({...})`, `module.exports = mongoose.model(...)`. Reuse the `institutionId` ref + `required` pattern and the `role` enum array. Reuse `createdAt: { type: Date, default: Date.now }`.

**Schema shape pattern** (from `User.js` lines 5-23):
```javascript
const userSchema = new mongoose.Schema({
  institutionId: {
    type: mongoose.Schema.Types.ObjectId,
    ref: 'Institution',
    required: true,
  },
  role: {
    type: String,
    required: true,
    enum: ['admin', 'coordenador', 'professor', 'funcionario', 'aluno'],
  },
  active: { type: Boolean, default: true },
  createdAt: { type: Date, default: Date.now },
});
```

**Invite-specific fields to add** (per CONTEXT.md D-02/D-09): `token` (String, unique — UUID), `invitedById` (ObjectId ref User), `roleTarget` (reuse the same enum), `maxUses` (Number), `usesCount` (Number, default 0), `status` (String enum `['active','revoked','expired']`, default `'active'`), `expiresAt` (Date). The `email` field is dropped / made optional (D-08/D-09).

> Note: `User.js` uses `email: { unique: true }` globally. Per D-08 the requirement is unique **per institution** — the planner should switch to a compound index `{ institutionId, email }` unique. This is a deviation from the Phase 1 analog, flag it explicitly.

### `src/controllers/registration.controller.js` (controller, request-response) — NEW

**Analog:** `src/controllers/auth.controller.js` (read in full, 45 lines) — closest because it already does bcrypt + token + the exact response/error conventions.

**Imports + bcrypt + token pattern** (from `auth.controller.js` lines 1-5, 33-37):
```javascript
'use strict';
const bcrypt = require('bcrypt');
const User = require('../models/User');
const { signToken } = require('../services/token.service');
// ... hash with bcrypt.hash(password, BCRYPT_ROUNDS) on create (see seed.js line 69)
```

**Handler + error envelope pattern** (from `auth.controller.js` lines 13-43):
```javascript
async function login(req, res) {
  const { email, password } = req.body || {};
  if (!email || !password) {
    return res.status(400).json({ error: 'email and password are required' });
  }
  try {
    // ...domain logic...
    return res.status(200).json({ token });
  } catch (err) {
    return res.status(500).json({ error: 'Internal server error' });
  }
}
module.exports = { login };
```

**Registration-specific logic** (CONTEXT.md D-12): validate invite via shared helper (status active + not expired + usesCount<maxUses), `bcrypt.hash(password, BCRYPT_ROUNDS)` (use `require('../config/env').BCRYPT_ROUNDS`, same as seed), create User scoped to `invite.institutionId` with `invite.roleTarget`, set `avatarUrl` (already on User schema, line 14), increment `usesCount`, flip `status` to `expired`/closed when `usesCount >= maxUses`. Use `400` for invalid invite, `409` for duplicate email, `201` on success.

### `src/controllers/invite.controller.js` (controller, request-response/CRUD) — NEW

**Analog:** `src/controllers/admin.controller.js` (read in full, 33 lines) — closest because it shows the `req.user.institutionId` scoping pattern + the protected-controller doc convention.

**Multi-tenant scoping pattern** (from `admin.controller.js` lines 12-22):
```javascript
async function getInstitutionOverview(req, res) {
  const { institutionId } = req.user;          // scope every query to caller's institution
  try {
    const institution = await Institution.findById(institutionId).lean();
    if (!institution) return res.status(404).json({ error: 'Institution not found' });
    // ...
  } catch (err) {
    return res.status(500).json({ error: 'Internal server error' });
  }
}
```

**Invite-specific handlers:**
- `createInvite` — scope to `req.user.institutionId`, set `invitedById = req.user.id`, enforce slot cap by `roleTarget` (D-04: professor/funcionario→10, aluno→50, admin/coordenador→10), enforce role permission (D-07: only `admin` can target `admin`; `aluno` blocked at the route layer via `requireRole`). Reject cap-exceeding generation with `400`/`422`.
- `getInvite` (public, `GET /api/invites/:token`) — validate status/expiry/slots, return role + institution name + remaining slots for the AcceptInvite screen. No auth.
- `revokeInvite` — scope to institution + `invitedById`, set `status: 'revoked'` (D-06).

### `src/controllers/media.controller.js` (controller, file-I/O) — NEW

**Analog:** `src/controllers/auth.controller.js` for the handler/error shape (no existing S3/file analog — partial match). Calls into the new `s3.service.js`.

`POST /api/media/presign` accepts `{ contentType, size }`, validates content-type allowlist (`image/jpeg|png|webp`) and size (≤ 5 MB, UI-SPEC line 190), returns `{ uploadUrl, objectUrl }`. Use the same `400` validation / `500` catch envelope as `auth.controller.js`.

### `src/services/s3.service.js` (service, file-I/O) — NEW

**Analog:** `src/services/token.service.js` (read in full, 30 lines) for the **module shape only** (no S3 analog exists — partial match).

**Service module shape pattern** (from `token.service.js` lines 1-6, 13-30):
```javascript
'use strict';
const jwt = require('jsonwebtoken');
const { JWT_SECRET, JWT_EXPIRES_IN } = require('../config/env');
function signToken(payload) { /* ... */ }
function verifyToken(token) { /* ... */ }
module.exports = { signToken, verifyToken };
```

Mirror this: read S3 config from `require('../config/env')`, export e.g. `{ getPresignedPutUrl, objectExists }`. The S3 SDK (`@aws-sdk/client-s3` + `@aws-sdk/s3-request-presigner`) is a NEW dependency to add to `package.json` (currently only bcrypt/dotenv/express/jsonwebtoken/mongoose).

### `src/routes/invite.routes.js` & `src/routes/media.routes.js` (route) — NEW

**Analog (protected routes):** `src/routes/admin.routes.js` (read in full, 14 lines) — exact match for the auth→role→controller chain.

```javascript
'use strict';
const { Router } = require('express');
const authenticate = require('../middlewares/auth.middleware');
const { requireRole } = require('../middlewares/role.middleware');
const { getInstitutionOverview } = require('../controllers/admin.controller');
const router = Router();
router.get('/overview', authenticate, requireRole('admin'), getInstitutionOverview);
module.exports = router;
```

**Analog (public route):** `src/routes/auth.routes.js` (lines 1-10) — for `GET /api/invites/:token` and `POST /api/auth/register`, which take NO auth middleware:
```javascript
const router = Router();
router.post('/login', login);
module.exports = router;
```

**Route assignments:**
- `POST /api/invites` → `authenticate, requireRole('admin','coordenador','professor','funcionario'), createInvite` — `requireRole` directly enforces INVT-04 (aluno → 403). The admin-target restriction (D-07) is enforced inside the controller, not the middleware.
- `GET /api/invites/:token` → public (no middleware), `getInvite`.
- `POST /api/invites/:token/revoke` → `authenticate, requireRole(...non-aluno), revokeInvite`.
- Registration: add `router.post('/register', register)` to **`auth.routes.js`** (public) OR a new `registration.routes.js`. Prefer adding to `auth.routes.js` since it lives under `/api/auth` alongside login (UI-SPEC calls `POST /api/auth/register`, line 197).

### `src/config/env.js` (config, modify)

**Analog:** itself (lines 5-22). Add S3 keys to the `module.exports` object following the existing `KEY: process.env.KEY || default` pattern. Add `AWS_REGION`, `S3_BUCKET`, `S3_ACCESS_KEY_ID`, `S3_SECRET_ACCESS_KEY` (and presign TTL / max-size if configurable). Add hard-required ones to the `required` array (line 5) so the app fails fast. Mirror new vars into `.env.example`.

### `src/app.js` (modify — mount routers)

**Analog:** itself (lines 13-15). Follow the existing mount pattern exactly:
```javascript
app.use('/api/auth', require('./routes/auth.routes'));
app.use('/api/admin', require('./routes/admin.routes'));
```
Add `app.use('/api/invites', require('./routes/invite.routes'))` and `app.use('/api/media', require('./routes/media.routes'))`. **CORS** for the frontend (CONTEXT.md integration note) must be added here — no CORS exists yet, so `cors` is a NEW dependency.

## Pattern Assignments — Frontend (greenfield, no in-repo analog)

There is no existing frontend in this repo. `MINIREDESOCIAL_FRONT` is scaffolded this phase. The authoritative contract for ALL frontend files is **`02-UI-SPEC.md`**, not a code analog. Planner must reference UI-SPEC sections directly:

| File | Contract source (UI-SPEC) |
|------|----------------------------|
| `src/styles/tokens.css` | Token File Reference (lines 355-399) — copy verbatim |
| `src/components/*.jsx` | Component Inventory (lines 124-138) + AuthShell Layout (lines 248-276) |
| `src/pages/AcceptInvite.jsx` | Screen 1 (lines 142-163) — states, `GET /api/invites/:token`, copy strings |
| `src/pages/Register.jsx` | Screen 2 (lines 166-215) — presigned upload flow (D-11), submit state machine |
| `src/pages/Login.jsx` | Screen 3 (lines 217-244) — localStorage `fademy_token`, post-register banner |
| `src/lib/api.js` | CONTEXT.md D-17 (axios + interceptor reading `localStorage`) |
| All copy | Copywriting Contract (lines 281-318) — Portuguese strings verbatim |

**Stack (CONTEXT.md D-16/D-17/D-18, UI-SPEC Design System):** React + Vite, axios, React Router, lucide-react, Inter font, CSS Modules (no Tailwind/styled-components/shadcn). JWT in `localStorage` key `fademy_token`. Logo at `MINIREDESOCIAL_FRONT/src/assets/logo_fademy.png` (D-19). The frontend folder is its own git repo, initialized this phase.

## Shared Patterns

### Module header + error envelope
**Source:** every backend file — `'use strict';` first line; controllers wrap logic in `try { ... } catch (err) { return res.status(500).json({ error: 'Internal server error' }); }`; success/validation use `res.status(N).json({ ... })`; errors are always `{ error: string }`.
**Apply to:** all new controllers and services.

### Multi-tenant scoping (PLAT-01)
**Source:** `src/controllers/admin.controller.js` lines 13-22; `src/middlewares/auth.middleware.js` lines 23-27 (attaches `req.user = { id, role, institutionId }`).
**Apply to:** invite controller (scope create/list/revoke to `req.user.institutionId`; new users inherit `invite.institutionId`).
```javascript
const { institutionId } = req.user;   // never trust client-supplied institution
```

### Auth + role gate chain (AUTH-03 / INVT-04)
**Source:** `src/routes/admin.routes.js` line 12 + `src/middlewares/role.middleware.js` lines 15-27.
**Apply to:** all protected routes — `authenticate, requireRole(...roles), handler`. `requireRole` rejects with 403 before the controller. Use it to block `aluno` from invite generation (INVT-04).

### Password hashing
**Source:** `src/seed/seed.js` line 69 (`bcrypt.hash(adminPassword, bcryptRounds)`) + `auth.controller.js` line 27 (`bcrypt.compare`). Rounds come from `env.BCRYPT_ROUNDS` (env.js line 21).
**Apply to:** registration controller (hash on create, store `passwordHash`, never plaintext).

### JWT signing payload shape
**Source:** `src/controllers/auth.controller.js` lines 33-37 + `token.service.js` lines 13-18.
**Apply to:** if registration auto-logs-in (optional) it must sign `{ sub, role, institutionId }` identically. Per UI-SPEC the flow redirects to `/login?registered=1`, so registration likely returns the user, not a token — confirm in planning.

### Config access
**Source:** `src/config/env.js` — all env reads go through this module (`require('../config/env')`), never raw `process.env` in controllers/services.
**Apply to:** s3.service.js, media.controller.js.

### Test structure
**Source (DB integration):** `src/seed/seed.test.js` lines 1-51 — `node:test` (`describe`/`it`/`before`/`after`), `mongodb-memory-server` spun up in `before`, `BCRYPT_ROUNDS='4'` for speed, `mongoose.disconnect()` + `mongoServer.stop()` in `after`.
**Source (pure unit):** `src/middlewares/role.middleware.test.js` lines 1-37 — hand-rolled `makeReq`/`makeRes`/`makeNext` mocks, no DB.
**Apply to:** invite/registration tests use the DB harness (seed.test.js pattern); slot-cap / permission logic can use the mock harness where no DB is needed. New test files MUST be added to the `test` script glob in `package.json` line 11 (it lists files explicitly, not a wildcard).

## No Analog Found

| File | Role | Data Flow | Reason | Use Instead |
|------|------|-----------|--------|-------------|
| `src/services/s3.service.js` | service | file-I/O | No S3/file-I/O code exists in repo | `token.service.js` for module shape; AWS SDK docs / RESEARCH.md for presign |
| `src/controllers/media.controller.js` | controller | file-I/O | No upload endpoint exists | `auth.controller.js` for handler/error shape only |
| All `MINIREDESOCIAL_FRONT/**` files | component/config/service | request-response | No frontend in repo — greenfield this phase | `02-UI-SPEC.md` (authoritative contract) + CONTEXT.md D-16/D-17/D-18 |

## New Dependencies Introduced (not in current package.json)

Backend: `@aws-sdk/client-s3`, `@aws-sdk/s3-request-presigner` (presign), `cors` (frontend origin), `uuid` (invite token — or use `crypto.randomUUID()` from node core to avoid a dep).
Frontend: full Vite + React + react-router-dom + axios + lucide-react toolchain (greenfield).

## Metadata

**Analog search scope:** `src/{models,routes,controllers,middlewares,services,config,seed}` (entire backend — 24 source files)
**Files scanned:** 17 backend files read in full (controllers, models, routes, middlewares, services, config, app, seed, tests, package.json, .env.example)
**Pattern extraction date:** 2026-05-21
