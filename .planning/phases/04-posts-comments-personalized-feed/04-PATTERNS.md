# Phase 4: Posts, Comments & Personalized Feed - Pattern Map

**Mapped:** 2026-06-07
**Files analyzed:** 15
**Analogs found:** 15 / 15

## File Classification

| New/Modified File | Role | Data Flow | Closest Analog | Match Quality |
|---|---|---|---|---|
| `src/models/Post.js` | model | CRUD | `src/models/Follow.js` | role-match |
| `src/models/Comment.js` | model | CRUD | `src/models/Notification.js` | role-match |
| `src/routes/post.routes.js` | route | request-response | `src/routes/user.routes.js` | exact |
| `src/routes/comment.routes.js` | route | request-response | `src/routes/follow.routes.js` | exact |
| `src/controllers/post.controller.js` | controller | CRUD + file-I/O | `src/controllers/media.controller.js` + `src/controllers/follow.controller.js` | role-match (composite) |
| `src/controllers/comment.controller.js` | controller | CRUD | `src/controllers/follow.controller.js` | exact |
| `src/services/s3.service.js` (extend) | service | file-I/O | self (`uploadAvatar` function, lines 36-48) | exact |
| `src/app.js` (mount routes) | config | request-response | self (lines 19-27) | exact |
| `MINIREDESOCIAL_FRONT/src/pages/FeedPage.jsx` | page | request-response + streaming | `MINIREDESOCIAL_FRONT/src/pages/SearchPage/SearchPage.jsx` | role-match |
| `MINIREDESOCIAL_FRONT/src/pages/PostDetailPage.jsx` | page | request-response | `MINIREDESOCIAL_FRONT/src/pages/ProfilePage/ProfilePage.jsx` | role-match |
| `MINIREDESOCIAL_FRONT/src/pages/ComposePage.jsx` | page | file-I/O + request-response | `MINIREDESOCIAL_FRONT/src/pages/ProfilePage/ProfilePage.jsx` | role-match |
| `MINIREDESOCIAL_FRONT/src/components/PostCard/PostCard.jsx` | component | request-response | `MINIREDESOCIAL_FRONT/src/components/UserResultCard/UserResultCard.jsx` | exact |
| `MINIREDESOCIAL_FRONT/src/components/AppShell/AppShell.jsx` (update) | component | request-response | self (current file) | exact |
| `MINIREDESOCIAL_FRONT/src/App.jsx` (add routes) | config | request-response | self (current file) | exact |
| `MINIREDESOCIAL_FRONT/src/lib/api.js` | utility | request-response | self (reference only) | exact |

---

## Pattern Assignments

### `src/models/Post.js` (model, CRUD)

**Analog:** `src/models/Follow.js`

**Full model pattern** (`src/models/Follow.js`, lines 1-31):
```javascript
'use strict';

const mongoose = require('mongoose');

const followSchema = new mongoose.Schema({
  followerId: {
    type: mongoose.Schema.Types.ObjectId,
    ref: 'User',
    required: true,
  },
  // ...fields...
  createdAt: {
    type: Date,
    default: Date.now,
  },
});

followSchema.index({ followerId: 1, followingId: 1 }, { unique: true });
followSchema.index({ followerId: 1, status: 1 });

module.exports = mongoose.model('Follow', followSchema);
```

**Post.js schema to create** — field list from CONTEXT.md canonical spec:
- `authorId` (ObjectId, ref: 'User', required)
- `institutionId` (ObjectId, ref: 'Institution', required) — multi-tenant scope
- `text` (String, required, maxlength: 1000)
- `imageUrls` ([String], default: []) — up to 4 S3 URLs
- `linkUrl` (String) — explicit link field (D-06)
- `hasLink` (Boolean, default: false) — set true when linkUrl is non-empty
- `categoryIds` ([ObjectId], ref: 'Category', default: [])
- `commentCount` (Number, default: 0) — denormalized counter
- `createdAt` (Date, default: Date.now)

