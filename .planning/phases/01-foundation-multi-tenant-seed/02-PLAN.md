---
phase: 01-foundation-multi-tenant-seed
plan: 02
type: execute
wave: 2
depends_on: [01-01]
files_modified:
  - src/middlewares/auth.middleware.js
  - src/middlewares/role.middleware.js
  - src/services/token.service.js
  - src/controllers/auth.controller.js
  - src/routes/auth.routes.js
  - src/routes/admin.routes.js
  - src/controllers/admin.controller.js
  - src/middlewares/auth.middleware.test.js
  - src/middlewares/role.middleware.test.js
  - src/app.js
autonomous: true
requirements: [AUTH-03, PLAT-01]
must_haves:
  truths:
    - "auth.middleware verifies the JWT and attaches the authenticated user (id, role, institutionId) to the request"
    - "role.middleware rejects unauthorized roles with 403 before the controller runs"
    - "A protected, institution-scoped route returns only data belonging to the authenticated user's institutionId"
    - "An expired or invalid or missing token is rejected with 401 before the controller runs"
  artifacts:
    - path: "src/middlewares/auth.middleware.js"
      provides: "JWT verification middleware attaching req.user"
      contains: "verify"
    - path: "src/middlewares/role.middleware.js"
      provides: "requireRole(...roles) factory returning 403 for disallowed roles"
      contains: "requireRole"
    - path: "src/services/token.service.js"
      provides: "signToken / verifyToken using JWT_SECRET and expiry"
      contains: "sign"
    - path: "src/routes/admin.routes.js"
      provides: "Protected admin-only route exercising the middleware chain end-to-end"
      contains: "requireRole"
  key_links:
    - from: "src/middlewares/role.middleware.js"
      to: "src/middlewares/auth.middleware.js"
      via: "role middleware reads req.user.role set by auth middleware"
      pattern: "req\\.user"
    - from: "src/controllers/admin.controller.js"
      to: "src/models/Institution.js"
      via: "query scoped by req.user.institutionId"
      pattern: "institutionId"
---

<objective>
Complete the Walking Skeleton's auth backbone: JWT verification middleware,
role-based authorization middleware that rejects before controllers (AUTH-03),
and a single protected, institution-scoped route that exercises the full chain
end-to-end against the seeded bootstrap admin. This proves a request flows
token → auth → role-gate → controller → institution-scoped query, establishing
the access-control and multi-tenant query pattern every later phase reuses.

Purpose: AUTH-03 demands authorization in middleware before controllers; PLAT-01
demands every query be scoped by institution. This plan makes both real and
testable on a live request path, not just in model definitions.
Output: token service, auth + role middleware (with tests), a login endpoint to
mint a real token, and a protected admin route that returns only the caller's
institution data.

