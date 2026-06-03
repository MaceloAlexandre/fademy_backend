# Phase 3: Profiles & Social Graph — Research

**Researched:** 2026-06-03
**Domain:** Mongoose social graph (Follow/Block models), Express REST API, React CSS Modules mobile shell
**Confidence:** HIGH

---

<user_constraints>
## User Constraints (from CONTEXT.md)

### Locked Decisions

**Navegacao pos-login (shell autenticada)**
- D-01: Shell autenticada usa bottom nav bar com 4 abas fixas: Feed / Busca / Notif / Perfil. Phase 3 entrega Busca e Perfil ativos; Feed e Notif ficam como placeholders.
- D-02: Header scroll-aware: esconde ao rolar para baixo, reaparece ao rolar para cima. Requer logica de scroll direction detection no React.
- D-03: Logout fica no header, nao na aba de Perfil. Header exibe wordmark "Fademy" + botao "Sair".
- D-04: O componente PostLoginShell atual em App.jsx e substituido pela nova AppShell com bottom nav + header scroll-aware.

**Visibilidade do perfil privado**
- D-05: Perfil privado visitado por nao-seguidor: exibe apenas nome, avatarUrl e botao "Solicitar follow". Posts, bio e contadores ficam ocultos.
- D-06: Quando ha solicitacao pendente (follow com status: pending): botao exibe "Solicitacao enviada" com opcao de cancelar (DELETE no follow pending).
- D-07: API backend (PATCH /api/follows/:id/accept e PATCH /api/follows/:id/reject) e implementada em Phase 3, mas a interface frontend de aceitar/rejeitar fica para Phase 5.

**Busca de usuarios (PROF-03)**
- D-08: Busca scoped para mesma institutionId do usuario logado.
- D-09: Query por nome parcial, case-insensitive via MongoDB regex. Resultados excluem usuarios que bloquearam o pesquisador ou foram bloqueados por ele.
- D-10: Resultados paginados — API aceita ?q=&page=1&limit=20. Frontend usa paginacao simples.
- D-11: Aba "Busca" no bottom nav com barra de pesquisa. Debounce de ~300ms.

**Controles de follow no perfil (FRNT-05)**
- D-12: Botao de follow/block no profile view reflete o estado atual:
  - null + perfil publico → "Seguir"
  - null + perfil privado → "Solicitar"
  - pending → "Solicitacao enviada" + opcao cancelar
  - accepted → "Seguindo" + opcao unfollow
  - Bloqueado (blocker ou blocked) → nenhum botao
- D-13: Block flow: bloquear remove os follows bilaterais automaticamente (SOCL-04), tratado na controller.

### Claude's Discretion
- Estrutura interna dos componentes React do profile view e da busca
- Naming dos endpoints (sugerido: /api/users/:id, /api/users/:id/follow, /api/users/:id/block, /api/users/search?q=)
- Estrutura de resposta da API para o estado do follow (retornar junto com GET /users/:id ou endpoint separado /api/users/:id/follow-status)
- Tratamento de erro e loading states nos componentes

### Deferred Ideas (OUT OF SCOPE)
- Interface de aceitar/rejeitar requests no perfil (fica para Phase 5 via notificacoes)
- Block middleware centralizado (block.middleware.js) para verificar bloqueios antes de posts/chat
- Notificacoes de follow (new_follower, follow_request, follow_accepted) — Claude decide: criar os registros no banco via service stub para que Phase 5 ja tenha dados
</user_constraints>

<phase_requirements>
## Phase Requirements

| ID | Description | Research Support |
|----|-------------|------------------|
| PROF-01 | User can set their profile public or private (isPrivate) | User.isPrivate already on model; PATCH /api/users/me endpoint needed |
| PROF-02 | User can view another user's profile | GET /api/users/:id with visibility rules based on follow status |
| PROF-03 | User can search for users (excluding blocked relationships) | GET /api/users/search?q=&page=&limit= with $nin exclusion of blocked IDs |
| SOCL-01 | User can follow a public profile directly (status accepted) | POST /api/users/:id/follow — creates Follow with status:accepted |
| SOCL-02 | User can request to follow a private profile (status pending) | POST /api/users/:id/follow — creates Follow with status:pending |
| SOCL-03 | Owner of a private profile can accept or reject follow requests | PATCH /api/follows/:id/accept and /api/follows/:id/reject |
| SOCL-04 | User can block another user, which removes follows on both sides | POST /api/users/:id/block — removes both Follow docs atomically |
| SOCL-05 | Blocked users cannot message, see posts, or appear in search | Enforced via blocked-ID exclusion in search; controller checks for block before profile read |
| FRNT-05 | Profile view with follow / request / block controls | AppShell + ProfilePage + FollowButton + BlockButton in React |
</phase_requirements>

---

## Summary

Phase 3 introduces two new Mongoose models (Follow and Block) on top of the existing multi-tenant foundation, eight new API endpoints across user and follow/block routes, and a React shell refactor that replaces the placeholder PostLoginShell with a full mobile navigation experience (bottom nav + scroll-aware header).

The backend work is self-contained and low-risk: the Follow and Block schemas match the canonical spec in CONTEXT.md exactly, the multi-tenant pattern (institutionId on every query using req.user.institutionId from JWT) is already established, and the test framework (node:test + mongodb-memory-server) is already installed and proven in Phase 2. The follow state machine has five well-defined transitions; the only non-trivial logic is blocking (bilateral follow removal) and the private-profile visibility check (single query approach recommended — embed follow status in GET /api/users/:id response to avoid a round-trip).

