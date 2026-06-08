---
phase: 05-notifications-polling
verified: 2026-06-08T00:00:00Z
status: passed
score: 9/9 must-haves verified
overrides_applied: 0
---

# Phase 5: Notifications (Polling) Verification Report

**Phase Goal:** Users receive and manage in-app notifications for social events, dispatched cleanly by a notification service and surfaced via polling on mobile
**Verified:** 2026-06-08
**Status:** passed
**Re-verification:** No — initial verification

## Goal Achievement

### Observable Truths (ROADMAP Success Criteria + PLAN must_haves merged)

| # | Truth | Status | Evidence |
|---|-------|--------|----------|
| 1 | Notifications are stored in the DB with a `read` flag and polymorphic `refId`/`refModel` (SC1/NOTF-01) | VERIFIED | `src/models/Notification.js` defines `userId`, `actorId`, `type` (6-value enum), `refId` (ObjectId), `refModel` (String), `read` (Boolean default false), `createdAt`, plus compound index `{userId:1, read:1, createdAt:-1}` |
| 2 | Notifications are dispatched through a notification service, never inline in controllers (SC2/NOTF-02) | VERIFIED | `src/services/notification.service.js` exports `create(type, actorId, userId, refId, refModel)`, fire-and-forget (swallows its own errors, `try/catch` with no rethrow). All 4 dispatch sites (`follow.controller.js:39,93`, `comment.controller.js:38`, `registration.controller.js:90`) call `notificationService.create(...)`, none construct `Notification` documents inline |
| 3 | The supported event types generate notifications when those events occur (SC3/NOTF-03) | VERIFIED | 5 of 6 enum types wired: `new_follower`/`follow_request` (`follow.controller.js:39-46`, conditional on `target.isPrivate`), `follow_accepted` (`follow.controller.js:93-99`), `post_comment` (`comment.controller.js:38`, fixed in `c6063bd` to use `post._id`/`'Post'`), `invite_received` (`registration.controller.js:90`, fires `invite.invitedById` as recipient). `message_received` is intentionally reserved for Phase 6 (Chat) — matches ROADMAP phase boundary, not a Phase 5 gap |
| 4 | A user can fetch their notifications and mark them read from the mobile notifications view (polling) (SC4/NOTF-04) | VERIFIED | `GET /api/notifications` (`notification.controller.js:listNotifications`) returns `{notifications, hasMore, nextCursor, unreadCount}` scoped by `userId: req.user.id`; `PATCH /api/notifications/:id/read` (`markRead`) flips `read=true` with 404/403/200 IDOR guard. Frontend `NotificationsPage.jsx` calls both via `api.get`/`api.patch`; 5 backend tests assert isolation + ownership + persistence, all passing |
| 5 | The bell icon shows a numeric unread badge that stays live app-wide via 5s polling | VERIFIED | `useNotificationPolling.js` (`POLL_INTERVAL_MS = 5000`, `setInterval`, fire-and-forget `api.get('/api/notifications', {params:{limit:1}})` reading `unreadCount`); `AppShell.jsx:35` calls the hook, derives `hasUnread`/`badgeLabel`, renders `.badge` span + sr-only text + accent-colored Bell |
| 6 | The notifications page lists the user's notifications, most recent first, with avatar + actor name + action text + timestamp | VERIFIED | `NotificationsPage.jsx` fetches `/api/notifications` (server sorts `_id: -1`), renders `<NotificationCard>` per item; `NotificationCard.jsx` renders `<img src={actorId.avatarUrl}>`, `<strong>{actorId.name}</strong>`, `getActionText(type)` (5 mapped Portuguese strings + fallback), `formatRelativeTime(createdAt)` |
| 7 | Tapping a notification navigates to the related content AND marks it read | VERIFIED | `handleClick` in `NotificationCard.jsx:39-46`: `if (!read) onRead(_id)` then `navigate('/posts/${refId}')` for `post_comment` or `navigate('/profile/${actorId._id}')` for follow/invite types — both fixed post-checkpoint (commits `c6063bd`, `5121ef0`) to use directly-navigable ids matching real route param names (`/posts/:postId`, `/profile/:userId`) |
| 8 | Unread notifications are visually distinct (tinted background + dot); read are default style | VERIFIED | `NotificationCard.jsx:97` conditionally applies `styles.unread` + renders `<span className={styles.unreadDot}>` when `!read`; `NotificationCard.module.css` defines `.unread {background: rgba(140,110,79,0.08)}` and `.unreadDot` (8px circle, accent background) |
| 9 | follow_request cards show inline Aceitar/Rejeitar that call the follows endpoint, mark read, resolve in-place to a status label, and don't bleed into card navigation | VERIFIED | `NotificationCard.jsx:60-117`: action row gated on `type === 'follow_request' && status === 'pending'`, wrapped in `onClick={(e) => e.stopPropagation()}`; `handleAccept`/`handleReject` call `api.patch('/api/follows/${refId}/accept|reject')` then `api.patch('/api/notifications/${_id}/read')`, set `status` to `'accepted'`/`'rejected'`, render `.statusLabel` ("Aceito"/"Rejeitado") in place of buttons; checkpoint approved live by user with verbatim quote confirming "a questão da notificação aceitar funcionou" |