**Indexes to add:**
```javascript
postSchema.index({ authorId: 1, createdAt: -1 });
postSchema.index({ institutionId: 1, createdAt: -1 });
// Supports cursor-based feed: _id < cursor, sorted _id desc
postSchema.index({ authorId: 1, _id: -1 });
```

---

### `src/models/Comment.js` (model, CRUD)

**Analog:** `src/models/Notification.js`

**Notification pattern** (`src/models/Notification.js`, lines 1-39) shows the `refId`/`refModel` convention and `userId`+`actorId` pairing. Comment is simpler:
- `postId` (ObjectId, ref: 'Post', required)
- `authorId` (ObjectId, ref: 'User', required)
- `institutionId` (ObjectId, ref: 'Institution', required) — multi-tenant scope
- `text` (String, required, maxlength: 500)
- `createdAt` (Date, default: Date.now)

**Index:**
```javascript
commentSchema.index({ postId: 1, createdAt: 1 });
```

---

### `src/routes/post.routes.js` (route, request-response)

**Analog:** `src/routes/user.routes.js`

**Route file pattern** (`src/routes/user.routes.js`, lines 1-34):
```javascript
'use strict';

const { Router } = require('express');
const authenticate = require('../middlewares/auth.middleware');
const { getProfile, ... } = require('../controllers/user.controller');

const router = Router();

router.get('/search', authenticate, searchUsers);
router.get('/:id', authenticate, getProfile);
router.patch('/me', authenticate, updateMe);

module.exports = router;
```

**Post routes to define:**
```javascript
// Multipart middleware pattern from src/routes/media.routes.js lines 11-26:
const multer = require('multer');
const { MAX_UPLOAD_BYTES } = require('../config/env');
const uploadMiddleware = multer({
  storage: multer.memoryStorage(),
  limits: { fileSize: MAX_UPLOAD_BYTES },
});

// POST /api/posts — create post (multipart/form-data: text, images[], linkUrl, categoryIds[])
router.post(
  '/',
  authenticate,
  (req, res, next) => {
    uploadMiddleware.array('images', 4)(req, res, (err) => {
      if (err) return res.status(400).json({ error: 'File upload error' });
      return next();
    });
  },
  createPost
);

// GET /api/posts/feed — cursor-based feed for authenticated user
router.get('/feed', authenticate, getFeed);

// GET /api/posts/:id — single post detail
router.get('/:id', authenticate, getPost);
```

**Note:** `requireRole` from `src/middlewares/role.middleware.js` applies inside the controller for link permission check (CONT-03), not at the route level, because all roles can POST — only `aluno` is blocked from using `linkUrl`.

---

### `src/routes/comment.routes.js` (route, request-response)

**Analog:** `src/routes/follow.routes.js`

**Minimal route file pattern** (`src/routes/follow.routes.js`, lines 1-15):
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

**Comment routes to define:**
```javascript
// POST /api/posts/:postId/comments — create comment
router.post('/:postId/comments', authenticate, createComment);

// GET /api/posts/:postId/comments — list all comments on a post
router.get('/:postId/comments', authenticate, listComments);
```

**Note:** Comment routes are mounted under `/api/posts` prefix in `app.js` (not `/api/comments`) so `:postId` is always available.

---

### `src/controllers/post.controller.js` (controller, CRUD + file-I/O)

**Analog:** `src/controllers/follow.controller.js` (structure) + `src/controllers/media.controller.js` (S3 pattern)

**Controller function structure** (`src/controllers/follow.controller.js`, lines 12-53):
```javascript
'use strict';

const Follow = require('../models/Follow');
const User = require('../models/User');
const Block = require('../models/Block');
const notificationService = require('../services/notification.service');

async function follow(req, res) {
  const followerId = req.user.id;
  const followingId = req.params.id;

  // validation guard
  if (followerId === followingId) {
    return res.status(400).json({ error: 'Cannot follow yourself' });
  }

  try {
    // business logic queries
    const blocked = await Block.findOne({ $or: [...] });
    if (blocked) {
      return res.status(403).json({ error: 'Forbidden' });
    }

    const followDoc = await Follow.create({ followerId, followingId, status });

    // fire-and-forget side effect
    await notificationService.create('new_follower', ...);

    return res.status(201).json({ status: followDoc.status, followId: followDoc._id });
  } catch (err) {
    if (err.code === 11000) {
      return res.status(409).json({ error: 'Already following...' });
    }
    return res.status(500).json({ error: 'Internal server error' });
  }
}

module.exports = { follow, unfollow, acceptFollow, rejectFollow };
```

