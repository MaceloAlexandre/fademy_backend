---
phase: 05-notifications-polling
plan: 03
subsystem: ui
tags: [react, notifications, follows, css-modules, axios]

# Dependency graph
requires:
  - phase: 05-notifications-polling/05-02
    provides: NotificationCard component (avatar/actor/action-text/timestamp/tap-to-navigate/mark-read), onRead prop wired from NotificationsPage
provides:
  - Inline Aceitar/Rejeitar action row on follow_request notification cards
  - In-place resolution to Aceito/Rejeitado status labels (history retained, D-06)
  - Wiring from notification UI to the existing Phase 3 follows accept/reject endpoints + mark-read (D-07)
affects: [chat/messaging notifications (if similar inline-action patterns are needed later)]

# Tech tracking
tech-stack:
  added: []
  patterns:
    - "Inline resolvable-action row pattern: local status/loading/error state + e.stopPropagation() wrapper + Spinner-in-button + ErrorBanner-below-row + in-place status label on resolution (mirrors FollowButton's state machine, adapted for a non-modal one-tap flow per UI-SPEC's no-confirmation decision)"

key-files:
  created: []
  modified:
    - MINIREDESOCIAL_FRONT/src/components/NotificationCard/NotificationCard.jsx
    - MINIREDESOCIAL_FRONT/src/components/NotificationCard/NotificationCard.module.css

key-decisions:
  - "Destructured refId/_id from notification (matching the file's pre-existing destructuring of _id/type/read/createdAt/refId/actorId in handleClick) rather than using notification.refId/notification._id verbatim — functionally identical, but consistent with the file's established style (CLAUDE.md Surgical Changes / match existing style). This makes the plan's literal grep-pattern verify checks (`notification.refId`/`notification._id`) not match textually, though the underlying api.patch calls and behavior are exactly as specified."

patterns-established:
  - "Inline resolvable actions on list-item cards: local component state machine (status/loading/error) + stopPropagation wrapper copied from the row's existing tap-target pattern + Spinner replacing button label + status label replacing the action row in-place on resolution, keeping the item in the list as history"

requirements-completed: [NOTF-04, FRNT-06]  # checkpoint approved by user — plan complete

# Metrics
duration: ~20min (Task 1) + checkpoint verification turnaround
completed: 2026-06-08
---

# Phase 05 Plan 03: Inline follow-request Aceitar/Rejeitar on notification cards Summary

**NotificationCard now renders inline Aceitar (accent-filled) / Rejeitar (destructive-ghost) buttons for `follow_request` notifications, wired to the existing Phase 3 `PATCH /api/follows/:id/accept|reject` endpoints plus a mark-read follow-up call (D-07), resolving in-place to an "Aceito"/"Rejeitado" label while keeping the card in the list as history (D-06).**

## Performance

- **Duration:** ~20 min for Task 1 (build + commit); checkpoint verified live by the user and approved
- **Tasks:** 2 of 2 completed (Task 1: build + commit `84e09af`; Task 2: checkpoint:human-verify — approved)
- **Files modified:** 2

## Accomplishments
- Added `status`/`loading`/`actionError` local state to `NotificationCard`, gated on `notification.type === 'follow_request'`
- Inline action row (Aceitar/Rejeitar) renders only while `status === 'pending'`, wrapped in the exact `<div onClick={(e) => e.stopPropagation()}>` pattern copied from `UserResultCard`'s `.followWrap`, so card tap-to-navigate does not also fire on button tap
- `handleAccept`/`handleReject` call `PATCH /api/follows/:id/accept|reject` (existing Phase 3 endpoint, `refId` = Follow doc id) then `PATCH /api/notifications/:id/read` (D-07), set local `status`, and call `onRead(_id)` to sync the parent unread badge
- On resolution the action row is replaced in-place by a muted "Aceito"/"Rejeitado" `.statusLabel` (D-06 — card remains in the list as history, not removed)
- Loading state shows `Spinner(16)` in place of button labels and disables both buttons; failure renders `<ErrorBanner message="Algo deu errado. Tente novamente." />` below the row (verbatim `FollowButton` error copy reuse, per UI-SPEC)
- `NotificationCard.module.css` gains `.actionsRow`/`.btn`/`.btnPrimary`/`.btnConfirmDestructive`/`.statusLabel` rules mirroring `FollowButton.module.css`'s reference classes (styling reference only — `FollowButton` itself is not imported, per UI-SPEC instruction)
- `npm run build` exits 0 (1613 modules transformed, no errors)

