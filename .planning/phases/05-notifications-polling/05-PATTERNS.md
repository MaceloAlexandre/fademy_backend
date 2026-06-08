# Phase 5: Notifications (Polling) - Pattern Map

**Mapped:** 2026-06-07
**Files analyzed:** 9 (5 backend, 4 frontend; 2 backend modify, 2 frontend modify)
**Analogs found:** 9 / 9

## File Classification

| New/Modified File | Role | Data Flow | Closest Analog | Match Quality |
|-------------------|------|-----------|----------------|---------------|
| `src/controllers/notification.controller.js` | controller | CRUD (list + mark-read) | `src/controllers/post.controller.js` (`getFeed` for cursor pagination) + `src/controllers/category.controller.js` (for simple list shape) | role-match (composite) |
| `src/routes/notification.routes.js` | route | request-response | `src/routes/follow.routes.js` | exact |
| `src/app.js` (modify — mount routes) | config | request-response | itself (existing mount block) | exact |
| `src/controllers/registration.controller.js` (modify — fire `invite_received`) | controller | event-driven (side-effect on existing CRUD) | `src/controllers/follow.controller.js` (`follow` — notification fired alongside primary CRUD write) | exact |
| `src/controllers/notification.controller.test.js` (new test) | test | CRUD | `src/controllers/comment.controller.test.js` | exact |
| `MINIREDESOCIAL_FRONT/src/pages/NotificationsPage/NotificationsPage.jsx` | component (page) | request-response (paginated list) | `MINIREDESOCIAL_FRONT/src/pages/FeedPage/FeedPage.jsx` | exact |
| `MINIREDESOCIAL_FRONT/src/components/NotificationCard/NotificationCard.jsx` | component (card) | request-response | `MINIREDESOCIAL_FRONT/src/components/UserResultCard/UserResultCard.jsx` (structure) + `PostCard.jsx` (timestamp helper) + `FollowButton.jsx` (inline action pattern) | role-match (composite) |
| `MINIREDESOCIAL_FRONT/src/hooks/useNotificationPolling.js` (new — first custom hook in repo) | hook | event-driven (interval polling) | none (no existing hooks dir); closest structural analog is `useScrollDirection` inline in `AppShell.jsx` (`useRef`+`useEffect`+cleanup) | no-analog (pattern borrowed from in-component hook) |
| `MINIREDESOCIAL_FRONT/src/components/AppShell/AppShell.jsx` + `.module.css` (modify) | component (shell/provider) | event-driven (polling mount) + request-response (nav wiring) | itself (existing bell button markup, Phase 4) | exact |
| `MINIREDESOCIAL_FRONT/src/App.jsx` (modify — add `/notifications` route) | route | request-response | itself (existing `<Route>` list) | exact |

---

## Pattern Assignments

### `src/controllers/notification.controller.js` (controller, CRUD)

**Analog A (cursor pagination):** `src/controllers/post.controller.js` → `getFeed` (lines 70-122)
**Analog B (simple list + auth destructure):** `src/controllers/category.controller.js` (whole file, 9-19)
**Analog C (PATCH-by-id with ownership check):** `src/controllers/follow.controller.js` → `acceptFollow` (lines 73-105)

**Imports pattern** (mirrors `comment.controller.js` lines 1-5):
```javascript
'use strict';

const Notification = require('../models/Notification');
// require referenced models so populate() can resolve them, e.g.:
// require('../models/User');
```

**Core list pattern — cursor pagination** (`post.controller.js` lines 77-122, adapt `_id`/`createdAt` cursor + `populate`):
```javascript
async function getFeed(req, res) {
  const { id: userId, institutionId } = req.user;
  try {
    // ...build query scoped to req.user...
    const query = { userId, institutionId }; // adapt: Notification has no institutionId — scope by userId only

    // Cursor-based pagination: _id < cursor
    if (req.query.cursor) {
      query._id = { $lt: req.query.cursor };
    }

    // Cap limit to 50 to prevent DoS
    const limit = Math.min(parseInt(req.query.limit, 10) || 20, 50);

    // Fetch one extra to detect hasMore
    const docs = await Post.find(query)
      .sort({ _id: -1 })
      .limit(limit + 1)
      .populate('authorId', 'name avatarUrl');

    const hasMore = docs.length > limit;
    const posts = docs.slice(0, limit);
    const nextCursor = hasMore ? posts[posts.length - 1]._id : null;

    return res.status(200).json({ posts, hasMore, nextCursor });
  } catch (err) {
    return res.status(500).json({ error: 'Internal server error' });
  }
}
```
For `notification.controller.js`, replace `Post`/`authorId` with `Notification`/`actorId`, and scope strictly by `userId: req.user.id` (Notification has **no** `institutionId` field — see Shared Pattern "Multi-tenant scoping" below). `populate('actorId', 'name avatarUrl')` gives the card its avatar+name (D-01).

