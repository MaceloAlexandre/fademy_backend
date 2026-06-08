---
phase: 05-notifications-polling
plan: 02
subsystem: ui
tags: [react, notifications, polling, navigation, mongoose, express]

# Dependency graph
requires:
  - phase: 05-notifications-polling/05-01
    provides: GET /api/notifications and PATCH /api/notifications/:id/read endpoints, Notification model (userId/actorId/type/refId/refModel/read)
provides:
  - Live unread-count badge on the AppShell bell (5s polling, app-wide)
  - /notifications route + NotificationsPage (paginated list, loading/empty/error states)
  - NotificationCard (avatar + actor name + action text + relative timestamp, unread styling, tap-to-navigate + mark-read)
  - Corrected refId semantics for post_comment notifications (post id, not comment id)
  - Corrected client-side navigation target resolution (actor profile vs. post)
affects: [05-03 (follow_request inline accept/reject), chat/messaging notifications]

# Tech tracking
tech-stack:
  added: []
  patterns:
    - "Polling hook owns count + refetch, mounted once at shell level (D-09), fire-and-forget errors"
    - "Notification refId stores a directly-navigable target id; refModel documents which collection it points to"
    - "Card navigation resolves target from actorId._id (populated actor) for person-centric notification types, and from refId for content-centric types (post_comment -> post id)"

key-files:
  created:
    - MINIREDESOCIAL_FRONT/src/hooks/useNotificationPolling.js
    - MINIREDESOCIAL_FRONT/src/pages/NotificationsPage/NotificationsPage.jsx
    - MINIREDESOCIAL_FRONT/src/pages/NotificationsPage/NotificationsPage.module.css
    - MINIREDESOCIAL_FRONT/src/components/NotificationCard/NotificationCard.jsx
    - MINIREDESOCIAL_FRONT/src/components/NotificationCard/NotificationCard.module.css
  modified:
    - MINIREDESOCIAL_FRONT/src/components/AppShell/AppShell.jsx
    - MINIREDESOCIAL_FRONT/src/components/AppShell/AppShell.module.css
    - MINIREDESOCIAL_FRONT/src/App.jsx
    - src/controllers/comment.controller.js (backend — refId/refModel fix for post_comment)

key-decisions:
  - "post_comment notifications now store the post's _id (refModel: 'Post') as refId instead of the comment's _id (refModel: 'Comment') — refId is meant to be a directly-navigable target id, matching the existing invite_received convention (refId = navigable User id)"
  - "Follow-family notifications (new_follower, follow_request, follow_accepted) and invite_received navigate using the populated actorId._id rather than refId, because refId for these types points at a Follow document (not a User) — the navigable profile is always the actor"

patterns-established:
  - "When adding new notification types whose refId references a non-directly-navigable document (e.g. a join/junction record like Follow), prefer navigating via the populated actorId, or change refId/refModel to store a directly-navigable id"

requirements-completed: [NOTF-04, FRNT-06]

# Metrics
duration: ~50min
completed: 2026-06-08
---

# Phase 05 Plan 02: Notification consumption frontend (polling badge, list, navigation) Summary

**Live 5s-polling unread badge on the bell, a /notifications list view with avatar/action/timestamp cards, and tap-to-navigate-and-mark-read — plus a post-checkpoint fix for two navigation bugs (wrong document id stored/used as the navigation target for comment and follow notifications).**

## Performance

- **Duration:** ~50 min (across two execution sessions — initial build + bug-fix continuation)
- **Tasks:** 3 (2 build tasks + 1 human-verify checkpoint, the latter requiring a follow-up fix)
- **Files modified:** 9 (5 created, 4 modified — 3 frontend build files + 1 backend fix + 1 frontend fix; AppShell/App.jsx counted under "modified")