**Score:** 9/9 truths verified

### Required Artifacts

| Artifact | Expected | Status | Details |
|----------|----------|--------|---------|
| `src/controllers/notification.controller.js` | listNotifications + markRead, userId-scoped | VERIFIED | Both exported (confirmed via require), `userId: req.user.id` scoping, `.populate('actorId', 'name avatarUrl')`, `countDocuments`, ownership `.toString() !== req.user.id` → 403, zero `institutionId` in queries |
| `src/routes/notification.routes.js` | GET / and PATCH /:id/read behind authenticate | VERIFIED | `router.get('/', authenticate, listNotifications)`, `router.patch('/:id/read', authenticate, markRead)` |
| `src/controllers/notification.controller.test.js` | ownership + pagination + mark-read tests | VERIFIED | 5 tests, all passing: list scoping/ordering/unreadCount, cross-user isolation, markRead 403/404/200 |
| `MINIREDESOCIAL_FRONT/src/hooks/useNotificationPolling.js` | 5000ms polling hook | VERIFIED | `setInterval(fetchUnreadCount, POLL_INTERVAL_MS)` with `POLL_INTERVAL_MS = 5000`, returns `{unreadCount, refetch}` |
| `MINIREDESOCIAL_FRONT/src/pages/NotificationsPage/NotificationsPage.jsx` | paginated list w/ loading/empty/error | VERIFIED | 113 lines; cursor pagination, IntersectionObserver, Spinner/ErrorBanner/empty-state all present |
| `MINIREDESOCIAL_FRONT/src/components/NotificationCard/NotificationCard.jsx` | row, tap-to-navigate + mark-read | VERIFIED | `formatRelativeTime`, `handleClick`, `onRead`, unread styling, accept/reject row all present |

### Key Link Verification

| From | To | Via | Status | Details |
|------|-----|-----|--------|---------|
| `notification.routes.js` | `notification.controller.js` | require + handlers | WIRED | `const { listNotifications, markRead } = require('../controllers/notification.controller')` + both mounted |
| `app.js` | `notification.routes.js` | app.use mount | WIRED | `app.use('/api/notifications', require('./routes/notification.routes'))` (line 34) |
| `registration.controller.js` | `notification.service.js` | `create('invite_received', ...)` | WIRED | Line 90, fires after `invite.save()` (line 87), before `res.status(201)` (line 93) |
| `AppShell.jsx` | `useNotificationPolling.js` | hook call at shell level | WIRED | `const { unreadCount } = useNotificationPolling()` (line 35) |
| `useNotificationPolling.js` | `/api/notifications` | `api.get` for unreadCount | WIRED | `api.get('/api/notifications', { params: { limit: 1 } })`, sets `unreadCount` from `res.data.unreadCount` |
| `NotificationCard.jsx` | `/api/notifications/:id/read` | `api.patch` (via `onRead` lifted to `NotificationsPage`, and directly in accept/reject) | WIRED | `NotificationsPage.handleMarkRead` does `api.patch('/api/notifications/${id}/read')`; `NotificationCard` also calls it directly post-accept/reject (`api.patch('/api/notifications/${_id}/read')`) — both confirmed live in source |
| `NotificationCard.jsx` | `/api/follows/:id/accept\|reject` | `api.patch` using `refId` | WIRED | `api.patch('/api/follows/${refId}/accept')` / `.../reject`, both followed by mark-read patch |