The frontend work replaces `PostLoginShell` (31 lines in App.jsx) with a new `AppShell` component that adds a fixed bottom nav, promotes the existing header to a separate tracked element with scroll-direction logic, and adds three new pages (SearchPage, ProfilePage, placeholder pages for Feed and Notifications). The UI spec is fully locked in 03-UI-SPEC.md — no design decisions remain open.

**Primary recommendation:** Slice into 5 plans: (1) Follow + Block models + all backend endpoints with tests, (2) AppShell navigation shell, (3) ProfilePage + FollowButton + BlockButton, (4) SearchPage + UserResultCard, (5) Notification stubs (service + Notification model). Each plan is independently deployable and end-to-end testable.

---

## Architectural Responsibility Map

| Capability | Primary Tier | Secondary Tier | Rationale |
|------------|-------------|----------------|-----------|
| Follow/Block persistence | Database (MongoDB/Mongoose) | API/Backend | Models own schema and indexes |
| Privacy visibility rules | API/Backend | — | Server enforces who sees what — never trust client |
| Follow state machine | API/Backend | Frontend (optimistic UI) | Authoritative state lives in DB; frontend reflects it |
| Search with block exclusion | API/Backend | Database (index) | Query logic in controller; MongoDB $nin on blocked IDs |
| Profile privacy toggle (isPrivate) | API/Backend | Frontend | PATCH endpoint; User model already has the field |
| AppShell navigation | Frontend (Browser) | — | Pure client-side routing with React Router |
| Scroll-aware header | Frontend (Browser) | — | window scroll listener + CSS transform |
| Follow button state display | Frontend (Browser) | API/Backend (data source) | API provides follow status; frontend renders correct state |
| Notification record creation | API/Backend (service) | Database | notification.service.js stub writes to DB |

---

## Standard Stack

Phase 3 introduces no new npm packages. All dependencies are already installed.

### Core (already installed)
| Library | Version (verified) | Purpose | Why Standard |
|---------|-------------------|---------|--------------|
| mongoose | 9.6.3 [VERIFIED: npm registry] | ODM for Follow and Block models | Locked in Phase 1 |
| express | 5.2.1 [VERIFIED: npm registry] | Route handlers for follow/block/user endpoints | Locked in Phase 1 |
| jsonwebtoken | 9.0.2 [VERIFIED: npm registry] | JWT verify in auth.middleware (reused unchanged) | Locked in Phase 1 |
| lucide-react | 1.17.0 [VERIFIED: npm registry] | Home/Search/Bell/User icons in bottom nav | Already in frontend package.json |
| react-router-dom | 6.24.0 [VERIFIED: npm registry] | Nested routes inside AppShell | Locked in Phase 2 |
| axios | 1.7.2 [VERIFIED: npm registry] | API calls from frontend (interceptor already in lib/api.js) | Locked in Phase 2 |
| mongodb-memory-server | 11.2.0 [VERIFIED: npm registry] | In-process test DB for follow/block controller tests | Already devDependency |

### No new packages needed
Phase 3 requires zero new npm installs. Every dependency is already present in both `package.json` files. The Notification model (for the stub) uses mongoose, already installed.

---

## Package Legitimacy Audit

All packages below are pre-existing installations verified in Phases 1 and 2. slopcheck was run and returned [OK] for all.

| Package | Registry | slopcheck | Disposition |
|---------|----------|-----------|-------------|
| mongoose | npm | [OK] | Approved — pre-existing |
| express | npm | [OK] | Approved — pre-existing |
| jsonwebtoken | npm | [OK] | Approved — pre-existing |
| lucide-react | npm | [OK] | Approved — pre-existing |
| react-router-dom | npm | [OK] | Approved — pre-existing |
| axios | npm | [OK] | Approved — pre-existing |
| mongodb-memory-server | npm | [OK] | Approved — pre-existing |

**Packages removed due to slopcheck [SLOP] verdict:** none
**Packages flagged as suspicious [SUS]:** none

No postinstall scripts found on any package above. [VERIFIED: npm registry — checked via `npm view <pkg> scripts.postinstall`]

---

## Architecture Patterns

### System Architecture Diagram

```
Browser (React)
  |
  |-- AppShell (fixed header + bottom nav)
  |     |-- ProfilePage  -->  GET /api/users/:id  -->  User + followStatus + Block check
  |     |                     PATCH /api/users/me  -->  User.isPrivate update
  |     |
  |     |-- FollowButton  -->  POST   /api/users/:id/follow
  |     |                      DELETE /api/users/:id/follow  (unfollow / cancel)
  |     |
  |     |-- BlockButton  -->  POST   /api/users/:id/block
  |     |
  |     |-- SearchPage  -->  GET /api/users/search?q=&page=&limit=
  |
  |-- Auth pages (unchanged: AcceptInvite, Register, Login)

Express API
  |
  |-- GET  /api/users/search   user.controller.searchUsers
  |-- GET  /api/users/:id      user.controller.getProfile
  |-- PATCH /api/users/me      user.controller.updateMe
  |
  |-- POST   /api/users/:id/follow   follow.controller.follow
  |-- DELETE /api/users/:id/follow   follow.controller.unfollow
  |-- PATCH  /api/follows/:id/accept  follow.controller.acceptFollow
  |-- PATCH  /api/follows/:id/reject  follow.controller.rejectFollow
  |
  |-- POST /api/users/:id/block  block.controller.block
  |
  |-- (stub) notification.service.create()  -->  Notification collection

MongoDB
  |-- users (existing, isPrivate already present)
  |-- follows (new: followerId, followingId, status, institutionId, createdAt)
  |-- blocks  (new: blockerId, blockedId, institutionId, createdAt)
  |-- notifications (new model, written by stub, read in Phase 5)
```