## Accomplishments
- Bell icon shows a live numeric unread badge (polls every 5s app-wide via `useNotificationPolling`), turns accent-colored when unread > 0, and navigates to `/notifications` on tap
- `/notifications` route renders a paginated, cursor-based list of notification cards (avatar, bold actor name, type-specific Portuguese action text, relative timestamp), with loading/empty/error states matching FeedPage conventions
- Tapping a card optimistically marks it read (PATCH, non-blocking on failure) and navigates to the related content
- **Fixed two navigation bugs found during manual verification**: comment notifications pointed at the wrong document type (comment id used as post id) and follow/invite notifications pointed at a Follow-document id instead of the actor's user id — both produced error pages ("erro ao carregar publicação" / "usuário não encontrado")

## Task Commits

Each task was committed atomically:

1. **Task 1: useNotificationPolling hook + AppShell bell wiring + badge + route** - `3d668f1` (feat) — committed by the prior executor in MINIREDESOCIAL_FRONT main
2. **Task 2: NotificationsPage + NotificationCard (display, tap-to-navigate, mark-read)** - `f709521` (feat) — committed by the prior executor in MINIREDESOCIAL_FRONT main
3. **Task 3: checkpoint:human-verify — bug fixes from manual verification:**
   - `c6063bd` (fix, miniredesocial backend) — store post id (not comment id) as `refId` for `post_comment` notifications
   - `5121ef0` (fix, MINIREDESOCIAL_FRONT) — navigate to `actorId._id` (not `refId`) for follow/invite notification types

**Plan metadata:** (this commit, miniredesocial — `docs(05-02): complete notification consumption frontend plan`)

## Files Created/Modified

### Created (Tasks 1-2, by prior executor — commits 3d668f1 / f709521)
- `MINIREDESOCIAL_FRONT/src/hooks/useNotificationPolling.js` - 5000ms polling hook; owns `unreadCount` + `refetch`, fire-and-forget error swallowing, mounted at AppShell level (D-09)
- `MINIREDESOCIAL_FRONT/src/pages/NotificationsPage/NotificationsPage.jsx` - cursor-paginated notification list adapted from FeedPage (IntersectionObserver load-more, loading/empty/error states, optimistic mark-read with non-blocking error banner)
- `MINIREDESOCIAL_FRONT/src/pages/NotificationsPage/NotificationsPage.module.css` - layout rules copied from FeedPage conventions (`.page/.spinnerCenter/.list/.spinnerMore/.emptyState/.sentinel`)
- `MINIREDESOCIAL_FRONT/src/components/NotificationCard/NotificationCard.jsx` - single notification row: avatar, bold actor name, type-mapped Portuguese action text, `formatRelativeTime`, unread tint+dot, keyboard-accessible tap-to-navigate-and-mark-read
- `MINIREDESOCIAL_FRONT/src/components/NotificationCard/NotificationCard.module.css` - card/avatar/unread/dot/timestamp styling per UI-SPEC

### Modified (Tasks 1-2, by prior executor)
- `MINIREDESOCIAL_FRONT/src/components/AppShell/AppShell.jsx` - wired `useNotificationPolling`, badge rendering (with `9+` cap and sr-only label), bell `onClick` navigates to `/notifications`, accent bell color when unread
- `MINIREDESOCIAL_FRONT/src/components/AppShell/AppShell.module.css` - `position: relative` on `.bellBtn`, new `.badge` pill rule
- `MINIREDESOCIAL_FRONT/src/App.jsx` - added `NotificationsPage` import + `<Route path="/notifications" .../>`

### Modified (Task 3 bug fixes — this session)
- `src/controllers/comment.controller.js` - changed `notificationService.create('post_comment', commenterId, post.authorId, comment._id, 'Comment')` to `notificationService.create('post_comment', commenterId, post.authorId, post._id, 'Post')` — `refId` now stores the post's id (directly navigable) instead of the comment's id
- `MINIREDESOCIAL_FRONT/src/components/NotificationCard/NotificationCard.jsx` - changed the non-`post_comment` navigation branch from `navigate(`/profile/${refId}`)` to `navigate(`/profile/${actorId._id}`)`