**Note on plan-internal grep mismatches (documented honestly in both 05-02 and 05-03 SUMMARYs):** The plans' literal `<verify>` grep patterns expected `notification.refId`/`notification._id` and a direct PATCH in `NotificationCard.jsx`; the actual code uses destructured `refId`/`_id` bindings and lifts the primary mark-read call to `NotificationsPage` (an architecturally correct "lift state up" pattern). I traced both call chains directly in source and confirmed they are functionally identical and correctly wired — these are cosmetic verify-string mismatches, not defects.

### Data-Flow Trace (Level 4)

| Artifact | Data Variable | Source | Produces Real Data | Status |
|----------|---------------|--------|--------------------|--------|
| `NotificationsPage.jsx` | `items` (rendered list) | `api.get('/api/notifications', {params})` → `Notification.find({userId}).populate('actorId')` (real Mongoose query, not static) | Yes | FLOWING |
| `AppShell.jsx` badge | `unreadCount` | `useNotificationPolling` → `api.get(...)` → `Notification.countDocuments({userId, read:false})` (real DB count) | Yes | FLOWING |
| `NotificationCard.jsx` | `notification` prop | passed from `NotificationsPage.items` (real fetched docs, populated `actorId`) | Yes | FLOWING |

### Behavioral Spot-Checks

| Behavior | Command | Result | Status |
|----------|---------|--------|--------|
| Backend notification test suite passes | `node --test src/controllers/notification.controller.test.js` | 5/5 pass (list scoping, isolation, markRead 403/404/200) | PASS |
| Full backend suite green (no regressions) | `npm test` | 78/78 pass | PASS |
| Frontend production build succeeds | `npm run build` (MINIREDESOCIAL_FRONT) | "1613 modules transformed... ✓ built in 1.49s", exit 0 | PASS |
| `require('./src/app')` loads with notification mount | inspected `app.js:34` directly + `node --test` suite (which boots the app) succeeds | `app.use('/api/notifications', ...)` present and the app boots cleanly across 78 tests | PASS |

### Probe Execution

No `scripts/*/tests/probe-*.sh` probes declared in this phase's plans or summaries; none found via `find scripts -path '*/tests/probe-*.sh'`. SKIPPED — not applicable to this phase type (vertical-slice feature work, not migration/tooling).

### Requirements Coverage

| Requirement | Source Plan | Description | Status | Evidence |
|-------------|------------|-------------|--------|----------|
| NOTF-01 | 05-01 | Notifications stored with `read` flag + polymorphic `refId`/`refModel` | SATISFIED | `Notification.js` schema (pre-existing per plan, confirmed present and correctly shaped) |
| NOTF-02 | 05-01 | Notifications dispatched via a notification service, never inline | SATISFIED | All 4 dispatch sites use `notificationService.create`; service is fire-and-forget, swallows own errors |
| NOTF-03 | 05-01 | All 6 supported types generate notifications on their events | SATISFIED | 5/6 wired (new_follower, follow_request, follow_accepted, post_comment, invite_received); `message_received` is explicitly Phase 6 (Chat) scope per ROADMAP — not a Phase 5 gap |
| NOTF-04 | 05-01, 05-02, 05-03 | User can fetch notifications and mark them read (polling) | SATISFIED | GET/PATCH endpoints live, tested, consumed end-to-end by polling hook + NotificationsPage + NotificationCard incl. inline accept/reject |
| FRNT-06 | 05-02, 05-03 | (Frontend notifications view requirement, cross-referenced via plan frontmatter) | SATISFIED | NotificationsPage + NotificationCard fully implement the notifications view per UI-SPEC, checkpoint-approved by user |

No orphaned requirements found — all 4 phase requirement IDs (NOTF-01 through NOTF-04) plus FRNT-06 are claimed across the 3 plans and have implementation evidence.