### Recommended Project Structure (additions only)

```
src/
├── models/
│   ├── Follow.js          # new
│   ├── Block.js           # new
│   └── Notification.js    # new (stub — Phase 5 reads it)
├── controllers/
│   ├── user.controller.js           # new: getProfile, updateMe, searchUsers
│   ├── user.controller.test.js      # new
│   ├── follow.controller.js         # new: follow, unfollow, acceptFollow, rejectFollow
│   ├── follow.controller.test.js    # new
│   ├── block.controller.js          # new: block
│   └── block.controller.test.js     # new
├── routes/
│   ├── user.routes.js   # new: /api/users
│   └── follow.routes.js # new: /api/follows (for accept/reject by follow _id)
└── services/
    └── notification.service.js  # new stub: create(type, actorId, userId, refId, refModel)

MINIREDESOCIAL_FRONT/src/
├── components/
│   ├── AppShell.jsx         # new — replaces PostLoginShell
│   ├── AppShell.module.css  # new
│   ├── FollowButton.jsx     # new
│   ├── FollowButton.module.css
│   ├── BlockButton.jsx      # new
│   └── BlockButton.module.css
└── pages/
    ├── ProfilePage.jsx      # new
    ├── ProfilePage.module.css
    ├── SearchPage.jsx       # new
    └── SearchPage.module.css
```

### Pattern 1: Follow Model with Compound Indexes

The canonical schema from `CONTEXT.md` has no institutionId on follows/blocks — it only appears on users. Research finding: for SOCL-05 exclusion in search and bilateral removal on block, you need to find follows by followerId OR followingId, and blocks by blockerId OR blockedId. The key indexes needed are:

```javascript
// Source: CONTEXT.md canonical schema + standard Mongoose index practice [ASSUMED for specific index choices]
'use strict';
const mongoose = require('mongoose');

const followSchema = new mongoose.Schema({
  followerId:  { type: mongoose.Schema.Types.ObjectId, ref: 'User', required: true },
  followingId: { type: mongoose.Schema.Types.ObjectId, ref: 'User', required: true },
  status:      { type: String, enum: ['pending', 'accepted'], required: true },
  createdAt:   { type: Date, default: Date.now },
});

// Prevent duplicate follow records
followSchema.index({ followerId: 1, followingId: 1 }, { unique: true });
// Fast lookup: "who does this user follow?" (feed, unfollow)
followSchema.index({ followerId: 1, status: 1 });
// Fast lookup: "who follows this user?" (followers list, accept/reject)
followSchema.index({ followingId: 1, status: 1 });

module.exports = mongoose.model('Follow', followSchema);
```

```javascript
'use strict';
const mongoose = require('mongoose');

const blockSchema = new mongoose.Schema({
  blockerId: { type: mongoose.Schema.Types.ObjectId, ref: 'User', required: true },
  blockedId: { type: mongoose.Schema.Types.ObjectId, ref: 'User', required: true },
  createdAt: { type: Date, default: Date.now },
});

// Prevent duplicate block records
blockSchema.index({ blockerId: 1, blockedId: 1 }, { unique: true });
// Fast lookup in search exclusion: "is userId in either column?"
blockSchema.index({ blockedId: 1 });

module.exports = mongoose.model('Block', blockSchema);
```

**Note on institutionId on Follow/Block:** The canonical CONTEXT.md schema does NOT include institutionId on follows or blocks. Users already carry institutionId; since follow/block only happens between users within the same institution (search is scoped to institutionId), adding institutionId to Follow/Block would be redundant. Do NOT add it. [VERIFIED: CONTEXT.md canonical schema]

### Pattern 2: Follow State Machine — Endpoint Mapping

```
State Transition Table:
  (viewer has no follow record)        → POST   /api/users/:id/follow
    → if target.isPrivate=false        → creates Follow{status:'accepted'}
    → if target.isPrivate=true         → creates Follow{status:'pending'}

  (follow exists, status='pending')    → DELETE /api/users/:id/follow   (cancel request)
  (follow exists, status='accepted')   → DELETE /api/users/:id/follow   (unfollow)

  (target is owner, follow is pending) → PATCH  /api/follows/:id/accept
                                       → PATCH  /api/follows/:id/reject

  (any state)                          → POST   /api/users/:id/block
    → deleteMany Follow where (followerId=A,followingId=B) or (followerId=B,followingId=A)
    → create Block{blockerId:A, blockedId:B}
```

The unfollow and cancel-request share one endpoint (DELETE /api/users/:id/follow) — they differ only in which Follow document exists. The controller finds the Follow by `{followerId: req.user.id, followingId: params.id}` and deletes it regardless of status.

### Pattern 3: Private Profile Visibility — Single-Query Approach

Recommended: embed follow status in the GET /api/users/:id response to avoid a second round-trip from the browser.

