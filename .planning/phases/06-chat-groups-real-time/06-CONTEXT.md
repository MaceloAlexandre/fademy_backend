# Phase 6: Chat (Real-Time, 1:1) - Context

**Gathered:** 2026-06-08
**Status:** Ready for planning

<domain>
## Phase Boundary

Real-time 1:1 chat between mutually-following users, with media support, presented Instagram-DM style — entry icon in the header opens a conversations list, with a "new message" picker to start fresh conversations with mutual followers. Messages support text and media (images/files) stored in S3, delivered live via Socket.io.

**Scope note:** This phase's scope was reduced mid-discussion. The original Phase 6 also covered group formation (CHAT-03/CHAT-04 — turning a 1:1 into a group, public/private groups). The presentation date moved up to **2 days away**, so the user explicitly chose to cut groups from this phase and ship 1:1 chat only. ROADMAP.md and REQUIREMENTS.md were updated this session to reflect the new boundary — see Canonical References.

</domain>

<decisions>
## Implementation Decisions

### Scope (timeline-driven)
- **D-01:** Presentation moved up to 2 days away. User chose to cut Phase 6 down to 1:1 chat only (CHAT-01, CHAT-02, FRNT-07). Group formation (CHAT-03) and public/private groups (CHAT-04) are deferred to v2 — moved out of ROADMAP.md Phase 6 success criteria and into REQUIREMENTS.md's deferred section. Do NOT plan or build group functionality in this phase.