**S3 upload integration** (`src/controllers/media.controller.js`, lines 13-36):
```javascript
async function upload(req, res) {
  const file = req.file;
  if (!file) return res.status(400).json({ error: 'file is required' });
  if (!ALLOWED_CONTENT_TYPES.has(file.mimetype)) {
    return res.status(400).json({ error: 'file must be image/jpeg, image/png, or image/webp' });
  }
  try {
    const { key, objectUrl } = await uploadAvatar({ buffer: file.buffer, contentType: file.mimetype });
    return res.status(201).json({ key, objectUrl });
  } catch (err) {
    return res.status(500).json({ error: 'Internal server error' });
  }
}
```

**Functions to implement in post.controller.js:**

`createPost(req, res)`:
1. Read `req.user.id`, `req.user.role`, `req.user.institutionId` from JWT (set by `authenticateToken`)
2. Validate `text` present; if `linkUrl` non-empty, check `req.user.role !== 'aluno'` → 403
3. For each file in `req.files` (up to 4), call `uploadPostImage({ buffer, contentType })` → collect `objectUrl` array
4. `Post.create({ authorId, institutionId, text, imageUrls, linkUrl, hasLink, categoryIds })`
5. Return `res.status(201).json({ post })`

`getFeed(req, res)`:
1. `Follow.find({ followerId: req.user.id, status: 'accepted' })` → `followingIds`
2. `Block.find({ $or: [{ blockerId: req.user.id }, { blockedId: req.user.id }] })` → `blockedIds` (both directions)
3. Build query: `{ authorId: { $in: followingIds, $nin: blockedIds }, institutionId: req.user.institutionId, ...(cursor ? { _id: { $lt: cursor } } : {}) }`
4. `.sort({ _id: -1 }).limit(21)` — fetch 21 to detect `hasMore`
5. Populate `authorId` (name, avatarUrl) and `categoryIds` (name, slug)
6. Return `{ posts: posts.slice(0, 20), hasMore: posts.length === 21, nextCursor: posts[19]?._id }`

`getPost(req, res)`:
1. `Post.findById(req.params.id)` with populate
2. Check institutionId match (multi-tenant)
3. Return `{ post }` or 404

---

### `src/controllers/comment.controller.js` (controller, CRUD)

**Analog:** `src/controllers/follow.controller.js`

**Functions to implement:**

`createComment(req, res)`:
1. `req.user.id`, `req.params.postId`, `req.body.text`
2. `Post.findById(postId)` → 404 if not found; check `institutionId` matches
3. `Comment.create({ postId, authorId: req.user.id, institutionId: req.user.institutionId, text })`
4. `Post.findByIdAndUpdate(postId, { $inc: { commentCount: 1 } })`
5. `notificationService.create('post_comment', req.user.id, post.authorId, comment._id, 'Comment')` — fire-and-forget, skip if self-comment
6. Return `res.status(201).json({ comment })`

`listComments(req, res)`:
1. `Comment.find({ postId: req.params.postId }).sort({ createdAt: 1 }).populate('authorId', 'name avatarUrl')`
2. Return `{ comments }`

**Error pattern** (`src/controllers/follow.controller.js`, lines 48-53):
```javascript
  } catch (err) {
    if (err.code === 11000) {
      return res.status(409).json({ error: '...' });
    }
    return res.status(500).json({ error: 'Internal server error' });
  }
```

---

### `src/services/s3.service.js` — extend with `uploadPostImage` (service, file-I/O)

