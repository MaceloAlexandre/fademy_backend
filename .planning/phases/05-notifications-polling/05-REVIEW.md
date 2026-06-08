---
phase: 05-notifications-polling
reviewed: 2026-06-08T13:39:18Z
depth: standard
files_reviewed: 11
files_reviewed_list:
  - src/controllers/notification.controller.js
  - src/routes/notification.routes.js
  - src/controllers/notification.controller.test.js
  - src/app.js
  - src/controllers/registration.controller.js
  - src/controllers/comment.controller.js
  - MINIREDESOCIAL_FRONT/src/hooks/useNotificationPolling.js
  - MINIREDESOCIAL_FRONT/src/components/AppShell/AppShell.jsx
  - MINIREDESOCIAL_FRONT/src/components/AppShell/AppShell.module.css
  - MINIREDESOCIAL_FRONT/src/App.jsx (route diff only)
  - MINIREDESOCIAL_FRONT/src/pages/NotificationsPage/NotificationsPage.jsx
  - MINIREDESOCIAL_FRONT/src/pages/NotificationsPage/NotificationsPage.module.css
  - MINIREDESOCIAL_FRONT/src/components/NotificationCard/NotificationCard.jsx
  - MINIREDESOCIAL_FRONT/src/components/NotificationCard/NotificationCard.module.css
findings:
  critical: 0
  warning: 5
  info: 4
  total: 9
status: issues_found
---

# Phase 05: Code Review Report

**Reviewed:** 2026-06-08T13:39:18Z
**Depth:** standard
**Files Reviewed:** 14
**Status:** issues_found

## Summary

Phase 05 ships notification polling end-to-end: a `GET /api/notifications` cursor-paginated list endpoint, a `PATCH /api/notifications/:id/read` IDOR-guarded mark-read endpoint, an `invite_received` trigger on registration, a 5s-polling unread-badge hook, a `/notifications` page, a `NotificationCard` component, and inline Aceitar/Rejeitar actions for `follow_request` cards.

The core multi-tenant/IDOR scoping is correct: `listNotifications` and `markRead` scope strictly by `userId: req.user.id` (the model genuinely has no `institutionId`, confirmed in `Notification.js`), and `markRead` follows the standard load→404→ownership-check→403→mutate→200 IDOR-guard shape that mirrors `follow.controller.acceptFollow`. The `invite_received` trigger fires with the correct actor/recipient/refId/refModel argument order. The `refId`/`refModel` polymorphic reference inconsistency that the summaries describe (comment id vs. post id for `post_comment`, and Follow-doc id vs. User id for follow-family types) was real, and the fixes applied (storing `post._id`/`'Post'` at the source, and navigating via `actorId._id` on the client for follow-family types) are both correct and consistent with the data shapes actually produced by `follow.controller.js` and `registration.controller.js`.

That said, several real defects remain, mostly around: (1) a stale-closure double-submit risk and a misleading dead `useState` initializer in the new Aceitar/Rejeitar handlers, (2) an unused `User` import left in the controller, (3) `unreadCount` semantics drifting from "badge count" to "all unread including ones the user has already opened/dismissed via the list", and (4) the pre-existing-data caveat called out in the 05-02 summary (stale `post_comment` notifications mis-navigating) which is acknowledged but unmitigated. None of these rise to data-loss or cross-tenant-leak severity, but several are real logic bugs that will surface in normal usage.

## Narrative Findings (AI reviewer)

### WR-01: Aceitar/Rejeitar handlers can double-fire on rapid double-click before React commits `loading` state

