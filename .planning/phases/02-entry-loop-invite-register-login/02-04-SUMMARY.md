---
phase: 02-entry-loop-invite-register-login
plan: "04"
subsystem: frontend/entry-loop + backend/storage-adapt
tags: [react, vite, frontend, mobile, upload, contabo, s3-compatible, multer, e2e]
dependency_graph:
  requires: [02-01, 02-02, 02-03]
  provides: [react-frontend, accept-invite-screen, register-screen, login-screen, logout, backend-proxied-upload]
  affects:
    - ../MINIREDESOCIAL_FRONT (separate repo)
    - src/services/s3.service.js
    - src/controllers/media.controller.js
    - src/routes/media.routes.js
    - src/config/env.js
tech_stack:
  added: ["react@19", "react-dom@19", "react-router-dom@7", "axios@1", "lucide-react@1", "vite@8", "@vitejs/plugin-react@6", "multer@^2.1.1"]
  removed: ["@aws-sdk/s3-request-presigner"]
  patterns: [mobile-first, css-modules, brand-tokens, backend-proxied-upload, path-style-s3-endpoint, jwt-localstorage]
key_files:
  created:
    - ../MINIREDESOCIAL_FRONT/package.json
    - ../MINIREDESOCIAL_FRONT/vite.config.js
    - ../MINIREDESOCIAL_FRONT/index.html
    - ../MINIREDESOCIAL_FRONT/src/main.jsx
    - ../MINIREDESOCIAL_FRONT/src/App.jsx
    - ../MINIREDESOCIAL_FRONT/src/styles/tokens.css
    - ../MINIREDESOCIAL_FRONT/src/lib/api.js
    - ../MINIREDESOCIAL_FRONT/src/components/ (AuthShell, FormField, PrimaryButton, PhotoUpload, InviteStatus, ErrorBanner, Spinner)
    - ../MINIREDESOCIAL_FRONT/src/pages/ (AcceptInvite, Register, Login)
  modified:
    - src/services/s3.service.js
    - src/controllers/media.controller.js
    - src/routes/media.routes.js
    - src/config/env.js
    - src/controllers/media.controller.test.js
decisions:
  - "Deployment storage is Contabo Object Storage (S3-compatible), not AWS — confirmed against the working ember_backend reference on the same tenant"
  - "Switched avatar upload from browser-direct presigned PUT (plan D-11) to backend-proxied upload (multer + putObject), matching ember_backend — Contabo does not expose bucket CORS, which browser-direct PUT would require"
  - "SDK bucket is the plain name (miniredesocial); the tenant prefix (<tenant>:<bucket>) lives only in the public read URL via S3_PUBLIC_URL — mirrors ember storage.public-url"
  - "S3_ENDPOINT + forcePathStyle enable any S3-compatible provider; empty S3_ENDPOINT preserves real-AWS behavior"
  - "JWT stored in localStorage 'fademy_token' (D-14 accepted MVP trade-off); axios interceptor attaches Bearer"
  - "No auto-login after register — redirect to /login?registered=1 (UI-SPEC)"
  - "Logo wrapped in a beige (surface) icon badge as a stopgap until a proper logo asset exists"
metrics:
  completed_date: "2026-05-27"
  tasks_completed: 2
  verification: "manual E2E confirmed by owner (invite -> register w/ photo -> login); user persisted tenant-scoped with Contabo avatarUrl + bcrypt hash"
---

# Phase 02 Plan 04: React Frontend + Backend-Proxied Upload Summary

The deployable, demo-able slice of the entry loop: a React + Vite mobile-first app (brand palette) running accept-invite, register-with-photo, and login/logout against the Plan 01–03 API. During live verification the storage target turned out to be Contabo Object Storage (S3-compatible), so the avatar upload was re-architected from presigned browser-direct PUT to a backend-proxied upload — matching the proven ember_backend reference and avoiding Contabo bucket-CORS.

## Tasks Completed