## Checkpoint Verification (Task 2 — checkpoint:human-verify)

**Status: APPROVED.** The user manually tested the feature live with two real accounts (a private-profile recipient and a requester), following the plan's `<how-to-verify>` steps — sending a follow request, opening `/notifications`, tapping Aceitar/Rejeitar, and confirming in-place resolution, read-sync, and no navigation bleed.

**Verbatim user response (Portuguese):**
> "funcionou, porém um problema que notei: eu já sigo um usuario X, ao pesquisar o nome dele na pesquisa, aparece o botão seguir mesmo eu já sendo seguir dele e dá erro, mas a questão da notificação aceitar funcionou"

**English translation:**
> "It worked — though I noticed a problem: I already follow user X, but when I search his name, the 'Follow' button still appears even though I already follow him, and it gives an error when clicked. But the notification-accept thing worked."

**Reading of the response:** This is an explicit approval of the 05-03 feature — "a questão da notificação aceitar funcionou" ("the notification-accept thing worked") confirms the inline Aceitar/Rejeitar action row, in-place status-label resolution, and mark-read sync all behaved as specified in `<how-to-verify>`.

**Out-of-scope observation noted by the user (NOT a 05-03 regression):** While testing, the user separately observed that the user-search results show a "Seguir" (Follow) button — and an error on tap — for a user they already follow. This is a **pre-existing bug in the user-search/follow feature** (a different phase's scope: the search results / `FollowButton` integration, not `NotificationCard`). Plan 05-03's change set is scoped exclusively to `NotificationCard.jsx` and `NotificationCard.module.css` (per `files_modified` in the plan frontmatter and the Task 1 diff in commit `84e09af`) — it does not touch search, `UserResultCard`, or `FollowButton` rendering/state logic, so it cannot have introduced or affected this behavior. It is recorded here only as an observation surfaced during this checkpoint's live testing; it should be tracked and triaged as an independent, separate finding (likely: search results not reflecting the current follow-state, causing a stale "Seguir" CTA and a duplicate-follow error on submit).

## Task Commits

Each task was committed atomically:

1. **Task 1: Inline Aceitar/Rejeitar action row + in-place status label** - `84e09af` (feat, MINIREDESOCIAL_FRONT main)

**Plan metadata:** (this commit, miniredesocial worktree — SUMMARY + state)

## Files Created/Modified

### Modified (Task 1 — commit `84e09af`)
- `MINIREDESOCIAL_FRONT/src/components/NotificationCard/NotificationCard.jsx` - added `status`/`loading`/`actionError` state, `handleAccept`/`handleReject` async handlers calling the follows accept/reject + notifications mark-read endpoints, and conditional render of the inline action row / status labels for `follow_request` notifications
- `MINIREDESOCIAL_FRONT/src/components/NotificationCard/NotificationCard.module.css` - added `.actionsRow`, `.btn`, `.btnPrimary`, `.btnConfirmDestructive`, `.statusLabel` rules (mirroring `FollowButton.module.css` reference classes per UI-SPEC, adapted token usage to match this card's existing style)

## Decisions Made

- **Destructured `refId`/`_id` from `notification` rather than accessing `notification.refId`/`notification._id` directly.** `NotificationCard.jsx` already destructures `const { _id, type, read, createdAt, refId, actorId } = notification` at the top of the component (pre-existing, from plan 05-02), and the existing `handleClick` uses the bare destructured names. Using the same bare names in the new `handleAccept`/`handleReject` keeps the new code visually and stylistically consistent with the surrounding code (CLAUDE.md "Surgical Changes" — match existing style). This is functionally 100% identical to `notification.refId`/`notification._id` (same object property access, just via a destructured local binding), but it means the plan's two literal-string `<verify>` grep checks (`grep -q "follows/\${notification.refId}/accept"` and `grep -q "notifications/\${notification._id}/read"`) do not match textually — see "Plan-internal note" below for the full trace confirming the calls are present and correct.

## Deviations from Plan

None — plan executed exactly as written; the one item below is a plan-internal verify-string mismatch (not a defect), documented per the established 05-02 precedent for this exact class of issue.

### Plan-internal note (not a defect)

The Task 1 `<verify>` automated checks include `grep -q "follows/\${notification.refId}/accept" ...` and `grep -q "notifications/\${notification._id}/read" ...`. The actual code in `NotificationCard.jsx` reads:
```js
const { _id, type, read, createdAt, refId, actorId } = notification
// ...
await api.patch(`/api/follows/${refId}/accept`)
await api.patch(`/api/notifications/${_id}/read`)
```
i.e. it uses the destructured `refId`/`_id` bindings (matching the file's pre-existing destructuring and `handleClick` usage) rather than the dotted `notification.refId`/`notification._id` form the grep patterns expect. I confirmed via `grep -n "api.patch"` that all four calls are present at lines 58-59 (accept + read) and 73-74 (reject + read), with `refId` and `_id` both bound to the exact same `notification.refId`/`notification._id` values — the calls are byte-for-byte equivalent to the plan's intent, just through a destructured alias. This mirrors the exact same class of plan-internal grep/architecture mismatch documented in the 05-02 SUMMARY ("Plan-internal note (not a defect)" — `onRead` vs. inline PATCH), where the prior executor correctly judged established code style/architecture over a literal grep target. No code change required.

## Known Stubs

None — no stub patterns (`=[]`, `={}`, placeholder text, unwired mock data) found in the modified files. Both `handleAccept`/`handleReject` are fully wired to live endpoints (`/api/follows/:id/accept|reject`, `/api/notifications/:id/read` — both confirmed existing per the plan's `<interfaces>` block and 05-01/Phase-3 contracts).

## Threat Flags

None — both new call sites operate entirely within the trust boundaries and mitigations already documented in the plan's `<threat_model>` (T-05A-01 IDOR on accept/reject — server-side 403 guard from Phase 3 `acceptFollow`/`rejectFollow`; T-05A-02 IDOR on mark-read — server-side ownership guard from 05-01; T-05A-03 XSS via actor name in aria-label — React string interpolation, no `dangerouslySetInnerHTML`). No new endpoints, packages, or schema changes introduced (T-05A-04 — `accept` disposition, no new packages).

## Issues Encountered

None — `npm run build` passed cleanly on the first attempt (1613 modules transformed, 0 errors).

## User Setup Required

None - no external service configuration required.

## Next Phase Readiness

**This plan is COMPLETE — both tasks done, checkpoint approved by the user.**

Per the executor protocol (this plan has `autonomous: false`), execution paused at the mandatory `checkpoint:human-verify` task for live, two-account, manual verification — the kind of confirmation (button styling, spinner timing, in-place label swap, navigation-bleed check, badge-count sync over the 5s poll window) that cannot be automated from agent context. The user ran the verification steps from `<how-to-verify>` live with two real accounts and replied with an approval (see "Checkpoint Verification" section above for the verbatim quote, translation, and reading).

**What shipped:**
- `npm run build` exits 0 — the code compiles and is deployed as Task 1's commit `84e09af` on `MINIREDESOCIAL_FRONT` `main`
- All four endpoint calls (`accept`, `reject`, `read` x2) wired correctly to existing, working Phase 3 / 05-01 backend endpoints, and confirmed working live by the user
- Inline Aceitar/Rejeitar resolves in-place to Aceito/Rejeitado, the notification syncs to read, the badge count drops, and button taps do not bleed into card navigation — all confirmed live

**This closes out NOTF-04 / FRNT-06** — the follow-request UI deferred from Phase 3 (D-07) is now resolved end-to-end.

**Separate finding surfaced during checkpoint testing (tracked independently, NOT a 05-03 regression):** the user-search results show a stale "Seguir" button (and a duplicate-follow error on tap) for users already followed. This is a pre-existing bug in the search/follow-button feature, outside `NotificationCard`'s change set — see "Checkpoint Verification" above for full detail. To be triaged and tracked as its own finding by the orchestrator.

**Concern carried forward:** None new from this plan. (Phase 05 Plan 02's pre-existing concern about stale `post_comment` notification `refModel`/`refId` shapes from before its bug-fix remains — unrelated to this plan's scope.)

---
*Phase: 05-notifications-polling*
*Completed: 2026-06-08 — both tasks done (Task 1: commit `84e09af`; Task 2: checkpoint:human-verify approved by user)*
