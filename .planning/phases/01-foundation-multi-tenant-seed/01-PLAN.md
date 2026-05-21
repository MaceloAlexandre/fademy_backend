---
phase: 01-foundation-multi-tenant-seed
plan: 01
type: execute
wave: 1
depends_on: []
files_modified:
  - package.json
  - .env.example
  - .gitignore
  - src/app.js
  - src/server.js
  - src/config/db.js
  - src/config/env.js
  - src/models/Institution.js
  - src/models/User.js
  - src/models/Category.js
  - src/seed/seed.js
  - src/seed/seed.test.js
  - src/routes/health.routes.js
autonomous: true
requirements: [PLAT-01, PLAT-02]
must_haves:
  truths:
    - "The server boots and GET /health returns 200 with status ok when MongoDB is connected"
    - "Running the seed creates the Faculdade Católica da Paraíba institution, all fixed content and area categories, and a bootstrap admin user"
    - "Re-running the seed does not duplicate any data (idempotent)"
    - "Every tenant-owned model (User) carries an institutionId referencing institutions"
  artifacts:
    - path: "src/models/Institution.js"
      provides: "Institution Mongoose model (name, slug unique, logoUrl, active, createdAt)"
      contains: "slug"
    - path: "src/models/User.js"
      provides: "User model with institutionId, role enum, passwordHash, isPrivate default false"
      contains: "institutionId"
    - path: "src/models/Category.js"
      provides: "Category model with name, slug unique, type enum content|area"
      contains: "type"
    - path: "src/seed/seed.js"
      provides: "Idempotent seed: institution + 10 categories + bootstrap admin"
      contains: "catolica-pb"
    - path: "src/config/db.js"
      provides: "Mongoose connection helper"
      contains: "mongoose"
    - path: "src/routes/health.routes.js"
      provides: "GET /health endpoint reporting Mongo connection state"
      contains: "health"
  key_links:
    - from: "src/models/User.js"
      to: "src/models/Institution.js"
      via: "institutionId ref"
      pattern: "ref.*[Ii]nstitution"
    - from: "src/seed/seed.js"
      to: "src/config/db.js"
      via: "connect before seeding"
      pattern: "connect"
---

<objective>
Stand up the Walking Skeleton for Fademy's backend: a runnable Express/MongoDB
server that boots, connects to Mongo, exposes a health check, and an idempotent
seed that creates Católica PB + all fixed categories + a bootstrap admin. This
proves the full stack end-to-end (boot → DB connect → real write via seed → real
read via health/seed verification) and establishes the multi-tenant
`institutionId` scoping pattern that every later model reuses.

Purpose: Without a running server, a connected DB, and seeded tenant data,
nothing else in the project can be built or demoed. This is the foundation.
Output: Project scaffold, three Mongoose models, db config, idempotent seed,
health route, and an end-to-end seed test.

Covers: PLAT-01 (multi-tenant institutionId scoping on every tenant model),
PLAT-02 (seed creates institution + categories + bootstrap admin).
</objective>

<execution_context>
@$HOME/.claude/get-shit-done/workflows/execute-plan.md
@$HOME/.claude/get-shit-done/templates/summary.md
</execution_context>

<context>
@.planning/STATE.md
@.planning/ROADMAP.md
@.planning/REQUIREMENTS.md
@.planning/phases/01-foundation-multi-tenant-seed/01-CONTEXT.md
@.planning/phases/01-foundation-multi-tenant-seed/SKELETON.md
@CONTEXT.md
@CLAUDE.md

<interfaces>
<!-- Exact schema shapes are authoritative in CONTEXT.md (repo root). Phase 1 builds only these three collections. -->

institutions: { name: String, slug: String (unique), logoUrl: String, active: Boolean, createdAt: Date }
users: { institutionId: ObjectId ref institutions, name: String, email: String (unique), passwordHash: String, avatarUrl: String, role: String enum[admin|coordenador|professor|funcionario|aluno], isPrivate: Boolean default false, active: Boolean, createdAt: Date }
categories: { name: String, slug: String (unique), type: String enum[content|area] }