### Anti-Patterns Found

| File | Line | Pattern | Severity | Impact |
|------|------|---------|----------|--------|
| `src/controllers/notification.controller.js` | 4 | Unused `User` import (`const User = require('../models/User')`, never referenced) | Info (from REVIEW WR-04) | Dead code; no functional impact — orphan import that should be removed in a follow-up cleanup |
| `MINIREDESOCIAL_FRONT/src/components/NotificationCard/NotificationCard.jsx` | 35 | `useState(notification.status ?? 'pending')` — `notification.status` is never returned by the API (REVIEW WR-02), so the `??` fallback is always taken; stale already-resolved follow_request notifications will still render live Aceitar/Rejeitar buttons that 400 on submit | Warning (from REVIEW) | Real logic gap surfaced by the independent code review; does not block the phase goal (the happy path works and was checkpoint-approved), but is a legitimate latent bug for users who resolve a follow request through another channel before opening /notifications |
| `src/controllers/comment.controller.js` (pre-fix data) | n/a | Stale `post_comment` notifications created before commit `c6063bd` retain `refModel:'Comment'`/comment-id `refId` and will mis-navigate (REVIEW WR-05, acknowledged in 05-02 SUMMARY) | Warning (from REVIEW) | Affects only pre-existing demo-seed data created before the fix; new notifications are correct. No migration shipped — explicitly flagged by both the executor and reviewer as a known, accepted gap for a pre-launch dataset |

No `TBD`/`FIXME`/`XXX`/`TODO`/`HACK`/`PLACEHOLDER` debt markers found in any file modified by this phase (backend or frontend) — debt-marker gate clear.

These three items (WR-04, WR-02, WR-05) mirror exactly the REVIEW.md findings (0 critical / 5 warning / 4 info) — the review is advisory and does not block goal achievement; none of them prevent any of the 9 observable truths from being true in normal operation, and the checkpoint-approved happy-path flows (list, badge, navigate+mark-read, accept/reject) all function correctly per live human verification.

### Human Verification Required

None outstanding. Both `checkpoint:human-verify` gates (plan 05-02 and 05-03) were executed during the phase and approved by the user with verbatim quotes recorded in the SUMMARYs:

- **05-02 checkpoint:** Bug-fix continuation found and fixed two navigation defects (`c6063bd`, `5121ef0`) discovered during live testing; final state confirmed working.
- **05-03 checkpoint:** User tested live with two real accounts and replied (verbatim, Portuguese): *"funcionou, porém um problema que notei: eu já sigo um usuario X, ao pesquisar o nome dele na pesquisa, aparece o botão seguir mesmo eu já sendo seguir dele e dá erro, mas a questão da notificação aceitar funcionou"* — explicitly confirming "a questão da notificação aceitar funcionou" (the notification-accept thing worked). The unrelated search/follow-button bug the user also surfaced is out of this phase's scope (does not touch `NotificationCard`/search code) and has been recorded separately for triage.

### Gaps Summary

No blocking gaps. All 9 observable truths (4 ROADMAP success criteria + 5 phase-level behaviors derived from PLAN must_haves) are verified against the actual codebase — both backend (this repo) and frontend (sibling `MINIREDESOCIAL_FRONT` repo) — with end-to-end wiring confirmed by tracing real call chains (not just grep matches), 78/78 backend tests passing, frontend production build succeeding, and both human-verify checkpoints approved live by the user.

The independent code review (05-REVIEW.md) surfaced 0 critical / 5 warning / 4 info findings. Three of these (WR-02, WR-04, WR-05) are reflected in the Anti-Patterns table above as honest disclosure — they represent real, narrow latent bugs (stale follow_request UI state, an unused import, and pre-fix stale notification data) that a future cleanup pass should address, but none of them prevent the phase goal — "Users receive and manage in-app notifications for social events, dispatched cleanly by a notification service and surfaced via polling" — from being observably true today. The happy-path flows that constitute that goal (receive, list, badge, navigate, mark-read, accept/reject) all work and were confirmed live.

---

_Verified: 2026-06-08_
_Verifier: Claude (gsd-verifier)_
