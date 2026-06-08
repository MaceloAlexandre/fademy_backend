# Phase 5: Notifications (Polling) - Discussion Log

> **Audit trail only.** Do not use as input to planning, research, or execution agents.
> Decisions are captured in CONTEXT.md — this log preserves the alternatives considered.

**Date:** 2026-06-07
**Phase:** 5-notifications-polling
**Areas discussed:** Notification list UX, Follow request actions, Polling cadence & badge, invite_received trigger

---

## Notification list UX

| Option | Description | Selected |
|--------|-------------|----------|
| Avatar + actor + action text | Mirrors PostCard/UserResultCard style | ✓ |
| Type icon + message text | Icon per type, no avatar fetch | |

**User's choice:** Avatar + actor + action text

| Option | Description | Selected |
|--------|-------------|----------|
| Navigate + mark read (Recommended) | Tap navigates to related content and marks read in one action | ✓ |
| Mark read only, no navigation | Tapping just marks read | |

**User's choice:** Navigate + mark read (Recommended)

| Option | Description | Selected |
|--------|-------------|----------|
| Unread highlighted background + dot | Subtle tint + dot badge for unread | ✓ |
| Unread bold text only | Minimal visual change | |

**User's choice:** Unread highlighted background + dot

| Option | Description | Selected |
|--------|-------------|----------|
| Simple reverse-chronological (Recommended) | Newest first, flat list, matches feed pattern | ✓ |
| Grouped by date | "Today"/"Yesterday"/"This week"/"Older" sections | |

**User's choice:** Simple reverse-chronological (Recommended)
**Notes:** None — straightforward selections, all matched the recommended/consistent options.

---

## Follow request actions

| Option | Description | Selected |
|--------|-------------|----------|
| Yes, inline Accept/Reject (Recommended) | Buttons on the card itself; backend endpoints already exist from Phase 3 (D-07) | ✓ |
| Tap navigates to requester's profile to act there | Accept/reject lives on the profile page | |

**User's choice:** Yes, inline Accept/Reject (Recommended)

| Option | Description | Selected |
|--------|-------------|----------|
| Card updates in place (Recommended) | Buttons replaced by status label "Aceito"/"Rejeitado" | ✓ |
| Notification disappears from the list | Card removed entirely after action | |

**User's choice:** Card updates in place (Recommended)

| Option | Description | Selected |
|--------|-------------|----------|
| Yes — acting on it marks it read | Action is a stronger signal than viewing | ✓ |
| No — read state is independent of actions | Could be acted on but still "unread" | |

**User's choice:** Yes — acting on it marks it read
**Notes:** This area resolves the decision Phase 3 (D-07 there) explicitly deferred — "frontend de aceitar/rejeitar fica para Phase 5 (via centro de notificações)".

---

## Polling cadence & badge

| Option | Description | Selected |
|--------|-------------|----------|
| Every 30 seconds (Recommended) | Responsive without hammering the API | |
| Every 60 seconds | Lighter server load | |

**User's choice:** Free text — "precisa ser mais curto, coisa de 5 segundos, para demonstrações praticas" (needs to be shorter, like 5 seconds, for practical demos)
**Notes:** User explicitly overrode both presented options in favor of a much shorter 5-second interval, prioritizing live-demo responsiveness over server load. Captured verbatim in CONTEXT.md D-08.

| Option | Description | Selected |
|--------|-------------|----------|
| Whenever the app is open (Recommended) | Poll continuously in foreground; badge stays accurate everywhere | ✓ |
| Only while viewing the Notifications screen | Lighter, but badge can feel stale elsewhere | |

**User's choice:** Whenever the app is open (Recommended)

| Option | Description | Selected |
|--------|-------------|----------|
| Yes — numeric badge with count (Recommended) | Small badge with unread count, e.g. "3"/"9+" | ✓ |
| Yes — simple dot indicator (no count) | Just a "something new" dot | |

**User's choice:** Yes — numeric badge with count (Recommended)

---

## invite_received trigger

| Option | Description | Selected |
|--------|-------------|----------|
| Notify the inviter when their link is used | Reframes the type as "your invite was redeemed" — the only moment with two real users | ✓ |
| Skip — don't wire this type for Phase 5 | Leave the type in the enum, unused, until invites become targeted | |

**User's choice:** Notify the inviter when their link is used
**Notes:** The original spec ("invite_received — convite recebido") assumes a known recipient, but `createInvite` generates anonymous shareable token links with no `invitedUserId`. The user chose to reframe the trigger around the only two-real-user moment in the flow: registration via the invite link, notifying `invite.invitedById`.

| Option | Description | Selected |
|--------|-------------|----------|
| The new user (refModel: 'User') (Recommended) | Tap navigates to the new member's profile — consistent with D-02 | ✓ |
| The invite document (refModel: 'Invite') | Technically accurate but no invite detail screen to navigate to | |

**User's choice:** The new user (refModel: 'User') (Recommended)

---

## Claude's Discretion

- Internal structure of `NotificationsPage`/`NotificationCard` React components
- `GET /api/notifications` pagination strategy (cursor-based, mirroring Phase 4 feed D-02, vs. simple pagination)
- Mark-as-read endpoint design (per-notification PATCH vs. bulk "mark all read")
- Exact unread-count badge cap threshold (e.g., "9+")
- Polling hook implementation details (where it's mounted, how it merges results)
- Loading/error state treatment in the notifications view

## Deferred Ideas

- **`post_like` notification** — ENGM-01, v2 backlog.
- **`message_received` dispatch wiring** — type already exists in the `Notification` model enum; real dispatch only makes sense once chat exists (Phase 6). Noted to resolve the apparent contradiction in Phase 5's roadmap success criterion #3.
- **Date-grouped notification list** ("Today"/"Yesterday"/"This week") — user chose simple reverse-chronological for the MVP; grouping could be a future polish item.