Seed content categories: Projetos, Aulas, Dia a dia, Meme
Seed area categories: Ciência da Computação, Direito, Marketing, Filosofia, Educação Física, NEPA
</interfaces>
</context>

<tasks>

<task type="auto" tdd="true">
  <name>Task 1: Scaffold project and write the failing end-to-end seed test</name>
  <files>package.json, .gitignore, .env.example, src/config/env.js, src/config/db.js, src/seed/seed.test.js</files>
  <read_first>
    - CONTEXT.md (repo root) — Stack section, Collections section, Seed inicial necessário, folder structure
    - .planning/phases/01-foundation-multi-tenant-seed/01-CONTEXT.md — Stack (locked), Seed (PLAT-02), Folder structure, Claude's Discretion
    - CLAUDE.md — Simplicity First, Goal-Driven Execution
  </read_first>
  <behavior>
    - Test: after running the seed against a test database, exactly 1 institution with slug catolica-pb exists
    - Test: after seeding, exactly 10 categories exist — 4 with type content, 6 with type area
    - Test: after seeding, exactly 1 user with role admin exists, linked to the catolica-pb institution by institutionId
    - Test: running the seed function twice in a row leaves counts unchanged (institution=1, categories=10, admin=1) — idempotent
    - Test: the bootstrap admin's passwordHash is NOT equal to the plaintext password from env (it is hashed)
  </behavior>
  <action>
    Initialize a Node project: create package.json with type module (ESM) or CommonJS — Claude's discretion, stay consistent across the codebase. Add dependencies express, mongoose, dotenv, bcrypt (or bcryptjs), jsonwebtoken; devDependencies for the test runner (node --test built-in, or jest/vitest — Claude's discretion, prefer node:test to minimize deps per Simplicity First). Add npm scripts: start (runs src/server.js), dev (nodemon or node --watch), seed (runs src/seed/seed.js), test (runs the test suite).
    Create .gitignore covering node_modules, .env, coverage. Create .env.example documenting MONGODB_URI, PORT, JWT_SECRET, JWT_EXPIRES_IN, BOOTSTRAP_ADMIN_EMAIL, BOOTSTRAP_ADMIN_PASSWORD, BOOTSTRAP_ADMIN_NAME, BCRYPT_ROUNDS (with safe placeholder values, never real secrets).
    Create src/config/env.js that loads dotenv and exports validated config; fail fast (throw on boot) if MONGODB_URI or JWT_SECRET is missing so the server never starts with an unsigned/misconfigured auth setup.
    Create src/config/db.js exporting connectDB(uri) (mongoose.connect wrapper, returns the connection) and a way to read connection readyState for the health check.
    Write src/seed/seed.test.js encoding the behaviors above. The test connects to a test Mongo URI (MONGODB_URI_TEST env, fallback to a mongodb-memory-server if you choose to add it, or skip-with-clear-message if no test DB) and imports the seed function from src/seed/seed.js. It MUST fail now because the models and seed do not yet exist (RED).
  </action>
  <verify>
    <automated>npm test 2>&1 | grep -Eiq "fail|cannot find|error" && echo "RED confirmed: seed test fails (models/seed not implemented)"</automated>
  </verify>
  <acceptance_criteria>
    - package.json exists with scripts start, dev, seed, test and the listed dependencies
    - .env.example lists MONGODB_URI, PORT, JWT_SECRET, JWT_EXPIRES_IN, BOOTSTRAP_ADMIN_EMAIL, BOOTSTRAP_ADMIN_PASSWORD, BCRYPT_ROUNDS and contains no real secret values
    - src/config/env.js throws when MONGODB_URI or JWT_SECRET is absent
    - src/config/db.js exports connectDB
    - src/seed/seed.test.js exists and asserts institution count 1, category counts (4 content + 6 area), admin count 1, idempotency, and hashed password
    - Running `npm test` fails (RED) because seed/models are not yet implemented
  </acceptance_criteria>
  <done>Project scaffolded; failing seed test exists and fails for the right reason (missing seed/models).</done>