| # | Task | Commit (repo) | Type |
|---|------|--------|------|
| SC | frontend toolchain supply-chain gate (pre-approved) | — | chore |
| 1 | Vite scaffold, brand tokens, api client, 7 primitives | d689b23 (front) | feat |
| 2 | Screens, router, auth guard, upload→register flow | fdda10f (front) | feat |
| — | Contabo endpoint support (S3_ENDPOINT + path-style) | 537c386 (back) | fix |
| — | Backend-proxied upload (multer/putObject) replaces presign | 08cecb7 (back) | refactor |
| — | Register uploads via backend instead of presigned PUT | eefc247 (front) | refactor |
| — | Logo framed in beige icon badge (stopgap) | b68635a (front) | style |

## What Was Built

**Frontend (`../MINIREDESOCIAL_FRONT`, separate git repo):**
- Vite + React 19 scaffold; `tokens.css` verbatim from UI-SPEC (dark blue / black / beige / tobacco)
- `lib/api.js`: axios instance, baseURL from `VITE_API_URL` (default http://localhost:3000), Bearer interceptor from localStorage `fademy_token`
- 7 primitives: AuthShell, FormField, PrimaryButton, PhotoUpload, InviteStatus, ErrorBanner, Spinner (CSS Modules)
- Screens: **AcceptInvite** (`/invite/:token` — GET invite, role badge, remaining slots, valid/invalid/expired/full states), **Register** (`/register` — photo + fields, validation, upload→register flow with submit state machine), **Login** (`/login` — JWT to localStorage, ?registered=1 success banner)
- Auth guard on `/` shell with "Sair" logout (clears token → /login)

**Backend (storage adaptation for Contabo):**
- `s3.service.js`: `uploadAvatar({buffer, contentType})` via PutObjectCommand; custom endpoint + forcePathStyle when `S3_ENDPOINT` set; public URL via `S3_PUBLIC_URL`
- `media`: `POST /api/media/upload` (multipart via multer, memory storage, size-limited) replaces `/presign`
- `env.js`: added `S3_ENDPOINT`, `S3_PUBLIC_URL`; removed `S3_PRESIGN_TTL`
- Removed unused `@aws-sdk/s3-request-presigner`

## Test Results

- Backend suite: 40/40 passing (media tests rewritten for the upload handler; S3 mocked, no network)
- Frontend: `npm run build` succeeds
- **Live E2E (owner-verified):** invite → register-with-photo → login confirmed in browser. Real avatar uploaded to Contabo (PutObject + HeadObject confirmed). User persisted with role=aluno, correct institutionId (tenant-scoped), Contabo avatarUrl, and bcrypt passwordHash — verified directly in MongoDB.

## Deviations from Plan

**Major (approved by owner): upload architecture changed.** Plan D-11 specified browser-direct presigned PUT (PLAT-03). The real deployment storage is Contabo Object Storage, which does not expose bucket CORS configuration that browser-direct uploads require. The existing ember_backend project (same Contabo tenant) proxies uploads through its backend; we adopted that proven model: the browser POSTs the file to `/api/media/upload`, and the backend streams it to Contabo via `putObject`. No bucket CORS needed (only the existing API CORS). registration.controller (02-03) was unchanged — it still validates `objectExists` on the server-issued key (T-02-08b).

**Minor: logo presentation.** Logo rendered small/low-contrast on the dark background; wrapped in a beige icon badge as an interim fix pending a proper logo asset.

## Threat Flags

- T-02-14 (JWT in localStorage): accepted MVP trade-off (D-14); React JSX escaping, no dangerouslySetInnerHTML
- T-02-15 (client-side size/type only): server re-validates content-type + size at upload time
- T-02-SC (npm installs): blocking-human gates approved for the frontend toolchain and multer

## Known Stubs / Follow-ups

- Public read of avatars depends on bucket/object read permission on Contabo (upload path is fully working). If avatars need to render publicly, configure read access — does not affect the registration loop.
- Logo asset is a stopgap (beige badge); a dedicated logo treatment is a future polish item.

## Self-Check: PASSED

- Frontend commits d689b23, fdda10f, eefc247, b68635a: FOUND (front repo)
- Backend commits 537c386, 08cecb7: FOUND
- Backend suite 40/40: PASS
- Live E2E + DB verification: CONFIRMED by owner
