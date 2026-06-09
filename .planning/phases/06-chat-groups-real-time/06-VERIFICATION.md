---
phase: 06-chat-groups-real-time
verified: 2026-06-08T00:00:00Z
status: human_needed
score: 16/16 must-haves verified
overrides_applied: 0
human_verification:
  - test: "Open two browser sessions as mutual followers. From Session A, open /chat, tap the chat icon, then 'Nova mensagem', select the other user, and confirm the thread opens."
    expected: "Thread page opens at /chat/:conversationId. Sending a text message in Session A shows an optimistic bubble immediately; the message appears in Session B without page refresh. The conversations list in Session B shows the unread dot for that conversation."
    why_human: "Real-time socket delivery and optimistic UI state cannot be verified programmatically without a running server and two authenticated browser sessions."
  - test: "Attach and send an image in the thread page (ChatComposer camera icon)."
    expected: "Image renders inside the message bubble. S3 URL appears in mediaUrls on the server-side message doc. Bubble is resolved from optimistic (opacity 0.6) to full opacity."
    why_human: "S3 upload requires live credentials and a running server; image render requires visual inspection."
  - test: "Tap a message_received notification in the Notifications view."
    expected: "Navigation goes directly to /chat/:conversationId (the thread), not to the actor's profile."
    why_human: "Notification dispatch requires a live message send, a live notification and socket delivery — all requiring a running server and real user sessions."
  - test: "Verify AppShell chat badge count updates when a new message arrives."
    expected: "The badge on the MessageCircle icon shows the count of distinct unread conversation IDs derived from message_received notifications with read:false. Count updates when a new message arrives via the socket."
    why_human: "Badge update on incoming socket events requires a live server session."
---

# Phase 6: Chat Groups Real-Time Verification Report

**Phase Goal:** Mutually-following users can chat 1:1 in real time and send media — the highest-risk feature, landed last so it can be cut without breaking the product. Scope reduced to 1:1 only (groups deferred to v2) after the presentation date moved up to 2 days away.
**Verified:** 2026-06-08
**Status:** human_needed
**Re-verification:** No — initial verification

## Goal Achievement

### Observable Truths