Covers: AUTH-03 (role enforcement in middleware before controllers),
PLAT-01 (queries scoped by the authenticated user's institutionId).
</objective>

<execution_context>
@$HOME/.claude/get-shit-done/workflows/execute-plan.md
@$HOME/.claude/get-shit-done/templates/summary.md
</execution_context>

<context>
@.planning/phases/01-foundation-multi-tenant-seed/01-CONTEXT.md
@.planning/phases/01-foundation-multi-tenant-seed/01-01-SUMMARY.md
@CONTEXT.md
@CLAUDE.md

<interfaces>
<!-- From Plan 01 (01-01-SUMMARY.md is authoritative once written). -->

User model: { _id, institutionId (ref Institution), name, email (unique), passwordHash, role (admin|coordenador|professor|funcionario|aluno), isPrivate, active }
Category model: { _id, name, slug, type (content|area) }  // NOTE: categories are global/fixed in this spec, NOT institution-scoped
env config exports: JWT_SECRET, JWT_EXPIRES_IN
src/app.js exports the Express app with a commented mount point for protected routers
Bootstrap admin exists after seed: email = BOOTSTRAP_ADMIN_EMAIL, role = admin, institutionId = catolica-pb
</interfaces>
</context>

<tasks>

<task type="auto" tdd="true">
  <name>Task 1: Token service + auth/role middleware with failing tests first</name>
  <files>src/services/token.service.js, src/middlewares/auth.middleware.js, src/middlewares/role.middleware.js, src/middlewares/auth.middleware.test.js, src/middlewares/role.middleware.test.js</files>
  <read_first>
    - .planning/phases/01-foundation-multi-tenant-seed/01-CONTEXT.md — Roles & auth middleware (AUTH-03), Stack (jsonwebtoken locked)
    - CONTEXT.md (repo root) — Roles e permissões table, Observações para o Claude Code ("Sempre validar role no middleware antes de chegar no controller")
    - src/config/env.js — JWT_SECRET, JWT_EXPIRES_IN (from Plan 01)
  </read_first>
  <behavior>
    - Test (auth): a request with a valid Bearer token calls next() and sets req.user with id, role, institutionId from the token claims
    - Test (auth): a request with no Authorization header is rejected 401 and next() is NOT called
    - Test (auth): a request with a malformed/tampered token is rejected 401
    - Test (auth): a request with an expired token is rejected 401
    - Test (auth): algorithm is pinned — a token signed/altered with alg none or a different algorithm is rejected (no algorithm-confusion)
    - Test (role): requireRole('admin') calls next() when req.user.role is admin
    - Test (role): requireRole('admin') responds 403 and does NOT call next() when req.user.role is aluno
    - Test (role): requireRole used with no req.user (auth not run) fails closed → 401/403, never passes through
  </behavior>
  <action>
    Create src/services/token.service.js exporting signToken(payload) and verifyToken(token) using jsonwebtoken, JWT_SECRET, expiresIn = JWT_EXPIRES_IN, and an explicit algorithm (HS256) passed to BOTH sign and verify so verification pins the algorithm (defeats alg-confusion / alg:none). Payload carries sub (user id), role, institutionId.
    Create src/middlewares/auth.middleware.js: read the Bearer token from the Authorization header; if absent, respond 401 and return (do not call next). Verify via token.service; on any verification error respond 401 and return. On success attach req.user = { id, role, institutionId } and call next(). It must fail closed — any uncertainty results in 401, never pass-through.
    Create src/middlewares/role.middleware.js exporting requireRole(...allowedRoles) which returns an Express middleware: if req.user is missing respond 401 (auth must run first); if req.user.role is not in allowedRoles respond 403 and return WITHOUT calling next (reject before controller); otherwise call next.
    Write the two test files encoding the behaviors above using mock req/res/next objects (no live server needed). Tests MUST fail first (RED) — write them before/alongside the implementations and confirm they fail when implementations are stubbed empty, then pass once implemented.
  </action>
  <verify>
    <automated>npm test 2>&1 | tail -25</automated>
  </verify>
  <acceptance_criteria>
    - src/middlewares/role.middleware.js exports requireRole
    - src/services/token.service.js exports signToken and verifyToken; verifyToken pins algorithm HS256
    - auth.middleware: missing/invalid/expired token → 401 and next NOT called; valid token → req.user set with id, role, institutionId and next called
    - role.middleware: disallowed role → 403 and next NOT called; missing req.user → 401/403 (fail closed); allowed role → next called
    - Token signed with a non-HS256 / none algorithm is rejected
    - `npm test` passes the auth and role middleware suites
  </acceptance_criteria>
  <done>Token service and both middlewares implemented and unit-tested; authorization fails closed and rejects before controllers.</done>
</task>

<task type="auto">
  <name>Task 2: Login endpoint + protected institution-scoped admin route wired into the app</name>
  <files>src/controllers/auth.controller.js, src/routes/auth.routes.js, src/controllers/admin.controller.js, src/routes/admin.routes.js, src/app.js</files>
  <read_first>
    - .planning/phases/01-foundation-multi-tenant-seed/01-CONTEXT.md — Roles & auth middleware, Multi-tenancy (PLAT-01), Specifics (Walking Skeleton end-to-end protected route)
    - CONTEXT.md (repo root) — Observações para o Claude Code (role validation in middleware; queries scoped by institution)
    - src/middlewares/auth.middleware.js and src/middlewares/role.middleware.js — chain to mount
    - src/services/token.service.js — signToken for login
    - src/models/User.js — passwordHash, institutionId, role (from Plan 01)
    - src/app.js — the commented mount point left by Plan 01
  </read_first>
  <action>
    Create src/controllers/auth.controller.js with a login handler: accept { email, password }; look up the user by email; if not found or inactive respond 401 with a generic message (no user-enumeration leak); bcrypt-compare password against passwordHash; on success signToken({ sub, role, institutionId }) and respond 200 { token }; on mismatch respond 401 generic. This exists to mint a real token so the protected route is exercisable end-to-end — it is the minimal slice of AUTH-01 needed to prove the middleware chain (full logout/AUTH-01/02 polish belongs to Phase 2).
    Create src/routes/auth.routes.js exposing POST /auth/login → auth.controller.login.
    Create src/controllers/admin.controller.js with a handler (e.g., getInstitutionOverview) that returns the caller's institution context: query scoped by req.user.institutionId — return the institution document for req.user.institutionId plus its user count scoped to that institutionId. This concretely demonstrates the PLAT-01 scoping pattern (never query without the institutionId filter).
    Create src/routes/admin.routes.js: GET /admin/overview protected by auth.middleware THEN requireRole('admin') THEN admin.controller — proving role rejection happens before the controller.
    Edit src/app.js at the Plan 01 mount point to mount auth.routes and admin.routes. Touch only the mount point and necessary imports (Surgical Changes).
  </action>
  <verify>
    <automated>npm test 2>&1 | tail -15 && node -e "import('./src/app.js').then(()=>console.log('app mounts auth+admin routers')).catch(e=>{console.error(e);process.exit(1)})"</automated>
  </verify>
  <acceptance_criteria>
    - POST /auth/login returns 200 { token } for valid seeded admin credentials and 401 (generic) for wrong password or unknown email
    - GET /admin/overview with a valid admin token returns 200 with institution data scoped to req.user.institutionId
    - GET /admin/overview with a valid aluno token returns 403 (role rejected before controller)
    - GET /admin/overview with no/invalid token returns 401
    - admin.controller queries are filtered by req.user.institutionId (no unscoped institution-owned query)
    - src/app.js mounts auth.routes and admin.routes at the previously-commented mount point
  </acceptance_criteria>
  <done>Login mints a JWT; a protected admin route runs auth → role-gate → institution-scoped controller end-to-end; the Walking Skeleton's protected-route proof is complete.</done>
</task>

</tasks>

<threat_model>
## Trust Boundaries

| Boundary | Description |
|----------|-------------|
| HTTP client → /auth/login | Untrusted credentials cross into auth |
| HTTP client → /admin/* | Untrusted token claims drive authorization + tenant scope |
| token claims → controller | institutionId/role from JWT govern data access |

## STRIDE Threat Register

| Threat ID | Category | Component | Disposition | Mitigation Plan |
|-----------|----------|-----------|-------------|-----------------|
| T-02-01 | Spoofing | JWT verification | mitigate | Pin algorithm HS256 on sign AND verify (no alg:none/confusion); verify signature with JWT_SECRET; reject malformed tokens with 401 |
| T-02-02 | Elevation of Privilege | role.middleware (AUTH-03) | mitigate | requireRole rejects 403 before controller; fail closed when req.user absent; role read from verified token only, never from request body/query |
| T-02-03 | Information Disclosure | Multi-tenant query (PLAT-01) | mitigate | admin controller filters every query by req.user.institutionId from the verified token; prevents cross-institution IDOR |
| T-02-04 | Information Disclosure | login error messages | mitigate | Generic 401 for both unknown email and wrong password — no user enumeration |
| T-02-05 | Tampering | Expiry / replay | mitigate | Tokens carry expiry (JWT_EXPIRES_IN); expired tokens rejected 401 |
| T-02-06 | Repudiation | Password handling | mitigate | bcrypt-compare against passwordHash; plaintext never stored or logged |
| T-02-SC | Tampering | npm installs | accept | No new packages beyond Plan 01 (jsonwebtoken, bcrypt already vetted, mainstream). If a new dep is needed, gate before install. |
</threat_model>

<verification>
- `npm test` passes auth.middleware, role.middleware, and any login/route suites
- With seeded admin: POST /auth/login → 200 token; GET /admin/overview with that token → 200 institution-scoped data
- A token whose role is aluno → GET /admin/overview returns 403 (rejected before controller)
- Missing/expired/tampered token → 401
- admin controller contains no institution-owned query lacking an institutionId filter
</verification>

<success_criteria>
- Role-based authorization middleware exists and rejects unauthorized roles before controllers — AUTH-03, Phase success criterion 3
- Protected route returns only the authenticated user's institution data — PLAT-01
</success_criteria>

<output>
Create `.planning/phases/01-foundation-multi-tenant-seed/01-02-SUMMARY.md` when done.
</output>
