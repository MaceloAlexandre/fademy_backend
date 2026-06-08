# Phase 6: Chat (Real-Time, 1:1) - Pattern Map

**Mapped:** 2026-06-08
**Files analyzed:** 17 (9 backend new/modified, 8 frontend new/modified)
**Analogs found:** 17 / 17

## File Classification

| New/Modified File | Role | Data Flow | Closest Analog | Match Quality |
|-------------------|------|-----------|----------------|---------------|
| `src/models/Conversation.js` | model | CRUD | `src/models/Follow.js` | role-match |
| `src/models/Message.js` | model | CRUD | `src/models/Comment.js` | exact |
| `src/controllers/conversation.controller.js` | controller | request-response (gated CRUD) | `src/controllers/follow.controller.js` | exact |
| `src/controllers/message.controller.js` | controller | CRUD + file-I/O + event-driven | `src/controllers/post.controller.js` (`createPost`) | exact |
| `src/routes/conversation.routes.js` | route | request-response | `src/routes/follow.routes.js` / `src/routes/notification.routes.js` | exact |
| `src/routes/message.routes.js` | route | request-response + file-I/O | `src/routes/post.routes.js` | exact |
| `src/services/socket.service.js` | service | event-driven | `src/services/notification.service.js` (dispatch) + `src/middlewares/auth.middleware.js` (handshake auth) | role-match (new pattern, composed from two analogs) |
| `src/services/s3.service.js` (MODIFIED — add `uploadMessageImage`) | service | file-I/O | `uploadPostImage` (same file, lines 56-68) | exact |
| `src/server.js` (MODIFIED) | config/bootstrap | event-driven | current `app.listen` block (lines 7-16) | exact (transform) |
| `MINIREDESOCIAL_FRONT/src/lib/socket.js` | utility/provider | event-driven | `MINIREDESOCIAL_FRONT/src/lib/api.js` | role-match |
| `MINIREDESOCIAL_FRONT/src/hooks/useChatSocket.js` | hook | event-driven/streaming | `MINIREDESOCIAL_FRONT/src/hooks/useNotificationPolling.js` | role-match (polling → push) |
| `MINIREDESOCIAL_FRONT/src/components/AppShell/AppShell.jsx` (MODIFIED) | component | request-response | itself — bell icon block (lines 51-70) | exact |
| `MINIREDESOCIAL_FRONT/src/components/ConversationListItem/ConversationListItem.jsx` | component | request-response | `NotificationCard.jsx` | exact |
| `MINIREDESOCIAL_FRONT/src/components/ChatComposer/ChatComposer.jsx` | component | file-I/O + request-response | `PhotoUpload.jsx` + `ComposePage` text input | role-match |
| `MINIREDESOCIAL_FRONT/src/components/MessageBubble/MessageBubble.jsx` | component | transform (render) | `PostCard` (text rendering, no dangerouslySetInnerHTML) | role-match |
| `MINIREDESOCIAL_FRONT/src/pages/ConversationsListPage/ConversationsListPage.jsx` | component (page) | CRUD + streaming | `NotificationsPage.jsx` | exact |
| `MINIREDESOCIAL_FRONT/src/pages/NewConversationPage/NewConversationPage.jsx` | component (page) | request-response | `SearchPage.jsx` (list + `UserResultCard` minus `FollowButton`) | role-match |
| `MINIREDESOCIAL_FRONT/src/pages/ConversationThreadPage/ConversationThreadPage.jsx` | component (page) | streaming + CRUD | `NotificationsPage.jsx` (cursor pagination) + new socket subscription | role-match |
| `MINIREDESOCIAL_FRONT/src/App.jsx` (MODIFIED) | route config | request-response | itself — `<Route>` block (lines 35-40) | exact |

## Pattern Assignments

### `src/models/Conversation.js` (model, CRUD)

**Analog:** `src/models/Follow.js` (whole file, 31 lines) — schema-definition style, indexes, `module.exports = mongoose.model(...)`

**Schema-definition pattern** (Follow.js, lines 1-25):
```javascript
'use strict';

const mongoose = require('mongoose');

const followSchema = new mongoose.Schema({
  followerId: {
    type: mongoose.Schema.Types.ObjectId,
    ref: 'User',
    required: true,
  },
  // ...
  status: {
    type: String,
    enum: ['pending', 'accepted'],
    required: true,
  },
  createdAt: {
    type: Date,
    default: Date.now,
  },
});

followSchema.index({ followerId: 1, followingId: 1 }, { unique: true });
followSchema.index({ followerId: 1, status: 1 });

module.exports = mongoose.model('Follow', followSchema);
```

**Apply to Conversation.js using the EXACT field names from repo-root `CONTEXT.md` (authoritative schema spec — do not improvise):**
```javascript
const conversationSchema = new mongoose.Schema({
  memberIds: [{ type: mongoose.Schema.Types.ObjectId, ref: 'User', required: true }],
  name: { type: String },
  isGroup: { type: Boolean, default: false },
  isPublic: { type: Boolean, default: false },
  createdById: { type: mongoose.Schema.Types.ObjectId, ref: 'User', required: true },
  updatedAt: { type: Date, default: Date.now },
});
conversationSchema.index({ memberIds: 1, isGroup: 1 });
```
Note: NO `institutionId` field — mirrors the established "scope flows through refs" pattern Phase 5 used for `Notification` (see RESEARCH.md "Established Patterns").

---

### `src/models/Message.js` (model, CRUD)