</task>

<task type="auto" tdd="true">
  <name>Task 2: Implement models and idempotent seed to turn the test GREEN</name>
  <files>src/models/Institution.js, src/models/User.js, src/models/Category.js, src/seed/seed.js</files>
  <read_first>
    - CONTEXT.md (repo root) — Collections section (institutions, users, categories exact shapes), Categorias fixas, Seed inicial necessário
    - .planning/phases/01-foundation-multi-tenant-seed/01-CONTEXT.md — Schemas (Phase 1 subset), Seed (PLAT-02), Multi-tenancy (PLAT-01)
    - src/seed/seed.test.js — the behaviors this implementation must satisfy
    - src/config/db.js — connection helper to call before seeding
  </read_first>
  <action>
    Create src/models/Institution.js: Mongoose model institutions with fields name (required), slug (required, unique), logoUrl, active (default true), createdAt (default now). Add a unique index on slug.
    Create src/models/Category.js: model categories with name (required), slug (required, unique), type (required, enum content|area). Unique index on slug.
    Create src/models/User.js: model users with institutionId (ObjectId, ref Institution, required — this is the PLAT-01 tenant scope field), name (required), email (required, unique), passwordHash (required), avatarUrl, role (required, enum admin|coordenador|professor|funcionario|aluno), isPrivate (default false), active (default true), createdAt (default now). Unique index on email.
    Create src/seed/seed.js exporting an async runSeed() function that: connects via connectDB only when run as a script (import.meta / require.main guard) but accepts an already-open connection when called from the test; upserts the institution by slug catolica-pb (name "Faculdade Católica da Paraíba", active true) using findOneAndUpdate with upsert; upserts each of the 10 fixed categories by slug (4 content: Projetos, Aulas, Dia a dia, Meme; 6 area: Ciência da Computação, Direito, Marketing, Filosofia, Educação Física, NEPA) — generate slugs deterministically (lowercase, accent-stripped, hyphenated); upserts the bootstrap admin by email (from BOOTSTRAP_ADMIN_EMAIL) with role admin, institutionId set to the catolica-pb institution _id, passwordHash = bcrypt hash of BOOTSTRAP_ADMIN_PASSWORD using BCRYPT_ROUNDS — only hash/set the password on insert, do not re-hash on every run (use $setOnInsert for the hash so re-runs stay idempotent and stable). All operations idempotent via upsert keyed on the natural unique field. When run as a script, log a summary and exit; do not leave the process hanging.
  </action>
  <verify>
    <automated>npm test 2>&1 | tail -20</automated>
  </verify>
  <acceptance_criteria>
    - src/models/User.js exports a model whose schema path institutionId has ref Institution and is required
    - src/models/Category.js type enum is exactly [content, area]; src/models/User.js role enum is exactly [admin, coordenador, professor, funcionario, aluno]
    - src/seed/seed.js exports runSeed
    - `npm test` passes (GREEN): institution count 1, categories 4 content + 6 area, admin count 1, idempotent on second run, admin passwordHash != plaintext
    - Running runSeed twice keeps catolica-pb institution count at 1
  </acceptance_criteria>
  <done>Three models implemented; idempotent seed implemented; the end-to-end seed test passes.</done>
</task>