| # | Truth | Status | Evidence |
|---|-------|--------|---------|
| 1 | Two mutually-following users can create/open a 1:1 conversation | VERIFIED | `conversation.controller.js` createConversation: bilateral Follow check (both 'accepted') + find-or-create on `{ isGroup:false, memberIds:{$all,size:2} }`. Test passes (7/7). |
| 2 | A non-mutual or blocked user attempting to start a chat gets 403 | VERIFIED | Controller returns 403 on unilateral follow and on bilateral block. Covered by tests (cases b, c). |
| 3 | A user attempting to start a chat with a user in a different institution gets 403 | VERIFIED | Explicit `String(targetUser.institutionId) !== String(req.user.institutionId)` check at line 28. Cross-institution test case (e) passes. |
| 4 | A Socket.io connection with a valid JWT authenticates; an invalid/missing token is rejected | VERIFIED | `socket.service.js` io.use() middleware reads `socket.handshake.auth.token`, calls `verifyToken`, sets `socket.data.user`. Tests: valid connects (1 pass), missing token rejects (1 pass), garbage token rejects (1 pass). |
| 5 | A user can list their conversations with last-message preview and other-member info | VERIFIED | `listConversations`: `Conversation.find({memberIds,isGroup:false}).populate('memberIds','name avatarUrl')` + N+1 `Message.findOne` per conversation. Tests: list returns with `lastMessage: null` when no messages. |
| 6 | A conversation member can send a text message and it persists | VERIFIED | `message.controller.js` createMessage: membership gate, `Message.create({conversationId, senderId, text})`. Test case (a) 201. |
| 7 | A conversation member can attach an image (stored in S3) and it persists in mediaUrls | VERIFIED | Upload loop calls `uploadMessageImage` (messages/ prefix verified). `s3.service.js` exports `uploadMessageImage`. Test case (b) image-only 201. |
| 8 | A non-member attempting to post to a conversation gets 403 | VERIFIED | `!conversation.memberIds.some(m => m.toString() === senderId)` → 403. Test case (e) passes. |
| 9 | Sending a message dispatches a message_received notification to the recipient whose refId is the conversationId (refModel 'Conversation') | VERIFIED | `notificationService.create('message_received', senderId, recipientId, conversationId, 'Conversation')`. Notification spy asserts refId === conversationId and refModel === 'Conversation'. Test case (a) passes. |
| 10 | Sending a message emits message:new to the recipient's socket room | VERIFIED | `req.app.get('io').to(recipientId.toString()).emit('message:new', message)`. Socket emit verified via no-op spy in test. |
| 11 | A member can list a conversation's messages, cursor-paginated reverse-chronological | VERIFIED | `listMessages`: `Message.find({conversationId}).sort({_id:-1}).limit(limit+1)` with hasMore/nextCursor. Tests (g) and listMessages non-member (403) pass. |
| 12 | The socket client connects with the JWT and exposes message:new events | VERIFIED | `src/lib/socket.js`: `io(url, { auth: { token: localStorage.getItem('fademy_token') }, autoConnect: false })`. `useChatSocket.js`: subscribes `message:new`, refreshes token on mount. |
| 13 | Tapping the chat header icon (next to the bell) opens the conversations list at /chat | VERIFIED | `AppShell.jsx` line 87: `onClick={() => navigate('/chat')}` on `.chatBtn` button with `MessageCircle` icon and `aria-label="Mensagens"`. |
| 14 | The conversations list shows avatar + name + last-message preview + timestamp + unread dot per row (D-05) | VERIFIED | `ConversationListItem.jsx`: avatar, name, preview (with "📷 Foto" fallback), `formatRelativeTime` timestamp, `unreadDot` span rendered when `unread` prop true. |
| 15 | Tapping a picker row creates/opens the conversation and navigates to its thread route | VERIFIED | `NewConversationPage.jsx` line 37: `api.post('/api/conversations', { userId: user._id })` → `navigate('/chat/${res.data.conversation._id}')`. 403 surfaced with documented copy. |
| 16 | Tapping a message_received notification navigates to /chat/:conversationId using notification.refId | VERIFIED | `NotificationCard.jsx` line 43-44: `else if (type === 'message_received') { navigate('/chat/${refId}') }`. ACTION_TEXT entry at line 25 confirmed. |

**Score:** 16/16 truths verified

### Required Artifacts