```javascript
// Source: project pattern (institutionId from JWT, never from body) [ASSUMED for specific implementation]
async function getProfile(req, res) {
  const targetId = req.params.id;
  const viewerId = req.user.id;

  // 1. Check if viewer is blocked by target or has blocked target
  const blockExists = await Block.findOne({
    $or: [
      { blockerId: targetId, blockedId: viewerId },
      { blockerId: viewerId, blockedId: targetId },
    ]
  }).lean();

  if (blockExists) return res.status(404).json({ error: 'User not found' });

  // 2. Load user
  const user = await User.findById(targetId).select('-passwordHash').lean();
  if (!user) return res.status(404).json({ error: 'User not found' });

  // 3. Check follow relationship
  const follow = await Follow.findOne({
    followerId: viewerId,
    followingId: targetId,
  }).lean();

  const followStatus = follow ? follow.status : null;  // null | 'pending' | 'accepted'
  const isOwner = viewerId === targetId;
  const canSeePrivate = isOwner || followStatus === 'accepted';

  // 4. Shape response based on visibility
  if (user.isPrivate && !canSeePrivate) {
    return res.status(200).json({
      id: user._id,
      name: user.name,
      avatarUrl: user.avatarUrl,
      role: user.role,
      isPrivate: true,
      followStatus,  // null or 'pending' — drives FollowButton state on frontend
    });
  }

  return res.status(200).json({
    id: user._id,
    name: user.name,
    avatarUrl: user.avatarUrl,
    role: user.role,
    isPrivate: user.isPrivate,
    followStatus,
    followersCount: await Follow.countDocuments({ followingId: targetId, status: 'accepted' }),
    followingCount: await Follow.countDocuments({ followerId: targetId, status: 'accepted' }),
  });
}
```

The follow status is returned inline (not as a separate endpoint). The frontend receives everything it needs in one request. This satisfies Claude's Discretion from CONTEXT.md.

### Pattern 4: Block Endpoint — Bilateral Follow Removal

```javascript
// Source: CONTEXT.md rule "ao bloquear: remove o follow dos dois lados automaticamente" [VERIFIED: CONTEXT.md]
async function block(req, res) {
  const blockerId = req.user.id;
  const blockedId = req.params.id;

  if (blockerId === blockedId) {
    return res.status(400).json({ error: 'Cannot block yourself' });
  }

  // Remove follows in both directions atomically
  await Follow.deleteMany({
    $or: [
      { followerId: blockerId, followingId: blockedId },
      { followerId: blockedId, followingId: blockerId },
    ]
  });

  // Upsert block (idempotent — blocking twice is not an error)
  await Block.findOneAndUpdate(
    { blockerId, blockedId },
    { blockerId, blockedId },
    { upsert: true, new: true }
  );

  // Notification stub: no follow_request/new_follower notifications needed for block
  return res.status(200).json({ blocked: true });
}
```

### Pattern 5: Search with Block Exclusion

The most efficient approach for MVP scale: two separate queries (fetch blocked IDs, then exclude in the User search). A MongoDB $lookup aggregation would also work but is harder to read and test at this scale. [ASSUMED: approach choice — either works correctly]

```javascript
// Source: standard MongoDB $nin pattern [ASSUMED]
async function searchUsers(req, res) {
  const { q = '', page = 1, limit = 20 } = req.query;
  const viewerId = req.user.id;
  const institutionId = req.user.institutionId;

  // 1. Fetch block relationships in both directions
  const blocks = await Block.find({
    $or: [
      { blockerId: viewerId },
      { blockedId: viewerId },
    ]
  }).lean();

  const excludedIds = blocks.map(b =>
    b.blockerId.toString() === viewerId ? b.blockedId : b.blockerId
  );
  excludedIds.push(viewerId); // exclude self from results

  // 2. Search within institution, excluding blocked users
  const regex = new RegExp(q, 'i');
  const skip = (Number(page) - 1) * Number(limit);

  const [users, total] = await Promise.all([
    User.find({
      institutionId,
      name: regex,
      _id: { $nin: excludedIds },
    })
      .select('name avatarUrl role isPrivate')
      .skip(skip)
      .limit(Number(limit))
      .lean(),
    User.countDocuments({
      institutionId,
      name: regex,
      _id: { $nin: excludedIds },
    }),
  ]);

  return res.status(200).json({ users, total, page: Number(page), limit: Number(limit) });
}
```

### Pattern 6: AppShell Scroll-Aware Header (React)

The scroll direction detection pattern from 03-UI-SPEC.md and 03-CONTEXT.md Specifics:

```jsx
// Source: 03-CONTEXT.md Specifics + 03-UI-SPEC.md Header behavior [VERIFIED: 03-CONTEXT.md]
import { useRef, useEffect, useState } from 'react'

function useScrollDirection() {
  const lastScrollY = useRef(window.scrollY)
  const [hidden, setHidden] = useState(false)

  useEffect(() => {
    function onScroll() {
      const current = window.scrollY
      setHidden(current > lastScrollY.current && current > 10)
      lastScrollY.current = current
    }
    window.addEventListener('scroll', onScroll, { passive: true })
    return () => window.removeEventListener('scroll', onScroll)
  }, [])

  return hidden
}
```

Applied in AppShell: `className={headerHidden ? styles.headerHidden : styles.header}`. The CSS: `transform: translateY(-56px); transition: transform 0.2s ease`.

### Pattern 7: Notification Stub Service

The deferred decision from CONTEXT.md was resolved: create notification records in Phase 3 so Phase 5 has data. Minimum viable stub — no delivery logic, no polling, just a DB write.

```javascript
// src/services/notification.service.js
'use strict';
const Notification = require('../models/Notification');

/**
 * Creates a notification record. Never throws — notification failures
 * must not break the action that triggered them.
 */
async function create(type, actorId, userId, refId, refModel) {
  try {
    await Notification.create({ type, actorId, userId, refId, refModel });
  } catch (_err) {
    // Silently ignore — notification is non-critical to the triggering action
  }
}

module.exports = { create };
```