## Decisions Made

- **Fix `refId` at the source (backend) for `post_comment` rather than teaching the frontend to resolve comment->post.** The Notification schema documents `refId`/`refModel` as a generic reference pair; making `refId` a directly-navigable id (the post, not the comment) keeps the contract simple and matches the `invite_received` convention already in place (`refId = user._id`, directly navigable). This avoids adding a `populate`/`refPath` to the schema and an extra round trip on the frontend — the smallest change that makes the data correct.
- **Resolve follow/invite navigation via `actorId._id` rather than fixing `refId`/`refModel` for those three notification types.** `new_follower`/`follow_request`/`follow_accepted` intentionally store `refId = followDoc._id` / `refModel = 'Follow'` (this is meaningful data — e.g. plan 05-03 will need the Follow doc id to wire inline Aceitar/Rejeitar actions on `follow_request` cards). Changing `refId` to a User id here would work for navigation but would break the upcoming accept/reject feature, which needs the Follow document id. The populated `actorId` is already present in every notification (server always populates `actorId`, per 05-01 contract) and is *always* the correct profile to view for these types (the follower, the accepter, the new registrant) — using it is zero-cost and future-proof.

## Deviations from Plan

### Auto-fixed Issues

**1. [Rule 1 - Bug] `post_comment` notifications stored the comment id as `refId`, but the frontend navigates to `/posts/:postId` — a Comment id is not a Post id, producing "erro ao carregar publicação"**
- **Found during:** Task 3 (human-verify checkpoint) — user reported "Ao clicar em uma notificação de comentario dá erro ao carregar publicação"
- **Issue:** `comment.controller.js` called `notificationService.create('post_comment', commenterId, post.authorId, comment._id, 'Comment')`. `NotificationCard` navigates `post_comment` notifications to `/posts/${refId}`, but `refId` held the **Comment** document's `_id`, not the **Post**'s. `PostDetailPage` then fetched a non-existent post and showed an error.
- **Fix:** Changed the `notificationService.create` call to pass `post._id` (already validated to exist earlier in the handler) and `refModel: 'Post'`. `refId` is now directly navigable, matching the existing `invite_received` convention.
- **Files modified:** `src/controllers/comment.controller.js`
- **Verification:** Traced the data flow: `post = await Post.findById(postId)` (line 23, guaranteed non-null past the 404 check) -> `notificationService.create(..., post._id, 'Post')` -> `Notification.refId = post._id` -> API response `notification.refId` -> `NotificationCard` navigates `/posts/${refId}` -> `PostDetailPage` fetches `GET /api/posts/${postId}` with the **correct** post id. Ran the full backend test suite (78/78 passing, 0 failures) — no test asserted the old `'Comment'`/`comment._id` shape, so nothing broke.
- **Committed in:** `c6063bd`
- **Note (pre-existing data):** Any `post_comment` notifications already persisted in the database before this fix retain the old (incorrect) `refModel: 'Comment'` / comment-id `refId` shape and will still mis-navigate until they age out of the unread window or are otherwise cleared. This is a pre-launch demo dataset; no migration was written (would be speculative complexity per Simplicity First — flag for the owner if stale notifications are observed in manual testing).