### Navigation & Entry Point
- **D-02:** The chat entry point is a header icon next to the notification bell (NOT a 5th bottom-nav tab) — keeps AppShell's existing 4-tab bottom nav unchanged; mirrors the bell icon's placement/style exactly.
- **D-03:** Tapping the icon opens a conversations list, Instagram-DM style (user's explicit reference: "igual ao instagram" / "like Instagram").
- **D-04:** New conversations start via a "New message" icon inside that list, which opens a picker of mutual followers — tapping one opens/creates the 1:1 conversation. (Not a "Message" button on the profile page — the user wants the Instagram-style compose flow inside the DM list itself.)
- **D-05:** Each conversation-list item shows: avatar + name + last-message preview + timestamp + unread indicator (dot) — the full chat-app style, not a bare-bones avatar+name+time list.

### Claude's Discretion
User explicitly deferred these to Claude's judgment — pick the simplest, fastest-to-ship approach given the 2-day window:
- **Real-time feedback features** (typing indicators, online/last-seen status, read receipts) — recommend skipping all three; ship plain real-time send/receive via Socket.io only. Add one only if it is genuinely trivial to bolt on.
- **Media handling specifics** — recommend images only (reusing the proven `uploadPostImage` S3 pattern from Phase 4, same multer + key-prefix approach), with inline preview display. Skip arbitrary file/document support unless trivial.
- **Conversation screen layout** (message bubbles, composer, scroll behavior) — standard chat UI; no specific preference captured. Reuse existing component patterns where possible.
- **Mutual-follow check implementation** — query `Follow` in both directions with `status: 'accepted'`; both must exist for the chat to be allowed (mirrors the bilateral logic already proven for blocks/feed filtering).

</decisions>

<canonical_refs>
## Canonical References

**Downstream agents MUST read these before planning or implementing.**

### Domain spec (authoritative)
- `CONTEXT.md` (repo root) §"Chat e grupos" (lines ~113-118) — 1:1 mutual-follow rule, media support rule. (The group-formation rule described here is now deferred — see Scope decision D-01.)
- `CONTEXT.md` (repo root) §schema `conversations` (lines ~248-258) — authoritative Mongoose schema reference: `memberIds`, `name`, `isGroup`, `isPublic`, `createdById`, `updatedAt`. For this phase, `isGroup`/`isPublic` exist on the schema but are not exercised by any feature (always `isGroup: false`).
- `CONTEXT.md` (repo root) §schema `messages` (lines ~261-271) — authoritative Mongoose schema reference: `conversationId`, `senderId`, `text`, `mediaUrls`, `createdAt`.
- `erd_mini_rede_social.html` (repo root) — visual ERD of the database

### Roadmap & requirements — UPDATED this session to reflect the cut scope
- `.planning/ROADMAP.md` Phase 6 section — renamed "Chat (Real-Time, 1:1)"; success criteria trimmed from 5 to 3 (group-related criteria removed); requirements now `CHAT-01, CHAT-02, FRNT-07` only. **This is the authoritative phase boundary for planning — read it fresh, do not rely on memory of the old 5-criteria version.**
- `.planning/REQUIREMENTS.md` — CHAT-03/CHAT-04 moved from "Active" to "v2 Requirements" with rationale (2-day timeline compression); traceability table and coverage counts updated (41 v1 requirements total).

### Foundations (prior phases — reuse, don't recreate)
- `.planning/phases/05-notifications-polling/05-CONTEXT.md` — notes `message_received` was deliberately left undispatched because "it only makes sense once chat exists (Phase 6)". **That condition is now met — wire it up in this phase.**
- `src/services/notification.service.js` → `create(type, actorId, userId, refId, refModel)` — already implemented and proven (used in follow/comment controllers); call with `'message_received'` when a message is sent. **NOTF-02 pattern — reuse, don't recreate.**
- `src/models/Notification.js` — enum already includes `message_received`; no schema change needed.
- `src/services/s3.service.js` → `uploadPostImage`, `buildObjectUrl`, `objectExists` — direct model for message-media uploads (same multer + S3 key pattern, different prefix).
- `src/models/Follow.js` — mutual-follow check source: query both directions with `status: 'accepted'`.
- `src/models/Block.js` — must also gate chat per SOCL-05 ("Blocked users cannot message, see posts, or appear in search") — reuse the bilateral block-check pattern from feed/comment controllers.
- `src/middlewares/auth.middleware.js` (`authenticate`) + `src/services/token.service.js` (`verifyToken`) — JWT verification pattern to mirror for the Socket.io connection handshake (HS256-pinned, per [01-02] decision).
- `MINIREDESOCIAL_FRONT/src/components/AppShell/AppShell.jsx` — header already implements the bell-icon + badge + `useNotificationPolling` pattern; the new chat icon follows the exact same placement/visual convention (D-02).
- `MINIREDESOCIAL_FRONT/src/lib/api.js` — axios instance with JWT interceptor.

</canonical_refs>

<code_context>
## Existing Code Insights

### Reusable Assets (backend)
- `src/services/s3.service.js` → `uploadPostImage` as the direct model for message-image uploads (same multer + S3 key pattern, different prefix e.g. `messages/<uuid>.<ext>`); `buildObjectUrl`/`objectExists` reusable as-is
- `src/services/notification.service.js` → `create('message_received', senderId, recipientId, messageId, 'Message')` when a message is sent
- `src/middlewares/auth.middleware.js` + `src/services/token.service.js` → JWT verify pattern to replicate for Socket.io handshake auth (pin HS256, per [01-02])
- `src/controllers/*.controller.js` + `src/routes/*.routes.js` — established controller/router pattern to follow for `conversation.controller.js`/`message.controller.js`
- `mongodb-memory-server` test pattern (see `follow.controller.test.js`, `comment.controller.test.js`) — reuse for conversation/message controller tests

### Reusable Assets (frontend)
- `PrimaryButton`, `ErrorBanner`, `Spinner` — reuse in chat screens
- `UserResultCard` / `NotificationCard` — layout reference for conversation-list items (avatar + name + preview + timestamp + unread state, D-05)
- `PhotoUpload.jsx` — reference for the media picker in the message composer
- `useNotificationPolling` hook + AppShell header bell-icon markup — the exact pattern/placement to mirror for the new chat icon (D-02)
- CSS Modules co-located — continue as the established pattern

### Established Patterns
- Multi-tenant: `Conversation`/`Message` don't need their own `institutionId` — scope flows naturally from `memberIds`/`senderId` (both `User`, already institution-scoped). Same reasoning Phase 5 applied to `Notification` — no schema change needed to satisfy PLAT-01.
- `req.user` injected by `authenticateToken` (`_id`, `institutionId`, `role`)
- Socket.io is a **new** real-time pattern in this codebase — the first feature to use it (everything else so far is REST + polling). The owner has limited real-time experience — favor well-trodden Socket.io + Express patterns (per PROJECT.md context note).
- Mutual-follow check pattern: `Follow.find({ followerId: A, followingId: B, status: 'accepted' })` AND the reverse — both directions must exist
- Bilateral block check pattern: `Block.find({ $or: [{blockerId: A, blockedId: B}, {blockerId: B, blockedId: A}] })` — already proven in feed (FEED-02) and comment controllers

### Integration Points
- `src/app.js` — mount new `conversation.routes`/`message.routes`
- `src/server.js` — attach Socket.io server to the existing HTTP server (currently `app.listen`)
- New backend files: `src/models/Conversation.js`, `src/models/Message.js`, `src/controllers/conversation.controller.js`, `src/controllers/message.controller.js`, `src/services/socket.service.js` (naming at planner's discretion)
- `MINIREDESOCIAL_FRONT/src/App.jsx` — add chat routes (e.g. `/chat`, `/chat/:conversationId`, `/chat/new`)
- `MINIREDESOCIAL_FRONT/src/components/AppShell/AppShell.jsx` — add chat icon to header, mirroring the bell icon (D-02)

</code_context>

<specifics>
## Specific Ideas

- **Reference UI — explicit user citation:** "igual ao instagram" (like Instagram). Concretely: a header icon opens a Direct-Messages-style conversations list; a "new message" / compose icon inside that list opens a picker of people to message.
- **Conversation list item layout:** avatar + name + last-message preview + timestamp + unread dot (D-05) — full chat-app style, not bare-bones.
- **Entry flow, end to end:** header icon → conversations list → either (a) tap an existing conversation to open it, or (b) tap "new message" → picker of mutual followers → tap one → opens/creates the 1:1 conversation.

</specifics>

<deferred>
## Deferred Ideas

- **Group formation (CHAT-03)** — "Adding a third person to a conversation turns it into a group." Moved to REQUIREMENTS.md v2 (deferred section) this session. Reason: presentation date moved up to 2 days away; 1:1 chat alone is the achievable, demoable scope. ROADMAP.md Phase 6 success criteria updated to drop this.
- **Public/private groups (CHAT-04)** — "Groups can be public (searchable) or private." Deferred alongside CHAT-03, same reason.
- **`message_received` notification dispatch** — NOT deferred; explicitly noted in Phase 5's CONTEXT.md as waiting for chat to exist. It is now in scope for this phase (see Canonical References — wire it via `notification.service.create()`).

None beyond the scope cut already captured above — discussion otherwise stayed within the (now-reduced) phase boundary.

</deferred>

---

*Phase: 6-chat-groups-real-time*
*Context gathered: 2026-06-08*
