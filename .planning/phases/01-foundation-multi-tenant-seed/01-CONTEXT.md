# Phase 1: Foundation & Multi-Tenant Seed - Context

**Gathered:** 2026-05-21
**Status:** Ready for planning
**Source:** Derived from project-wide `CONTEXT.md` (repo root) + ROADMAP Phase 1 scope

<domain>
## Phase Boundary

This phase delivers the **backend foundation only**: a running Express/MongoDB
server with multi-tenant institution scoping baked in, a seed that creates
Católica PB + all fixed categories + a bootstrap admin, and the auth/role
middleware skeleton. It does **not** implement invites, posts, feed, follows,
notifications, or chat — those are later phases.

In scope (PLAT-01, PLAT-02, AUTH-03):
- Express app boot + MongoDB connection (Mongoose) + health check endpoint
- `institutionId` scoping pattern established on every tenant-owned model
- Seed script: institution `catolica-pb`, all `content` + `area` categories, bootstrap admin
- `auth.middleware` (verify JWT) + `role.middleware` (reject unauthorized roles before controller)

Models needed THIS phase: `Institution`, `User`, `Category`. Other collections
(invites, posts, follows, etc.) are defined in the root CONTEXT.md but are
**out of scope** here — only build them when their phase arrives.

</domain>

<decisions>
## Implementation Decisions

### Stack (locked)
- Node.js + Express + MongoDB + Mongoose + dotenv
- JWT for auth. **Library: `jsonwebtoken`** (root CONTEXT lists it as the first/default option; pick it unless a blocker emerges).

### Multi-tenancy (PLAT-01)
- Every tenant-owned model carries `institutionId: ObjectId` (ref `institutions`).
- Queries are scoped by institution — establish a consistent scoping pattern this
  phase that later models reuse (e.g. always filter by the authenticated user's
  `institutionId`).

### Roles & auth middleware (AUTH-03)
- Role enum: `admin | coordenador | professor | funcionario | aluno`.
- `auth.middleware.js` verifies the JWT and attaches the user (incl. `institutionId`, `role`).
- `role.middleware.js` checks permissions by role and **rejects with 403 before reaching the controller**.
- Password stored as `passwordHash` (hash on create — bcrypt).

### Seed (PLAT-02)
- Institution: `name: "Faculdade Católica da Paraíba"`, `slug: "catolica-pb"`, `active: true`.
- Categories (fixed, idempotent upsert by slug):
  - `type: content` → Projetos, Aulas, Dia a dia, Meme
  - `type: area` → Ciência da Computação, Direito, Marketing, Filosofia, Educação Física, NEPA
- Bootstrap admin user linked to Católica PB (`role: admin`), credentials from env.
- Seed must be **idempotent** — re-running does not duplicate data.

### Folder structure (locked — from root CONTEXT.md)
`src/config/`, `src/models/`, `src/routes/`, `src/controllers/`, `src/middlewares/`,
`src/services/`, `src/app.js`. This phase only creates the subset it needs.

### Schemas (Phase 1 subset — exact shapes in root CONTEXT.md)
- `institutions`: name, slug, logoUrl, active, createdAt
- `users`: institutionId, name, email (unique), passwordHash, avatarUrl, role, isPrivate (default false), active, createdAt
- `categories`: name, slug, type (enum content|area)

### Claude's Discretion
- Project scaffolding details (package.json scripts, folder bootstrapping), env var
  naming, health-check route shape, bcrypt cost factor, seed script invocation
  (`npm run seed`), and how the institution-scoping helper is factored.

</decisions>

<canonical_refs>
## Canonical References

**Downstream agents MUST read these before planning or implementing.**

### Project specification (authoritative)
- `CONTEXT.md` (repo root) — full product spec: all collections, business rules,
  roles/permissions table, folder structure, seed requirements, and "Observações
  para o Claude Code". Phase 1 builds the foundation subset; this file is the
  source of truth for schema shapes and naming.

### Requirements
- `.planning/REQUIREMENTS.md` — PLAT-01, PLAT-02, AUTH-03 definitions.

### Roadmap
- `.planning/ROADMAP.md` — Phase 1 goal + success criteria.

</canonical_refs>

<specifics>
## Specific Ideas

- Walking Skeleton target (MVP mode, first phase): the thinnest end-to-end slice —
  server boots → connects to MongoDB → seed runs → health check responds → a
  protected route exercises auth + role middleware end-to-end.
- Validate `hasLink`/post rules are NOT this phase; only roles + scoping + seed.

</specifics>

<deferred>
## Deferred Ideas

- Invites, posts, comments, follows, blocks, notifications, conversations, messages
  collections and routes — defined in root CONTEXT.md, built in later phases.
- Media upload (Cloudinary/S3) — deferred.
- Real-time (Socket.io) — deferred.

</deferred>

---

*Phase: 01-foundation-multi-tenant-seed*
*Context derived: 2026-05-21 from repo-root CONTEXT.md + ROADMAP Phase 1*