| Artifact | Expected | Status | Details |
|----------|----------|--------|---------|
| `src/models/Conversation.js` | Conversation Mongoose model | VERIFIED | `mongoose.model('Conversation', ...)`, fields memberIds, isGroup, createdById, `{timestamps:true}` (provides updatedAt), index `{memberIds:1, isGroup:1}`, no institutionId. |
| `src/models/Message.js` | Message Mongoose model | VERIFIED | `mongoose.model('Message', ...)`, fields conversationId, senderId, text (not required), mediaUrls, createdAt, index `{conversationId:1, _id:-1}`. |
| `src/controllers/conversation.controller.js` | createConversation + listConversations | VERIFIED | Both functions present, gated, exported. `listEligibleContacts` also exported (added as Plan 03 deviation). |
| `src/services/socket.service.js` | initSocket(httpServer) + JWT auth + per-user rooms | VERIFIED | `initSocket` returns io, CORS on Server constructor, `io.use()` handshake auth via `verifyToken`, `socket.join(socket.data.user.id)`. `getIO()` accessor exported. |
| `src/controllers/conversation.controller.test.js` | CHAT-01 gate coverage | VERIFIED | 7 tests pass: self 400, not-found 404, one-direction 403, block 403, cross-institution 403, find-or-create 201+200, list with lastMessage null. |
| `src/services/socket.service.test.js` | Handshake auth + room delivery | VERIFIED | 4 tests pass: valid JWT connects, no token rejects, garbage token rejects, room delivery only to targeted user. |
| `src/controllers/message.controller.js` | createMessage + listMessages | VERIFIED | Both functions present, gated, notification dispatch, socket emit, S3 upload loop, exported. |
| `src/services/s3.service.js` (uploadMessageImage) | messages/ prefix S3 upload | VERIFIED | `uploadMessageImage` function at line 76, key prefix `messages/`, exported at line 108. |
| `src/routes/message.routes.js` | Multer-wrapped POST+GET under /:id/messages | VERIFIED | File exists, defines `router.post('/:id/messages', authenticate, uploadImages, createMessage)` and `router.get('/:id/messages', authenticate, listMessages)`. |
| `src/controllers/message.controller.test.js` | CHAT-02 persistence + S3 mock + notification spy | VERIFIED | 9 tests pass: text 201, image 201, empty 400, bad-mimetype 400, non-member 403, no-conversation 404, listMessages, listMessages non-member, listMessages no-conversation. |
| `MINIREDESOCIAL_FRONT/src/lib/socket.js` | socket.io-client singleton, JWT auth.token, autoConnect:false | VERIFIED | `auth:{token}` object form, `autoConnect:false`, singleton exported. |
| `MINIREDESOCIAL_FRONT/src/hooks/useChatSocket.js` | connect-on-mount, message:new subscription, cleanup | VERIFIED | Connects on mount, `socket.on('message:new', handler)`, `socket.off` + `socket.disconnect()` on unmount. |
| `MINIREDESOCIAL_FRONT/src/components/ConversationListItem/ConversationListItem.jsx` | D-05 row layout | VERIFIED | avatar, name, preview (📷 Foto fallback), formatRelativeTime timestamp, unreadDot span conditionally rendered, whole row is `<Link to="/chat/:id">`. |
| `MINIREDESOCIAL_FRONT/src/pages/ConversationsListPage/ConversationsListPage.jsx` | /chat list page + Notification-derived unread state | VERIFIED | Fetches `/api/conversations` + `/api/notifications`, builds `unreadConvIds` Set from `message_received` with `read:false` on `refId`, passes `unread={unreadConvIds.has(String(conv._id))}` to each row. `useChatSocket` re-fetches on `message:new`. |
| `MINIREDESOCIAL_FRONT/src/pages/NewConversationPage/NewConversationPage.jsx` | /chat/new mutual-follower picker | VERIFIED | Fetches `/api/conversations/eligible-contacts`, row tap POSTs to `/api/conversations`, navigates to thread on success, 403 surfaced with documented gate copy. |
| `MINIREDESOCIAL_FRONT/src/components/MessageBubble/MessageBubble.jsx` | sent/received bubble + image + optimistic state | VERIFIED | Sent: `styles.sent` (accent bg); received: `styles.received`; image above text; no `dangerouslySetInnerHTML`; `pending` → opacity via CSS class; `failed` → RotateCw retry affordance. |
| `MINIREDESOCIAL_FRONT/src/components/ChatComposer/ChatComposer.jsx` | Fixed bottom composer, 5MB guard, send disabled when empty | VERIFIED | `position:fixed`-equivalent `.inputBar` class, 5MB guard (`MAX_FILE_BYTES = 5*1024*1024`), verbatim error copy, send button disabled when `!hasContent || submitting`, Spinner during submit. |
| `MINIREDESOCIAL_FRONT/src/pages/ConversationThreadPage/ConversationThreadPage.jsx` | /chat/:conversationId thread | VERIFIED | History fetch via `GET /api/conversations/:id/messages`, `useChatSocket` live append filtered by conversationId, multipart `api.post`, optimistic bubble, retry, empty thread "Diga oi para {Nome}", all 6 states. Stub fully replaced (no null export). |

### Key Link Verification

