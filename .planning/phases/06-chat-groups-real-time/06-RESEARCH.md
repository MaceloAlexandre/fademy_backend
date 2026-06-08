# Phase 6: Chat (Real-Time, 1:1) - Research

**Researched:** 2026-06-08
**Domain:** Real-time chat (Socket.io + Express/Mongoose REST), S3 media uploads, React SPA real-time client integration
**Confidence:** HIGH

## Summary

This phase adds the first real-time feature to Fademy: 1:1 chat between mutually-following users, gated on the existing social graph (`Follow`/`Block` models from Phase 3), with text + image messages stored via the existing S3 upload pattern (Phase 4's `uploadPostImage`), and delivered live via Socket.io. The codebase is a clean, consistent Express/Mongoose REST API (CommonJS, controller/router/model layering, JWT auth via `authenticate` middleware) plus a React 18 + Vite SPA with axios and CSS Modules. Nothing in the existing stack uses WebSockets — `socket.io`/`socket.io-client` are **not yet installed** and must be added as a new dependency pair (verified on npm registry: v4.8.3, ~14M/13M weekly downloads, official `socketio` GitHub org, no postinstall scripts).

The de-risking path is straightforward because every building block this phase needs already has a proven sibling in the codebase: `Follow`/`Block` bilateral query patterns (Phase 3), `uploadPostImage` + multer memory-storage + key-prefix S3 pattern (Phase 4), `notification.service.create()` fire-and-forget dispatch (Phase 5), and `authenticate`/`verifyToken` JWT verification (Phase 1) — the last of which is the direct model for the Socket.io handshake auth middleware (`io.use()` reading `socket.handshake.auth.token`, verifying with the same `verifyToken`, and rejecting with `next(new Error(...))`). The conversations/messages schemas are even pre-specified in the repo-root `CONTEXT.md`. The only genuinely new pattern is wiring `http.createServer(app)` + `new Server(httpServer)` in `server.js` (replacing the current `app.listen` call) and the Socket.io room-per-user delivery model (`socket.join(userId)`, `io.to(userId).emit(...)`).

**Primary recommendation:** Install `socket.io`/`socket.io-client` v4.8.3, attach Socket.io to the existing HTTP server in `server.js`, authenticate the handshake with the existing `verifyToken` (mirroring `auth.middleware.js`), use a per-user room (`socket.join(userId)`) for targeted delivery, persist every message via a REST `POST /api/conversations/:id/messages` endpoint that also emits over the socket (REST-as-source-of-truth + socket-as-transport — never trust client-emitted events to write to the DB), and reuse `uploadPostImage`'s exact multer + S3 pattern with a `messages/` key prefix for image attachments.

## Architectural Responsibility Map

| Capability | Primary Tier | Secondary Tier | Rationale |
|------------|-------------|----------------|-----------|
| Mutual-follow + block gate (can A message B?) | API / Backend | — | Authorization must be enforced server-side on both REST (create conversation) and socket (join/send) paths; mirrors existing `Follow`/`Block` bilateral checks (SOCL-05) |
| Conversation/message persistence | Database / Storage | API / Backend | MongoDB via Mongoose models `Conversation`/`Message`; API owns writes, DB owns durability |
| Real-time message delivery | API / Backend | Browser / Client | Socket.io server emits to per-user rooms; browser receives and renders — but REST POST is the durable write path, socket is transport-only |
| Message media upload | API / Backend | CDN / Static (S3) | Same pattern as `uploadPostImage`: browser → multer (memory) → API → S3; S3 serves the bytes afterward |
| Conversation list / unread state | API / Backend | Browser / Client | API computes last-message preview + unread counts; browser renders and optionally optimistic-updates on socket events |
| Auth handshake for socket connection | API / Backend | — | Socket.io `io.use()` middleware verifies the same JWT used for REST (`verifyToken`), attaches `socket.data.user`; no separate auth scheme |
| `message_received` notification dispatch | API / Backend | — | Reuses `notification.service.create()` — fire-and-forget, called from the message-creation path, not from socket handlers (keeps it on the durable write path) |
| Conversations/chat UI (list, thread, composer) | Browser / Client | — | React SPA renders conversation list, thread view, composer; connects to Socket.io with JWT in `auth` handshake option |

## Standard Stack

### Core
| Library | Version | Purpose | Why Standard |
|---------|---------|---------|--------------|
| `socket.io` | 4.8.3 | WebSocket/long-polling server, attaches to existing Express HTTP server | The only realistic real-time choice already locked in by the project (PROJECT.md / STATE.md decision: "Socket.io for chat"); official, ~14M weekly downloads, actively maintained [VERIFIED: npm registry — version, download count, repo URL confirmed via `npm view`] |
| `socket.io-client` | 4.8.3 | Browser-side Socket.io client for the React SPA | Must match server major version (v4); official client, ~13M weekly downloads [VERIFIED: npm registry] |

### Supporting
| Library | Version | Purpose | When to Use |
|---------|---------|---------|-------------|
| `multer` | ^2.1.1 (already installed) | Memory-storage multipart parsing for message image attachments | Already used identically in `post.routes.js` for post images — reuse the exact `multer.memoryStorage()` + `limits: { fileSize: MAX_UPLOAD_BYTES }` setup, just with a different field name/array count |
| `@aws-sdk/client-s3` | ^3.1055.0 (already installed) | S3 upload via existing `s3.service.js` | Add a `uploadMessageImage` function to `s3.service.js` mirroring `uploadPostImage` but with a `messages/` key prefix |

### Alternatives Considered
| Instead of | Could Use | Tradeoff |
|------------|-----------|----------|
| Socket.io | Raw `ws` library | Lower-level, no automatic reconnection/fallback/room abstraction — would mean hand-rolling exactly the things Socket.io provides; wrong choice for a 2-day window and a team with "limited real-time experience" (per CONTEXT.md note) |
| Socket.io | Server-Sent Events (SSE) + REST POST for sending | SSE is one-directional (server→client only); would still need a separate channel for typing/presence if ever added, and the project decision already locked Socket.io |
| Per-user rooms | Per-conversation rooms only | Per-user rooms (`socket.join(userId)`) are simpler to reason about for 1:1 (the recipient may have multiple tabs/devices) and trivially extend to "new conversation created" notifications without the recipient having joined a room yet; per-conversation rooms require both members to already be "in" the conversation view to receive events |

**Installation:**
```bash
npm install socket.io@4.8.3
```
(in `MINIREDESOCIAL_FRONT/`)
```bash
npm install socket.io-client@4.8.3
```

**Version verification:** Confirmed via `npm view socket.io version` → `4.8.3` (published 2025-12-23) and `npm view socket.io-client version` → `4.8.3`. Both match the latest major (v4), avoiding the v2/v4 incompatibility documented in Socket.io's own NestJS compatibility notes — client and server **must** be on the same major version.

## Package Legitimacy Audit

| Package | Registry | Age | Downloads | Source Repo | slopcheck | Disposition |
|---------|----------|-----|-----------|-------------|-----------|-------------|
| `socket.io` | npm | ~13 yrs (est. from long-tenured major-version history; latest 4.8.3 published 2025-12-23) | ~14.5M/wk | github.com/socketio/socket.io | [OK] | Approved |
| `socket.io-client` | npm | ~13 yrs (co-released with socket.io) | ~12.9M/wk | github.com/socketio/socket.io | [OK] (slopcheck flagged the `-client` suffix as a "classic LLM naming pattern" but explicitly noted "package is established") | Approved |

**Packages removed due to slopcheck [SLOP] verdict:** none
**Packages flagged as suspicious [SUS]:** none

Both packages confirmed via the official `socket.io` documentation (Context7-equivalent: official docs at socket.io/docs/v4) AND via `npm view` (registry existence, no `postinstall` scripts present — only `prepack`/`compile`/`test` which do not run on consumer install). Per the package-name-provenance rule: these names came from the project's own pre-locked architectural decision ("Socket.io for chat" — see STATE.md Roadmap decisions) and official socket.io.com documentation, not from search/training-data guesswork — they are tagged `[VERIFIED: npm registry]` in the Standard Stack table above on that basis.

> Note: a `slopcheck install` dry-run was executed to obtain the verdict above; it had the side effect of running `npm install` and modifying `package.json`/`package-lock.json`/`node_modules`. These changes were reverted (`git checkout -- package.json package-lock.json` + manual removal of the added `node_modules` entries) before this research concluded — the actual install is the planner/executor's responsibility, not research's.

## Architecture Patterns

### System Architecture Diagram

```
[React SPA]                          [Express API + Socket.io Server]              [MongoDB]            [S3]
                                       (single Node process, one HTTP server)

  AppShell                                    app.js (Express routes)
   │ chat icon (D-02)                          │
   │                                            ├─ POST /api/conversations  ───────▶ Conversation.create()
   ▼                                            │   (gated: mutual-follow + ¬block)        │
ConversationsListPage                           │                                          │
   │ GET /api/conversations ───────────────────▶│                                          │
   │ (last message, unread, members)            │                                          │
   │                                            ├─ GET /api/conversations/:id/messages ───▶ Message.find().sort(-createdAt)
   ▼                                            │   (cursor-paginated, like notifications)
ConversationThreadPage                          │
   │                                            ├─ POST /api/conversations/:id/messages ──▶ Message.create() ─┐
   │  1. socket.io-client connects              │   (multer→S3 for images, then persist)                     │
   │     io(url, { auth: { token } })           │                                          │                  │
   │                                            │                                          ▼                  │
   │  2. on connect, server middleware           │                                   notification.service
   │     io.use((socket,next) => {               │                                     .create('message_received')
   │       verifyToken(socket.handshake          │                                                            │
   │         .auth.token) → socket.data.user     │                                                            │
   │       socket.join(socket.data.user.id)      │                                                            │
   │     })                                      │                                                            │
   │                                            │◀─────── after persist: io.to(recipientUserId)               │
   │  3. socket.on('message:new', handler)      │           .emit('message:new', messageDoc)                 │
   │     appends to thread, updates list         │                                                            │
   │                                            │                                                             │
   │  4. composer: image picked                 │   multer.memoryStorage() ──▶ uploadMessageImage() ─────────┼──▶ S3 PutObject
   │     → multipart POST (REST, not socket)    │     (mirrors uploadPostImage,                              │      (messages/<uuid>.<ext>)
   │                                            │      messages/ key prefix)                                  │
   ▼                                            │                                                             │
ComposerBar (text + image picker)               └─ Socket.io attaches to the SAME http.Server                │
                                                    instance Express listens on (httpServer.listen,          │
                                                    NOT app.listen — critical integration detail)            ▼
                                                                                                       buildObjectUrl(key)
                                                                                                       → public read URL
                                                                                                         stored in mediaUrls[]
```

**Primary use-case trace (send a message with an image):**
1. User opens `ConversationThreadPage` → SPA establishes a Socket.io connection passing the JWT in `auth: { token }`.
2. Server's `io.use()` middleware verifies the token (same `verifyToken` as REST), attaches `socket.data.user = { id, role, institutionId }`, and joins the socket to room `socket.data.user.id`.
3. User picks an image and types text, taps send → SPA does a normal `multipart/form-data` **REST POST** to `/api/conversations/:id/messages` (NOT a socket emit — sockets are for receiving live updates, REST is for the durable write, exactly like posts).
4. Controller validates: caller is a member of the conversation (membership check), uploads the image via `uploadMessageImage` (mirrors `uploadPostImage`), creates the `Message` document, fires `notification.service.create('message_received', ...)`.
5. Controller (or a thin socket-emitting helper called right after persist) emits `io.to(recipientUserId).emit('message:new', messageDoc)` — delivering to the recipient's room (any/all of their open tabs/devices).
6. Sender's own UI updates via the REST response (optimistic or post-hoc); recipient's UI updates live via the `message:new` socket event.

### Recommended Project Structure

Backend (additions to existing `src/`):
```
src/
├── models/
│   ├── Conversation.js      # memberIds[], isGroup (always false this phase), createdById, updatedAt
│   └── Message.js           # conversationId, senderId, text, mediaUrls[], createdAt
├── controllers/
│   ├── conversation.controller.js   # list, create (gated), getMessages
│   └── message.controller.js        # createMessage (or fold into conversation.controller — planner's call)
├── routes/
│   ├── conversation.routes.js
│   └── message.routes.js            # (or nest under conversation.routes — REST convention: /api/conversations/:id/messages)
├── services/
│   ├── s3.service.js                # ADD: uploadMessageImage (mirrors uploadPostImage, messages/ prefix)
│   └── socket.service.js            # initSocket(httpServer): handshake auth, room join, emit helpers
└── server.js                        # MODIFIED: http.createServer(app) + new Server(httpServer) + initSocket
```

Frontend (additions to existing `MINIREDESOCIAL_FRONT/src/`):
```
src/
├── lib/
│   └── socket.js                    # singleton socket.io-client instance, connect/disconnect helpers
├── hooks/
│   └── useChatSocket.js             # connects on mount, exposes message events, handles reconnection
├── components/
│   ├── ConversationListItem/        # avatar+name+preview+timestamp+unread dot (D-05), mirrors NotificationCard
│   ├── MessageBubble/               # sent/received styling
│   └── ChatComposer/                # text input + image picker (mirrors PhotoUpload pattern), send button
└── pages/
    ├── ConversationsListPage/       # GET /api/conversations, "new message" icon → picker
    ├── NewConversationPage/         # picker of mutual followers (reuse UserResultCard-like layout, no FollowButton)
    └── ConversationThreadPage/      # GET .../messages (cursor), socket subscription, composer
```

### Pattern 1: Socket.io attached to the existing Express HTTP server
**What:** Replace `app.listen(...)` with an explicit `http.createServer(app)` + `new Server(httpServer, { cors: {...} })`, then call `httpServer.listen(...)`.
**When to use:** Always, when adding Socket.io to an existing Express app — `app.listen()` creates its own internal HTTP server that Socket.io cannot attach to.
**Example:**
```javascript
// Source: https://socket.io/docs/v4/server-initialization/ (official docs, verified)
const express = require('express');
const { createServer } = require('http');
const { Server } = require('socket.io');

const app = express();
const httpServer = createServer(app);
const io = new Server(httpServer, {
  cors: { origin: env.FRONTEND_ORIGIN } // mirror existing cors() config in app.js
});

io.on('connection', (socket) => { /* ... */ });

httpServer.listen(env.PORT, () => { /* existing log message */ });
```
This requires restructuring `src/server.js` (currently `connectDB().then(() => app.listen(...))`). The `io` instance and its helpers should live in a new `src/services/socket.service.js` exposing an `initSocket(httpServer)` function, keeping `server.js` thin and matching the project's service-layer convention.

### Pattern 2: JWT handshake authentication (mirrors `auth.middleware.js`)
**What:** Verify the same JWT used for REST during the Socket.io connection handshake, using `socket.handshake.auth.token` (NOT the `extraHeaders` approach, which the official docs note "only works with HTTP long-polling enabled" because "browsers do not provide a way to provide additional headers for WebSocket connections").
**When to use:** On every socket connection — fail closed, exactly like `authenticate` does for REST.
**Example:**
```javascript
// Source: https://socket.io/docs/v4/middlewares/ (official docs, verified)
// Server — mirrors src/middlewares/auth.middleware.js exactly
const { verifyToken } = require('../services/token.service');

io.use((socket, next) => {
  const token = socket.handshake.auth && socket.handshake.auth.token;
  if (!token) return next(new Error('Authentication required'));
  try {
    const decoded = verifyToken(token); // same HS256-pinned verify as REST (T-01-XX)
    socket.data.user = { id: decoded.sub, role: decoded.role, institutionId: decoded.institutionId };
    return next();
  } catch (_err) {
    return next(new Error('Invalid or expired token'));
  }
});

io.on('connection', (socket) => {
  socket.join(socket.data.user.id); // per-user room — see Pattern 3
});
```
```javascript
// Client — pass token at connect time (Source: https://socket.io/docs/v4/client-options/, official docs)
import { io } from 'socket.io-client';
const socket = io(import.meta.env.VITE_API_URL, {
  auth: { token: localStorage.getItem('fademy_token') }
});

// Token-refresh / reconnect pattern (official docs):
socket.on('connect_error', (err) => {
  if (err.message === 'Invalid or expired token') {
    // re-fetch token if your app refreshes JWTs; Fademy currently has no refresh flow,
    // so this should redirect to /login (mirrors RequireAuth's behavior on 401)
  }
});
```

### Pattern 3: Per-user rooms for targeted delivery
**What:** Join every authenticated socket to a room named after the user's ID; emit to that room to reach all of that user's open tabs/devices.
**When to use:** Any time you need to push an event to "this specific user," which covers every delivery need in this phase (`message:new`, and optionally a "conversation:created" event for the picker flow).
**Example:**
```javascript
// Source: https://socket.io/docs/v4/rooms/ (official docs — "Per-user delivery" pattern, verified)
socket.join(socket.data.user.id);
// later, after persisting a message:
io.to(recipientUserId.toString()).emit('message:new', messagePayload);
```
Rooms are server-only — "Upon disconnection, sockets automatically leave all their rooms without requiring manual cleanup" (official docs), so no explicit cleanup logic is needed.

### Pattern 4: REST-as-source-of-truth, socket-as-transport
**What:** Never let a socket event directly write to the database. The client sends messages via a normal authenticated REST POST (which validates membership, uploads media, persists, dispatches notification); the server then emits a socket event to notify the recipient. This is the same shape as the existing notification flow (REST creates → `notification.service.create()` fires → polling picks it up later) — chat just swaps "polling" for "live push."
**When to use:** Always, for this phase. Avoids a second, parallel authorization surface (you'd otherwise have to re-implement the mutual-follow + block + membership checks inside socket event handlers).
**Example:**
```javascript
// conversation/message controller — the durable write path
async function createMessage(req, res) {
  const { id: senderId } = req.user;
  const conversation = await Conversation.findById(req.params.id);
  if (!conversation || !conversation.memberIds.some(m => m.toString() === senderId)) {
    return res.status(403).json({ error: 'Forbidden' });
  }
  // ... upload media via uploadMessageImage, create Message ...
  const message = await Message.create({ conversationId, senderId, text, mediaUrls });
  await notificationService.create('message_received', senderId, recipientId, message._id, 'Message');

  const io = req.app.get('io'); // or import the singleton from socket.service
  io.to(recipientId.toString()).emit('message:new', message);

  return res.status(201).json({ message });
}
```

### Pattern 5: Mutual-follow + block gate on conversation creation (reuse, don't reinvent)
**What:** Before creating (or returning an existing) 1:1 conversation, verify (a) no block exists in either direction (SOCL-05) and (b) both `Follow` documents exist with `status: 'accepted'` in both directions.
**When to use:** `POST /api/conversations` (find-or-create flow triggered by tapping a user in the "new message" picker).
**Example:**
```javascript
// Mirrors the bilateral patterns already proven in follow.controller.js / post.controller.js getFeed
const blocked = await Block.findOne({
  $or: [
    { blockerId: userId, blockedId: targetId },
    { blockerId: targetId, blockedId: userId },
  ],
});
if (blocked) return res.status(403).json({ error: 'Forbidden' });

const [aFollowsB, bFollowsA] = await Promise.all([
  Follow.findOne({ followerId: userId, followingId: targetId, status: 'accepted' }),
  Follow.findOne({ followerId: targetId, followingId: userId, status: 'accepted' }),
]);
if (!aFollowsB || !bFollowsA) {
  return res.status(403).json({ error: 'Both users must follow each other to start a chat' });
}

// find-or-create — memberIds order-independent lookup
let conversation = await Conversation.findOne({
  isGroup: false,
  memberIds: { $all: [userId, targetId], $size: 2 },
});
if (!conversation) {
  conversation = await Conversation.create({
    memberIds: [userId, targetId],
    isGroup: false,
    createdById: userId,
  });
}
```

### Anti-Patterns to Avoid
- **Trusting socket-emitted "send message" events to write to the DB:** Bypasses REST-layer validation/auth that's already battle-tested; creates a second authorization surface to maintain and audit. Use REST POST + socket emit-on-persist instead (Pattern 4).
- **Using `app.listen()` after adding Socket.io:** Silently creates a second, unconnected HTTP server — Socket.io will never receive connections. Must use `http.createServer(app)` (Pattern 1).
- **Per-conversation rooms as the *only* delivery mechanism:** A user who hasn't yet opened a conversation thread (e.g., receiving a brand-new conversation's first message) won't be in that room and will miss the live event entirely. Per-user rooms (Pattern 3) sidestep this; per-conversation rooms can be added *additionally* later (e.g., for typing indicators) but should not replace per-user delivery.
- **`extraHeaders` for handshake auth:** Documented by Socket.io itself to only work with long-polling, not WebSocket — fragile and will silently degrade. Use `auth: { token }` + `socket.handshake.auth.token` (Pattern 2).
- **Re-deriving `institutionId`/`senderId` from the request body:** Exactly the IDOR/spoofing class of bug the project has guarded against everywhere (T-02-03, T-04-05, T-04C-02). `senderId` and `institutionId` (if ever needed) must come from `req.user` / `socket.data.user`, never from client payloads.

## Don't Hand-Roll

| Problem | Don't Build | Use Instead | Why |
|---------|-------------|-------------|-----|
| WebSocket transport, fallback, reconnection | Custom `ws`-based reconnect/heartbeat logic | `socket.io` + `socket.io-client` (already the locked architectural decision) | Socket.io provides automatic reconnection with backoff (`reconnectionDelay`/`reconnectionDelayMax`), transport fallback (polling↔websocket), and room abstraction out of the box — reimplementing any of this in 2 days is the textbook "highest-risk feature" trap |
| Per-user message routing | A custom map of `userId → socket[]` with manual cleanup on disconnect | Socket.io rooms (`socket.join(userId)`, `io.to(userId).emit(...)`) | Rooms auto-cleanup on disconnect ("sockets automatically leave all their rooms" per official docs); a hand-rolled map needs explicit disconnect handlers and risks memory leaks across reconnects/multi-tab |
| S3 upload plumbing for message images | A new multer config + S3 client + key-naming scheme | Extend `s3.service.js` with `uploadMessageImage` mirroring `uploadPostImage` (same `EXTENSION_MAP`, same `crypto.randomUUID()` key generation, same `buildObjectUrl`) | The exact problem (server owns the S3 key, client never controls it — T-02-08b) is already solved; a new implementation risks reintroducing the exact vulnerability class that was fixed once |
| JWT verification for the socket handshake | A second token-verification routine (e.g., decoding without pinning algorithm) | The existing `verifyToken` from `token.service.js` (HS256-pinned per [01-02]) | A second, slightly-different verification path is a classic place for an `alg: none` or algorithm-confusion regression to creep back in |
| Notification dispatch on new message | Inline `Notification.create()` in the message controller | `notificationService.create('message_received', senderId, recipientId, messageId, 'Message')` | NOTF-02 explicitly requires "Notifications are dispatched via a notification service (never inline in controllers)" — already proven, already wired into `Notification.js`'s enum |

**Key insight:** Every "hard part" of this phase — transport, routing, auth, uploads, notification dispatch — already has either an off-the-shelf library (Socket.io) or a proven in-repo pattern (S3 service, notification service, JWT verify, bilateral Follow/Block checks). The only *new* code is the glue: wiring these together into `Conversation`/`Message` models and two new controllers. Resist the urge to "improve" any of the proven patterns while reusing them — surgical reuse, not refactoring, fits both the 2-day window and CLAUDE.md's "Surgical Changes" directive.

## Common Pitfalls

### Pitfall 1: `app.listen()` silently breaks Socket.io
**What goes wrong:** Socket.io never receives any connections; client gets connection timeouts or `connect_error` with no useful message.
**Why it happens:** `app.listen(port)` is shorthand for `http.createServer(app).listen(port)` — it creates its OWN internal HTTP server instance that `new Server(...)` never sees.
**How to avoid:** Explicitly create `const httpServer = createServer(app)`, pass `httpServer` to `new Server(httpServer, ...)`, and call `httpServer.listen(...)` (Pattern 1). This is a one-line-looking change in `server.js` that is easy to get subtly wrong (e.g., calling `app.listen` AND creating an `io` instance separately).
**Warning signs:** Client logs show repeated `connect_error` / `xhr poll error`; server logs show the port is listening but `io.engine.clientsCount` stays at 0.

### Pitfall 2: CORS misconfiguration breaks the socket handshake even though REST works fine
**What goes wrong:** REST API calls succeed (axios + `cors()` middleware works), but the Socket.io handshake fails with a CORS error in the browser console.
**Why it happens:** Socket.io's CORS is configured **separately**, on the `Server` constructor's `cors` option — it does NOT inherit Express's `cors()` middleware (the underlying `engine.io` transport handles its own preflight/handshake requests outside the Express middleware chain for WebSocket upgrades).
**How to avoid:** Pass the same `origin: env.FRONTEND_ORIGIN` (and any other matching options) to `new Server(httpServer, { cors: { origin: env.FRONTEND_ORIGIN, credentials: false } })`.
**Warning signs:** Browser console shows a CORS error specifically on the `socket.io/?EIO=4&transport=polling` request, while all `/api/...` requests succeed.

### Pitfall 3: Conversation find-or-create races / duplicate 1:1 conversations
**What goes wrong:** If two users tap "message" on each other near-simultaneously (or a user double-taps), two `Conversation` documents with the same `memberIds` pair can be created.
**Why it happens:** The `findOne` then `create` sequence in Pattern 5 is not atomic — no unique index exists on `memberIds` (and a naive compound index wouldn't handle order-independence or array-vs-array matching well).
**How to avoid:** For a 2-day MVP, accept the small race window (it's cosmetically annoying — two threads with the same person — but not a security or data-integrity issue) OR add an application-level lock-free guard: query with `{ isGroup: false, memberIds: { $all: [a, b], $size: 2 } }` immediately before create, and if a duplicate slips through, the conversation list UI can simply show both (acceptable for MVP). Do not over-engineer a unique-index solution under this timeline — flag it as a known minor edge case instead.
**Warning signs:** A user sees two separate threads with the same person after rapid double-taps in the picker.

### Pitfall 4: Forgetting that `socket.data.user` must be (re)populated on every reconnect
**What goes wrong:** After a network blip, Socket.io auto-reconnects — but if the handshake middleware isn't re-run (or the client doesn't resend `auth`), the reconnected socket has no `socket.data.user`, and `socket.join(undefined)` silently creates a junk room.
**Why it happens:** Socket.io re-runs `io.use()` middleware on every reconnection attempt by default (the handshake happens again), but ONLY if the client passes `auth` again — which the object form (`auth: { token }`) does automatically, while the function form (`auth: (cb) => cb({...})`) re-evaluates fresh each time (useful if the token can expire and refresh).
**How to avoid:** Use the object form `auth: { token }` for simplicity (Fademy has no token-refresh flow — JWTs are long-lived per `JWT_EXPIRES_IN: '7d'`), OR the callback form if you want to re-read `localStorage` on each (re)connection attempt (slightly more robust against a user logging out in another tab). Either way, verify `socket.data.user` exists before calling `socket.join()` in the connection handler.
**Warning signs:** `io.engine.clientsCount` grows but `io.to(userId).emit(...)` reaches nobody; server logs show rooms with `undefined` or garbage names.

### Pitfall 5: Treating Socket.io events as the authorization boundary
**What goes wrong:** A client could, in principle, emit a custom socket event (e.g., `message:send`) directly with a forged `conversationId`/`recipientId`, bypassing the membership/mutual-follow/block checks that live in the REST controller.
**Why it happens:** It's tempting to "skip the round-trip" and have the client emit messages directly over the socket for lower latency.
**How to avoid:** Strictly follow Pattern 4 — sockets only ever *receive* server-pushed events (`message:new`); the client always sends via authenticated REST POST, which re-runs every authorization check. This is also simpler to test (existing `mongodb-memory-server` + supertest-less `makeRes()` controller-test pattern works unchanged).
**Warning signs:** Code review finds an `socket.on('message:send', ...)` handler that calls `Message.create()` directly.

## Code Examples

### Conversation list query with last-message preview (no aggregation precedent in repo — two viable approaches)
```javascript
// Source: pattern synthesized from existing Notification list (notification.controller.js)
// + Mongoose 8.x aggregation pipeline docs (official: mongoosejs.com/docs/api/aggregate.html)
//
// Option A (simpler, fits 2-day window): N+1 but bounded — conversation lists are short for MVP.
// Fetch conversations the user is a member of, then fetch the latest message per conversation.
const conversations = await Conversation.find({ memberIds: userId, isGroup: false })
  .populate('memberIds', 'name avatarUrl')
  .sort({ updatedAt: -1 })
  .lean();

const withPreviews = await Promise.all(conversations.map(async (c) => {
  const lastMessage = await Message.findOne({ conversationId: c._id })
    .sort({ _id: -1 })
    .lean();
  const unreadCount = await Message.countDocuments({
    conversationId: c._id,
    senderId: { $ne: userId },
    readBy: { $ne: userId }, // requires a readBy[] field on Message — planner's call whether to add it
  });
  return { ...c, lastMessage, unreadCount };
}));

// Option B (more "correct" at scale): a single aggregation with $lookup — more code, more risk
// of a subtle pipeline bug under time pressure. Recommend Option A for this phase.
```
**Note:** The `CONTEXT.md` schema for `messages` does NOT include a `readBy`/`read` field — only `Notification` has `read`. The planner must decide: either (a) skip per-message read state entirely and derive "unread" from `Notification` documents of type `message_received` with `read: false` for that conversation (reuses NOTF-01 — no schema change), or (b) add a lightweight read-tracking field to `Message`/`Conversation`. **Option (a) is recommended** — it requires zero schema changes beyond what `CONTEXT.md` already specifies, and the unread-dot requirement (D-05) can be satisfied by counting unread `message_received` notifications scoped to that `actorId`/conversation.

### Mongoose schema sketches (per repo-root CONTEXT.md authoritative spec)
```javascript
// Source: CONTEXT.md (repo root) §schema `conversations` / `messages` — authoritative, verbatim field names
// src/models/Conversation.js
const conversationSchema = new mongoose.Schema({
  memberIds: [{ type: mongoose.Schema.Types.ObjectId, ref: 'User', required: true }],
  name: { type: String },               // unused this phase (always undefined for 1:1)
  isGroup: { type: Boolean, default: false },  // ALWAYS false this phase — do not build group logic
  isPublic: { type: Boolean, default: false }, // unused this phase
  createdById: { type: mongoose.Schema.Types.ObjectId, ref: 'User', required: true },
  updatedAt: { type: Date, default: Date.now },
});
conversationSchema.index({ memberIds: 1, isGroup: 1 });

// src/models/Message.js
const messageSchema = new mongoose.Schema({
  conversationId: { type: mongoose.Schema.Types.ObjectId, ref: 'Conversation', required: true },
  senderId: { type: mongoose.Schema.Types.ObjectId, ref: 'User', required: true },
  text: { type: String },
  mediaUrls: [{ type: String }],
  createdAt: { type: Date, default: Date.now },
});
messageSchema.index({ conversationId: 1, _id: -1 }); // matches cursor-pagination pattern (FEED-03/NOTF style)
```

### `uploadMessageImage` — extend `s3.service.js` (mirrors `uploadPostImage` verbatim, different prefix)
```javascript
// Source: pattern lifted directly from src/services/s3.service.js uploadPostImage (lines 56-68)
async function uploadMessageImage({ buffer, contentType }) {
  const ext = EXTENSION_MAP[contentType] || '';
  const key = `messages/${crypto.randomUUID()}${ext ? '.' + ext : ''}`;

  await s3.send(new PutObjectCommand({
    Bucket: S3_BUCKET,
    Key: key,
    Body: buffer,
    ContentType: contentType,
  }));

  return { key, objectUrl: buildObjectUrl(key) };
}
// Add 'uploadMessageImage' to the module.exports alongside uploadAvatar/uploadPostImage/objectExists
```

## State of the Art

| Old Approach | Current Approach | When Changed | Impact |
|--------------|------------------|--------------|--------|
| Polling for all "new activity" (current Fademy pattern — Phase 5 notifications) | Socket.io push for chat only; polling retained for notifications | This phase (per project-level decision logged in STATE.md: "Notifications use polling (Phase 5); only chat is real-time") | Chat is the FIRST real-time surface in the codebase — there is no existing precedent to follow for the socket layer itself, only for the REST/auth/upload layers it builds on |
| `extraHeaders` for Socket.io auth (older tutorials, ~v2/early-v4 era) | `auth` option + `socket.handshake.auth` (current official recommendation) | Documented as current best practice in the official Socket.io v4 docs (`/how-to/use-with-jwt` still shows `extraHeaders` as one option but explicitly flags its WebSocket limitation; `/docs/v4/middlewares/` shows `handshake.auth` as the cleaner pattern) | Use `auth: { token }` / `socket.handshake.auth.token` — works uniformly across WebSocket and polling transports, and is the pattern shown in the rooms/middlewares official guides |

**Deprecated/outdated:**
- Socket.io v2.x patterns (e.g., `io.set('authorization', ...)`, `socket.request`): superseded by `io.use()` middleware + `socket.handshake.auth` in v3+/v4. Not relevant here since this is a greenfield integration on v4.8.3.

## Assumptions Log

| # | Claim | Section | Risk if Wrong |
|---|-------|---------|---------------|
| A1 | `Conversation`/`Message` model field names exactly match the repo-root `CONTEXT.md` schema spec (`memberIds`, `isGroup`, `mediaUrls`, etc.) and no deviation is expected | Code Examples — Mongoose schema sketches | Low — `CONTEXT.md` is explicitly cited as the "authoritative Mongoose schema reference" in 06-CONTEXT.md's Canonical References, so this is CITED not assumed; flagged here only because the planner should still treat it as the single source of truth and not improvise field names |
| A2 | Per-message read-tracking is best handled by reusing `Notification` documents (`message_received`, `read: false`) rather than adding a new field to `Message`/`Conversation` | Code Examples — conversation list query note | Medium — if the planner instead adds a `readBy[]`/`read` field to `Message`, that's a schema deviation from the CONTEXT.md spec (which has no such field); could cause friction with the "schema is authoritative" instruction. Recommend confirming this approach with the user/planner before locking it in, since CONTEXT.md's schema is silent on read-state for messages |
| A3 | The "new message" picker should reuse a layout similar to `UserResultCard` (avatar+name+role) but WITHOUT the `FollowButton` (since mutual-follow is already a precondition for appearing in the picker) | Architecture Patterns / Project Structure | Low — purely a UI-composition choice; if wrong, only affects component reuse efficiency, not correctness |
| A4 | `socket.io`/`socket.io-client` should be pinned to the exact version `4.8.3` rather than a caret range, to avoid any v4.x minor-version handshake-protocol drift between client and server during the 2-day window | Standard Stack — Installation | Low — Socket.io maintains backward compatibility within v4; a caret range (`^4.8.3`) is equally safe and matches the project's existing convention (`"express": "^4.22.2"`, etc.). Recommend matching existing convention (`^4.8.3`) rather than pinning, contradicting this assumption — noted for planner awareness |

**On A4:** On reflection, the existing `package.json` uses caret ranges throughout (`"express": "^4.22.2"`, `"mongoose": "^8.24.0"`) — the planner should follow that convention (`^4.8.3`) rather than exact-pinning, for consistency (CLAUDE.md "Match existing style").

## Open Questions

1. **Should unread-message state live on `Message`/`Conversation` (new field) or be derived from `Notification` (`message_received`, `read`)?**
   - What we know: `CONTEXT.md`'s `messages`/`conversations` schemas have no read-tracking field; `Notification` already has `read: Boolean` and the `message_received` type is pre-wired in its enum.
   - What's unclear: Whether deriving unread-dot state (D-05) from `Notification` documents is precise enough for a chat UI (e.g., does marking ONE notification read also need to mark the whole conversation read? What happens to the unread dot when the user opens the thread — does it PATCH every unread `message_received` notification for that actor to `read: true`, mirroring `markRead`?).
   - Recommendation: Derive from `Notification` (zero schema changes, reuses NOTF-01/04 + `markRead` pattern) — when a user opens a conversation thread, batch-`PATCH` (or a new bulk-mark-read endpoint) all `message_received` notifications where `actorId` is the other member. This is the lowest-risk path for a 2-day window; flag to the user/planner as a confirm-before-lock decision (see A2).

2. **Does the conversation-list "last message preview" need to show media-only messages distinctly (e.g., "📷 Photo") when `text` is empty?**
   - What we know: D-05 requires a "last-message preview" in the list item; `messages.text` can presumably be empty if `mediaUrls` is non-empty (text-or-media, or both).
   - What's unclear: Whether `text` is required (CONTEXT.md schema doesn't mark it required, unlike `Post.text` which IS required) — i.e., can a message be image-only?
   - Recommendation: Treat `text` as optional (allow image-only messages, matching the `mediaUrls: [String]` + `text: String` schema with neither marked `required`), and render a fallback preview like "📷 Foto" (or a `Paperclip`/`Image` icon + label) when `text` is empty but `mediaUrls.length > 0`. This is a small, contained UI decision — flag for the planner to confirm wording/icon choice with the user if there's time, otherwise ship the sensible default.

3. **Should `socket.service.js` expose the `io` instance via `app.set('io', io)` (Express convention) or a module-level singleton export?**
   - What we know: The codebase has no precedent for cross-cutting server instances; `notification.service.js` is a stateless module export.
   - What's unclear: Which wiring style best fits "mount in `app.js`, attach in `server.js`" given that `app.js` currently exports the bare Express app (no server reference) and Socket.io needs the HTTP server, which is created in `server.js`.
   - Recommendation: Export `initSocket(httpServer)` from `socket.service.js` returning the `io` instance; call it from `server.js` after `createServer(app)`; store the result via `app.set('io', io)` so controllers can retrieve it with `req.app.get('io')` — this is the most idiomatic Express pattern for sharing a server-scoped singleton with route handlers, and keeps `socket.service.js` testable in isolation (pass a fake `httpServer`).

## Environment Availability

| Dependency | Required By | Available | Version | Fallback |
|------------|------------|-----------|---------|----------|
| Node.js | Backend runtime | ✓ | (project requires CommonJS-compatible Node; `--test` flag used in test script implies Node ≥18) | — |
| MongoDB / `mongodb-memory-server` | Conversation/Message persistence + tests | ✓ (devDependency present, used by all existing controller tests) | mongodb-memory-server ^10.0.0 | — |
| `socket.io` / `socket.io-client` | Real-time transport (CHAT-01) | ✗ — not yet installed in either backend or frontend `package.json` | — (target: 4.8.3, verified on registry) | None — this is a hard requirement of the phase; planner MUST include an install task as Wave 0 / Task 1 |
| AWS S3 / S3-compatible bucket | Message media storage (CHAT-02) | ✓ (env vars have safe defaults; `npm test` boots without AWS per [02-02] decision; production bucket already used for avatars/post images) | — | If S3 credentials are unavailable in the dev/test environment, `uploadMessageImage` will fail at runtime but the existing pattern (env defaults + `objectExists` mocking in tests) means controller tests can run without real AWS access — mirror `post.controller.test.js`'s approach |

**Missing dependencies with no fallback:**
- `socket.io` (server) and `socket.io-client` (frontend) — must be installed before any chat code can run. This is THE blocking dependency of the entire phase; make it the very first task.

**Missing dependencies with fallback:**
- None beyond the above — everything else (S3, MongoDB, JWT) is already wired and proven.

## Validation Architecture

### Test Framework
| Property | Value |
|----------|-------|
| Framework | Node.js built-in `node:test` + `node:assert/strict`, with `mongodb-memory-server` for DB-backed controller tests |
| Config file | none — tests are listed explicitly in `package.json`'s `"test"` script |
| Quick run command | `node --test src/controllers/conversation.controller.test.js` (or `message.controller.test.js`) — run a single new test file directly |
| Full suite command | `npm test` (runs the full explicit list in `package.json`; **new test files must be appended to that list** or they won't run in CI) |

### Phase Requirements → Test Map
| Req ID | Behavior | Test Type | Automated Command | File Exists? |
|--------|----------|-----------|-------------------|-------------|
| CHAT-01 | Mutually-following users can create/open a 1:1 conversation; non-mutual/blocked users get 403 | controller (integration, mongodb-memory-server) | `node --test src/controllers/conversation.controller.test.js` | ❌ Wave 0 |
| CHAT-01 | Authenticated socket connection succeeds with valid JWT; rejected with invalid/missing token | unit/integration (socket.io-client against a test server instance) | `node --test src/services/socket.service.test.js` | ❌ Wave 0 |
| CHAT-01 | Sent message is delivered live to the recipient's room (`message:new` event) | integration (two `socket.io-client` instances against an in-process server) | `node --test src/services/socket.service.test.js` | ❌ Wave 0 |
| CHAT-02 | Message persists with `text` and/or `mediaUrls`; image upload follows `uploadPostImage`-mirrored S3 flow | controller (integration, mongodb-memory-server + mocked S3 like `post.controller.test.js`) | `node --test src/controllers/message.controller.test.js` | ❌ Wave 0 |
| CHAT-02 | `message_received` notification is dispatched on message creation (reuses NOTF-02 spy pattern from `comment.controller.test.js`) | controller (integration, with `notification.service` call-counting spy via `Module._load` interception) | `node --test src/controllers/message.controller.test.js` | ❌ Wave 0 |
| FRNT-07 | Conversations list renders avatar+name+preview+timestamp+unread dot (D-05); "new message" picker shows mutual followers only | manual / component-level (no existing frontend test infra found — see note below) | manual verification per `human_verify_mode: end-of-phase` | ❌ — no frontend test framework detected |

**Frontend test infrastructure note:** No `*.test.jsx`/`vitest.config.*`/`jest.config.*` was found anywhere under `MINIREDESOCIAL_FRONT/`. The project's `nyquist_validation` expectations for FRNT-07 should default to manual verification (matching `human_verify_mode: end-of-phase` already configured), consistent with how FRNT-04/FRNT-05/FRNT-06 were presumably handled in prior phases (no frontend test files exist for any of them either).

### Sampling Rate
- **Per task commit:** run the single new test file directly (`node --test src/controllers/<name>.controller.test.js`)
- **Per wave merge:** `npm test` (full backend suite — remember to append new `*.test.js` paths to the script string in `package.json`)
- **Phase gate:** Full suite green before `/gsd:verify-work`; FRNT-07 verified manually (no frontend test infra)

### Wave 0 Gaps
- [ ] `src/controllers/conversation.controller.test.js` — covers CHAT-01 (mutual-follow gate, block gate, find-or-create, list)
- [ ] `src/controllers/message.controller.test.js` — covers CHAT-02 (text+media persistence, S3 mock, notification dispatch spy)
- [ ] `src/services/socket.service.test.js` — covers CHAT-01 real-time delivery (handshake auth accept/reject, room join, cross-socket emit) — **new test pattern**: requires spinning up an in-process `http.Server` + `socket.io` server + `socket.io-client` connections inside the test; no precedent exists in the repo, budget extra time for this
- [ ] Append all three new test file paths to the `"test"` script string in `package.json` (the existing pattern is an explicit space-separated list, not a glob)
- [ ] Framework install: `npm install socket.io@^4.8.3` (backend) and `npm install socket.io-client@^4.8.3` (in `MINIREDESOCIAL_FRONT/`)

## Security Domain

### Applicable ASVS Categories

| ASVS Category | Applies | Standard Control |
|---------------|---------|-----------------|
| V2 Authentication | yes | Socket.io handshake reuses `verifyToken` (HS256-pinned JWT verify) — same control as REST `authenticate` middleware; no new auth scheme introduced |
| V3 Session Management | yes | No server-side session state beyond the JWT itself (stateless, matches existing REST pattern); Socket.io connection is authenticated per-handshake, re-validated on every reconnect |
| V4 Access Control | yes | Conversation creation gated on mutual-follow (`Follow` bilateral `accepted` check) + block check (`Block` bilateral); message creation gated on conversation membership (`memberIds` contains `req.user.id`); mirrors SOCL-05 enforcement already proven in feed/comment/search controllers |
| V5 Input Validation | yes | `text` trimmed/length-checked (mirror `createPost`'s `text.trim()` guard); `mediaUrls` populated server-side only from validated S3 uploads (never accepted as raw client input — same T-02-08b control as avatars/post images); multer content-type allowlist (`image/jpeg`/`image/png`/`image/webp`) reused verbatim from `post.controller.js` |
| V6 Cryptography | yes | No new cryptographic primitives — JWT signing/verification reuses the existing HS256-pinned `token.service.js`; never hand-roll a parallel verification routine for sockets (see Don't Hand-Roll table) |

### Known Threat Patterns for Socket.io + Express + MongoDB stack

| Pattern | STRIDE | Standard Mitigation |
|---------|--------|---------------------|
| Forged `senderId`/`conversationId` in a direct socket emit, bypassing REST auth checks | Spoofing / Elevation of Privilege | REST-as-source-of-truth (Pattern 4) — sockets never write to the DB; `senderId` always derived from `req.user.id` (verified JWT), never from payload, mirroring T-04-05/T-04C-02 |
| Cross-tenant message delivery (institution A user messages institution B user) | Information Disclosure / Tampering | `Conversation`/`Message` don't carry `institutionId` directly (per 06-CONTEXT.md's "Established Patterns" note — scope flows through `memberIds`/`senderId`, both institution-scoped `User` refs), BUT the conversation-creation gate (mutual-follow + block check) implicitly should also confirm both users share an institution — **the mutual-follow check alone may not be sufficient if cross-institution follows are somehow possible**; verify `Follow`/`User` model constraints prevent cross-institution follows (if they don't, add an explicit `targetUser.institutionId === req.user.institutionId` check to the conversation-creation gate) |
| Algorithm-confusion / `alg: none` JWT attack on the socket handshake | Tampering / Spoofing | Reuse `verifyToken` (HS256-pinned per [01-02], already mitigates T-02-01) — do not write a second verification path for sockets |
| Unbounded socket connections / room-join flooding (DoS) | Denial of Service | Socket.io has built-in connection-rate limiting via `engine.io` defaults; for a 2-day MVP with a known, small institutional user base, explicit rate-limiting is likely out of scope — flag as a known gap, not a blocker |
| Stored XSS via message `text` rendered unsanitized in the chat UI | Tampering (client-side) | React escapes text content by default (no `dangerouslySetInnerHTML` precedent anywhere in the codebase) — render `message.text` as plain text content, exactly like `PostCard`/`NotificationCard` render `actorId.name`/`post.text` |
| Unrestricted file upload via the message-image endpoint | Tampering / Elevation of Privilege | Reuse `post.controller.js`'s `ALLOWED_CONTENT_TYPES` allowlist + `MAX_UPLOAD_BYTES` size cap + multer memory storage verbatim — do not introduce a looser policy "to save time" |

## Sources

### Primary (HIGH confidence)
- Official Socket.io v4 docs — `/docs/v4/server-initialization/` — verified `http.createServer(app)` + `new Server(httpServer)` attachment pattern, and the explicit warning that `app.listen()` "will not work"
- Official Socket.io v4 docs — `/docs/v4/middlewares/` — verified `io.use((socket, next) => {...})` middleware pattern, `socket.handshake.auth.token` access, `next(new Error(...))` rejection semantics
- Official Socket.io v4 docs — `/how-to/use-with-jwt` — verified JWT integration options, including the `extraHeaders` limitation note ("only functions with HTTP long-polling enabled… browsers do not provide a way to provide additional headers for WebSocket connections")
- Official Socket.io v4 docs — `/docs/v4/client-options/` — verified `auth: { token }` object/callback forms, `connect_error` token-refresh pattern (`socket.auth.token = ...; socket.connect()`), reconnection option defaults
- Official Socket.io v4 docs — `/docs/v4/rooms/` — verified `socket.join()`/`io.to().emit()` API, the explicit "Per-user delivery" pattern (`socket.join(userId)`), and the auto-cleanup-on-disconnect guarantee
- `npm view socket.io version` / `npm view socket.io-client version` — confirmed both at `4.8.3`, matching major versions (client/server compatibility requirement)
- `npm view socket.io scripts` / `npm view socket.io-client scripts` — confirmed no `postinstall` scripts (only build/test/prepack, none of which run on consumer install)
- `curl https://api.npmjs.org/downloads/point/last-week/{socket.io,socket.io-client}` — confirmed ~14.5M / ~12.9M weekly downloads
- Direct codebase reads (this session): `src/services/s3.service.js`, `src/services/notification.service.js`, `src/middlewares/auth.middleware.js`, `src/services/token.service.js`, `src/controllers/{post,follow,user,notification}.controller.js`, `src/models/{Follow,Block,Notification,User}.js`, `src/routes/post.routes.js`, `src/server.js`, `src/app.js`, `src/config/env.js`, repo-root `CONTEXT.md` (chat/conversations/messages schema spec), `MINIREDESOCIAL_FRONT/src/{App.jsx, lib/api.js, components/AppShell/AppShell.jsx, hooks/useNotificationPolling.js, components/{NotificationCard,UserResultCard,PhotoUpload}.jsx, pages/{NotificationsPage,ComposePage}/...}`

### Secondary (MEDIUM confidence)
- WebSearch result summary cross-referencing the JWT-auth pattern across multiple community sources (Medium, dev.to, GitHub `express-socket.io-jwt`) — all converged on the same `io.use()` + `socket.handshake.auth.token` + `jsonwebtoken` pattern that the official docs confirm; used only to corroborate, not as primary source

### Tertiary (LOW confidence)
- None — all material claims were verified against official Socket.io documentation and/or direct codebase inspection.

## Metadata

**Confidence breakdown:**
- Standard stack: HIGH — socket.io/socket.io-client versions, download counts, and absence of postinstall scripts directly verified via `npm view` and npmjs.org downloads API; package choice was already a locked project decision, not a research recommendation
- Architecture: HIGH — every pattern (handshake auth, rooms, server attachment, REST-as-source-of-truth) is sourced from official Socket.io v4 documentation fetched and quoted directly in this session, cross-referenced against the project's own proven REST/auth/upload patterns
- Pitfalls: HIGH — each pitfall is either an explicit warning from the official docs (`app.listen` won't work; `extraHeaders` WebSocket limitation) or derived directly from a documented codebase convention (T-02-08b, T-04-05, NOTF-02, SOCL-05) being at risk of violation in the new real-time surface

**Research date:** 2026-06-08
**Valid until:** 2026-07-08 (30 days — Socket.io v4 is a stable, mature major version; no fast-moving API surface expected to shift within the phase's 2-day execution window)
