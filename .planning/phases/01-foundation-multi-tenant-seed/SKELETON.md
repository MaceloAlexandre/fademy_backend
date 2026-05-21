# Walking Skeleton — Fademy

**Phase:** 1
**Generated:** 2026-05-21

## Capability Proven End-to-End

The seeded bootstrap admin can `POST /auth/login` to receive a JWT, then call a
protected `GET /admin/overview` route that runs auth → role-gate → an
institution-scoped query and returns only their own institution's data — served
by the running Express app against a live MongoDB, with `GET /health` confirming
the DB connection.

## Architectural Decisions

| Decision | Choice | Rationale |
|---|---|---|
| Framework | Node.js + Express | Fixed by project spec; minimal, well-understood REST backend |
| Data layer | MongoDB + Mongoose | Fixed by spec; document model fits the social-graph schema in root CONTEXT.md |
| Auth | JWT (`jsonwebtoken`, HS256 pinned) + bcrypt password hashing | Stateless tokens carry `role` + `institutionId`; algorithm pinned to defeat alg-confusion |
| Authorization | `requireRole(...)` middleware, fails closed, rejects 403 before controller | AUTH-03 contract — authorization happens in middleware, never in the controller |
| Multi-tenancy | `institutionId` ref required on every tenant-owned model; every query scoped to `req.user.institutionId` | PLAT-01 — tenant isolation from day one, IDOR/cross-institution leakage prevented |
| Deployment target | Local dev run (`npm run dev` + local/remote MongoDB) | VPS production deploy is Phase 7; Phase 1 only needs a runnable dev slice |
| Directory layout | `src/{config,models,routes,controllers,middlewares,services,seed}` + `app.js`/`server.js` | Locked in root CONTEXT.md "Estrutura de pastas"; later phases fill in siblings |

## Stack Touched in Phase 1

- [x] Project scaffold (package.json, scripts: start/dev/seed/test, .env.example, .gitignore, `node:test` runner)
- [x] Routing — `GET /health`, `POST /auth/login`, `GET /admin/overview`
- [x] Database — real write (idempotent seed: institution + 10 categories + admin) AND real read (login lookup + scoped overview query)
- [x] Auth/role middleware exercised end-to-end on a protected route (the "interactive" slice for a backend-only phase)
- [x] Deployment — documented local full-stack run: `npm run seed` then `npm run dev` against MongoDB

## Out of Scope (Deferred to Later Slices)

- Invites, registration flow, full login/logout polish (Phase 2 — only the minimal login needed to mint a token lands here)
- Profiles, follows, blocks, social graph (Phase 3)
- Posts, comments, categories *usage*, personalized feed (Phase 4 — categories are only *seeded* here)
- Notifications (Phase 5)
- Chat, groups, real-time/Socket.io (Phase 6)
- Media upload (Cloudinary/S3), production VPS deployment (Phase 7)
- All collections beyond Institution/User/Category (defined in root CONTEXT.md, built when their phase arrives)

## Subsequent Slice Plan

Each later phase adds one vertical slice on top of this skeleton without altering its architectural decisions:

- Phase 2: Entry Loop — invite generation/acceptance, register, full login/session
- Phase 3: Profiles & social graph — follows (public/private), blocks
- Phase 4: Posts, comments & personalized feed (categories applied to posts)
- Phase 5: In-app notifications via polling
- Phase 6: Chat & groups over Socket.io (real-time)
- Phase 7: Production deployment to the owner's VPS