| From | To | Via | Status | Details |
|------|----|-----|--------|---------|
| `src/server.js` | `src/services/socket.service.js` | `initSocket(httpServer)` | WIRED | `const io = initSocket(httpServer); app.set('io', io)` at lines 11-13. |
| `src/services/socket.service.js` | `src/services/token.service.js` | `verifyToken` in `io.use()` handshake | WIRED | `const { verifyToken } = require('./token.service')` + `const decoded = verifyToken(token)` at line 29. |
| `src/controllers/conversation.controller.js` | `src/models/Follow.js` | `Follow.findOne` bilateral mutual-follow check | WIRED | `Follow.findOne({ followerId: userId, followingId: targetId, status: 'accepted' })` at lines 45-47. |
| `src/controllers/message.controller.js` | `src/services/notification.service.js` | `message_received` dispatch on send | WIRED | `notificationService.create('message_received', senderId, recipientId, conversationId, 'Conversation')` at lines 69-75. |
| `src/controllers/message.controller.js` | `socket.service.js` (io singleton) | `req.app.get('io').to(recipientId).emit('message:new', ...)` | WIRED | Lines 79-82: `const io = req.app.get('io'); if (io && recipientId) io.to(recipientId.toString()).emit(...)`. |
| `src/controllers/message.controller.js` | `src/services/s3.service.js` | `uploadMessageImage` for attachments | WIRED | `const { uploadMessageImage } = require('../services/s3.service')` + called in upload loop at line 49. |
| `AppShell.jsx` | `/chat` route | `navigate('/chat')` on chat icon click | WIRED | `onClick={() => navigate('/chat')}` at line 87. |
| `ConversationsListPage.jsx` | `GET /api/notifications` | derive unread conversationIds from `message_received` `read:false` `refId` | WIRED | Lines 43-46: filter `type === 'message_received' && n.read === false`, map `n.refId`. |
| `NewConversationPage.jsx` | `POST /api/conversations` | find-or-create on row tap | WIRED | `api.post('/api/conversations', { userId: user._id })` at line 37. |
| `src/lib/socket.js` | Socket.io server | `io(url, { auth: { token } })` | WIRED | `auth: { token: localStorage.getItem('fademy_token') }` at line 4. |
| `ConversationThreadPage.jsx` | `POST /api/conversations/:id/messages` | multipart send (REST source-of-truth) | WIRED | `api.post('/api/conversations/${conversationId}/messages', formData)` at line 168. |
| `ConversationThreadPage.jsx` | `useChatSocket` message:new | live append filtered by conversationId | WIRED | `useChatSocket({ onMessage: handleSocketMessage })` at line 143; `handleSocketMessage` checks `String(msg.conversationId) !== String(conversationId)` at line 139. |
| `NotificationCard.jsx` | `/chat/:conversationId` | `message_received` branch navigates to `notification.refId` | WIRED | `else if (type === 'message_received') { navigate('/chat/${refId}') }` at lines 43-44. |

### Data-Flow Trace (Level 4)

| Artifact | Data Variable | Source | Produces Real Data | Status |
|----------|---------------|--------|--------------------|--------|
| `ConversationsListPage.jsx` | `conversations` | `GET /api/conversations` → `listConversations` → `Conversation.find` + `Message.findOne` | Yes — DB queries | FLOWING |
| `ConversationsListPage.jsx` | `unreadConvIds` | `GET /api/notifications` → filter `message_received && read:false` → Set of `refId` | Yes — derived from DB | FLOWING |
| `ConversationThreadPage.jsx` | `messages` | `GET /api/conversations/:id/messages` → `listMessages` → `Message.find` | Yes — DB query | FLOWING |
| `AppShell.jsx` | `unreadChatCount` | `GET /api/notifications` → filter `message_received && read:false` → Set.size | Yes — derived from DB | FLOWING |

### Behavioral Spot-Checks

| Behavior | Command | Result | Status |
|----------|---------|--------|--------|
| conversation.controller tests (7 cases) | `node --test src/controllers/conversation.controller.test.js` | 7 pass, 0 fail | PASS |
| socket.service tests (4 cases) | `node --test src/services/socket.service.test.js` | 4 pass, 0 fail | PASS |
| message.controller tests (9 cases) | `node --test src/controllers/message.controller.test.js` | 9 pass, 0 fail | PASS |
| Full backend test suite | `npm test` | 98 pass, 0 fail | PASS |