**Analog:** Self — `uploadAvatar` function (lines 36-48)

**Exact pattern to copy and adapt:**
```javascript
// src/services/s3.service.js lines 36-48
async function uploadAvatar({ buffer, contentType }) {
  const ext = EXTENSION_MAP[contentType] || '';
  const key = `avatars/${crypto.randomUUID()}${ext ? '.' + ext : ''}`;

  await s3.send(new PutObjectCommand({
    Bucket: S3_BUCKET,
    Key: key,
    Body: buffer,
    ContentType: contentType,
  }));

  return { key, objectUrl: buildObjectUrl(key) };
}
```

**New function — change only the key prefix:**
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

Add `uploadPostImage` to the `module.exports` at line 65.

---

### `src/app.js` — mount new routes (config, request-response)

**Analog:** Self (lines 19-27)

**Existing mount pattern:**
```javascript
// src/app.js lines 24-27
// Social graph routers (Plan 03)
app.use('/api/users', require('./routes/user.routes'));
app.use('/api/follows', require('./routes/follow.routes'));
```

**Lines to add after line 27:**
```javascript
// Posts, Comments & Feed (Plan 04)
app.use('/api/posts', require('./routes/post.routes'));
```

Comment routes are nested under `/api/posts/:postId/comments`, so they are mounted inside `post.routes.js` (no separate top-level mount in `app.js`).

---

### `MINIREDESOCIAL_FRONT/src/pages/FeedPage.jsx` (page, request-response + streaming)

**Analog:** `/home/maceloalexandre/Documentos/projetos/MINIREDESOCIAL_FRONT/src/pages/SearchPage/SearchPage.jsx`

**Imports pattern** (SearchPage, lines 1-8):
```jsx
import { useState, useEffect, useRef } from 'react'
import { Search } from 'lucide-react'
import api from '../../lib/api'
import UserResultCard from '../../components/UserResultCard/UserResultCard'
import Spinner from '../../components/Spinner'
import ErrorBanner from '../../components/ErrorBanner'
import styles from './SearchPage.module.css'
```

**State and API call pattern** (SearchPage, lines 9-51):
```jsx
const [results, setResults] = useState([])
const [loading, setLoading] = useState(false)
const [apiError, setApiError] = useState('')

useEffect(() => {
  setLoading(true)
  try {
    const res = await api.get(`/api/users/search?...`)
    setResults(res.data.users)
  } catch (_) {
    setApiError('Erro ao buscar usuários. Tente novamente.')
  } finally {
    setLoading(false)
  }
}, [query])
```

**FeedPage-specific pattern to adapt:**
- Replace `useState('')` query debounce with cursor state: `const [cursor, setCursor] = useState(null)` and `const [hasMore, setHasMore] = useState(true)`
- Replace "load more" button with `IntersectionObserver` on a sentinel `<div ref={sentinelRef}>` at bottom of list (D-01)
- Pull-to-refresh: reset cursor to `null` and re-fetch via a `<button>` or touch handler at top (D-03)
- Render list of `<PostCard>` instead of `<UserResultCard>`

**IntersectionObserver pattern** (D-01, no existing analog — new code):
```jsx
const sentinelRef = useRef(null)
useEffect(() => {
  const obs = new IntersectionObserver(([entry]) => {
    if (entry.isIntersecting && hasMore && !loading) fetchNextPage()
  }, { threshold: 0.1 })
  if (sentinelRef.current) obs.observe(sentinelRef.current)
  return () => obs.disconnect()
}, [hasMore, loading])
```

---

### `MINIREDESOCIAL_FRONT/src/pages/PostDetailPage.jsx` (page, request-response)

**Analog:** `/home/maceloalexandre/Documentos/projetos/MINIREDESOCIAL_FRONT/src/pages/ProfilePage/ProfilePage.jsx`