<task type="auto">
  <name>Task 3: Boot Express with health check wired to the live Mongo connection</name>
  <files>src/app.js, src/server.js, src/routes/health.routes.js</files>
  <read_first>
    - .planning/phases/01-foundation-multi-tenant-seed/01-CONTEXT.md — Walking Skeleton target, Folder structure
    - CONTEXT.md (repo root) — Estrutura de pastas (src/app.js, src/routes/, src/config/db.js)
    - src/config/db.js — connectDB and connection readyState accessor
    - src/config/env.js — PORT, MONGODB_URI
  </read_first>
  <action>
    Create src/routes/health.routes.js exporting an Express router with GET /health that returns 200 and JSON { status: "ok", db: "connected" } when the Mongoose connection readyState is 1 (connected), and 503 with { status: "degraded", db: "disconnected" } otherwise. Read connection state via the db config helper, not by importing mongoose directly in the route.
    Create src/app.js that builds and exports the Express app: JSON body parsing, mounts the health router at root so GET /health resolves, leaves a clearly commented mount point where Plan 02 will attach the protected/auth routers. Do NOT call listen here (keep app testable).
    Create src/server.js that imports env config, calls connectDB(MONGODB_URI), then app.listen(PORT) only after the connection resolves; on connection failure, log and exit non-zero so the process never serves traffic without a DB.
  </action>
  <verify>
    <automated>node -e "import('./src/app.js').then(m=>{const app=m.default||m.app; if(!app) throw new Error('no app export'); console.log('app boots, health route mountable');}).catch(e=>{console.error(e);process.exit(1);})"</automated>
  </verify>
  <acceptance_criteria>
    - src/routes/health.routes.js exports a router with a GET /health handler
    - GET /health returns 200 { status: "ok" } when Mongo readyState is 1, and 503 when not connected
    - src/app.js exports the Express app and mounts the health router; it does not call listen
    - src/server.js connects to Mongo before listening and exits non-zero if the connection fails
    - With a running Mongo + server, `curl -s localhost:$PORT/health` returns status ok
  </acceptance_criteria>
  <done>Server boots, connects to Mongo, and GET /health reports connection state. Walking Skeleton stack (boot → DB → real write via seed → health read) is end-to-end runnable.</done>
</task>

</tasks>

<threat_model>
## Trust Boundaries

| Boundary | Description |
|----------|-------------|
| seed script → DB | Bootstrap admin credentials sourced from env cross into persisted user records |
| env/config → app | Secrets (JWT_SECRET, DB URI, admin password) enter the process |
| HTTP client → /health | Unauthenticated public probe |

## STRIDE Threat Register

| Threat ID | Category | Component | Disposition | Mitigation Plan |
|-----------|----------|-----------|-------------|-----------------|
| T-01-01 | Information Disclosure | Bootstrap admin password | mitigate | Store only bcrypt passwordHash (BCRYPT_ROUNDS); never persist or log plaintext; credentials come from env, not code; .env in .gitignore |
| T-01-02 | Tampering | Seed idempotency | mitigate | Upsert keyed on natural unique fields; $setOnInsert for password hash so re-runs cannot silently overwrite/duplicate the admin |
| T-01-03 | Spoofing | Multi-tenant data (PLAT-01) | mitigate | Every tenant-owned model carries required institutionId ref; pattern established here for all later queries to scope by institution |
| T-01-04 | Information Disclosure | GET /health | accept | Reports only boolean connection state, no PII or secrets; low-value public probe |
| T-01-05 | Denial of Service | env misconfiguration | mitigate | env.js fails fast (throws) if MONGODB_URI or JWT_SECRET missing; server exits non-zero on DB connect failure rather than serving unconfigured |
| T-01-SC | Tampering | npm installs (express, mongoose, bcrypt, jsonwebtoken, dotenv) | accept | All are widely-used, well-known mainstream packages; no [ASSUMED]/[SUS] packages introduced. If RESEARCH adds an obscure dep, gate it before install. |
</threat_model>

<verification>
- `npm run seed` creates 1 catolica-pb institution, 10 categories (4 content + 6 area), 1 admin; second run leaves counts unchanged
- `npm test` passes the seed end-to-end test
- Server boots against a running Mongo and `curl localhost:$PORT/health` returns { status: "ok" }
- src/models/User.js institutionId is required and refs Institution (PLAT-01 pattern)
</verification>

<success_criteria>
- Running the seed creates Católica PB + all fixed content/area categories + bootstrap admin (idempotent) — PLAT-02
- User model carries institutionId; scoping pattern established for later models — PLAT-01
- Server connects to MongoDB and responds to a health check — Phase success criterion 4
</success_criteria>

<output>
Create `.planning/phases/01-foundation-multi-tenant-seed/01-01-SUMMARY.md` when done.
</output>