Call sites:
- `follow` controller (public profile, status accepted): `notificationService.create('new_follower', followerId, followingId, follow._id, 'Follow')`
- `follow` controller (private profile, status pending): `notificationService.create('follow_request', followerId, followingId, follow._id, 'Follow')`
- `acceptFollow` controller: `notificationService.create('follow_accepted', followingId, followerId, follow._id, 'Follow')`

### Pattern 8: Notification Model (stub)

```javascript
// Source: CONTEXT.md canonical notifications schema [VERIFIED: CONTEXT.md]
'use strict';
const mongoose = require('mongoose');

const notificationSchema = new mongoose.Schema({
  userId:   { type: mongoose.Schema.Types.ObjectId, ref: 'User', required: true }, // recipient
  actorId:  { type: mongoose.Schema.Types.ObjectId, ref: 'User', required: true }, // who triggered
  type:     { type: String, enum: ['new_follower','follow_request','follow_accepted','post_comment','invite_received','message_received'], required: true },
  refId:    { type: mongoose.Schema.Types.ObjectId },
  refModel: { type: String },
  read:     { type: Boolean, default: false },
  createdAt:{ type: Date, default: Date.now },
});

notificationSchema.index({ userId: 1, read: 1, createdAt: -1 }); // Phase 5 query pattern

module.exports = mongoose.model('Notification', notificationSchema);
```

### Anti-Patterns to Avoid

- **Trusting client-supplied institutionId:** Always source from `req.user.institutionId` (JWT). Established in Phase 1 (T-02-03). Never from req.body.
- **Follow endpoint on /api/follows instead of /api/users/:id/follow:** The resource-oriented naming (action on a user) is cleaner for the frontend and aligns with Claude's Discretion suggestion in CONTEXT.md. Reserve /api/follows/:id for the accept/reject endpoints where you address the Follow document by its own _id.
- **Separate follow-status endpoint:** Avoids extra round-trip — embed followStatus in GET /api/users/:id response (covered in Pattern 3).
- **Using CSS hide/show for private profile content:** The UI spec says "not rendered (not hidden via CSS — not returned by API or conditional render to null)". The API must return reduced data for private profiles.
- **Modal dialogs for destructive confirmations:** The UI spec explicitly prohibits modals — use inline expansion below the triggering button for cancel, unfollow, and block confirmations.

---

## Don't Hand-Roll

| Problem | Don't Build | Use Instead | Why |
|---------|-------------|-------------|-----|
| Case-insensitive partial name search | Custom regex escaping | `new RegExp(query, 'i')` directly in Mongoose | Simple, well-tested, already the project standard (D-09) |
| Duplicate follow prevention | Custom check-before-insert | Compound unique index `{followerId,followingId}` + catch error code 11000 | DB enforces; race-condition safe |
| Bilateral follow removal on block | Two sequential deletes | `Follow.deleteMany({ $or: [...] })` | Single atomic query |
| Icon SVGs | Custom inline SVGs | `lucide-react` (already installed, [OK]) | Already in package.json; Home/Search/Bell/User icons available |
| Test DB for controller tests | External MongoDB | `mongodb-memory-server` (already devDependency) | Established Phase 1/2 pattern |
| Scroll direction tracking state | Throttled timeInterval | `window.scroll` event + `useRef` for lastScrollY | Passive listener, no re-renders except state change |

---

## Common Pitfalls

### Pitfall 1: /api/users/:id route catches /api/users/search
**What goes wrong:** Express evaluates `GET /api/users/search` as matching `GET /api/users/:id` with id="search".
**Why it happens:** Express route matching is first-match; if `GET /api/users/:id` is registered before `GET /api/users/search`, the search route never fires.
**How to avoid:** Register the search route BEFORE the parameterized route in user.routes.js:
```javascript
router.get('/search', authenticate, searchUsers);   // must come first
router.get('/me', authenticate, getMe);             // also before /:id
router.get('/:id', authenticate, getProfile);
router.patch('/me', authenticate, updateMe);
```
**Warning signs:** Search endpoint returns a user object with id="search" instead of results.

### Pitfall 2: Block endpoint bypasses blocked user's ability to see it
**What goes wrong:** A user blocked by another can still call GET /api/users/:id on the blocker and see their profile — if only `blockerId=viewerId` is checked.
**Why it happens:** Forgetting to check the reverse direction (target has blocked viewer).
**How to avoid:** The block check in getProfile uses `$or` covering both directions (Pattern 3 above).
**Warning signs:** User A blocks User B; User B can still see User A's profile.

### Pitfall 3: Notification service exceptions breaking follow/block actions
**What goes wrong:** Notification.create() throws (e.g., validation error); the follow action returns 500 instead of 200.
**Why it happens:** Awaiting notificationService.create() without try/catch at the call site.
**How to avoid:** The service itself wraps in try/catch and swallows errors (Pattern 7 above). Call sites do not need additional try/catch for the notification call.
**Warning signs:** Following fails with 500 after Notification model is introduced.

### Pitfall 4: Accept/reject follow endpoint accessible by anyone
**What goes wrong:** Any authenticated user can call PATCH /api/follows/:id/accept to accept someone else's follow request.
**Why it happens:** Missing ownership check — only the target of the follow (followingId) can accept it.
**How to avoid:** In acceptFollow controller: `if (follow.followingId.toString() !== req.user.id) return res.status(403).json(...)`.

