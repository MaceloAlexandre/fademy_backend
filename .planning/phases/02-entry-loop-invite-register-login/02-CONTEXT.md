# Phase 2: Entry Loop — Invite, Register, Login - Context

**Gathered:** 2026-05-21
**Status:** Ready for planning

<domain>
## Phase Boundary

The first deployable, demo-able slice of Fademy: an invited person opens the app
on their phone, accepts an invite via a shared link, registers with a mandatory
profile photo, and logs in/out. Delivers the backend (invites, registration,
S3 photo upload, logout) **and** the React mobile-first frontend (`MINIREDESOCIAL_FRONT`)
running the invite-acceptance + registration + login flow against the API.

Builds on Phase 1 (Express/MongoDB foundation, multi-tenant scoping, JWT
auth/role middleware, `POST /api/auth/login`).

Requirements: INVT-01, INVT-02, INVT-03, INVT-04, AUTH-01 (done in P1), AUTH-02,
AUTH-04, PLAT-03, FRNT-01, FRNT-02, FRNT-03.

In scope: invite generation (multi-use links), invite acceptance + registration,
mandatory profile photo to S3, logout, and the React front for these flows.
Out of scope: profiles/social graph, posts, feed, notifications, chat (later phases).

</domain>

<decisions>
## Implementation Decisions

### Invite model (multi-use links — supersedes the single-email invite in root CONTEXT.md)
- **D-01:** Invites are **multi-use shared links**, not bound to a single email. The
  backend generates a `token` (uuid) + link; the inviter copies and sends it manually
  (WhatsApp, etc.). **No email infrastructure** this phase (aligns with root CONTEXT.md
  "sem email por ora").
- **D-02:** Each invite carries: target `role`, `maxUses` (number of slots), `usesCount`
  (incremented on each successful registration), configurable `expiresAt`, and `status`.
- **D-03:** **Default expiry: 7 days**, configurable by the inviter at generation time.
- **D-04:** **Slot caps by target role:** `professor` and `funcionario` invites →
  max **10** slots; `aluno` invites → max **50** slots. `admin`/`coordenador` invites →
  max **10** (ASSUMED — confirm later; conservative default). Reject generation that
  exceeds the cap for the target role.
- **D-05:** A user can have **multiple active invites simultaneously**, each with its own
  role / `maxUses` / `expiresAt` (e.g. one "aluno" link + one "professor" link).
- **D-06:** **Revoke is in scope:** the inviter can deactivate an active invite before
  expiry (`status: revoked`). Accept is valid only when `status === active` AND not
  expired AND `usesCount < maxUses`. Closed invites (full/expired/revoked) are rejected.
- **D-07:** `aluno` cannot generate invites → **403** (reuse `requireRole` from Phase 1). INVT-04.
- **D-08:** The invited person types **their own email** at registration (validated unique
  per institution). The invite fixes only **institutionId + role**.
- **D-09:** **`invites` schema update** (from root CONTEXT.md): make `email` optional (or
  drop the fixed-email semantic), add `maxUses: Number`, `usesCount: Number` (default 0),
  `status: enum [active|revoked|expired]`. Keep `institutionId`, `invitedById`, `roleTarget`,
  `token`, `expiresAt`, `createdAt`.

### Registration & profile photo (S3)
- **D-10:** **Mandatory profile photo uploaded to S3 BEFORE the account is created**
  (success criterion 3, AUTH-04). Registration fails if no photo.
- **D-11:** **Upload mechanism: presigned URL.** Backend issues a presigned S3 PUT URL
  (validating content-type + size limit when signing); the browser uploads the image
  directly to S3; the resulting object URL becomes `user.avatarUrl`. Backend confirms the
  object exists before creating the user. PLAT-03.
- **D-12:** Registration accepting a token: validate invite (D-06), create the user in the
  invite's institution with the invite's role, hash the password (bcrypt, Phase 1 pattern),
  increment the invite's `usesCount`, flip `status` to `expired`/closed when `usesCount`
  reaches `maxUses`.

### Auth / session
- **D-13:** **Logout (AUTH-02) is stateless** — the client discards the JWT. No server-side
  blacklist or refresh tokens this phase.