**2. [Rule 1 - Bug] Follow/invite notifications navigated using `refId`, but `refId` for `new_follower`/`follow_request`/`follow_accepted` points at a `Follow` document — not a `User` — producing "usuário não encontrado"**
- **Found during:** Task 3 (human-verify checkpoint) — user reported "ao clicar em notificação de seguir dá usuario não encontrado"
- **Issue:** `NotificationCard.handleClick` navigated all non-`post_comment` types to `/profile/${refId}`. But `follow.controller.js` creates `new_follower`/`follow_request` notifications with `refId: followDoc._id, refModel: 'Follow'` (line 39-45) and `follow_accepted` with the same shape (line 93-99) — `refId` is a **Follow** document id, not a **User** id. `ProfilePage` then fetched `GET /api/users/${followDocId}`, which 404'd as "user not found". (Only `invite_received`, where `refId = user._id`/`refModel: 'User'`, was actually correct.)
- **Fix:** Changed the navigation branch to use `actorId._id` (the populated actor user — always present per the 05-01 API contract's `actorId: { _id, name, avatarUrl }` populate) instead of `refId`. Verified this is correct for all four affected types: `new_follower`/`follow_request` -> `actorId = followerId` (the person who followed — correct profile to view); `follow_accepted` -> `actorId = followDoc.followingId` (the person who accepted — correct); `invite_received` -> `actorId = user._id` (the new registrant — correct, and happens to equal the old `refId` value so no regression there).
- **Files modified:** `MINIREDESOCIAL_FRONT/src/components/NotificationCard/NotificationCard.jsx`
- **Verification:** Traced the data flow for each of the 4 affected types from notification-creation call site (`follow.controller.js:39-45`, `follow.controller.js:93-99`, `registration.controller.js:90`) through `Notification.actorId` (always populated server-side with `name avatarUrl`, per `notification.controller.js:28`) to `NotificationCard`'s destructured `actorId._id` -> `navigate(`/profile/${actorId._id}`)` -> `ProfilePage` `GET /api/users/${userId}`. Ran `npm run build` in MINIREDESOCIAL_FRONT — exits 0 (1613 modules transformed, no errors).
- **Committed in:** `5121ef0` (frontend repo, on `main`)

### Plan-internal note (not a defect)

The Task 2 `<verify>` automated check `grep -q "notifications/\${" src/components/NotificationCard/NotificationCard.jsx` greps `NotificationCard.jsx` for the mark-read PATCH call pattern (`notifications/${id}/read`). That call actually lives in `NotificationsPage.jsx`'s `handleMarkRead`, not in `NotificationCard.jsx` — `NotificationCard` only *invokes* `onRead(_id)`, which `NotificationsPage` wires to the PATCH. This is **correct architecture**, exactly matching the "lift state up" pattern the plan's Task 2 `<action>` text describes ("Implement `handleMarkRead(id)` that optimistically marks the item read in local state and calls `api.patch(...)`" — explicitly assigned to `NotificationsPage`, with `NotificationCard` only calling `onRead`). The grep target in the `<verify>` block is simply the wrong file path for that one assertion; the prior executor correctly judged the architecture over the literal grep target. No code change required — documenting per the continuation brief's instruction to record this as a plan-internal inconsistency, not a defect.

## Known Stubs

None — no stub patterns (`=[]`, `={}`, placeholder text, unwired mock data) found in any file created or modified by this plan.

## Threat Flags

None — both bug fixes operate entirely within the trust boundaries and mitigations already documented in the plan's `<threat_model>` (T-05F-01 server-side userId scoping, T-05F-02 IDOR mark-read guard). No new endpoints, auth paths, or schema changes were introduced; `refModel` value change (`'Comment'` -> `'Post'`) is an internal bookkeeping field with no client-facing trust implications — the client never queries by `refModel`, only renders/navigates using server-populated/scoped data.

## Self-Check: PASSED

- FOUND: `.planning/phases/05-notifications-polling/05-02-SUMMARY.md`
- FOUND: `src/controllers/comment.controller.js`
- FOUND: `MINIREDESOCIAL_FRONT/src/components/NotificationCard/NotificationCard.jsx`
- FOUND commit `c6063bd` (backend fix) — confirmed `post._id, 'Post'` present in diff
- FOUND commit `15e7c2b` (SUMMARY doc commit)
- FOUND commit `3d668f1` (Task 1, prior executor, frontend main)
- FOUND commit `f709521` (Task 2, prior executor, frontend main)
- FOUND commit `5121ef0` (frontend fix) — confirmed `actorId._id` present in diff