### Pitfall 5: useEffect scroll listener on mount before window exists (SSR)
**What goes wrong:** Not applicable here — this is a Vite/React SPA, no SSR. Window is always available.
**How to avoid:** No action needed. Noted to dismiss SSR caution patterns that don't apply.

### Pitfall 6: Search returns all users when q is empty
**What goes wrong:** `new RegExp('', 'i')` matches every string — returns all institution users unpaginated.
**Why it happens:** Empty string regex matches everything.
**How to avoid:** Return empty results (not all users) when `q` is blank or shorter than 1 character:
```javascript
if (!q || q.trim().length === 0) {
  return res.status(200).json({ users: [], total: 0, page: 1, limit: Number(limit) });
}
```

### Pitfall 7: Profile /api/users/me route vs /api/users/:id for own profile
**What goes wrong:** Frontend calls GET /api/users/me (convenience route) but backend only has GET /api/users/:id, requiring the frontend to know its own user ID at all times.
**How to avoid:** Add `GET /api/users/me` as an alias: `router.get('/me', authenticate, (req, res) => getProfile({ ...req, params: { id: req.user.id } }, res))`. The ProfilePage at /profile/me uses this endpoint.

---

## Code Examples

### Follow controller — follow action
```javascript
// Source: project pattern + CONTEXT.md state machine [ASSUMED for specific implementation]
'use strict';
const Follow = require('../models/Follow');
const User = require('../models/User');
const Block = require('../models/Block');
const notificationService = require('../services/notification.service');

async function follow(req, res) {
  const followerId = req.user.id;
  const followingId = req.params.id;

  if (followerId === followingId) {
    return res.status(400).json({ error: 'Cannot follow yourself' });
  }

  // Check block in either direction
  const blockExists = await Block.findOne({
    $or: [
      { blockerId: followerId, blockedId: followingId },
      { blockerId: followingId, blockedId: followerId },
    ]
  }).lean();
  if (blockExists) return res.status(403).json({ error: 'Cannot follow this user' });

  const target = await User.findById(followingId).lean();
  if (!target) return res.status(404).json({ error: 'User not found' });

  const status = target.isPrivate ? 'pending' : 'accepted';

  try {
    const follow = await Follow.create({ followerId, followingId, status });

    const notifType = status === 'accepted' ? 'new_follower' : 'follow_request';
    await notificationService.create(notifType, followerId, followingId, follow._id, 'Follow');

    return res.status(201).json({ status: follow.status, followId: follow._id });
  } catch (err) {
    if (err.code === 11000) {
      return res.status(409).json({ error: 'Already following or request pending' });
    }
    return res.status(500).json({ error: 'Internal server error' });
  }
}
```

### Test boilerplate (follow controller)
```javascript
// Source: invite.controller.test.js pattern (Phase 2) [VERIFIED: existing codebase]
'use strict';
const { describe, it, before, after } = require('node:test');
const assert = require('node:assert/strict');
const mongoose = require('mongoose');

const { follow, unfollow } = require('./follow.controller');

let mongoServer;

function makeRes() {
  const res = { statusCode: null, body: null,
    status(code) { this.statusCode = code; return this; },
    json(data) { this.body = data; return this; },
  };
  return res;
}

before(async () => {
  const { MongoMemoryServer } = require('mongodb-memory-server');
  mongoServer = await MongoMemoryServer.create();
  const uri = mongoServer.getUri();
  process.env.MONGODB_URI = uri;
  process.env.JWT_SECRET = 'test-secret';
  process.env.BCRYPT_ROUNDS = '4';
  const { connectDB } = require('../config/db');
  await connectDB(uri);
});

after(async () => {
  await mongoose.disconnect();
  await mongoServer.stop();
});
```

### AppShell bottom nav (React)
```jsx
// Source: 03-UI-SPEC.md tab definitions + CONTEXT.md D-01 [VERIFIED: 03-UI-SPEC.md]
import { Home, Search, Bell, User } from 'lucide-react'
import { NavLink } from 'react-router-dom'
import styles from './AppShell.module.css'

const TABS = [
  { to: '/', icon: Home, label: 'Feed' },
  { to: '/search', icon: Search, label: 'Busca' },
  { to: '/notifications', icon: Bell, label: 'Notif' },
  { to: '/profile/me', icon: User, label: 'Perfil' },
]

// In AppShell render:
<nav className={styles.bottomNav} aria-label="Navegacao principal">
  {TABS.map(({ to, icon: Icon, label }) => (
    <NavLink
      key={to}
      to={to}
      end={to === '/'}
      className={({ isActive }) => isActive ? styles.tabActive : styles.tab}
      aria-current={({ isActive }) => isActive ? 'page' : undefined}
    >
      <Icon size={24} aria-hidden="true" />
      <span>{label}</span>
    </NavLink>
  ))}
</nav>
```

---

## State of the Art

| Old Approach | Current Approach | Notes |
|--------------|------------------|-------|
| PostLoginShell (header-only, no nav) | AppShell (header + bottom nav + nested routes) | Phase 3 replacement |
| Single /api/users routes file | Separate user.routes.js + follow.routes.js + (existing) auth.routes.js | Clean separation of concerns |
| No social graph models | Follow.js + Block.js with compound unique indexes | New in Phase 3 |
| Inline notification logic | notification.service.js stub (async, swallows errors) | Prepares Phase 5 |