- **D-14:** Frontend stores the JWT in **`localStorage`**. (Trade-off: XSS exposure —
  mitigated by React's default escaping; acceptable for the MVP timeline.)
- **D-15:** `POST /api/auth/login` already exists (Phase 1). This phase adds the logout
  affordance and wires login into the frontend.

### Frontend stack (Claude's discretion — locked here for downstream)
- **D-16:** **React + Vite**, mobile-first, in sibling folder **`MINIREDESOCIAL_FRONT`**
  (FRNT-01). Separate from the backend repo/folder.
- **D-17:** `axios` for API calls; React Router for routing. Auth token attached via an
  axios interceptor reading `localStorage`.
- **D-18:** Brand palette applied (FRNT-02) — exact hex from root `CONTEXT.md`
  (primárias `#083C5D`, `#8C6E4F`, `#D6CFC1`; secundárias `#333333`, `#FD8080`, `#E6E6FA`).
  Fine-grained UI/interaction design is deferred to `/gsd:ui-phase 2` (UI-SPEC).

### Claude's Discretion
- API route shapes/naming, exact S3 key structure, presigned URL TTL, image size/type
  limits, axios interceptor structure, frontend folder layout, form validation UX details,
  and which screens compose the flow (accept-invite, register, login). UI specifics defer
  to UI-SPEC.

</decisions>

<canonical_refs>
## Canonical References

**Downstream agents MUST read these before planning or implementing.**

### Project spec & schema (authoritative)
- `CONTEXT.md` (repo root) — full product spec: `invites`/`users` schemas, roles table,
  business rules, folder structure. **NOTE:** the invite model here (D-01..D-09) supersedes
  the single-email invite in this file; the schema must be updated per D-09.

### Phase 1 foundation (reuse, don't rebuild)
- `.planning/phases/01-foundation-multi-tenant-seed/01-CONTEXT.md` — locked foundation decisions
- `src/middlewares/auth.middleware.js`, `src/middlewares/role.middleware.js` — JWT verify + `requireRole` (reuse for INVT-04 403)
- `src/services/token.service.js` — JWT sign/verify (HS256 pinned)
- `src/controllers/auth.controller.js`, `src/routes/auth.routes.js` — existing login endpoint
- `src/models/User.js`, `src/models/Institution.js` — existing models; `Invite` model to be added
- `src/config/env.js` — env config pattern (add S3 + bucket vars here)

### Requirements & roadmap
- `.planning/REQUIREMENTS.md` — INVT-01..04, AUTH-01/02/04, PLAT-03, FRNT-01..03
- `.planning/ROADMAP.md` — Phase 2 goal + 5 success criteria (UI hint: yes)

</canonical_refs>

<code_context>
## Existing Code Insights

### Reusable Assets
- `requireRole(...)` middleware → directly enforces INVT-04 (aluno → 403 on invite generation)
- `auth.middleware` (attaches `req.user` with `institutionId` + `role`) → invite generation
  is scoped to `req.user.institutionId`; invites inherit the inviter's institution
- `token.service` (bcrypt + JWT patterns) → registration password hashing + login token
- `User` model with required `institutionId` → new users created scoped to the invite's institution

### Established Patterns
- Multi-tenant scoping: every query filters by `req.user.institutionId` (PLAT-01) — invites
  and new users must follow this
- Folder layout `src/{models,routes,controllers,middlewares,services,config}` — add `Invite`
  model, invite + registration routes/controllers, an S3 service
- `node:test` + `mongodb-memory-server` test pattern from Phase 1 — reuse for invite/registration tests

### Integration Points
- New `Invite` model + invite/registration controllers join the existing Express app (`src/app.js` mount points)
- New S3 service reads bucket config from `src/config/env.js`
- The new React app (`MINIREDESOCIAL_FRONT`) consumes the API over HTTP (CORS to configure)

</code_context>

<specifics>
## Specific Ideas

- Invite as a "link with slots" — like a class-wide signup link a professor shares once for
  up to 50 students, rather than 50 individual email invites.
- Inviter can run several invite links in parallel (one per role).
- Revoke must work immediately — a revoked link stops accepting registrations even before expiry.

</specifics>

<deferred>
## Deferred Ideas

- Automatic email delivery of invites (SMTP/SES) — deferred; manual link sharing this phase.
- `invite_received` notification type — belongs to the Notifications phase (Phase 5).
- Server-side token revocation / refresh tokens — deferred; stateless logout this phase.
- Profiles, follows, posts, feed, chat — later phases.
- Confirm the slot cap for `admin`/`coordenador` target invites (assumed 10).

None blocking — discussion stayed within phase scope.

</deferred>

---

*Phase: 2-entry-loop-invite-register-login*
*Context gathered: 2026-05-21*