**File:** `MINIREDESOCIAL_FRONT/src/components/NotificationCard/NotificationCard.jsx:54-67` and `:69-82`
**Issue:** `handleAccept`/`handleReject` call `setLoading(true)` as their *first* state update, then `await`. React batches/schedules state updates asynchronously — `loading` does not become `true` synchronously within the same event-loop tick as the click handler invocation. Two rapid clicks (or a click plus an Enter/Space keydown bubbling — though `stopPropagation` prevents the keydown path here) dispatched before the first re-render commits can both pass the `disabled={loading}` gate and both fire `api.patch(/api/follows/:id/accept)`. The Phase 3 `acceptFollow` guard (`followDoc.status !== 'pending'` → 400) will catch the second call server-side, but the client will then show the generic `actionError` banner ("Algo deu errado. Tente novamente.") for what is actually a successful accept on the first call — a confusing false-failure UX, and two network round trips for one user action.
**Fix:** Use a `useRef` guard (synchronous, not subject to batching) in addition to/instead of `loading` state for the re-entrancy check:
```js
const submittingRef = useRef(false)

async function handleAccept() {
  if (submittingRef.current) return
  submittingRef.current = true
  setActionError('')
  setLoading(true)
  try {
    await api.patch(`/api/follows/${refId}/accept`)
    await api.patch(`/api/notifications/${_id}/read`)
    setStatus('accepted')
    onRead(_id)
  } catch {
    setActionError('Algo deu errado. Tente novamente.')
  } finally {
    setLoading(false)
    submittingRef.current = false
  }
}
```

### WR-02: `useState(notification.status ?? 'pending')` initializes from a field the API never returns — dead/misleading code that masks a real default-state question

**File:** `MINIREDESOCIAL_FRONT/src/components/NotificationCard/NotificationCard.jsx:34`
**Issue:** `Notification` documents (per `src/models/Notification.js`) have no `status` field — only `Follow` documents do (`pending`/`accepted`/`rejected`... actually `pending`/`accepted` per `follow.controller.js:36`). `GET /api/notifications` populates `actorId` only (`notification.controller.js:28`); it never returns `notification.status`. So `notification.status` is always `undefined`, and this expression always evaluates to `'pending'`. The `?? 'pending'` fallback reads as if it's handling a real "resolved" initial state (e.g., resuming a page reload after a prior accept/reject), but it can never do so — every card mounts as `'pending'` regardless of whether the underlying Follow request was already resolved by another channel (e.g., the user accepted from the Follow Requests list elsewhere, or a second device). This means a `follow_request` notification for an *already-resolved* Follow doc still renders live Aceitar/Rejeitar buttons, and clicking them will hit the server's `followDoc.status !== 'pending'` 400 guard, surfacing the generic error banner for what is actually stale UI state.
**Fix:** Either remove the misleading fallback (`useState('pending')`) to be honest that this is always the initial value, or — better — actually solve the staleness problem by deriving initial status from server data, e.g. have the list endpoint populate `refId` for `follow_request` types (or a lightweight `followStatus` projection) so the card can render the correct resolved state on load instead of presenting stale action buttons:
```js
// If the contract can't be changed cheaply, at minimum stop implying it's wired:
const [status, setStatus] = useState('pending')
```

### WR-03: `unreadCount` returned by `listNotifications` can diverge from the AppShell badge's intended "items needing attention" semantics once a user opens `/notifications`