**Analog:** `src/models/Comment.js` (whole file, 35 lines) — closest sibling: a child document referencing a parent (`postId`→`conversationId`), an author (`authorId`→`senderId`), with `createdAt` + a compound index for chronological listing.

**Pattern** (Comment.js, lines 5-33):
```javascript
const commentSchema = new mongoose.Schema({
  postId: { type: mongoose.Schema.Types.ObjectId, ref: 'Post', required: true },
  authorId: { type: mongoose.Schema.Types.ObjectId, ref: 'User', required: true },
  institutionId: { type: mongoose.Schema.Types.ObjectId, ref: 'Institution', required: true },
  text: { type: String, required: true, maxlength: 500 },
  createdAt: { type: Date, default: Date.now },
});
commentSchema.index({ postId: 1, createdAt: 1 });
```

**Apply to Message.js (per CONTEXT.md authoritative spec — note: NO `institutionId`, `text` NOT required since image-only messages are allowed per RESEARCH Open Question #2):**
```javascript
const messageSchema = new mongoose.Schema({
  conversationId: { type: mongoose.Schema.Types.ObjectId, ref: 'Conversation', required: true },
  senderId: { type: mongoose.Schema.Types.ObjectId, ref: 'User', required: true },
  text: { type: String },
  mediaUrls: [{ type: String }],
  createdAt: { type: Date, default: Date.now },
});
messageSchema.index({ conversationId: 1, _id: -1 }); // matches cursor-pagination convention (NOTF-01 style)
```

---

### `src/controllers/conversation.controller.js` (controller, request-response / gated CRUD)

**Analog:** `src/controllers/follow.controller.js` (whole file, 132 lines) — bilateral-check + create + notification dispatch pattern is identical in shape to "find-or-create a 1:1 conversation gated on mutual-follow + block."

**Imports pattern** (follow.controller.js, lines 1-6):
```javascript
'use strict';

const Follow = require('../models/Follow');
const User = require('../models/User');
const Block = require('../models/Block');
const notificationService = require('../services/notification.service');
```

**Bilateral block + mutual-follow gate** (follow.controller.js, lines 20-34, adapted per RESEARCH Pattern 5):
```javascript
const blocked = await Block.findOne({
  $or: [
    { blockerId: followerId, blockedId: followingId },
    { blockerId: followingId, blockedId: followerId },
  ],
});
if (blocked) {
  return res.status(403).json({ error: 'Forbidden' });
}

const target = await User.findById(followingId);
if (!target) {
  return res.status(404).json({ error: 'User not found' });
}
```
For conversation creation, extend this with the bilateral mutual-follow check (RESEARCH Pattern 5, both directions `status: 'accepted'`):
```javascript
const [aFollowsB, bFollowsA] = await Promise.all([
  Follow.findOne({ followerId: userId, followingId: targetId, status: 'accepted' }),
  Follow.findOne({ followerId: targetId, followingId: userId, status: 'accepted' }),
]);
if (!aFollowsB || !bFollowsA) {
  return res.status(403).json({ error: 'Both users must follow each other to start a chat' });
}
```

**Self-action guard** (follow.controller.js, lines 16-18):
```javascript
if (followerId === followingId) {
  return res.status(400).json({ error: 'Cannot follow yourself' });
}
```
Apply the same shape: `if (userId === targetId) return res.status(400).json({ error: 'Cannot message yourself' });`

**Create + notification dispatch + response** (follow.controller.js, lines 37-47):
```javascript
const status = target.isPrivate ? 'pending' : 'accepted';
const followDoc = await Follow.create({ followerId, followingId, status });

await notificationService.create(
  status === 'accepted' ? 'new_follower' : 'follow_request',
  followerId,
  followingId,
  followDoc._id,
  'Follow'
);

return res.status(201).json({ status: followDoc.status, followId: followDoc._id });
```

**Error handling pattern** (follow.controller.js, lines 48-53 — try/catch wraps the whole handler, `err.code === 11000` for duplicate-key, generic 500 fallback):
```javascript
} catch (err) {
  if (err.code === 11000) {
    return res.status(409).json({ error: 'Already following or request pending' });
  }
  return res.status(500).json({ error: 'Internal server error' });
}
```

**IDOR guard / ownership check pattern** (follow.controller.js, lines 82-84 — also mirrored in notification.controller.js `markRead` lines 54-56): use for membership checks (caller must be in `conversation.memberIds`):
```javascript
if (followDoc.followingId.toString() !== req.user.id) {
  return res.status(403).json({ error: 'Forbidden' });
}
```

**Find-or-create flow** — use RESEARCH.md Pattern 5's exact `findOne` + `create` shape (memberIds order-independent lookup via `$all`/`$size`):
```javascript
let conversation = await Conversation.findOne({
  isGroup: false,
  memberIds: { $all: [userId, targetId], $size: 2 },
});
if (!conversation) {
  conversation = await Conversation.create({ memberIds: [userId, targetId], isGroup: false, createdById: userId });
}
```

**List query pattern** — mirrors `notification.controller.js`'s `listNotifications` (cursor pagination, capped limit, populate):
```javascript
// notification.controller.js lines 17-28
if (req.query.cursor) {
  query._id = { $lt: req.query.cursor };
}
const limit = Math.min(parseInt(req.query.limit, 10) || 20, 50);
const docs = await Notification.find(query)
  .sort({ _id: -1 })
  .limit(limit + 1)
  .populate('actorId', 'name avatarUrl');
```
For conversations, RESEARCH.md recommends Option A (bounded N+1, `Promise.all` over `Conversation.find(...).populate('memberIds', 'name avatarUrl')` then per-conversation `Message.findOne(...).sort({_id: -1})` for last-message preview) — see RESEARCH.md "Code Examples" section for the full sketch.

---

### `src/controllers/message.controller.js` (controller, CRUD + file-I/O + event-driven)

**Analog:** `src/controllers/post.controller.js` `createPost` (lines 18-68) — closest sibling for "validate text, upload images to S3 via the multer→service pattern, persist, respond." This is the EXACT shape to copy for `createMessage`, then layer notification dispatch + socket emit on top (RESEARCH.md Pattern 4).

**Imports pattern** (post.controller.js, lines 1-8):
```javascript
'use strict';

const Post = require('../models/Post');
const Follow = require('../models/Follow');
const Block = require('../models/Block');
require('../models/Category'); // model must be required to register before populate
const { uploadPostImage } = require('../services/s3.service');

const ALLOWED_CONTENT_TYPES = new Set(['image/jpeg', 'image/png', 'image/webp']);
```

**Auth/ownership derivation — req.user, NEVER body** (post.controller.js, lines 18-19):
```javascript
async function createPost(req, res) {
  const { id: authorId, role, institutionId } = req.user;
  const { text, linkUrl } = req.body;
```
For messages: `const { id: senderId } = req.user;` — `senderId` MUST come from `req.user.id`, never the request body (T-04-05 / T-04C-02 anti-spoofing convention, called out explicitly in RESEARCH.md Anti-Patterns).

**Text validation guard** (post.controller.js, lines 22-25 — note: messages allow image-only, so this becomes "text OR mediaUrls required" rather than "text required"):
```javascript
if (!text || typeof text !== 'string' || !text.trim()) {
  return res.status(400).json({ error: 'text is required' });
}
```

**S3 image-upload loop — mirror VERBATIM with `uploadMessageImage`** (post.controller.js, lines 32-41):
```javascript
try {
  const imageUrls = [];
  for (const file of (req.files || [])) {
    if (!ALLOWED_CONTENT_TYPES.has(file.mimetype)) {
      return res.status(400).json({ error: 'Invalid file type' });
    }
    const { objectUrl } = await uploadPostImage({ buffer: file.buffer, contentType: file.mimetype });
    imageUrls.push(objectUrl);
  }
  // ... swap uploadPostImage → uploadMessageImage, imageUrls → mediaUrls
```

**Persist + respond** (post.controller.js, lines 51-61):
```javascript
const post = await Post.create({ authorId, institutionId, text: text.trim(), imageUrls, /* ... */ });
return res.status(201).json({ post });
```

**Error handling** (post.controller.js, lines 62-67 — identical shape to follow.controller.js):
```javascript
} catch (err) {
  if (err.code === 11000) {
    return res.status(409).json({ error: 'Duplicate entry' });
  }
  return res.status(500).json({ error: 'Internal server error' });
}
```

**Layer on top — notification dispatch (NOTF-02, mirrors follow.controller.js lines 39-45) + socket emit (RESEARCH.md Pattern 4, lines 244-258):**
```javascript
const message = await Message.create({ conversationId, senderId, text, mediaUrls });
await notificationService.create('message_received', senderId, recipientId, message._id, 'Message');

const io = req.app.get('io');
io.to(recipientId.toString()).emit('message:new', message);

return res.status(201).json({ message });
```

**Membership/IDOR guard** (RESEARCH.md Pattern 4, lines 246-249 — same shape as notification.controller.js `markRead` lines 54-56):
```javascript
const conversation = await Conversation.findById(req.params.id);
if (!conversation || !conversation.memberIds.some(m => m.toString() === senderId)) {
  return res.status(403).json({ error: 'Forbidden' });
}
```

---

### `src/routes/conversation.routes.js` / `src/routes/message.routes.js` (route, request-response)

**Analog:** `src/routes/follow.routes.js` (whole file, 16 lines) for plain authenticated routes; `src/routes/post.routes.js` (whole file, 35 lines) for the multer-wrapped upload route.

**Plain authenticated router pattern** (follow.routes.js, lines 1-16):
```javascript
'use strict';

const { Router } = require('express');
const authenticate = require('../middlewares/auth.middleware');
const { acceptFollow, rejectFollow } = require('../controllers/follow.controller');

const router = Router();

router.patch('/:id/accept', authenticate, acceptFollow);
router.patch('/:id/reject', authenticate, rejectFollow);

module.exports = router;
```

**Multer-wrapped upload route pattern — mirror VERBATIM for message image attachments** (post.routes.js, lines 1-25):
```javascript
const multer = require('multer');
const { MAX_UPLOAD_BYTES } = require('../config/env');

const uploadMiddleware = multer({
  storage: multer.memoryStorage(),
  limits: { fileSize: MAX_UPLOAD_BYTES },
});

// Wraps multer.array to surface upload errors as 400 (not 500)
function uploadImages(req, res, next) {
  uploadMiddleware.array('images', 4)(req, res, (err) => {
    if (err) {
      return res.status(400).json({ error: 'File upload error' });
    }
    return next();
  });
}

router.post('/', authenticate, uploadImages, createPost);
```
Apply identically to `POST /api/conversations/:id/messages` — same `multer.memoryStorage()`, same `MAX_UPLOAD_BYTES`, same 400-on-upload-error wrapper (field name / array count at planner's discretion, e.g. `images`/1 for a single attachment).

**Mount order note** (post.routes.js line 27-28 comment — apply the same "specific-before-param" rule if `/conversations/new` or similar specific paths are added):
```javascript
// IMPORTANT: /feed MUST be declared before /:id — otherwise 'feed' is captured as an id param
router.get('/feed', authenticate, getFeed);
```

---

### `src/services/socket.service.js` (service, event-driven — NEW pattern, composed from two analogs)

No direct analog exists (first real-time feature in the codebase — RESEARCH.md confirms this explicitly). Compose from:
1. **Handshake-auth** mirrors `src/middlewares/auth.middleware.js` (verbatim verify pattern, lines 21-31):
```javascript
const { verifyToken } = require('../services/token.service');

try {
  const decoded = verifyToken(token);
  req.user = { id: decoded.sub, role: decoded.role, institutionId: decoded.institutionId };
  return next();
} catch (_err) {
  return res.status(401).json({ error: 'Invalid or expired token' });
}
```
   Adapted for Socket.io `io.use()` per RESEARCH.md Pattern 2 (lines 194-204):
```javascript
io.use((socket, next) => {
  const token = socket.handshake.auth && socket.handshake.auth.token;
  if (!token) return next(new Error('Authentication required'));
  try {
    const decoded = verifyToken(token);
    socket.data.user = { id: decoded.sub, role: decoded.role, institutionId: decoded.institutionId };
    return next();
  } catch (_err) {
    return next(new Error('Invalid or expired token'));
  }
});

io.on('connection', (socket) => {
  socket.join(socket.data.user.id);
});
```

2. **Module shape / fire-and-forget dispatch convention** mirrors `src/services/notification.service.js` (whole file, 13 lines — stateless module export, single responsibility):
```javascript
'use strict';

const Notification = require('../models/Notification');

async function create(type, actorId, userId, refId, refModel) {
  try {
    await Notification.create({ type, actorId, userId, refId, refModel });
  } catch (_err) {
    // fire-and-forget: swallow all errors silently
  }
}

module.exports = { create };
```
   Apply the same "thin, single-purpose, stateless module" shape: export `initSocket(httpServer)` returning the `io` instance (RESEARCH.md Open Question #3 recommendation — store via `app.set('io', io)` in `server.js`, retrieve with `req.app.get('io')` in controllers).

3. **Server-attachment** — mirrors `src/server.js` structure (see MODIFIED file below) but per RESEARCH.md Pattern 1 (lines 168-182):
```javascript
const { createServer } = require('http');
const { Server } = require('socket.io');

const httpServer = createServer(app);
const io = new Server(httpServer, { cors: { origin: env.FRONTEND_ORIGIN } });
io.on('connection', (socket) => { /* ... */ });
httpServer.listen(env.PORT, () => { /* ... */ });
```

---

### `src/services/s3.service.js` (MODIFIED — add `uploadMessageImage`)

**Analog:** `uploadPostImage` in the SAME file (lines 56-68) — copy verbatim, change only the key prefix.

```javascript
async function uploadPostImage({ buffer, contentType }) {
  const ext = EXTENSION_MAP[contentType] || '';
  const key = `posts/${crypto.randomUUID()}${ext ? '.' + ext : ''}`;

  await s3.send(new PutObjectCommand({
    Bucket: S3_BUCKET,
    Key: key,
    Body: buffer,
    ContentType: contentType,
  }));

  return { key, objectUrl: buildObjectUrl(key) };
}
```
New function — identical body, `messages/` prefix, add to `module.exports` alongside `uploadAvatar`/`uploadPostImage`/`objectExists` (line 85):
```javascript
async function uploadMessageImage({ buffer, contentType }) {
  const ext = EXTENSION_MAP[contentType] || '';
  const key = `messages/${crypto.randomUUID()}${ext ? '.' + ext : ''}`;
  // ... identical PutObjectCommand body ...
  return { key, objectUrl: buildObjectUrl(key) };
}

module.exports = { uploadAvatar, uploadPostImage, uploadMessageImage, objectExists };
```

---

### `src/server.js` (MODIFIED — bootstrap transform)

**Current shape** (whole file, 17 lines — what to transform):
```javascript
'use strict';

const env = require('./config/env');
const { connectDB } = require('./config/db');
const app = require('./app');

connectDB(env.MONGODB_URI)
  .then(() => {
    app.listen(env.PORT, () => {
      console.log(`Fademy server running on port ${env.PORT}`);
    });
  })
  .catch((err) => {
    console.error('Failed to connect to MongoDB:', err.message);
    process.exit(1);
  });
```

**Target shape** — per RESEARCH.md Pattern 1 (Pitfall 1: `app.listen()` silently breaks Socket.io). Replace `app.listen` with `http.createServer(app)` + `initSocket(httpServer)` + `httpServer.listen`, preserving the existing `connectDB().then().catch()` structure and log message verbatim:
```javascript
const { createServer } = require('http');
const { initSocket } = require('./services/socket.service');

connectDB(env.MONGODB_URI)
  .then(() => {
    const httpServer = createServer(app);
    initSocket(httpServer); // attaches io, sets app.set('io', io) internally
    httpServer.listen(env.PORT, () => {
      console.log(`Fademy server running on port ${env.PORT}`);
    });
  })
  .catch((err) => { /* unchanged */ });
```

---

### `MINIREDESOCIAL_FRONT/src/lib/socket.js` (utility, event-driven)

**Analog:** `MINIREDESOCIAL_FRONT/src/lib/api.js` (whole file, 16 lines) — singleton instance creation + JWT-from-localStorage convention.

```javascript
import axios from 'axios'

const api = axios.create({
  baseURL: import.meta.env.VITE_API_URL || 'http://localhost:3000',
})

api.interceptors.request.use((config) => {
  const token = localStorage.getItem('fademy_token')
  if (token) {
    config.headers.Authorization = `Bearer ${token}`
  }
  return config
})

export default api
```
Apply the same "create singleton, read JWT from `localStorage.getItem('fademy_token')`, export default" shape per RESEARCH.md Pattern 2 client-side example (lines 211-215):
```javascript
import { io } from 'socket.io-client'

const socket = io(import.meta.env.VITE_API_URL || 'http://localhost:3000', {
  auth: { token: localStorage.getItem('fademy_token') },
  autoConnect: false,
})

export default socket
```

---

### `MINIREDESOCIAL_FRONT/src/hooks/useChatSocket.js` (hook, event-driven/streaming)

**Analog:** `MINIREDESOCIAL_FRONT/src/hooks/useNotificationPolling.js` (whole file, 26 lines) — same "encapsulate a recurring data-fetch concern behind a hook returning state" shape, swap `setInterval` polling for socket event subscription.

```javascript
import { useState, useEffect, useCallback } from 'react'
import api from '../lib/api'

const POLL_INTERVAL_MS = 5000

export default function useNotificationPolling() {
  const [unreadCount, setUnreadCount] = useState(0)

  const fetchUnreadCount = useCallback(async () => {
    try {
      const res = await api.get('/api/notifications', { params: { limit: 1 } })
      setUnreadCount(res.data.unreadCount ?? 0)
    } catch (_) {
      // fire-and-forget — never surface polling errors to the UI
    }
  }, [])

  useEffect(() => {
    fetchUnreadCount()
    const id = setInterval(fetchUnreadCount, POLL_INTERVAL_MS)
    return () => clearInterval(id)
  }, [fetchUnreadCount])

  return { unreadCount, refetch: fetchUnreadCount }
}
```
Apply the same "useState + useEffect + cleanup-on-unmount + return state object" shape: connect on mount (`socket.connect()`), `socket.on('message:new', handler)`, cleanup via `socket.off(...)` / `socket.disconnect()` in the effect's return function — mirrors the `clearInterval(id)` cleanup convention exactly.

---

### `MINIREDESOCIAL_FRONT/src/components/AppShell/AppShell.jsx` (MODIFIED — add chat icon)

**Analog:** itself — the bell-icon block (lines 51-70), per D-02 ("mirrors the bell icon's placement/style exactly"). Copy this block verbatim, change icon (`MessageCircle` from `lucide-react` is the conventional chat icon), label, badge source (`unreadConversationCount` from `useChatSocket` or similar), and `onClick` target (`navigate('/chat')`):

```jsx
<button
  type="button"
  className={styles.bellBtn}
  aria-label="Notificações"
  onClick={() => navigate('/notifications')}
>
  <Bell
    size={24}
    aria-hidden="true"
    color={hasUnread ? 'var(--color-accent)' : undefined}
  />
  {hasUnread && (
    <span className={styles.badge} aria-hidden="true">{badgeLabel}</span>
  )}
  {hasUnread && (
    <span className={styles['sr-only']}>
      {unreadCount > 9 ? 'mais de 9 notificações não lidas' : `${unreadCount} notificações não lidas`}
    </span>
  )}
</button>
```
Place the new button immediately adjacent to this one (before or after, matching D-02 "next to the notification bell"); reuse `styles.bellBtn`/`styles.badge`/`styles['sr-only']` CSS-module classes as-is (CSS Modules co-located convention — do not introduce new classes for identical visual treatment).

---

### `MINIREDESOCIAL_FRONT/src/components/ConversationListItem/ConversationListItem.jsx` (component, request-response)

**Analog:** `MINIREDESOCIAL_FRONT/src/components/NotificationCard/NotificationCard.jsx` (whole file, 136 lines) — exact layout shape for D-05 (avatar + name + preview + timestamp + unread dot).

**Relative-time formatter — copy verbatim** (NotificationCard.jsx, lines 8-17):
```javascript
function formatRelativeTime(dateStr) {
  const diff = Date.now() - new Date(dateStr).getTime()
  const minutes = Math.floor(diff / 60000)
  if (minutes < 1) return 'agora'
  if (minutes < 60) return `${minutes}min`
  const hours = Math.floor(minutes / 60)
  if (hours < 24) return `${hours}h`
  const days = Math.floor(hours / 24)
  return `${days}d`
}
```

**Card structure with unread dot — mirror this exact shape** (NotificationCard.jsx, lines 84-102):
```jsx
<div
  className={`${styles.card} ${!read ? styles.unread : ''}`}
  role="button"
  tabIndex={0}
  onClick={handleClick}
  onKeyDown={handleKeyDown}
>
  {!read && <span className={styles.unreadDot} aria-hidden="true" />}
  <img src={actorId.avatarUrl} alt={actorId.name} className={styles.avatar} />
  <div className={styles.content}>
    <p className={styles.actionText}>
      <strong className={styles.actorName}>{actorId.name}</strong> {getActionText(type)}
    </p>
    <span className={styles.timestamp}>{formatRelativeTime(createdAt)}</span>
    {/* ... */}
  </div>
</div>
```
For `ConversationListItem`: replace `actionText` with the last-message preview (text, or "📷 Foto" fallback per RESEARCH.md Open Question #2 when `text` is empty and `mediaUrls.length > 0`), keep avatar/name/timestamp/unread-dot structure and `role="button"` + `onKeyDown` keyboard-accessibility pattern (lines 47-52) identical.

**Click-to-navigate + keyboard handler pattern** (NotificationCard.jsx, lines 38-52):
```javascript
function handleClick() {
  if (!read) onRead(_id)
  navigate(`/profile/${actorId._id}`)
}

function handleKeyDown(e) {
  if (e.key === 'Enter' || e.key === ' ') {
    e.preventDefault()
    handleClick()
  }
}
```

---

### `MINIREDESOCIAL_FRONT/src/components/ChatComposer/ChatComposer.jsx` (component, file-I/O + request-response)

**Analog:** `MINIREDESOCIAL_FRONT/src/components/PhotoUpload.jsx` (whole file, 65 lines) — image-picker trigger + preview + size-guard pattern.

**File-select + size-guard pattern — mirror verbatim** (PhotoUpload.jsx, lines 13-21):
```javascript
function handleChange(e) {
  const selected = e.target.files?.[0]
  if (!selected) return
  if (selected.size > 5 * 1024 * 1024) {
    // size guard: 5 MB max
    return
  }
  onFileSelect(selected)
}
```

**Hidden-input + trigger-button structure** (PhotoUpload.jsx, lines 23-64):
```jsx
<button type="button" className={styles.trigger} onClick={handleTriggerClick} aria-label="Selecionar foto de perfil">
  {previewUrl ? <img src={previewUrl} alt="Pré-visualização" className={styles.preview} /> : <Camera size={24} />}
</button>
<input
  ref={inputRef}
  type="file"
  accept="image/jpeg,image/png,image/webp"
  className={styles.hiddenInput}
  onChange={handleChange}
  tabIndex={-1}
  aria-hidden="true"
/>
```
Reuse the `accept="image/jpeg,image/png,image/webp"` allowlist verbatim (matches `ALLOWED_CONTENT_TYPES` server-side). Combine with a plain text `<input>`/`<textarea>` + send button (no existing analog for a combined text+media composer — `ComposePage` has the text-area pattern, `PhotoUpload` has the image-picker; compose the two).

---

### `MINIREDESOCIAL_FRONT/src/pages/ConversationsListPage/ConversationsListPage.jsx` (page, CRUD + streaming)

**Analog:** `MINIREDESOCIAL_FRONT/src/pages/NotificationsPage/NotificationsPage.jsx` (whole file, 97 lines) — exact shape for cursor-paginated list with `IntersectionObserver` infinite scroll, loading/empty/error states.

**Cursor-pagination fetch pattern — mirror verbatim, swap endpoint** (NotificationsPage.jsx, lines 22-40):
```javascript
const fetchPage = useCallback(async (reset) => {
  setLoading(true)
  if (reset) setApiError('')
  const useCursor = reset ? null : currentCursorRef.current
  try {
    const params = { limit: 20 }
    if (useCursor) params.cursor = useCursor
    const res = await api.get('/api/notifications', { params })
    const data = res.data
    if (reset) setItems(data.notifications)
    else setItems((prev) => prev.concat(data.notifications))
    setCursor(data.nextCursor)
    setHasMore(data.hasMore)
  } catch (_) {
    setApiError('Erro ao carregar notificações. Tente novamente.')
  } finally {
    setLoading(false)
  }
}, [])
```

**IntersectionObserver infinite-scroll sentinel — mirror verbatim** (NotificationsPage.jsx, lines 46-55):
```javascript
useEffect(() => {
  const obs = new IntersectionObserver(
    ([entry]) => {
      if (entry.isIntersecting && hasMore && !loading) fetchPage(false)
    },
    { threshold: 0.1 }
  )
  if (sentinelRef.current) obs.observe(sentinelRef.current)
  return () => obs.disconnect()
}, [hasMore, loading, fetchPage])
```

**Empty-state + loading + error layout structure** (NotificationsPage.jsx, lines 66-95) — mirror exactly, swap copy text and `NotificationCard` → `ConversationListItem`. Add the "new message" compose icon (D-04) to the page header, styled like — but separate from — the AppShell chat icon (placement at planner's discretion, "inside that list" per D-04).

---

### `MINIREDESOCIAL_FRONT/src/pages/NewConversationPage/NewConversationPage.jsx` (page, request-response)

**Analog:** `MINIREDESOCIAL_FRONT/src/pages/SearchPage/SearchPage.jsx` (whole file, 112 lines) for the list-rendering shape, combined with `MINIREDESOCIAL_FRONT/src/components/UserResultCard/UserResultCard.jsx` (whole file, 47 lines) MINUS the `FollowButton` (RESEARCH.md Assumption A3 — mutual-follow is already a precondition for appearing in this picker).

**List rendering + empty/loading/error structure** (SearchPage.jsx, lines 75-108):
```jsx
<div className={styles.results}>
  {loading && <div className={styles.spinnerWrap}><Spinner /></div>}
  <ErrorBanner message={apiError} />
  {!loading && !apiError && results.length === 0 && (
    <div className={styles.noResults}><p>Nenhum usuário encontrado</p></div>
  )}
  {results.length > 0 && results.map(user => (
    <UserResultCard key={user._id} user={user} onFollowStatusChange={handleFollowStatusChange} />
  ))}
</div>
```

**UserResultCard structure to mirror, dropping the FollowButton/`followWrap` block** (UserResultCard.jsx, lines 21-31):
```jsx
<div className={styles.card} onClick={handleCardClick}>
  <img src={user.avatarUrl} alt={user.name} className={styles.avatar} />
  <div className={styles.info}>
    <span className={styles.name}>{user.name}</span>
    <span className={styles.role}>{user.role}</span>
  </div>
  {/* DROP: followWrap + FollowButton — tapping the row itself opens/creates the conversation (D-04) */}
</div>
```
On tap: `onClick` should call `POST /api/conversations` (find-or-create, gated server-side per Pattern 5) then `navigate(`/chat/${conversation._id}`)` — replacing `navigate('/profile/...')`.

**Data source:** no existing "list mutual followers" endpoint — planner must add one (or filter client-side from `/api/users/search` results / a new query param). Flag this as a backend addition not yet covered by RESEARCH.md's file list — confirm with planner whether `conversation.controller.js` should expose a `GET /api/conversations/eligible-contacts`-style endpoint or whether the picker reuses `/api/users/search` with a `mutualFollowsOnly` filter.

---

### `MINIREDESOCIAL_FRONT/src/pages/ConversationThreadPage/ConversationThreadPage.jsx` (page, streaming + CRUD)

**Analog:** `MINIREDESOCIAL_FRONT/src/pages/NotificationsPage/NotificationsPage.jsx` for cursor-paginated message history (same `fetchPage`/`cursor`/`hasMore` shape — see ConversationsListPage section above), PLUS a new socket-subscription layer via `useChatSocket` (no analog — first live-update page in the codebase).

**Combine:**
1. Initial load + "load older messages" via the same cursor-pagination shape as `NotificationsPage.fetchPage` (reverse-chronological fetch, prepend on scroll-up instead of append-on-scroll-down).
2. Live updates via `useChatSocket`'s `message:new` subscription — append to `items` state on receipt (mirrors `setItems((prev) => prev.concat(...))` from `fetchPage`, but for a single incoming message rather than a page).
3. Render messages via `MessageBubble` (sent/received styling — no existing analog; render `message.text` as plain text content, exactly like `PostCard` renders `post.text`, NEVER `dangerouslySetInnerHTML` — RESEARCH.md Security Domain note on stored-XSS).
4. `ChatComposer` at the bottom for sending (REST POST, not socket emit — RESEARCH.md Pattern 4 / Anti-Pattern "Treating Socket.io events as the authorization boundary").

---

### `MINIREDESOCIAL_FRONT/src/App.jsx` (MODIFIED — add chat routes)

**Analog:** itself — the `<Route>` block inside the `RequireAuth`+`AppShell` wrapper (lines 35-40). Add new routes in the same place, same shape:

```jsx
<Route path="/" element={<FeedPage />} />
<Route path="/search" element={<SearchPage />} />
<Route path="/posts/:postId" element={<PostDetailPage />} />
<Route path="/compose" element={<ComposePage />} />
<Route path="/profile/:userId" element={<ProfilePage />} />
<Route path="/notifications" element={<NotificationsPage />} />
```
Add (matching the path-naming convention `/chat`, `/chat/new`, `/chat/:conversationId` from RESEARCH.md's Integration Points):
```jsx
<Route path="/chat" element={<ConversationsListPage />} />
<Route path="/chat/new" element={<NewConversationPage />} />
<Route path="/chat/:conversationId" element={<ConversationThreadPage />} />
```
And the corresponding `import` lines at the top, matching the existing flat import-list style (lines 2-11).

## Shared Patterns

### Authentication / Authorization (REST)
**Source:** `src/middlewares/auth.middleware.js` (whole file) + `src/services/token.service.js` (whole file)
**Apply to:** `conversation.routes.js`, `message.routes.js` — every route uses `authenticate` middleware exactly as `follow.routes.js`/`post.routes.js`/`notification.routes.js` do (`router.get('/', authenticate, handler)`). `req.user = { id, role, institutionId }` is the ONLY source of `senderId`/`createdById` — never the request body.
```javascript
// auth.middleware.js, lines 21-31 — verbatim verify shape
try {
  const decoded = verifyToken(token);
  req.user = { id: decoded.sub, role: decoded.role, institutionId: decoded.institutionId };
  return next();
} catch (_err) {
  return res.status(401).json({ error: 'Invalid or expired token' });
}
```

### Authentication (Socket.io handshake — NEW for this phase)
**Source:** RESEARCH.md Pattern 2 (composed from `auth.middleware.js` + official Socket.io `io.use()` docs)
**Apply to:** `socket.service.js` exclusively — fail-closed (`next(new Error(...))`), pin `verifyToken`'s HS256 algorithm, never write a second verification routine (RESEARCH.md "Don't Hand-Roll").

### Bilateral Block Check
**Source:** `src/controllers/follow.controller.js`, lines 21-26 (also proven in feed/comment controllers per FEED-02/SOCL-05)
**Apply to:** `conversation.controller.js` (gate on conversation creation, per SOCL-05 "Blocked users cannot message")
```javascript
const blocked = await Block.findOne({
  $or: [
    { blockerId: userId, blockedId: targetId },
    { blockerId: targetId, blockedId: userId },
  ],
});
if (blocked) return res.status(403).json({ error: 'Forbidden' });
```

### Mutual-Follow Check (bilateral, both directions `accepted`)
**Source:** RESEARCH.md Pattern 5 (synthesized from `Follow` schema + `follow.controller.js` query conventions)
**Apply to:** `conversation.controller.js` only
```javascript
const [aFollowsB, bFollowsA] = await Promise.all([
  Follow.findOne({ followerId: userId, followingId: targetId, status: 'accepted' }),
  Follow.findOne({ followerId: targetId, followingId: userId, status: 'accepted' }),
]);
if (!aFollowsB || !bFollowsA) {
  return res.status(403).json({ error: 'Both users must follow each other to start a chat' });
}
```

### Notification Dispatch (NOTF-02 — never inline)
**Source:** `src/services/notification.service.js` (whole file) + call-site convention in `follow.controller.js` lines 39-45 / `comment.controller.js`
**Apply to:** `message.controller.js` — call `notificationService.create('message_received', senderId, recipientId, message._id, 'Message')` from the message-creation path (durable write path, NOT a socket handler — RESEARCH.md Architectural Responsibility Map).
```javascript
async function create(type, actorId, userId, refId, refModel) {
  try {
    await Notification.create({ type, actorId, userId, refId, refModel });
  } catch (_err) {
    // fire-and-forget: swallow all errors silently
  }
}
```

### Error Handling (controllers)
**Source:** Consistent across `follow.controller.js` (lines 48-53), `post.controller.js` (lines 62-67), `notification.controller.js` (lines 37-39, 62-64) — try/catch wraps the whole async handler body; `err.code === 11000` → 409; everything else → generic 500 with `{ error: 'Internal server error' }`
**Apply to:** `conversation.controller.js`, `message.controller.js` — copy this try/catch shape verbatim; never leak `err.message` to the client (matches the pattern across all controllers read).

### S3 Upload (server owns the key — T-02-08b)
**Source:** `src/services/s3.service.js` `uploadPostImage` (lines 56-68) + `src/controllers/post.controller.js` upload loop (lines 32-41) + `src/routes/post.routes.js` multer config (lines 11-25)
**Apply to:** `s3.service.js` (`uploadMessageImage`), `message.controller.js` (upload loop), `message.routes.js` (multer middleware) — three-file mirror, change only key prefix (`messages/`) and field name/array-count.

### Cursor Pagination (capped limit, `_id`-based, `hasMore`/`nextCursor`)
**Source:** `src/controllers/notification.controller.js` `listNotifications` (lines 12-40)
**Apply to:** `conversation.controller.js` (list conversations), `message.controller.js` (list messages in a thread — reverse-chronological)
```javascript
const query = { /* scoping field */ };
if (req.query.cursor) query._id = { $lt: req.query.cursor };
const limit = Math.min(parseInt(req.query.limit, 10) || 20, 50);
const docs = await Model.find(query).sort({ _id: -1 }).limit(limit + 1).populate(/* ... */);
const hasMore = docs.length > limit;
const items = docs.slice(0, limit);
const nextCursor = hasMore ? items[items.length - 1]._id : null;
```

### Frontend: List Page Shape (loading/empty/error/infinite-scroll)
**Source:** `MINIREDESOCIAL_FRONT/src/pages/NotificationsPage/NotificationsPage.jsx` (whole file)
**Apply to:** `ConversationsListPage`, `ConversationThreadPage` (message history) — `useState`/`useRef`/`useCallback`/`useEffect` composition, `IntersectionObserver` sentinel, `Spinner`/`ErrorBanner` reuse, empty-state copy block.

### Frontend: CSS Modules co-location
**Source:** every component/page in `MINIREDESOCIAL_FRONT/src/components/` and `pages/` (e.g., `NotificationCard.jsx` + `NotificationCard.module.css`)
**Apply to:** all new components/pages — `Component.jsx` + `Component.module.css` side-by-side, `import styles from './Component.module.css'`, reuse shared CSS variables (`var(--color-accent)`, etc.).

## No Analog Found

| File | Role | Data Flow | Reason |
|------|------|-----------|--------|
| `src/services/socket.service.js` | service | event-driven | First real-time feature in the codebase (RESEARCH.md confirms explicitly: "everything else so far is REST + polling") — composed from `auth.middleware.js` (handshake verify) + `notification.service.js` (module shape) + RESEARCH.md Patterns 1-3 (official Socket.io docs); see Pattern Assignments above for the composed reference |
| `MINIREDESOCIAL_FRONT/src/lib/socket.js` | utility | event-driven | No existing socket client singleton; closest shape is `lib/api.js` (axios singleton + JWT-from-localStorage convention), adapted per RESEARCH.md Pattern 2 client example |
| `MINIREDESOCIAL_FRONT/src/hooks/useChatSocket.js` | hook | streaming | No existing push-based hook (only `useNotificationPolling`'s pull-based `setInterval` shape); structural convention (useState+useEffect+cleanup) carries over, the subscription mechanism itself is new |
| `MINIREDESOCIAL_FRONT/src/components/MessageBubble/MessageBubble.jsx` | component | transform (render) | No existing chat-bubble component; render-as-plain-text convention mirrors `PostCard`'s text rendering (no `dangerouslySetInnerHTML` anywhere in the codebase) |
| `MINIREDESOCIAL_FRONT/src/components/ChatComposer/ChatComposer.jsx` | component | file-I/O + request-response | No existing combined text+image composer; synthesized from `PhotoUpload.jsx` (image picker) + `ComposePage` (text input) — two analogs, no single exact match |

## Metadata

**Analog search scope:** `src/{models,controllers,routes,services,middlewares}/`, `MINIREDESOCIAL_FRONT/src/{components,pages,hooks,lib}/`
**Files scanned:** ~30 (backend models/controllers/routes/services/middlewares + frontend components/pages/hooks/lib referenced in RESEARCH.md canonical refs)
**Pattern extraction date:** 2026-06-08