**Deprecated / to replace:**
- `PostLoginShell` in App.jsx: replaced by `AppShell`. The function definition AND its usage in the Route element will be removed.

---

## MVP Slicing Strategy

The phase has two largely independent tracks: backend models+API and frontend shell+pages. The recommended plan sequence delivers each as a thin end-to-end slice:

**Plan 03-01: Follow + Block models and all backend endpoints**
- Follow.js, Block.js, Notification.js models
- notification.service.js stub
- user.controller.js (getProfile, getMe, updateMe, searchUsers)
- follow.controller.js (follow, unfollow, acceptFollow, rejectFollow)
- block.controller.js (block)
- user.routes.js, follow.routes.js mounted in app.js
- Tests: user.controller.test.js, follow.controller.test.js, block.controller.test.js
- Covers: PROF-01 (PATCH /api/users/me isPrivate), PROF-02 (GET /api/users/:id), PROF-03 (search), SOCL-01–05

**Plan 03-02: AppShell — authenticated navigation shell**
- AppShell.jsx replacing PostLoginShell in App.jsx
- Scroll-aware header with useScrollDirection hook
- Bottom nav with 4 tabs (Feed + Notifications as placeholders)
- Routes nested: /, /search, /notifications, /profile/me, /profile/:userId
- Covers: D-01, D-02, D-03, D-04 (FRNT-05 partial)

**Plan 03-03: ProfilePage + FollowButton + BlockButton**
- ProfilePage.jsx (own profile + other user profile)
- FollowButton.jsx (4 states + inline confirmations)
- BlockButton.jsx
- API integration: GET /api/users/:id, PATCH /api/users/me, follow/unfollow/block calls
- Covers: PROF-01 (toggle UI), PROF-02 (view UI), SOCL-01, SOCL-02, SOCL-04, FRNT-05

**Plan 03-04: SearchPage + UserResultCard**
- SearchPage.jsx with debounced search input
- UserResultCard.jsx with compact FollowButton
- API integration: GET /api/users/search
- Covers: PROF-03, SOCL-05 (blocked excluded from results)

Each plan is independently deployable and testable. Plans 03-01 and 03-02 can be done in parallel (no dependency between backend and the shell); Plans 03-03 and 03-04 depend on 03-01 (need the API) and 03-02 (need the shell routes).

---

## Assumptions Log

| # | Claim | Section | Risk if Wrong |
|---|-------|---------|---------------|
| A1 | Search should return empty when q is blank | Common Pitfalls #6 | Could return all institution users — UX issue, not a security issue |
| A2 | GET /api/users/:id embeds followStatus in single response (vs. separate endpoint) | Pattern 3 | One extra round-trip per profile load if wrong — low impact |
| A3 | Two-query approach for search block exclusion ($nin) is sufficient at MVP scale | Pattern 5 | At very large scale $lookup aggregation would be faster; not relevant for MVP |
| A4 | notification.service.js silently swallows errors (fire-and-forget) | Pattern 7 | If Phase 5 debugging is needed, silent errors make it harder — could add console.error |
| A5 | Specific compound index choices on Follow and Block models | Pattern 1 | Missing index = slower queries; extra index = slightly more write overhead |

**If this table is empty:** Not empty — 5 assumptions documented.

---

## Open Questions (RESOLVED)

1. **Should GET /api/users/:id include follower/following counts for private profiles viewed by accepted followers?**
   - What we know: Accepted followers can see full profile (bio, posts placeholder).
   - What's unclear: Whether counts belong in the same "full" response or are a separate counter fetch.
   - Recommendation: Include counts in the full response (no extra endpoint). Counter queries are O(1) with the followingId index.

2. **Should follow.controller.unfollow (DELETE /api/users/:id/follow) return 404 or 200 when no follow exists?**
   - What we know: Idempotent deletes commonly return 200/204 regardless.
   - Recommendation: Return 200 with `{ unfollowed: true }` even if no record existed. Simplifies frontend retry logic.

---

## Environment Availability

| Dependency | Required By | Available | Version | Fallback |
|------------|------------|-----------|---------|----------|
| Node.js | All backend code | Yes | (system node) | — |
| MongoDB (local/cloud) | Development and tests | Via mongodb-memory-server | — | mongodb-memory-server eliminates need for local MongoDB in tests |
| npm | Package install | Yes | — | — |
| Vite dev server | Frontend development | Yes (in package.json) | 5.x | — |

No missing dependencies. Phase 3 introduces no new external tools or services.

---

## Validation Architecture

### Test Framework
| Property | Value |
|----------|-------|
| Framework | node:test (built-in Node.js 18+) + mongodb-memory-server |
| Config file | none — test files listed explicitly in package.json scripts.test |
| Quick run command | `node --test src/controllers/follow.controller.test.js` |
| Full suite command | `npm test` |

### Phase Requirements to Test Map

