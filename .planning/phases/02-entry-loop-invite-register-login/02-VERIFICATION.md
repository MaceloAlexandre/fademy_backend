---
phase: 02-entry-loop-invite-register-login
verified_date: "2026-05-27"
verdict: PASS
method: automated tests + owner live E2E + DB inspection
---

# Phase 02 Verification — Entry Loop (Invite, Register, Login)

**Verdict: PASS** — all 5 success criteria met. Verified by automated suite (40/40), owner-run live E2E in the browser, and direct MongoDB inspection.

## Success Criteria (goal-backward)

| # | Criterion | Verdict | Evidence |
|---|-----------|---------|----------|
| 1 | admin/coordenador/professor/funcionario generate an institution-bound invite carrying a role; aluno → 403 | ✅ | invite.controller + 9 tests (02-01); `requireRole` gate excludes aluno; live: admin minted an aluno invite (token b06c119b…) |
| 2 | Accept valid invite → user created in inviting institution with invited role; expired rejected | ✅ | registration.controller + 9 tests (02-03); live: user persisted role=aluno, institutionId = Católica PB (same as admin), tenant-scoped |
| 3 | Registration requires + uploads a photo before the account is created | ✅ | backend-proxied upload + objectExists guard; submit disabled without photo; live: avatarUrl on Contabo (`…:miniredesocial/avatars/44aaf050….png`), real PutObject+HeadObject confirmed |
| 4 | Registered user logs in for a JWT and can log out | ✅ | auth login (Phase 1) + frontend login storing fademy_token + "Sair" logout guard; owner confirmed login works |
| 5 | React mobile-first branded frontend runs accept-invite + register + login against the API | ✅ | 02-04 React/Vite app, brand tokens (dark blue/black/beige/tobacco), 3 screens; owner ran the full flow on a mobile viewport (screenshot) |

## Evidence Detail

- **Automated:** 40/40 tests across seed, auth.middleware, role.middleware, invite.controller, media.controller (rewritten for proxied upload), registration.controller. `require('./src/app')` loads cleanly with all routes mounted.
- **Live loop proof:** invite `usesCount` advanced 0→1 (status still active, 4 slots left) after the owner registered — confirms the invite was consumed by a real registration.
- **Persisted user (Mongo):** name set, role=aluno (from invite, not body), institutionId tenant-scoped, avatarUrl → Contabo, passwordHash bcrypt `$2b$10$…` (never plaintext, never returned).
- **Storage connectivity:** live uploadAvatar + objectExists against Contabo Object Storage succeeded.

## Deviations Carried (documented in 02-04-SUMMARY)

1. **Upload architecture:** browser-direct presigned PUT (D-11) → backend-proxied upload (multer/putObject), to support Contabo (no bucket CORS), matching the proven ember_backend reference. registration.controller unchanged.
2. **S3-compatible endpoint:** added S3_ENDPOINT + forcePathStyle + S3_PUBLIC_URL; AWS behavior preserved when S3_ENDPOINT is empty.
3. **Logo:** beige icon-badge stopgap.

## Follow-ups (non-blocking)

- Public read permission for avatars on Contabo (upload path fully works; rendering avatars publicly is a separate config).
- Proper logo asset/treatment.
- Reconcile REQUIREMENTS.md header count (pre-existing note from Phase 1).