**File:** `src/controllers/notification.controller.js:34`, `MINIREDESOCIAL_FRONT/src/hooks/useNotificationPolling.js:9-16`, `MINIREDESOCIAL_FRONT/src/pages/NotificationsPage/NotificationsPage.jsx:57-62`
**Issue:** `unreadCount` is computed server-side as `countDocuments({ userId, read: false })` — a simple "how many are unread" count, recomputed fresh on every poll. `NotificationsPage.handleMarkRead` optimistically flips `read: true` locally and fires a non-blocking PATCH; if that PATCH silently fails (network blip, 500, etc. — caught and only surfaces a banner, `:59-61`), the server-side `read` flag remains `false`. The next 5-second poll then re-reads `unreadCount` from the server (still counting that notification as unread) and the AppShell badge "resurrects" a count the user believes they already cleared by viewing/tapping the item — confusing, because the locally-rendered card already shows as read (no dot, no `unread` background) while the badge simultaneously claims otherwise. This is a real (if narrow) state-divergence bug introduced by the optimistic-update + fire-and-forget-PATCH + independent-poll combination, not merely a style nit.
**Fix:** On `handleMarkRead` PATCH failure, revert the optimistic local flip (so the card's read/unread visual state matches what the server actually persisted) rather than leaving it permanently "read" in the UI while the server disagrees:
```js
const handleMarkRead = useCallback((id) => {
  setItems((prev) => prev.map((n) => (n._id === id ? { ...n, read: true } : n)))
  api.patch(`/api/notifications/${id}/read`).catch(() => {
    setReadError('Não foi possível marcar como lida. Tente novamente.')
    setItems((prev) => prev.map((n) => (n._id === id ? { ...n, read: false } : n)))
  })
}, [])
```

### WR-04: Unused `User` import in `notification.controller.js`

**File:** `src/controllers/notification.controller.js:4`
**Issue:** `const User = require('../models/User');` is imported but never referenced anywhere in the file (`listNotifications` and `markRead` only touch `Notification`, and `actorId` is populated by ref-string `'actorId'` rather than via an explicit `User` model query). This is dead code left over from drafting (likely copy-pasted from a sibling controller) — it adds a misleading signal that the controller queries `User` directly, and is the kind of orphan import CLAUDE.md's "Surgical Changes" section calls out as something the author should remove.
**Fix:**
```js
'use strict';

const Notification = require('../models/Notification');
// remove: const User = require('../models/User');
```

### WR-05: Stale `post_comment` notifications created before the `c6063bd` fix will continue to mis-navigate indefinitely — acknowledged risk with no mitigation shipped

**File:** `src/controllers/comment.controller.js:38` (fix point); affects `MINIREDESOCIAL_FRONT/src/components/NotificationCard/NotificationCard.jsx:40-41`
**Issue:** Prior to commit `c6063bd`, `post_comment` notifications were created with `refId = comment._id, refModel = 'Comment'`. `NotificationCard.handleClick` navigates `post_comment` types to `/posts/${refId}` (line 41), assuming `refId` is a Post id. Any notification document persisted before the fix retains the old (wrong) shape — clicking it still sends the user to `/posts/<comment-id>`, which 404s as "erro ao carregar publicação". The 05-02 summary explicitly flags this as a known, unmitigated gap ("Any `post_comment` notifications already persisted... will still mis-navigate until they age out... no migration was written"). For a pre-launch demo dataset this may be acceptable, but it is a real, reproducible bug a reviewer/demo audience can trigger by clicking an old notification, and there's no guard in the click handler to detect/skip a malformed `refId`.
**Fix:** Either run a one-time data migration to rewrite existing `post_comment` notifications' `refId`/`refModel` from `Comment`/comment-id to `Post`/post-id (a `Comment.findById(refId)` → `comment.postId` backfill), or add a defensive check before navigating (e.g., only navigate if `refModel === 'Post'`, otherwise no-op or fall back to the actor's profile). A short migration script is the more correct fix and is not "speculative complexity" — it directly repairs data the shipped code already corrupted:
```js
// one-time backfill, run once against prod data
const stale = await Notification.find({ type: 'post_comment', refModel: 'Comment' });
for (const n of stale) {
  const comment = await Comment.findById(n.refId);
  if (comment) {
    n.refId = comment.postId;
    n.refModel = 'Post';
    await n.save();
  }
}
```

## Info

### IN-01: `useNotificationPolling` ignores `limit: 1` payload cost and re-fetches the full notifications shape on every 5s tick just to read `unreadCount`

**File:** `MINIREDESOCIAL_FRONT/src/hooks/useNotificationPolling.js:9-16`
**Issue:** The hook calls `GET /api/notifications?limit=1` purely to read `res.data.unreadCount`, discarding `notifications`, `hasMore`, and `nextCursor`. The endpoint still does a full `find().limit(2).populate()` plus a separate `countDocuments` on every poll tick (every 5 seconds, for every logged-in user, app-wide per the AppShell mount). This is flagged as Info rather than Warning because the task description explicitly places performance concerns out of scope for v1 — but a dedicated `GET /api/notifications/unread-count` (or similar lightweight endpoint) would be the more correct contract shape for a polling consumer that only needs a scalar.
**Fix:** Consider a lightweight count-only endpoint for the badge poll; not required for this phase to ship, but worth tracking for when polling frequency or user count grows.