### Probe Execution

Step 7c SKIPPED — no `scripts/*/tests/probe-*.sh` files declared or found.

### Requirements Coverage

| Requirement | Source Plan | Description | Status | Evidence |
|-------------|------------|-------------|--------|---------|
| CHAT-01 | 06-01-PLAN.md | Mutually-following users can chat 1:1 in real time (Socket.io) | SATISFIED | Conversation model + gated controller + Socket.io service + handshake auth + per-user rooms — all implemented, tested (7+4 tests), wired. |
| CHAT-02 | 06-02-PLAN.md | Messages support text and media (images/files) | SATISFIED | `message.controller.js` supports text-only, image-only, both; S3 via `uploadMessageImage`; `message_received` notification with `refId=conversationId`; `message:new` socket emit — 9 tests pass. |
| FRNT-07 | 06-03-PLAN.md, 06-04-PLAN.md | Chat / conversations view | SATISFIED (pending human) | Chat icon in AppShell, /chat list page, /chat/new picker, /chat/:id thread, MessageBubble, ChatComposer, NotificationCard branch — all wired to live backend. Human verification required for real-time behavior and visual UX. |

### Anti-Patterns Found

| File | Line | Pattern | Severity | Impact |
|------|------|---------|----------|--------|
| None found | — | No TBD/FIXME/XXX markers | — | — |
| No stub returns | — | No `return null` as component stub | — | The two `return null` in `ConversationThreadPage.jsx` are in the `getCurrentUserId()` helper, not component-level stubs. |
| No hardcoded empty arrays passed as props | — | All list data flows from API | — | — |

### Human Verification Required

#### 1. Full Real-Time Send/Receive Loop

**Test:** Open two browser sessions authenticated as mutual followers. Session A: open /chat → tap "Nova mensagem" → select Session B's user → confirm thread opens. Send a text message.
**Expected:** Optimistic bubble appears immediately in Session A. The same message appears live in Session B's open thread without any page refresh. The conversations list in Session B shows the unread dot for the conversation.
**Why human:** Real-time socket delivery + optimistic UI state requires a running server, live JWT auth, two simultaneous browser sessions.

#### 2. Image Attachment Send

**Test:** In an open thread, tap the camera icon in ChatComposer, select an image under 5 MB, tap Send.
**Expected:** Image renders inside the message bubble above any text. Bubble resolves from semi-transparent (pending) to full opacity. The other user sees the image in their thread.
**Why human:** S3 upload requires live credentials; image bubble rendering requires visual inspection; two-session behavior for delivery.

#### 3. message_received Notification Deep-Link

**Test:** With a message sent to user B, user B opens the Notifications page and taps the "enviou uma mensagem para você" notification.
**Expected:** Navigation goes directly to `/chat/:conversationId` (the correct thread), not to the sender's profile page.
**Why human:** Requires a live message send, resulting notification, and user navigating through the notification tap — cannot be verified without a running server and real user sessions.

#### 4. AppShell Chat Badge Update

**Test:** With user B having an unread message, observe the chat badge on the MessageCircle icon in the header. Then open the thread and observe again after viewing the conversation.
**Expected:** Badge shows the count of conversations with unread `message_received` notifications. The badge uses accent color when count > 0. Arriving `message:new` socket events increment the count immediately (via re-fetch).
**Why human:** Badge behavior on socket events and count accuracy require visual inspection in a running browser session.

### Gaps Summary

No gaps found. All 16 must-have truths are verified in the codebase. The 4 human verification items above are UX/real-time behaviors that cannot be verified by static analysis — they do not block the phase from being considered implemented; they require live testing before the demo.

---

_Verified: 2026-06-08_
_Verifier: Claude (gsd-verifier)_