**State machine pattern** (ProfilePage, lines 13-35):
```jsx
const { userId } = useParams()
const [state, setState] = useState('loading') // loading | loaded | error
const [profile, setProfile] = useState(null)

useEffect(() => {
  setState('loading')
  api.get(`/api/users/${id}`)
    .then((res) => {
      setProfile(res.data)
      setState('loaded')
    })
    .catch((err) => {
      setState(err?.response?.status === 404 ? 'blocked' : 'error')
    })
}, [userId])

if (state === 'loading') return <div><Spinner size={32} /></div>
if (state === 'error') return <div><ErrorBanner message="..." /></div>
```

**PostDetailPage-specific structure:**
- `const { postId } = useParams()`
- Fetch `GET /api/posts/:id` and `GET /api/posts/:postId/comments` in parallel (`Promise.all`)
- Render post content at top, scrollable comment thread below, fixed-bottom textarea + submit button
- Comment submit: `POST /api/posts/:postId/comments` with `{ text }` → append to comments list optimistically

---

### `MINIREDESOCIAL_FRONT/src/pages/ComposePage.jsx` (page, file-I/O + request-response)

**Analog:** `/home/maceloalexandre/Documentos/projetos/MINIREDESOCIAL_FRONT/src/pages/ProfilePage/ProfilePage.jsx` (form + async submit pattern) + `PhotoUpload.jsx` (file input pattern)

**Form submit pattern** (ProfilePage, lines 37-48):
```jsx
async function handlePrivacyToggle(e) {
  const newValue = e.target.checked
  setIsPrivate(newValue)
  setPrivacyLoading(true)
  try {
    await api.patch('/api/users/me', { isPrivate: newValue })
  } catch {
    setIsPrivate(!newValue) // revert on failure
  } finally {
    setPrivacyLoading(false)
  }
}
```

**Multi-file input pattern** (PhotoUpload.jsx, lines 54-64):
```jsx
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

**ComposePage-specific pattern:**
- `FormData` submit (multipart) — not JSON. Use `api.post('/api/posts', formData, { headers: { 'Content-Type': 'multipart/form-data' } })`
- `input type="file" multiple accept="image/jpeg,image/png,image/webp"` — up to 4 files (D-04)
- After success: `navigate('/')` to reset to feed (D-12)
- Category multi-select: fetch `GET /api/categories` on mount (if endpoint exists) or hard-code if not — planner should check

---

### `MINIREDESOCIAL_FRONT/src/components/PostCard/PostCard.jsx` (component, request-response)

**Analog:** `/home/maceloalexandre/Documentos/projetos/MINIREDESOCIAL_FRONT/src/components/UserResultCard/UserResultCard.jsx`

**Card component pattern** (UserResultCard.jsx, lines 1-46):
```jsx
import { useState } from 'react'
import { useNavigate } from 'react-router-dom'
import FollowButton from '../FollowButton/FollowButton'
import styles from './UserResultCard.module.css'

export default function UserResultCard({ user, onFollowStatusChange }) {
  const navigate = useNavigate()

  function handleCardClick() {
    navigate(`/profile/${user._id}`)
  }

  return (
    <div className={styles.card} onClick={handleCardClick}>
      <img src={user.avatarUrl} alt={user.name} className={styles.avatar} />
      <div className={styles.info}>
        <span className={styles.name}>{user.name}</span>
        <span className={styles.role}>{user.role}</span>
      </div>
    </div>
  )
}
```

**PostCard-specific structure** (from D-07):
```jsx
export default function PostCard({ post }) {
  const navigate = useNavigate()

  function handleClick() {
    navigate(`/posts/${post._id}`)
  }

  return (
    <div className={styles.card} onClick={handleClick}>
      {/* avatar + author name */}
      {/* post text */}
      {/* image thumbnails (up to 4) */}
      {/* category chips */}
      {/* comment count button */}
    </div>
  )
}
```

Co-located CSS Module: `PostCard/PostCard.module.css` — same pattern as `UserResultCard/UserResultCard.module.css`.

---

### `MINIREDESOCIAL_FRONT/src/components/AppShell/AppShell.jsx` — update nav + header (component)

**Analog:** Self (current file)

**Current TABS definition** (AppShell.jsx, lines 6-11):
```jsx
const TABS = [
  { to: '/', icon: Home, label: 'Feed', end: true },
  { to: '/search', icon: Search, label: 'Busca', end: false },
  { to: '/notifications', icon: Bell, label: 'Notif', end: false },
  { to: '/profile/me', icon: User, label: 'Perfil', end: false },
]
```

**Changes required (D-10, D-11):**
1. Replace tab index 2 (`/notifications`, Bell) with `{ to: '/compose', icon: Plus, label: 'Criar', end: false }`
2. Add `Plus` to the `lucide-react` import (line 3): `import { Home, Search, Plus, User, Bell } from 'lucide-react'`
3. Add bell icon button to header (lines 40-53) next to "Sair" button — Phase 5 will wire up badge count; for now renders with `0` badge or no badge

**Current header** (AppShell.jsx, lines 40-53):
```jsx
<header className={`${styles.header} ${headerHidden ? styles.headerHidden : ''}`}>
  <span className={styles.wordmark}>Fademy</span>
  <button type="button" className={styles.logoutBtn} onClick={handleLogout}>
    Sair
  </button>