| Req ID | Behavior | Test Type | Automated Command | File Exists? |
|--------|----------|-----------|-------------------|-------------|
| PROF-01 | PATCH /api/users/me toggles isPrivate | unit | `node --test src/controllers/user.controller.test.js` | Wave 0 |
| PROF-02 | GET /api/users/:id returns reduced data for private profile non-follower | unit | `node --test src/controllers/user.controller.test.js` | Wave 0 |
| PROF-03 | GET /api/users/search excludes blocked users | unit | `node --test src/controllers/user.controller.test.js` | Wave 0 |
| SOCL-01 | follow() creates Follow{status:accepted} for public profile | unit | `node --test src/controllers/follow.controller.test.js` | Wave 0 |
| SOCL-02 | follow() creates Follow{status:pending} for private profile | unit | `node --test src/controllers/follow.controller.test.js` | Wave 0 |
| SOCL-03 | acceptFollow() only works for followingId owner (403 otherwise) | unit | `node --test src/controllers/follow.controller.test.js` | Wave 0 |
| SOCL-04 | block() removes both Follow docs and creates Block | unit | `node --test src/controllers/block.controller.test.js` | Wave 0 |
| SOCL-05 | search excludes users who blocked viewer or whom viewer blocked | unit | `node --test src/controllers/user.controller.test.js` | Wave 0 |
| FRNT-05 | AppShell renders bottom nav; FollowButton shows correct state | manual smoke | open browser, navigate tabs | — |

### Sampling Rate
- **Per task commit:** `node --test src/controllers/<controller>.test.js` (whichever controller was changed)
- **Per wave merge:** `npm test`
- **Phase gate:** `npm test` green before `/gsd:verify-work`

### Wave 0 Gaps
- [ ] `src/controllers/user.controller.test.js` — covers PROF-01, PROF-02, PROF-03, SOCL-05
- [ ] `src/controllers/follow.controller.test.js` — covers SOCL-01, SOCL-02, SOCL-03
- [ ] `src/controllers/block.controller.test.js` — covers SOCL-04
- [ ] Update `package.json scripts.test` to include the three new test files

---

## Security Domain

Security enforcement is enabled (`security_enforcement: true`, ASVS level 1).

### Applicable ASVS Categories

| ASVS Category | Applies | Standard Control |
|---------------|---------|-----------------|
| V2 Authentication | yes | authenticateToken middleware (reused unchanged — HS256 pinned) |
| V3 Session Management | no | Stateless JWT — no session storage |
| V4 Access Control | yes | Ownership checks in acceptFollow/rejectFollow; follow/block scoped to req.user.id |
| V5 Input Validation | yes | Regex input: `new RegExp(q, 'i')` — see threat below |
| V6 Cryptography | no | No new crypto in this phase |

### Known Threat Patterns for This Stack

| Pattern | STRIDE | Standard Mitigation |
|---------|--------|---------------------|
| Cross-tenant profile access | Info Disclosure | All user queries implicitly scoped — search adds institutionId filter; profile access checks only follow/block status (users in other institutions can't be searched anyway) |
| ReDoS via search query | DoS | `new RegExp(untrustedInput, 'i')` can be exploited with pathological regex patterns. Mitigate: limit q to 100 chars, reject if contains regex metacharacters, or use a literal string match. Simplest: `q.slice(0, 100)` + escape metacharacters with `q.replace(/[.*+?^${}()|[\]\\]/g, '\\$&')` |
| IDOR on accept/reject follow | Elevation of Privilege | Ownership check: `follow.followingId.toString() !== req.user.id → 403` (Pattern documented above) |
| Blocking self | Tampering | `if (blockerId === blockedId) return 400` — already in Pattern 4 |
| Follow self | Tampering | `if (followerId === followingId) return 400` — already in Pattern 5 follow() |

**ReDoS mitigation code:**
```javascript
// Escape regex metacharacters before using q in RegExp [ASSUMED: standard escaping pattern]
const safeQ = q.slice(0, 100).replace(/[.*+?^${}()|[\]\\]/g, '\\$&');
const regex = new RegExp(safeQ, 'i');
```

---

## Sources

### Primary (HIGH confidence)
- `CONTEXT.md` (repo root) — canonical Follow and Block schemas, business rules for follow/block/privacy, notification types [VERIFIED: read in this session]
- `03-CONTEXT.md` — all locked decisions D-01 through D-13 [VERIFIED: read in this session]
- `03-UI-SPEC.md` — complete component specifications, interaction states, route structure [VERIFIED: read in this session]
- `src/controllers/invite.controller.test.js` — established test pattern (node:test + mongodb-memory-server) [VERIFIED: read in this session]
- `src/app.js` — existing router mount pattern [VERIFIED: read in this session]
- `MINIREDESOCIAL_FRONT/src/App.jsx` — PostLoginShell to replace, existing RequireAuth pattern [VERIFIED: read in this session]
- `package.json` (backend) + `MINIREDESOCIAL_FRONT/package.json` — exact installed versions [VERIFIED: read in this session]
- slopcheck — all 9 packages returned [OK] [VERIFIED: slopcheck run in this session]

### Secondary (MEDIUM confidence)
- npm registry version checks: mongoose@9.6.3, express@5.2.1, lucide-react@1.17.0, mongodb-memory-server@11.2.0

### Tertiary (LOW confidence)
- ReDoS risk for `new RegExp(userInput)` — well-documented attack class [ASSUMED: standard web security knowledge, not verified against OWASP in this session]
- Two-query approach ($nin) vs $lookup for block exclusion at scale [ASSUMED: performance comparison not benchmarked]

---

## Metadata

**Confidence breakdown:**
- Standard Stack: HIGH — all packages verified via npm registry, slopcheck, existing package.json
- Architecture: HIGH — schemas from canonical CONTEXT.md, patterns from existing codebase
- Pitfalls: HIGH (route ordering, ownership checks) / MEDIUM (ReDoS — assumed standard knowledge)
- MVP Slicing: HIGH — derived from CONTEXT.md scope + existing phase velocity (~6 min/plan)

**Research date:** 2026-06-03
**Valid until:** 2026-07-03 (stable stack, no fast-moving dependencies)
