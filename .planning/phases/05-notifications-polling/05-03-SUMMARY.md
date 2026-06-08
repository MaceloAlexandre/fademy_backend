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

requirements-completed: []  # NOT marking complete — plan paused at human-verify checkpoint, not yet approved

# Metrics
duration: ~20min (Task 1 only; checkpoint pending)
completed: 2026-06-08
---

# Phase 05 Plan 03: Inline follow-request Aceitar/Rejeitar on notification cards Summary

**NotificationCard now renders inline Aceitar (accent-filled) / Rejeitar (destructive-ghost) buttons for `follow_request` notifications, wired to the existing Phase 3 `PATCH /api/follows/:id/accept|reject` endpoints plus a mark-read follow-up call (D-07), resolving in-place to an "Aceito"/"Rejeitado" label while keeping the card in the list as history (D-06).**

## Performance

- **Duration:** ~20 min for Task 1 (build + commit); plan PAUSED at the mandatory `checkpoint:human-verify` task — awaiting manual verification, not yet approved
- **Tasks:** 1 of 2 completed (Task 1: build; Task 2: checkpoint — pending)
- **Files modified:** 2

## Accomplishments
- Added `status`/`loading`/`actionError` local state to `NotificationCard`, gated on `notification.type === 'follow_request'`
- Inline action row (Aceitar/Rejeitar) renders only while `status === 'pending'`, wrapped in the exact `<div onClick={(e) => e.stopPropagation()}>` pattern copied from `UserResultCard`'s `.followWrap`, so card tap-to-navigate does not also fire on button tap
- `handleAccept`/`handleReject` call `PATCH /api/follows/:id/accept|reject` (existing Phase 3 endpoint, `refId` = Follow doc id) then `PATCH /api/notifications/:id/read` (D-07), set local `status`, and call `onRead(_id)` to sync the parent unread badge
- On resolution the action row is replaced in-place by a muted "Aceito"/"Rejeitado" `.statusLabel` (D-06 — card remains in the list as history, not removed)
- Loading state shows `Spinner(16)` in place of button labels and disables both buttons; failure renders `<ErrorBanner message="Algo deu errado. Tente novamente." />` below the row (verbatim `FollowButton` error copy reuse, per UI-SPEC)
- `NotificationCard.module.css` gains `.actionsRow`/`.btn`/`.btnPrimary`/`.btnConfirmDestructive`/`.statusLabel` rules mirroring `FollowButton.module.css`'s reference classes (styling reference only — `FollowButton` itself is not imported, per UI-SPEC instruction)
- `npm run build` exits 0 (1613 modules transformed, no errors)

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

**This plan is PAUSED at the mandatory `checkpoint:human-verify` task (Task 2 of 2) — it is NOT complete.**

Per the executor protocol (this plan has `autonomous: false`), execution stops here for manual verification because the remaining work requires:
1. A live two-account interaction (a private-profile user receiving and acting on a real `follow_request` notification) — cannot be automated/simulated from this agent context
2. Visual/interaction confirmation (button styling, spinner timing, in-place label swap, navigation-bleed check, badge-count sync over the 5s poll window) — exactly the kind of "90% human-verify" case the checkpoint protocol exists for

**To resume:** A fresh agent (or the orchestrator) should pick up at the `checkpoint:human-verify` task in `05-03-PLAN.md`, present the verification steps to the user (listed in the plan's `<how-to-verify>` block — create a follow_request via a second private-profile account, open `/notifications`, tap Aceitar/Rejeitar, confirm in-place resolution + read-sync + no navigation bleed), and process the "approved" / fix-needed response.

**What's ready:**
- `npm run build` exits 0 — the code compiles and is deployable as-is
- All four endpoint calls (`accept`, `reject`, `read` x2) are wired correctly and traced to existing, working Phase 3 / 05-01 backend endpoints
- No blockers for the checkpoint itself — purely awaiting a human to run the manual verification steps

**Concern carried forward:** None new. (Phase 05 Plan 02's pre-existing concern about stale `post_comment` notification `refModel`/`refId` shapes from before its bug-fix remains — unrelated to this plan's scope.)

---
*Phase: 05-notifications-polling*
*Completed: PAUSED — Task 1 complete (commit `84e09af`); Task 2 (checkpoint:human-verify) pending*