</header>
```

---

### `MINIREDESOCIAL_FRONT/src/App.jsx` — add routes (config)

**Analog:** Self (current file)

**Current protected route block** (App.jsx, lines 22-55):
```jsx
<Route element={<RequireAuth><AppShell /></RequireAuth>}>
  <Route path="/" element={<div>Feed em breve...</div>} />
  <Route path="/search" element={<SearchPage />} />
  <Route path="/notifications" element={<div>Notificações em breve...</div>} />
  <Route path="/profile/:userId" element={<ProfilePage />} />
</Route>
```

**Changes required:**
```jsx
import FeedPage from './pages/FeedPage/FeedPage'
import PostDetailPage from './pages/PostDetailPage/PostDetailPage'
import ComposePage from './pages/ComposePage/ComposePage'

// Replace placeholder / route and add new routes:
<Route path="/" element={<FeedPage />} />
<Route path="/posts/:postId" element={<PostDetailPage />} />
<Route path="/compose" element={<ComposePage />} />
// Keep /search, /profile/:userId; remove /notifications (bell moves to header in D-10)
```

---

### `MINIREDESOCIAL_FRONT/src/lib/api.js` (utility — reference only)

**Current file** (lines 1-15) — already configured with JWT interceptor:
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

**No changes needed.** All new pages import `api` from `../../lib/api` the same way SearchPage and ProfilePage do.

---

## Shared Patterns

### Authentication Guard (backend)
**Source:** `src/middlewares/auth.middleware.js` (lines 12-34)
**Apply to:** All `post.routes.js` and `comment.routes.js` route definitions

```javascript
// Every protected route:
router.get('/feed', authenticate, getFeed);
router.post('/', authenticate, uploadMiddleware, createPost);
```

`req.user` is guaranteed to have `{ id, role, institutionId }` after `authenticate` runs.

### Role Guard (backend)
**Source:** `src/middlewares/role.middleware.js` (lines 15-28)
**Apply to:** `createPost` — `aluno` role may not use `linkUrl` (CONT-03)

```javascript
// Pattern: check role inside controller, not at route level,
// because all roles can create posts — only linkUrl is gated.
if (req.body.linkUrl && req.user.role === 'aluno') {
  return res.status(403).json({ error: 'Insufficient permissions' });
}
```

### Multi-Tenant Scoping (backend)
**Source:** `src/controllers/follow.controller.js` + `src/models/User.js` `institutionId` field
**Apply to:** All Post and Comment queries

```javascript
// Every query must filter by institutionId:
Post.find({ authorId: { $in: followingIds }, institutionId: req.user.institutionId, ... })
// Every document creation must include institutionId:
Post.create({ ..., institutionId: req.user.institutionId })
```

### Notification Fire-and-Forget (backend)
**Source:** `src/services/notification.service.js` (lines 5-11) + usage in `src/controllers/follow.controller.js` (lines 39-45)
**Apply to:** `comment.controller.js` → `createComment`

```javascript
// Fire-and-forget — swallows errors internally; do not await if you want non-blocking
await notificationService.create(
  'post_comment',
  req.user.id,        // actorId (commenter)
  post.authorId,      // userId (post author receives notification)
  comment._id,        // refId
  'Comment'           // refModel
);
// Skip if req.user.id === post.authorId.toString() (no self-notification)
```

### Controller Error Handling (backend)
**Source:** `src/controllers/follow.controller.js` (lines 48-53)
**Apply to:** All controller functions in `post.controller.js` and `comment.controller.js`

```javascript
  } catch (err) {
    if (err.code === 11000) {
      return res.status(409).json({ error: 'Duplicate entry' });
    }
    return res.status(500).json({ error: 'Internal server error' });
  }