**Simple-list / auth-destructure pattern** (`category.controller.js` lines 9-16 — use this shape if you choose simple offset pagination instead of cursor, per CONTEXT "Claude's Discretion"):
```javascript
async function listCategories(req, res) {
  try {
    const categories = await Category.find({});
    return res.status(200).json({ categories });
  } catch (err) {
    return res.status(500).json({ error: 'Internal server error' });
  }
}
```

**Mark-as-read (PATCH-by-id, ownership check) pattern** (`follow.controller.js` `acceptFollow`, lines 73-105):
```javascript
async function acceptFollow(req, res) {
  const followId = req.params.id;
  try {
    const followDoc = await Follow.findById(followId);
    if (!followDoc) {
      return res.status(404).json({ error: 'Follow request not found' });
    }
    if (followDoc.followingId.toString() !== req.user.id) {
      return res.status(403).json({ error: 'Forbidden' });
    }
    if (followDoc.status !== 'pending') {
      return res.status(400).json({ error: 'No pending request' });
    }
    followDoc.status = 'accepted';
    await followDoc.save();
    // ... notification fired here ...
    return res.status(200).json({ status: 'accepted' });
  } catch (err) {
    return res.status(500).json({ error: 'Internal server error' });
  }
}
```
Adapt for `PATCH /api/notifications/:id/read`: load by `_id`, verify `notification.userId.toString() === req.user.id` (403 if not owner), set `read = true`, save, return 200. A "mark all as read" variant (Claude's discretion) would instead do `Notification.updateMany({ userId: req.user.id, read: false }, { read: true })` — same try/catch/500 envelope.

**Error handling pattern** (uniform across all controllers — `category.controller.js` line 13-15, `follow.controller.js` line 102-104):
```javascript
} catch (err) {
  return res.status(500).json({ error: 'Internal server error' });
}
```
Always `return res.status(500).json({ error: 'Internal server error' })` in the generic catch — no error logging, no error-type branching beyond Mongo's `err.code === 11000` (not relevant for Notification reads/updates).

**Auth pattern** — `req.user` is injected by `authenticate` middleware (see Shared Patterns); destructure `{ id, institutionId }` or `{ id: userId, institutionId }` per `post.controller.js` line 78 / `comment.controller.js` line 16.

---

### `src/routes/notification.routes.js` (route, request-response)

**Analog:** `src/routes/follow.routes.js` (whole file, lines 1-16)

```javascript
'use strict';

const { Router } = require('express');
const authenticate = require('../middlewares/auth.middleware');
const { listNotifications, markRead } = require('../controllers/notification.controller');

const router = Router();

// GET /api/notifications — paginated list of the caller's notifications
router.get('/', authenticate, listNotifications);

// PATCH /api/notifications/:id/read — mark a single notification as read
router.patch('/:id/read', authenticate, markRead);

module.exports = router;
```
Note route-ordering convention from `user.routes.js` (lines 11-19): static-segment routes (`/search`, `/me`) MUST be declared before `/:id`-style dynamic routes — not directly applicable here since there's no name collision, but keep `GET /` before `PATCH /:id/read` for readability/consistency with `follow.routes.js` ordering (accept before reject).

---

### `src/app.js` (modify — mount notification routes)

**Analog:** existing mount block, `src/app.js` lines 26-32

```javascript
// Posts, Comments & Feed (Plan 04)
app.use('/api/posts', require('./routes/post.routes'));
app.use('/api/posts', require('./routes/comment.routes'));
app.use('/api/categories', require('./routes/category.routes'));
```
Add a new line following this exact `app.use('/api/<resource>', require('./routes/<resource>.routes'))` shape:
```javascript
app.use('/api/notifications', require('./routes/notification.routes'));
```
Place it after the Phase 4 block (or create a "Notifications (Plan 05)" comment section) — match the existing comment-header convention (`// Posts, Comments & Feed (Plan 04)`).

---

### `src/controllers/registration.controller.js` (modify — fire `invite_received`)

**Analog:** `src/controllers/follow.controller.js` → `follow` (lines 12-54), specifically the "primary write, then fire notification, then respond" sequencing at lines 37-47

**Pattern — fire notification alongside the primary CRUD write, before the final response:**
```javascript
const status = target.isPrivate ? 'pending' : 'accepted';
const followDoc = await Follow.create({ followerId, followingId, status });

await notificationService.create(
  status === 'accepted' ? 'new_follower' : 'follow_request',
  followerId,    // actorId — who triggered the event
  followingId,   // userId — who receives the notification
  followDoc._id, // refId
  'Follow'       // refModel
);

return res.status(201).json({ status: followDoc.status, followId: followDoc._id });
```

**Apply to `register()`** (`registration.controller.js` lines 63-96): after `user = await User.create(...)` succeeds (line 65-79) and the invite slot is consumed/saved (lines 82-86), insert:
```javascript
const notificationService = require('../services/notification.service'); // add to top-level requires (line 4-7 area)

// ... inside register(), after invite.save():
await notificationService.create(
  'invite_received',
  user._id,           // actorId — the newly registered user
  invite.invitedById, // userId — recipient = invite creator (D-11)
  user._id,           // refId — points to the new user (D-12, refModel: 'User')
  'User'
);
```
This mirrors `comment.controller.js` lines 37-39 (conditional notify-after-write) and `follow.controller.js` lines 39-45 (unconditional notify-after-write) — `invite_received` is unconditional (every successful invite-based registration has exactly one real recipient: `invite.invitedById`). Place the call AFTER `await invite.save()` (so it doesn't fire if invite consumption somehow throws) and BEFORE the `return res.status(201)...` — matches `follow()`'s "create → notify → respond" ordering. Because `notificationService.create` swallows all errors internally (see `notification.service.js` lines 6-10), no extra try/catch is needed around this call.

---

### `src/controllers/notification.controller.test.js` (test)

**Analog:** `src/controllers/comment.controller.test.js` (whole structure; spy pattern lines 8-20, `makeRes` lines 32-44, `before`/`after` lifecycle lines ~46-70, `describe`/`it` blocks lines 133-208)

**Module-load mock for `notification.service` BEFORE requiring the controller** (lines 8-20 — REQUIRED if the controller under test ever calls `notificationService.create`; for `notification.controller.js` this is likely NOT needed since it only reads/updates `Notification` docs directly, but copy the env-var-bootstrap header regardless):
```javascript
'use strict';

// MUST be set before any require so env.js does not throw on MONGODB_URI / JWT_SECRET
process.env.MONGODB_URI = process.env.MONGODB_URI || 'mongodb://localhost:27017/test';
process.env.JWT_SECRET = process.env.JWT_SECRET || 'test-secret';
process.env.BCRYPT_ROUNDS = '4';

const { listNotifications, markRead } = require('./notification.controller');

const { describe, it, before, after, beforeEach } = require('node:test');
const assert = require('node:assert/strict');
const mongoose = require('mongoose');
```

**`makeRes` helper** (verbatim from `comment.controller.test.js` lines 32-44 / `follow.controller.test.js` lines 32-44):
```javascript
function makeRes() {
  const res = {
    statusCode: null,
    body: null,
    status(code) { this.statusCode = code; return this; },
    json(data) { this.body = data; return this; },
  };
  return res;
}
```

**`before`/`after` mongodb-memory-server lifecycle** (verbatim from `follow.controller.test.js` lines 46-70):
```javascript
before(async () => {
  let MongoMemoryServer;
  try {
    ({ MongoMemoryServer } = require('mongodb-memory-server'));
  } catch (err) {
    console.log('SKIP: mongodb-memory-server not available');
    process.exit(0);
  }
  const uri = process.env.MONGODB_URI_TEST;
  if (uri) { testUri = uri; }
  else { mongoServer = await MongoMemoryServer.create(); testUri = mongoServer.getUri(); }
  process.env.MONGODB_URI = testUri;
  const { connectDB } = require('../config/db');
  await connectDB(testUri);
});

after(async () => {
  await mongoose.disconnect();
  if (mongoServer) { await mongoServer.stop(); }
});
```

**Test naming/assertion style** (`comment.controller.test.js` lines 136-155, 157-171):
```javascript
describe('notification.controller', () => {
  describe('listNotifications()', () => {
    it('returns 200 with paginated notifications, most recent first (NOTF-03)', async () => {
      // seed fixture, build req/res, call handler, assert res.statusCode/res.body
    });
  });
  describe('markRead()', () => {
    it('returns 403 when caller is not the notification owner', async () => { /* ... */ });
    it('returns 200 and sets read=true (NOTF-04)', async () => { /* ... */ });
  });
});
```
Use `seedFixture()`/`seedUsers()`-style async helper functions (see `comment.controller.test.js` for the `seedFixture` shape) to create `Institution` + `User` + `Notification` docs before each test group.

---

### `MINIREDESOCIAL_FRONT/src/pages/NotificationsPage/NotificationsPage.jsx` (page, paginated list)

**Analog:** `MINIREDESOCIAL_FRONT/src/pages/FeedPage/FeedPage.jsx` (whole file, 159 lines)

**Imports pattern** (lines 1-7, drop pull-to-refresh-only imports if simplifying):
```javascript
import { useState, useEffect, useRef, useCallback } from 'react'
import { useNavigate } from 'react-router-dom'
import api from '../../lib/api'
import NotificationCard from '../../components/NotificationCard/NotificationCard'
import Spinner from '../../components/Spinner'
import ErrorBanner from '../../components/ErrorBanner'
import styles from './NotificationsPage.module.css'
```

**Core fetch + cursor pagination + infinite scroll pattern** (lines 11-72 — reuse nearly verbatim, swap `posts`/`/api/posts/feed`/`PostCard` for `notifications`/`/api/notifications`/`NotificationCard`; the UI-SPEC explicitly permits dropping pull-to-refresh — lines 74-98 of FeedPage — since polling already covers freshness):
```javascript
const [items, setItems] = useState([])
const [cursor, setCursor] = useState(null)
const [hasMore, setHasMore] = useState(true)
const [loading, setLoading] = useState(false)
const [apiError, setApiError] = useState('')
const sentinelRef = useRef(null)
const currentCursorRef = useRef(null)

useEffect(() => { currentCursorRef.current = cursor }, [cursor])

const fetchPage = useCallback(async (reset) => {
  setLoading(true)
  if (reset) setApiError('')
  const useCursor = reset ? null : currentCursorRef.current
  try {
    const params = { limit: 20 }
    if (useCursor) params.cursor = useCursor
    const res = await api.get('/api/notifications', { params })
    const data = res.data
    if (reset) setItems(data.notifications)
    else setItems((prev) => prev.concat(data.notifications))
    setCursor(data.nextCursor)
    setHasMore(data.hasMore)
  } catch (_) {
    setApiError('Erro ao carregar notificações. Tente novamente.')
  } finally {
    setLoading(false)
  }
}, [])

useEffect(() => { fetchPage(true) }, [fetchPage])

useEffect(() => {
  const obs = new IntersectionObserver(([entry]) => {
    if (entry.isIntersecting && hasMore && !loading) fetchPage(false)
  }, { threshold: 0.1 })
  if (sentinelRef.current) obs.observe(sentinelRef.current)
  return () => obs.disconnect()
}, [hasMore, loading, fetchPage])
```

**Render structure — loading / list / empty / error / sentinel** (lines 102-158, swap copy per UI-SPEC Copywriting Contract):
```javascript
const isInitialLoading = loading && items.length === 0 && !apiError

return (
  <div className={styles.page}>
    {isInitialLoading && (
      <div className={styles.spinnerCenter}>
        <Spinner size={28} aria-label="Carregando notificações..." />
      </div>
    )}
    {!isInitialLoading && (
      <div className={styles.list}>
        <ErrorBanner message={apiError} />
        {!loading && items.length === 0 && !apiError && (
          <div className={styles.emptyState}>
            <p className={styles.emptyHeading}>Nenhuma notificação ainda</p>
            <p className={styles.emptyBody}>Quando alguém interagir com você, vai aparecer aqui.</p>
          </div>
        )}
        {items.map((n) => (
          <NotificationCard key={n._id} notification={n} onRead={handleMarkRead} />
        ))}
        {loading && items.length > 0 && (
          <div className={styles.spinnerMore}><Spinner size={24} /></div>
        )}
        <div ref={sentinelRef} className={styles.sentinel} aria-hidden="true" />
      </div>
    )}
  </div>
)
```

**CSS module — list/empty-state/spinner classes** (`FeedPage.module.css` lines 1-77, copy verbatim — class names `.page`, `.spinnerCenter`, `.list`, `.spinnerMore`, `.emptyState`, `.emptyHeading`, `.emptyBody`, `.sentinel` map 1:1 to UI-SPEC requirements; drop `.spinnerTop`/pull-to-refresh-only rules and the `.emptyLink` rule since NotificationsPage has no CTA per UI-SPEC).

---

### `MINIREDESOCIAL_FRONT/src/components/NotificationCard/NotificationCard.jsx` (card, request-response)

**Analog A (row structure):** `MINIREDESOCIAL_FRONT/src/components/UserResultCard/UserResultCard.jsx` (whole file, 47 lines) + `.module.css` (47 lines)
**Analog B (relative-time helper):** `MINIREDESOCIAL_FRONT/src/components/PostCard/PostCard.jsx` lines 5-14
**Analog C (inline action + stopPropagation + spinner/error):** `MINIREDESOCIAL_FRONT/src/components/FollowButton/FollowButton.jsx`

**`formatRelativeTime` helper — copy verbatim** (`PostCard.jsx` lines 5-14, per UI-SPEC explicit instruction "reuse this exact function — extract to shared util or duplicate verbatim"):
```javascript
function formatRelativeTime(dateStr) {
  const diff = Date.now() - new Date(dateStr).getTime()
  const minutes = Math.floor(diff / 60000)
  if (minutes < 1) return 'agora'
  if (minutes < 60) return `${minutes}min`
  const hours = Math.floor(minutes / 60)
  if (hours < 24) return `${hours}h`
  const days = Math.floor(hours / 24)
  return `${days}d`
}
```

**Card row + tap-to-navigate + stopPropagation pattern** (`UserResultCard.jsx` lines 1-46):
```javascript
import { useState } from 'react'
import { useNavigate } from 'react-router-dom'
import styles from './NotificationCard.module.css'

export default function NotificationCard({ notification, onRead }) {
  const navigate = useNavigate()

  function handleCardClick() {
    if (!notification.read) onRead(notification._id) // mark read (D-02, non-blocking — see error contract)
    // navigate by type (D-02):
    //   post_comment        -> /posts/${notification.refId}
    //   new_follower / follow_accepted / invite_received -> /profile/${notification.actorId._id or refId}
    navigate(targetPathFor(notification))
  }

  return (
    <div className={`${styles.card} ${!notification.read ? styles.unread : ''}`} onClick={handleCardClick}>
      {!notification.read && <span className={styles.unreadDot} aria-hidden="true" />}
      <img src={notification.actorId.avatarUrl} alt={notification.actorId.name} className={styles.avatar} />
      <div className={styles.content}>
        <p className={styles.text}>
          <strong className={styles.actorName}>{notification.actorId.name}</strong>
          {' '}{actionTextFor(notification.type)}
        </p>
        <span className={styles.timestamp}>{formatRelativeTime(notification.createdAt)}</span>
        {notification.type === 'follow_request' && notification.status !== 'resolved' && (
          <div className={styles.actionsRow} onClick={(e) => e.stopPropagation()}>
            {/* Aceitar / Rejeitar inline buttons — see Analog C below */}
          </div>
        )}
      </div>
    </div>
  )
}
```
Note: `UserResultCard` wraps the inline-action zone in a `<div onClick={(e) => e.stopPropagation()}>` (lines 32-35) — copy this exact `stopPropagation` wrapper for the Aceitar/Rejeitar row (UI-SPEC explicitly calls this out, "mirrors the `e.stopPropagation()` pattern in `UserResultCard`'s `.followWrap`").

**`UserResultCard.module.css` `.card`/`.avatar`/`.info`/`.name` classes — direct visual template** (lines 1-40, D-01 visual consistency requirement):
```css
.card {
  display: flex;
  flex-direction: row;
  align-items: center;
  gap: var(--space-md);
  padding: var(--space-md) 0;
  border-bottom: 1px solid rgba(214, 207, 193, 0.1);
  cursor: pointer;
}
.avatar {
  width: 40px; height: 40px;
  border-radius: var(--radius-avatar);
  object-fit: cover;
  flex-shrink: 0;
}
.name {
  font-size: var(--text-body);
  font-weight: var(--weight-semibold);
  color: var(--color-surface);
}
```
Adapt: NotificationCard needs `padding: var(--space-md)` on all sides (not just vertical, per UI-SPEC "internal padding `--space-md`"), an `.unread`/background-tint variant (`background: rgba(140, 110, 79, 0.08)`), and an `.unreadDot` (8px circle, `background: var(--color-accent)`, `border-radius: 50%`).

**Inline accept/reject action pattern — `FollowButton.jsx` lines 18-44 (API call + state machine) and lines 46-65 (button render w/ Spinner + ErrorBanner)**:
```javascript
async function handleAccept() {
  setApiError('')
  setLoading(true)
  try {
    await api.patch(`/api/follows/${notification.refId}/accept`)
    await api.patch(`/api/notifications/${notification._id}/read`) // D-07 — also mark read
    setStatus('accepted')
    onRead(notification._id) // sync parent unread count
  } catch {
    setApiError('Algo deu errado. Tente novamente.')
  } finally {
    setLoading(false)
  }
}
```
Button render — copy `btnPrimary` (Aceitar) and `btnConfirmDestructive` (Rejeitar) class usage verbatim from `FollowButton.jsx` lines 53-65 / 91-98:
```jsx
<button type="button" className={`${styles.btn} ${styles.btnPrimary}`} disabled={loading} onClick={handleAccept} aria-label={`Aceitar solicitação de ${notification.actorId.name}`}>
  {loading ? <Spinner size={16} /> : 'Aceitar'}
</button>
<button type="button" className={`${styles.btn} ${styles.btnConfirmDestructive}`} disabled={loading} onClick={handleReject} aria-label={`Rejeitar solicitação de ${notification.actorId.name}`}>
  {loading ? <Spinner size={16} /> : 'Rejeitar'}
</button>
{apiError && <ErrorBanner message={apiError} />}
```

**`FollowButton.module.css` `.btnPrimary`/`.btnConfirmDestructive` — copy these two class blocks verbatim into `NotificationCard.module.css`** (lines 31-35 and 89-112):
```css
.btnPrimary {
  background-color: var(--color-accent);
  color: var(--color-surface);
  border: none;
}
.btnConfirmDestructive {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  min-height: var(--touch-min);
  padding: var(--space-xs) var(--space-md);
  border-radius: var(--radius-button);
  font-size: var(--text-label);
  font-weight: var(--weight-semibold);
  background-color: transparent;
  border: 1px solid var(--color-destructive);
  color: var(--color-destructive);
  cursor: pointer;
}
.btnConfirmDestructive:disabled { opacity: 0.5; cursor: not-allowed; }
.btnConfirmDestructive:focus-visible { outline: 2px solid var(--color-accent-alt); outline-offset: 2px; }
```
(UI-SPEC explicitly says: "Reuse `FollowButton`'s confirm-action CSS class names as a styling reference... do not import `FollowButton` itself.")

---

### `MINIREDESOCIAL_FRONT/src/hooks/useNotificationPolling.js` (hook, event-driven/interval)

**No direct analog — first custom hook in this codebase** (no `src/hooks/` directory exists). Closest structural reference: the inline `useScrollDirection` hook defined directly in `AppShell.jsx` (lines 13-29), which establishes the local convention for `useRef` + `useEffect` + cleanup-on-unmount:

```javascript
function useScrollDirection() {
  const lastScrollY = useRef(0)
  const [hidden, setHidden] = useState(false)
  useEffect(() => {
    function handleScroll() {
      const current = window.scrollY
      setHidden(current > lastScrollY.current && current > 10)
      lastScrollY.current = current
    }
    window.addEventListener('scroll', handleScroll, { passive: true })
    return () => window.removeEventListener('scroll', handleScroll)
  }, [])
  return hidden
}
```

**Adapt to `setInterval`-based polling** (per UI-SPEC contract: 5000ms interval, fire-and-forget, no visibility-API integration, owns `unreadCount`):
```javascript
import { useState, useEffect, useCallback } from 'react'
import api from '../lib/api'

const POLL_INTERVAL_MS = 5000

export default function useNotificationPolling() {
  const [unreadCount, setUnreadCount] = useState(0)

  const fetchUnreadCount = useCallback(async () => {
    try {
      const res = await api.get('/api/notifications', { params: { limit: 1 } })
      // adapt to whatever shape the list endpoint returns for unread count
      // (e.g. a dedicated `unreadCount` field, or derive client-side)
      setUnreadCount(res.data.unreadCount ?? 0)
    } catch (_) {
      // fire-and-forget — do not surface polling errors to the UI (background concern)
    }
  }, [])

  useEffect(() => {
    fetchUnreadCount() // initial fetch — does not block render
    const id = setInterval(fetchUnreadCount, POLL_INTERVAL_MS)
    return () => clearInterval(id)
  }, [fetchUnreadCount])

  return { unreadCount, refetch: fetchUnreadCount }
}
```
Decide during planning whether `GET /api/notifications` returns an `unreadCount` directly (cheapest: `Notification.countDocuments({ userId, read: false })` alongside the list query) or whether the hook derives it client-side from the first page — a dedicated lightweight count is simpler and avoids over-fetching (CLAUDE.md "Simplicity First"); consider exposing it via the same list endpoint's response envelope (e.g. `{ notifications, hasMore, nextCursor, unreadCount }`) rather than adding a second endpoint.

**`api` import pattern** — identical to every other component/page (`FollowButton.jsx` line 4, `FeedPage.jsx` line 3): `import api from '../lib/api'` (path depth adjusted for `hooks/` location — one level up from `components/`/`pages/`, so `'../lib/api'`).

---

### `MINIREDESOCIAL_FRONT/src/components/AppShell/AppShell.jsx` + `.module.css` (modify)

**Analog:** itself — existing bell button markup (Phase 4, lines 47-54) and `.bellBtn`/`.sr-only` CSS (lines 39-50, 126-136)

**Current inert bell markup to wire up** (lines 47-54):
```jsx
<button
  type="button"
  className={styles.bellBtn}
  aria-label="Notificações"
  onClick={() => {}}
>
  <Bell size={24} aria-hidden="true" />
</button>
```

**Target — wired with navigation, conditional color, and badge** (per UI-SPEC "AppShell update" section):
```jsx
const { unreadCount } = useNotificationPolling()
const hasUnread = unreadCount > 0
const badgeLabel = unreadCount > 9 ? '9+' : String(unreadCount)

<button
  type="button"
  className={styles.bellBtn}
  aria-label="Notificações"
  onClick={() => navigate('/notifications')}
>
  <Bell size={24} aria-hidden="true" color={hasUnread ? 'var(--color-accent)' : undefined} />
  {hasUnread && (
    <span className={styles.badge} aria-hidden="true">{badgeLabel}</span>
  )}
  {hasUnread && (
    <span className={styles.srOnly}>
      {unreadCount > 9 ? 'mais de 9 notificações não lidas' : `${unreadCount} notificações não lidas`}
    </span>
  )}
</button>
```
Note: `navigate` is already in scope (line 32, `const navigate = useNavigate()`), and `Bell` is already imported (line 3). Hook the `useNotificationPolling()` call at the top of `AppShell` (D-09 — mount point) alongside `useScrollDirection()`.

**`.bellBtn` needs `position: relative`** for the absolutely-positioned badge — adapt the existing rule (lines 39-50):
```css
.bellBtn {
  position: relative;  /* ADD — anchor for .badge */
  display: flex;
  align-items: center;
  justify-content: center;
  width: 44px;
  min-height: 44px;
  background: transparent;
  border: none;
  color: rgba(214, 207, 193, 0.5);
  cursor: pointer;
  padding: 0;
}
```

**New `.badge` rule** (per UI-SPEC pill spec — position/size/colors fully specified):
```css
.badge {
  position: absolute;
  top: 4px;
  right: 4px;
  display: flex;
  align-items: center;
  justify-content: center;
  min-width: 20px;
  height: 20px;
  padding: 0 4px;
  border-radius: 10px;
  background: var(--color-accent);
  color: var(--color-surface);
  font-size: var(--text-label);
  font-weight: var(--weight-semibold);
  line-height: 1;
}
```

**`.sr-only` already exists** (lines 126-136) — reuse the existing class verbatim (note: the JSX above references `styles.srOnly`, but the CSS class is named `sr-only` with a hyphen; CSS Modules exposes hyphenated class names via bracket access `styles['sr-only']` — match whichever convention the rest of `AppShell.jsx` uses, or rename consistently; this is a pre-existing naming quirk to be aware of, not introduced by this phase).

---

### `MINIREDESOCIAL_FRONT/src/App.jsx` (modify — add `/notifications` route)

**Analog:** itself — existing nested route list inside the `RequireAuth`/`AppShell` wrapper (lines 27-39)

```jsx
<Route
  element={
    <RequireAuth>
      <AppShell />
    </RequireAuth>
  }
>
  <Route path="/" element={<FeedPage />} />
  <Route path="/search" element={<SearchPage />} />
  <Route path="/posts/:postId" element={<PostDetailPage />} />
  <Route path="/compose" element={<ComposePage />} />
  <Route path="/profile/:userId" element={<ProfilePage />} />
</Route>
```
Add `<Route path="/notifications" element={<NotificationsPage />} />` to this list, plus the corresponding top-level import:
```jsx
import NotificationsPage from './pages/NotificationsPage/NotificationsPage'
```
**IMPORTANT — route param naming:** the live code uses `/posts/:postId` and `/profile/:userId` (NOT `/posts/:id` / `/profile/:id` as the UI-SPEC route table states). When `NotificationCard` builds navigation targets, use these actual param names: `navigate(`/posts/${notification.refId}`)` for `post_comment` and `navigate(`/profile/${notification.refId}`)` (or `notification.actorId._id`, depending on whether `refId` is populated) for follow/invite-related types.

---

## Shared Patterns

### Authentication
**Source:** `src/middlewares/auth.middleware.js` (whole file, lines 1-35)
**Apply to:** `notification.routes.js` (every route), `notification.controller.js` (reads `req.user.id`/`req.user.institutionId`)
```javascript
function authenticate(req, res, next) {
  const authHeader = req.headers && req.headers.authorization;
  if (!authHeader || !authHeader.startsWith('Bearer ')) {
    return res.status(401).json({ error: 'Authentication required' });
  }
  const token = authHeader.slice(7);
  try {
    const decoded = verifyToken(token);
    req.user = { id: decoded.sub, role: decoded.role, institutionId: decoded.institutionId };
    return next();
  } catch (_err) {
    return res.status(401).json({ error: 'Invalid or expired token' });
  }
}
```
Mount with `router.get('/', authenticate, listNotifications)` exactly like every existing protected route (`follow.routes.js`, `comment.routes.js`, `post.routes.js`, `user.routes.js`).

### Error Handling (backend)
**Source:** every controller's generic catch block, e.g. `src/controllers/follow.controller.js` lines 102-104, `src/controllers/category.controller.js` lines 13-15
**Apply to:** `notification.controller.js` (all handlers)
```javascript
} catch (err) {
  return res.status(500).json({ error: 'Internal server error' });
}
```
No logging, no stack traces in response. Mongo duplicate-key (`err.code === 11000`) branching only where `.create()` is used on a unique-indexed field — not applicable to `Notification` reads/updates.

### Notification firing (fire-and-forget service)
**Source:** `src/services/notification.service.js` (whole file, lines 1-13)
**Apply to:** `registration.controller.js` (new `invite_received` call site)
```javascript
async function create(type, actorId, userId, refId, refModel) {
  try {
    await Notification.create({ type, actorId, userId, refId, refModel });
  } catch (_err) {
    // fire-and-forget: swallow all errors silently
  }
}
```
Signature is `create(type, actorId, userId, refId, refModel)` — `actorId` = who triggered, `userId` = recipient. The service already swallows errors, so call sites never need to wrap it in try/catch (see `follow.controller.js` lines 39-45, `comment.controller.js` line 38).

### Multi-tenant scoping (Notification has NO institutionId)
**Source:** `src/models/Notification.js` (whole file) + 05-CONTEXT "Established Patterns"
**Apply to:** `notification.controller.js` `listNotifications`/`markRead`
Unlike `Post`/`Comment` (which carry `institutionId` and are filtered by it — see `post.controller.js` lines 93-97, `comment.controller.js` line 24), `Notification` has **no** `institutionId` field. Scope queries strictly by `userId: req.user.id` — the schema's compound index `{ userId: 1, read: 1, createdAt: -1 }` (line 37) is purpose-built for exactly this query shape (`Notification.find({ userId, read: false }).sort({ createdAt: -1 })` or the cursor-based `_id` variant). Do NOT add an `institutionId` filter — it would be a no-op at best and a schema mismatch at worst (PLAT-01 is satisfied transitively via `userId`/`actorId` both being institution-scoped `User` refs).

### Frontend: axios instance with JWT interceptor
**Source:** `MINIREDESOCIAL_FRONT/src/lib/api.js` (whole file, lines 1-15)
**Apply to:** `NotificationsPage`, `NotificationCard`, `useNotificationPolling`
```javascript
import api from '../../lib/api'   // from pages/<X>/ or components/<X>/
import api from '../lib/api'      // from hooks/
// usage:
await api.get('/api/notifications', { params })
await api.patch(`/api/notifications/${id}/read`)
await api.patch(`/api/follows/${followId}/accept`)
```
Token attached automatically via request interceptor (`api.js` lines 7-13) — never manually set `Authorization` headers.

### Frontend: error copy + ErrorBanner reuse
**Source:** `FollowButton.jsx` line 26/40 (`'Algo deu errado. Tente novamente.'`), `FeedPage.jsx` line 49 (`'Erro ao carregar feed. Tente novamente.'`)
**Apply to:** `NotificationsPage` (list-load error), `NotificationCard` (mark-read / accept-reject errors)
Reuse `<ErrorBanner message={apiError} />` (renders nothing if `message` falsy — `ErrorBanner.jsx` line 4) with the **exact** copy strings specified in the UI-SPEC Copywriting Contract — particularly "Algo deu errado. Tente novamente." for accept/reject, which the UI-SPEC explicitly mandates be reused verbatim from `FollowButton`.

### Frontend: Spinner reuse
**Source:** `Spinner.jsx` (whole file)
**Apply to:** loading states across `NotificationsPage` (28px initial / 24px pagination) and `NotificationCard` (16px inline action)
```jsx
<Spinner size={28} aria-label="Carregando notificações..." />  // page-level (note: Spinner.jsx ignores aria-label prop today — it hardcodes 'Carregando'; if a distinct label is required, this is a pre-existing limitation worth flagging to the planner, not silently working around)
<Spinner size={24} />   // pagination
<Spinner size={16} />   // inline button (replaces label text, FollowButton.jsx line 61/78/97 pattern)
```

---

## No Analog Found

| File | Role | Data Flow | Reason |
|------|------|-----------|--------|
| `MINIREDESOCIAL_FRONT/src/hooks/useNotificationPolling.js` | hook | event-driven (setInterval polling) | No `hooks/` directory or standalone custom hook exists yet — `useScrollDirection` is the only custom hook, defined inline inside `AppShell.jsx`. This is the first **polling** pattern in the codebase (chat will use Socket.io, not polling, per 05-CONTEXT "Established Patterns"). Use `useScrollDirection`'s `useRef`+`useEffect`+cleanup shape as the structural reference, and `setInterval`/`clearInterval` as the new mechanism (see Pattern Assignment above for full adapted code). Decide at plan time whether to extract it to `src/hooks/` (new directory) or co-locate in `AppShell.jsx` like `useScrollDirection` — `src/hooks/` is recommended since the hook's state (`unreadCount`) needs to be consumed by both the bell badge AND potentially the page itself, making a standalone, importable module cleaner. |

---

## Metadata

**Analog search scope:** `src/controllers/`, `src/routes/`, `src/services/`, `src/models/`, `src/middlewares/`, `MINIREDESOCIAL_FRONT/src/pages/`, `MINIREDESOCIAL_FRONT/src/components/`, `MINIREDESOCIAL_FRONT/src/lib/`
**Files scanned:** ~30 (controllers, routes, services, models, middlewares, frontend pages/components, lib)
**Pattern extraction date:** 2026-06-07