### IN-02: `formatRelativeTime` has no upper bound — durations beyond 99 days render as raw day counts with no "weeks/months/years" tier

**File:** `MINIREDESOCIAL_FRONT/src/components/NotificationCard/NotificationCard.jsx:8-17`
**Issue:** The function returns `${days}d` for any value ≥ 24 hours, with no ceiling — a year-old notification renders as `365d`. This is a minor display-polish gap rather than a functional bug (no crash, no wrong data), and may be acceptable for a fresh demo dataset where no notification is more than a few days old, but it will look unpolished as soon as real usage accumulates history.
**Fix:** Add a final tier, e.g. `if (days < 30) return `${days}d`; const months = Math.floor(days / 30); if (months < 12) return `${months}mês`... ` etc. — cosmetic, low priority.

### IN-03: `getActionText` fallback string is in Portuguese-but-generic and doesn't match any defined `type`, masking future schema/type drift silently

**File:** `MINIREDESOCIAL_FRONT/src/components/NotificationCard/NotificationCard.jsx:19-29`
**Issue:** `ACTION_TEXT` enumerates five of the six `type` enum values from `Notification.js` (`new_follower`, `follow_request`, `follow_accepted`, `post_comment`, `invite_received`) but omits `message_received` (present in the model's enum, presumably reserved for the chat phase). `getActionText` falls back to `'Você recebeu uma mensagem'` ("You received a message") for any unmapped type — which happens to be a plausible message for `message_received` specifically, but would also silently apply to any *other* future/unexpected type value, producing a misleading label. This is low-severity (the enum is closed server-side and `message_received` notifications presumably aren't created yet), but worth a comment or an explicit `message_received` entry so the fallback's intent is clear and doesn't accidentally mislabel a genuinely-unknown type.
**Fix:**
```js
const ACTION_TEXT = {
  new_follower: 'começou a seguir você',
  follow_request: 'quer seguir você',
  follow_accepted: 'aceitou sua solicitação de follow',
  post_comment: 'comentou no seu post',
  invite_received: 'se cadastrou usando seu convite',
  message_received: 'enviou uma mensagem para você',
}

function getActionText(type) {
  return ACTION_TEXT[type] ?? 'interagiu com você'
}
```

### IN-04: `markRead` and `listNotifications` have no protection against malformed `:id` / `cursor` causing a Mongoose `CastError` to bubble to the generic 500 handler

**File:** `src/controllers/notification.controller.js:18, 49`
**Issue:** `Notification.findById(req.params.id)` (line 49) and the cursor filter `query._id = { $lt: req.query.cursor }` (line 18) both pass client-controlled strings directly into Mongoose ObjectId-typed fields. A non-ObjectId string (e.g. `PATCH /api/notifications/not-an-id/read` or `?cursor=garbage`) throws a `CastError`, which is caught by the generic `catch` and returned as a `500 Internal server error` — masking what is really a `400 Bad Request` / malformed-input case as a server fault. This is flagged Info (not Warning) because it is the **pre-existing pattern across the entire codebase** — `grep` confirms zero `CastError`/`ObjectId.isValid` handling in `follow.controller.js`, `post.controller.js`, or `comment.controller.js` either — so this phase is consistent with established conventions, not introducing a regression. Still worth tracking as a cross-cutting improvement.
**Fix (if/when addressed codebase-wide, not specific to this phase):**
```js
const mongoose = require('mongoose');
if (!mongoose.Types.ObjectId.isValid(req.params.id)) {
  return res.status(404).json({ error: 'Notification not found' });
}
```

---

_Reviewed: 2026-06-08T13:39:18Z_
_Reviewer: Claude (gsd-code-reviewer)_
_Depth: standard_
