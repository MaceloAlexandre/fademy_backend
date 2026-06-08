# Phase 6: Chat (Real-Time, 1:1) - Discussion Log

> **Audit trail only.** Do not use as input to planning, research, or execution agents.
> Decisions are captured in CONTEXT.md — this log preserves the alternatives considered.

**Date:** 2026-06-08
**Phase:** 6-chat-groups-real-time
**Areas discussed:** Scope reduction (timeline), Starting a chat & navigation

---

## Scope reduction (triggered before area selection)

The user interrupted the initial gray-area selection to flag that the presentation date moved up to **2 days away**, requiring Phase 6 to be simplified.

| Option | Description | Selected |
|--------|-------------|----------|
| Cut scope — 1:1 chat only, defer groups | Ship CHAT-01/CHAT-02 (real-time 1:1 chat + media); defer CHAT-03/CHAT-04 (groups) to a later milestone. Requires updating ROADMAP/REQUIREMENTS. | ✓ |
| Keep all 4 criteria, build the leanest version of each | Still ship all of CHAT-01..04, but with minimal implementation (no presence, minimal UI). | |
| Something else | User describes a different cut. | |

**User's choice:** "Cut scope — 1:1 chat only, defer groups"
**Notes:** This triggered an immediate update to `.planning/ROADMAP.md` (Phase 6 renamed "Chat (Real-Time, 1:1)", success criteria trimmed 5→3, requirements list reduced to CHAT-01/CHAT-02/FRNT-07) and `.planning/REQUIREMENTS.md` (CHAT-03/CHAT-04 moved to v2 deferred section, traceability + coverage counts updated to 41 v1 requirements).

---

## Gray area selection (post scope-cut)

Re-presented narrowed gray areas reflecting 1:1-only scope:

| Option | Description | Selected |
|--------|-------------|----------|
| Starting a chat & navigation | Where a user starts a 1:1 conversation; where Chat lives in the app's nav. | ✓ |
| Real-time feedback — keep it minimal? | Skip typing/online/read-receipts entirely, or keep one for demo wow-factor? | |
| Media messages — simplest viable | Images only vs. images+files. | |

**User's choice:** "Starting a chat & navigation" only — left Real-time feedback and Media messages to Claude's discretion.

---

## Starting a chat & navigation

### Q1 — Where should Chat live in the app's navigation?

| Option | Description | Selected |
|--------|-------------|----------|
| 5th bottom-nav tab | Add a Chat tab to AppShell's TABS array. | |
| Header icon (next to the bell) | Add a chat icon in the header beside notifications; bottom nav stays at 4 tabs. | ✓ |
| You decide | Claude picks the simplest fit. | |

**User's choice:** "Header icon (next to the bell)"

### Q2 — How does a user start a new 1:1 conversation?

| Option | Description | Selected |
|--------|-------------|----------|
| "Message" button on profile | Opens/creates conversation directly from ProfilePage. | |
| Dedicated "New chat" picker | Separate screen listing mutual followers. | |
| You decide | Claude picks the fastest natural path. | |

**User's choice (free text):** "Achei que na pergunta passada já era onde ia ficar o ícone de 'direct', igual ao instagram" — clarifying that the header icon (Q1 answer) **is** the Instagram-DM-style entry point that opens a conversations list, not a separate question.
**Notes:** This reframed the discussion — the header icon opens a DM-list, and "starting a chat" happens *within* that list (handled in Q3).

### Q3 — Inside the conversations list, how does a user start a brand-new chat?

| Option | Description | Selected |
|--------|-------------|----------|
| "New message" icon → picker of mutual followers | Instagram-style compose icon opens a list of mutual followers to start with. | ✓ |
| "Message" button on profile | Navigate to profile, tap Message there. | |
| You decide | Claude picks fastest path matching Instagram reference. | |

**User's choice:** "'New message' icon → picker of mutual followers"

### Q4 — What should each conversation-list item show?

| Option | Description | Selected |
|--------|-------------|----------|
| Avatar + name + last message preview + time + unread dot | Full chat-app style (Instagram DM look). | ✓ |
| Avatar + name + timestamp only | Simpler, less data to track. | |
| You decide | Claude picks fastest-to-build-well option. | |

**User's choice:** "Avatar + name + last message preview + time + unread dot"

---

## Claude's Discretion

- **Real-time feedback** (typing indicators, online/last-seen, read receipts) — user did not select this area for discussion; recommended approach in CONTEXT.md is to skip all three and ship plain real-time send/receive.
- **Media handling** (allowed types, size limits, display) — user did not select this area; recommended approach in CONTEXT.md is images-only, reusing the proven `uploadPostImage` S3 pattern.
- **Conversation screen layout** (bubbles, composer) — no preference captured; standard chat UI, reuse existing component patterns.
- **Mutual-follow check implementation** — bilateral `Follow` query with `status: 'accepted'`, mirroring proven block-check patterns.

## Deferred Ideas

- **CHAT-03 (group formation)** and **CHAT-04 (public/private groups)** — moved to REQUIREMENTS.md v2 section due to the 2-day timeline compression. ROADMAP.md Phase 6 updated to drop these from success criteria.