```

### Frontend API Call Pattern (frontend)
**Source:** `src/pages/SearchPage/SearchPage.jsx` (lines 25-39) and `src/pages/ProfilePage/ProfilePage.jsx` (lines 21-35)
**Apply to:** FeedPage, PostDetailPage, ComposePage

```jsx
// Async with loading/error state:
setLoading(true)
setApiError('')
try {
  const res = await api.get('/api/...')
  setData(res.data)
} catch (err) {
  setApiError('Mensagem de erro amigável.')
} finally {
  setLoading(false)
}
```

```jsx
// Error check pattern from ProfilePage:
.catch((err) => {
  setState(err?.response?.status === 404 ? 'notFound' : 'error')
})
```

### CSS Module Co-location (frontend)
**Source:** All existing components — `UserResultCard/UserResultCard.module.css`, `AppShell/AppShell.module.css`, `SearchPage/SearchPage.module.css`
**Apply to:** All new pages and components

```
PostCard/
  PostCard.jsx
  PostCard.module.css
FeedPage/
  FeedPage.jsx
  FeedPage.module.css
PostDetailPage/
  PostDetailPage.jsx
  PostDetailPage.module.css
ComposePage/
  ComposePage.jsx
  ComposePage.module.css
```

Design token usage: `var(--color-surface)`, `var(--color-accent)`, etc. from `src/styles/tokens.css`.

### Multipart S3 Upload (backend)
**Source:** `src/routes/media.routes.js` (lines 11-26) + `src/controllers/media.controller.js` (lines 13-36)
**Apply to:** `post.routes.js` + `post.controller.js`

```javascript
// Route: multer array (not single) for up to 4 images
uploadMiddleware.array('images', 4)(req, res, (err) => {
  if (err) return res.status(400).json({ error: 'File upload error' });
  return next();
});

// Controller: iterate req.files
const ALLOWED_CONTENT_TYPES = new Set(['image/jpeg', 'image/png', 'image/webp']);
const imageUrls = [];
for (const file of (req.files || [])) {
  if (!ALLOWED_CONTENT_TYPES.has(file.mimetype)) {
    return res.status(400).json({ error: 'Invalid file type' });
  }
  const { objectUrl } = await uploadPostImage({ buffer: file.buffer, contentType: file.mimetype });
  imageUrls.push(objectUrl);
}
```

---

## No Analog Found

All files have close analogs in the existing codebase.

| File | Note |
|---|---|
| `FeedPage.jsx` — IntersectionObserver | New browser API usage; no existing infinite scroll in codebase. Use pattern from D-01 specifics section in CONTEXT.md |
| `ComposePage.jsx` — FormData multipart | `api.post` with `FormData` is new frontend pattern; backend analog exists in `media.routes.js`. Frontend sends `Content-Type: multipart/form-data` via axios automatically when body is `FormData` |

---

## Metadata

**Analog search scope:** `src/models/`, `src/controllers/`, `src/routes/`, `src/services/`, `src/middlewares/`, `MINIREDESOCIAL_FRONT/src/`
**Files scanned:** 18
**Pattern extraction date:** 2026-06-07
