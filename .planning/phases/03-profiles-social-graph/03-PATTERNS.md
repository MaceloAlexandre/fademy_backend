# Phase 3: Profiles & Social Graph - Pattern Map

**Mapped:** 2026-06-03
**Files analyzed:** 16 new/modified files
**Analogs found:** 15 / 16

---

## File Classification

| New/Modified File | Role | Data Flow | Closest Analog | Match Quality |
|-------------------|------|-----------|----------------|---------------|
| `src/models/Follow.js` | model | CRUD | `src/models/Invite.js` | role-match |
| `src/models/Block.js` | model | CRUD | `src/models/Invite.js` | role-match |
| `src/models/Notification.js` | model | CRUD | `src/models/Invite.js` | role-match |
| `src/controllers/follow.controller.js` | controller | CRUD | `src/controllers/invite.controller.js` | exact |
| `src/controllers/block.controller.js` | controller | CRUD | `src/controllers/invite.controller.js` | exact |
| `src/controllers/user.controller.js` | controller | request-response | `src/controllers/admin.controller.js` | exact |
| `src/routes/follow.routes.js` | route | request-response | `src/routes/invite.routes.js` | exact |
| `src/routes/user.routes.js` | route | request-response | `src/routes/invite.routes.js` | exact |
| `src/controllers/follow.controller.test.js` | test | CRUD | `src/controllers/invite.controller.test.js` | exact |
| `src/controllers/block.controller.test.js` | test | CRUD | `src/controllers/invite.controller.test.js` | exact |
| `src/controllers/user.controller.test.js` | test | request-response | `src/controllers/invite.controller.test.js` | exact |
| `src/services/notification.service.js` | service | event-driven | `src/services/s3.service.js` | role-match |
| `MINIREDESOCIAL_FRONT/src/components/AppShell/AppShell.jsx` | component | request-response | `MINIREDESOCIAL_FRONT/src/App.jsx` (PostLoginShell) | exact |
| `MINIREDESOCIAL_FRONT/src/pages/ProfilePage/ProfilePage.jsx` | page | request-response | `MINIREDESOCIAL_FRONT/src/pages/AcceptInvite.jsx` | role-match |
| `MINIREDESOCIAL_FRONT/src/components/FollowButton/FollowButton.jsx` | component | request-response | `MINIREDESOCIAL_FRONT/src/components/PrimaryButton.jsx` | role-match |
| `MINIREDESOCIAL_FRONT/src/pages/SearchPage/SearchPage.jsx` | page | request-response | `MINIREDESOCIAL_FRONT/src/pages/AcceptInvite.jsx` | role-match |
| `MINIREDESOCIAL_FRONT/src/components/UserResultCard/UserResultCard.jsx` | component | request-response | none | no analog |
| `src/app.js` (modify) | config | request-response | `src/app.js` itself | self |
| `MINIREDESOCIAL_FRONT/src/App.jsx` (modify) | config | request-response | `MINIREDESOCIAL_FRONT/src/App.jsx` itself | self |
| `package.json` (modify) | config | — | `package.json` itself | self |

---

## Pattern Assignments

### `src/models/Follow.js` (model, CRUD)

**Analog:** `src/models/Invite.js`

**Imports + schema pattern** (Invite.js lines 1-49 — copy structure exactly):
```javascript
'use strict';

const mongoose = require('mongoose');

const followSchema = new mongoose.Schema({
  followerId:  { type: mongoose.Schema.Types.ObjectId, ref: 'User', required: true },
  followingId: { type: mongoose.Schema.Types.ObjectId, ref: 'User', required: true },
  status:      { type: String, enum: ['pending', 'accepted'], required: true },
  createdAt:   { type: Date, default: Date.now },
});

followSchema.index({ followerId: 1, followingId: 1 }, { unique: true });
followSchema.index({ followerId: 1, status: 1 });
followSchema.index({ followingId: 1, status: 1 });

module.exports = mongoose.model('Follow', followSchema);
```

**Key conventions from analog:**
- `'use strict'` at top (line 1 of every model)
- Fields: ObjectId refs use `mongoose.Schema.Types.ObjectId`
- `default: Date.now` (not `Date.now()`) for createdAt
- Single `module.exports = mongoose.model(...)` at bottom

---

### `src/models/Block.js` (model, CRUD)

**Analog:** `src/models/Invite.js`

**Pattern** (same conventions as Follow.js):
```javascript
'use strict';

const mongoose = require('mongoose');

const blockSchema = new mongoose.Schema({
  blockerId: { type: mongoose.Schema.Types.ObjectId, ref: 'User', required: true },
  blockedId: { type: mongoose.Schema.Types.ObjectId, ref: 'User', required: true },
  createdAt: { type: Date, default: Date.now },
});

blockSchema.index({ blockerId: 1, blockedId: 1 }, { unique: true });
blockSchema.index({ blockedId: 1 });

module.exports = mongoose.model('Block', blockSchema);
```

---

### `src/models/Notification.js` (model, CRUD)

**Analog:** `src/models/Invite.js`

**Pattern** (enum + index per canonical CONTEXT.md schema):
```javascript
'use strict';

const mongoose = require('mongoose');

const notificationSchema = new mongoose.Schema({
  userId:    { type: mongoose.Schema.Types.ObjectId, ref: 'User', required: true },
  actorId:   { type: mongoose.Schema.Types.ObjectId, ref: 'User', required: true },
  type:      { type: String, enum: ['new_follower','follow_request','follow_accepted','post_comment','invite_received','message_received'], required: true },
  refId:     { type: mongoose.Schema.Types.ObjectId },
  refModel:  { type: String },
  read:      { type: Boolean, default: false },
  createdAt: { type: Date, default: Date.now },
});

notificationSchema.index({ userId: 1, read: 1, createdAt: -1 });

module.exports = mongoose.model('Notification', notificationSchema);
```

---

### `src/controllers/follow.controller.js` (controller, CRUD)

**Analog:** `src/controllers/invite.controller.js`

**Imports pattern** (invite.controller.js lines 1-5):
```javascript
'use strict';

const Follow = require('../models/Follow');
const User = require('../models/User');
const Block = require('../models/Block');
const notificationService = require('../services/notification.service');
```

**Core pattern — self-check + block check before action** (invite.controller.js lines 21-68 structure):
```javascript
async function follow(req, res) {
  const followerId = req.user.id;
  const followingId = req.params.id;

  if (followerId === followingId) {
    return res.status(400).json({ error: 'Cannot follow yourself' });
  }

  // ... block check, then target lookup, then create ...
  try {
    const follow = await Follow.create({ followerId, followingId, status });
    // ...
    return res.status(201).json({ status: follow.status, followId: follow._id });
  } catch (err) {
    if (err.code === 11000) {
      return res.status(409).json({ error: 'Already following or request pending' });
    }
    return res.status(500).json({ error: 'Internal server error' });
  }
}
```

**Error handling pattern** (invite.controller.js lines 65-68 — used throughout):
```javascript
  } catch (err) {
    return res.status(500).json({ error: 'Internal server error' });
  }
```

**findOneAndUpdate (idempotent upsert)** (invite.controller.js lines 119-124 — model for block upsert):
```javascript
const invite = await Invite.findOneAndUpdate(
  { token, institutionId },
  { status: 'revoked' },
  { new: true }
);
```

**Module exports** (invite.controller.js line 135):
```javascript
module.exports = { follow, unfollow, acceptFollow, rejectFollow };
```

---

### `src/controllers/block.controller.js` (controller, CRUD)

**Analog:** `src/controllers/invite.controller.js`

Same import and error-handling conventions. Key pattern: bilateral Follow.deleteMany + Block.findOneAndUpdate upsert. Self-block check at entry (400). Returns `res.status(200).json({ blocked: true })`.

---

### `src/controllers/user.controller.js` (controller, request-response)

**Analog:** `src/controllers/admin.controller.js`

**Imports + institutionId-scoped query pattern** (admin.controller.js lines 1-13):
```javascript
'use strict';

const Institution = require('../models/Institution');
const User = require('../models/User');

async function getInstitutionOverview(req, res) {
  const { institutionId } = req.user;

  try {
    const institution = await Institution.findById(institutionId).lean();
    if (!institution) {
      return res.status(404).json({ error: 'Institution not found' });
    }
    // ...
    return res.status(200).json({ institution, userCount });
  } catch (err) {
    return res.status(500).json({ error: 'Internal server error' });
  }
}
```

**Key conventions from analog:**
- `const { institutionId } = req.user` — destructure from JWT, never from req.body (PLAT-01)
- `.lean()` on all read queries for plain JS objects
- `return res.status(404).json(...)` early-exit on not-found
- `return res.status(500).json({ error: 'Internal server error' })` in catch

**Multi-model read pattern** (admin.controller.js lines 23-24):
```javascript
const institution = await Institution.findById(institutionId).lean();
const userCount = await User.countDocuments({ institutionId });
```

**Route ordering warning** (RESEARCH.md Pitfall 1) — in `user.routes.js`, register `/search` and `/me` before `/:id`:
```javascript
router.get('/search', authenticate, searchUsers);  // first
router.get('/me', authenticate, getMe);            // before /:id
router.get('/:id', authenticate, getProfile);
router.patch('/me', authenticate, updateMe);
```

---

### `src/routes/follow.routes.js` (route, request-response)

**Analog:** `src/routes/invite.routes.js`

**Full pattern** (invite.routes.js lines 1-20):
```javascript
'use strict';

const { Router } = require('express');
const authenticate = require('../middlewares/auth.middleware');
const { follow, unfollow, acceptFollow, rejectFollow } = require('../controllers/follow.controller');

const router = Router();

// All follow routes require authentication
router.post('/:id/follow', authenticate, follow);
router.delete('/:id/follow', authenticate, unfollow);

// Accept/reject by follow document _id (on /api/follows/:id)
router.patch('/:id/accept', authenticate, acceptFollow);
router.patch('/:id/reject', authenticate, rejectFollow);

module.exports = router;
```

**Key conventions from analog:**
- `'use strict'` + `const { Router } = require('express')`
- Named import for `authenticate` (not default destructured)
- `module.exports = router` at bottom (no curly braces)

---

### `src/routes/user.routes.js` (route, request-response)

**Analog:** `src/routes/invite.routes.js`

Same structure as follow.routes.js. Route ordering: `/search` and `/me` MUST precede `/:id` (Express first-match).

```javascript
'use strict';

const { Router } = require('express');
const authenticate = require('../middlewares/auth.middleware');
const { searchUsers, getProfile, getMe, updateMe } = require('../controllers/user.controller');

const router = Router();

router.get('/search', authenticate, searchUsers);
router.get('/me', authenticate, getMe);
router.get('/:id', authenticate, getProfile);
router.patch('/me', authenticate, updateMe);

module.exports = router;
```

---

### `src/controllers/follow.controller.test.js` (test, CRUD)

**Analog:** `src/controllers/invite.controller.test.js`

**DB setup block** (invite.controller.test.js lines 34-64 — copy exactly, adjust controller import):
```javascript
'use strict';

const { describe, it, before, after } = require('node:test');
const assert = require('node:assert/strict');
const mongoose = require('mongoose');

const { follow, unfollow, acceptFollow, rejectFollow } = require('./follow.controller');

let mongoServer;
let testUri;

function makeRes() {
  const res = {
    statusCode: null,
    body: null,
    status(code) { this.statusCode = code; return this; },
    json(data) { this.body = data; return this; },
  };
  return res;
}

before(async () => {
  let MongoMemoryServer;
  try {
    ({ MongoMemoryServer } = require('mongodb-memory-server'));
  } catch (err) {
    console.log('SKIP: mongodb-memory-server not available');
    process.exit(0);
  }

  const uri = process.env.MONGODB_URI_TEST;
  if (uri) {
    testUri = uri;
  } else {
    mongoServer = await MongoMemoryServer.create();
    testUri = mongoServer.getUri();
  }

  process.env.MONGODB_URI = testUri;
  process.env.JWT_SECRET = process.env.JWT_SECRET || 'test-secret';
  process.env.BCRYPT_ROUNDS = '4';

  const { connectDB } = require('../config/db');
  await connectDB(testUri);
});

after(async () => {
  await mongoose.disconnect();
  if (mongoServer) await mongoServer.stop();
});
```

**Seed helper pattern** (invite.controller.test.js lines 68-90):
```javascript
async function seedInstitutionAndUsers() {
  const Institution = require('../models/Institution');
  const User = require('../models/User');
  const bcrypt = require('bcrypt');
  const { BCRYPT_ROUNDS } = require('../config/env');

  const inst = await Institution.create({ name: 'Test Inst', slug: 'test-' + Date.now() });

  const userA = await User.create({
    institutionId: inst._id,
    name: 'User A',
    email: 'a-' + Date.now() + '@test.com',
    passwordHash: await bcrypt.hash('pass', BCRYPT_ROUNDS),
    role: 'aluno',
  });

  const userB = await User.create({
    institutionId: inst._id,
    name: 'User B',
    email: 'b-' + Date.now() + '@test.com',
    passwordHash: await bcrypt.hash('pass', BCRYPT_ROUNDS),
    role: 'aluno',
  });

  return { inst, userA, userB };
}
```

**Test body pattern** (invite.controller.test.js lines 96-125):
```javascript
describe('follow.controller', () => {
  describe('follow', () => {
    it('creates Follow{status:accepted} when target profile is public', async () => {
      const { userA, userB } = await seedInstitutionAndUsers();

      const req = {
        user: { id: userA._id.toString(), institutionId: userA.institutionId.toString() },
        params: { id: userB._id.toString() },
      };
      const res = makeRes();

      await follow(req, res);

      assert.equal(res.statusCode, 201);
      assert.equal(res.body.status, 'accepted');
    });
  });
});
```

---

### `src/controllers/block.controller.test.js` and `src/controllers/user.controller.test.js` (test, CRUD / request-response)

**Analog:** `src/controllers/invite.controller.test.js`

Identical DB setup and makeRes() pattern. The registration.controller.test.js (lines 1-23) shows the Module._load mock pattern for mocking services — use the same approach to mock `notification.service.js` in follow and block controller tests to avoid needing a real Notification model:

```javascript
// registration.controller.test.js lines 8-23 — mock pattern:
const Module = require('module');
const originalLoad = Module._load.bind(Module);
Module._load = function (request, parent, isMain) {
  if (request.includes('notification.service')) {
    return { create: async () => {} };  // silent no-op stub
  }
  return originalLoad(request, parent, isMain);
};
```

---

### `src/services/notification.service.js` (service, event-driven)

**Analog:** `src/services/s3.service.js`

**Service module pattern** (s3.service.js lines 1-65 — note: simpler for the stub):
```javascript
'use strict';

const Notification = require('../models/Notification');

/**
 * Creates a notification record. Never throws — notification failures
 * must not break the action that triggered them.
 */
async function create(type, actorId, userId, refId, refModel) {
  try {
    await Notification.create({ type, actorId, userId, refId, refModel });
  } catch (_err) {
    // Silently ignore — notification is non-critical
  }
}

module.exports = { create };
```

**Key conventions from s3.service.js:**
- `'use strict'` at top
- Named function exports (not object literal methods)
- `module.exports = { create }` at bottom

---

### `src/app.js` (modify — mount new routers)

**Pattern from existing** (app.js lines 18-22):
```javascript
// Auth + protected routers (Plan 02)
app.use('/api/auth', require('./routes/auth.routes'));
app.use('/api/admin', require('./routes/admin.routes'));
app.use('/api/invites', require('./routes/invite.routes'));
app.use('/api/media', require('./routes/media.routes'));
```

**Add after existing mounts (same pattern):**
```javascript
app.use('/api/users', require('./routes/user.routes'));
app.use('/api/follows', require('./routes/follow.routes'));
```

Note: block routes are mounted under `/api/users/:id/block` — handled in user.routes.js or a dedicated block.routes.js also mounted at `/api/users`.

---

### `MINIREDESOCIAL_FRONT/src/components/AppShell/AppShell.jsx` (component, request-response)

**Analog:** `MINIREDESOCIAL_FRONT/src/App.jsx` (PostLoginShell function, lines 15-36) + `MINIREDESOCIAL_FRONT/src/App.module.css`

**Shell structure pattern** (App.jsx lines 15-36 — PostLoginShell is the direct predecessor):
```jsx
function PostLoginShell() {
  const navigate = useNavigate()

  function handleLogout() {
    localStorage.removeItem('fademy_token')
    navigate('/login')
  }

  return (
    <div className={styles.shell}>
      <header className={styles.header}>
        <span className={styles.wordmark}>Fademy</span>
        <button type="button" className={styles.logoutBtn} onClick={handleLogout}>
          Sair
        </button>
      </header>
      <main className={styles.main}>
        <p className={styles.placeholder}>Feed em breve...</p>
      </main>
    </div>
  )
}
```

**Imports pattern** (App.jsx lines 1-6):
```jsx
import { BrowserRouter, Routes, Route, Navigate, useNavigate } from 'react-router-dom'
import styles from './App.module.css'
```

**AppShell extends PostLoginShell with:**
1. `useScrollDirection` hook (useRef + useEffect scroll listener — from RESEARCH.md Pattern 6)
2. Bottom nav using `NavLink` from react-router-dom + lucide-react icons
3. `<Outlet />` from react-router-dom replacing the hardcoded `<main>` content
4. CSS class toggling: `className={headerHidden ? styles.headerHidden : styles.header}`

**CSS token conventions** (tokens.css lines 1-40 — use these vars in AppShell.module.css):
- Bottom nav background: `var(--color-dominant)` (#083C5D)
- Active tab color: `var(--color-accent)` (#8C6E4F)
- Header height: 56px (consistent with tokens `--space-md` padding)
- `--touch-min: 44px` for all tappable elements

---

### `MINIREDESOCIAL_FRONT/src/pages/ProfilePage/ProfilePage.jsx` (page, request-response)

**Analog:** `MINIREDESOCIAL_FRONT/src/pages/AcceptInvite.jsx`

**Loading state pattern** (AcceptInvite.jsx lines 10-38):
```jsx
export default function AcceptInvite() {
  const { token } = useParams()
  const [state, setState] = useState('loading')  // loading | valid | error | ...
  const [invite, setInvite] = useState(null)

  useEffect(() => {
    api
      .get(`/api/invites/${token}`)
      .then((res) => {
        // ...setState based on response
      })
      .catch(() => {
        setState('error')
      })
  }, [token])
```

**ProfilePage adapts this to:**
```jsx
export default function ProfilePage() {
  const { userId } = useParams()                     // or 'me'
  const [state, setState] = useState('loading')      // loading | loaded | error | private | blocked
  const [profile, setProfile] = useState(null)

  useEffect(() => {
    const id = userId === 'me' ? 'me' : userId
    api
      .get(`/api/users/${id}`)
      .then((res) => {
        setProfile(res.data)
        setState('loaded')
      })
      .catch((err) => {
        setState(err?.response?.status === 404 ? 'blocked' : 'error')
      })
  }, [userId])
```

**Import pattern** (AcceptInvite.jsx lines 1-8):
```jsx
import { useEffect, useState } from 'react'
import { useParams } from 'react-router-dom'
import Spinner from '../components/Spinner'
import PrimaryButton from '../components/PrimaryButton'
import ErrorBanner from '../components/ErrorBanner'
import api from '../lib/api'
import styles from './ProfilePage.module.css'
```

**Spinner usage** (AcceptInvite.jsx lines 44-47):
```jsx
{state === 'loading' && (
  <div className={styles.loadingWrap}>
    <Spinner size={32} />
  </div>
)}
```

---

### `MINIREDESOCIAL_FRONT/src/components/FollowButton/FollowButton.jsx` (component, request-response)

**Analog:** `MINIREDESOCIAL_FRONT/src/components/PrimaryButton.jsx`

**PrimaryButton base pattern** (PrimaryButton.jsx lines 1-16):
```jsx
import Spinner from './Spinner'
import styles from './PrimaryButton.module.css'

export default function PrimaryButton({ children, loading = false, disabled = false, type = 'submit', onClick }) {
  return (
    <button
      type={type}
      className={styles.btn}
      disabled={disabled || loading}
      onClick={onClick}
    >
      {loading && <Spinner size={20} />}
      {children}
    </button>
  )
}
```

**FollowButton adapts this pattern:**
- Props: `{ followStatus, targetId, isPrivate, onStatusChange }`
- `followStatus` is `null | 'pending' | 'accepted'`
- Maps status to button label and API call
- Inline confirmation for cancel/unfollow (no modals — per RESEARCH.md Anti-Patterns)
- Loading state via `useState` + `api.post/delete` calls
- Calls `onStatusChange(newStatus)` after successful API call to update parent

---

### `MINIREDESOCIAL_FRONT/src/pages/SearchPage/SearchPage.jsx` (page, request-response)

**Analog:** `MINIREDESOCIAL_FRONT/src/pages/Login.jsx`

**Controlled input + API call pattern** (Login.jsx lines 10-59):
```jsx
export default function Login() {
  const [email, setEmail] = useState('')
  const [apiError, setApiError] = useState('')
  const [loading, setLoading] = useState(false)

  async function handleSubmit(e) {
    e.preventDefault()
    setApiError('')
    try {
      setLoading(true)
      const res = await api.post('/api/auth/login', { email, password })
      // ...
    } catch (err) {
      setLoading(false)
      setApiError('Não foi possível conectar. Verifique sua conexão.')
    }
  }
```

**SearchPage adapts this pattern:**
- Controlled `query` state for input
- Debounce via `useRef` timer (same pattern as `bannerTimerRef` in Login.jsx lines 21-28)
- `results` state array + `loading` + `apiError`
- `useEffect` triggered by query change (debounced 300ms)
- No form submit — fires on keystroke via debounce

**Debounce pattern** (from Login.jsx bannerTimerRef, lines 21-28):
```jsx
const debounceRef = useRef(null)

useEffect(() => {
  clearTimeout(debounceRef.current)
  if (!query.trim()) { setResults([]); return; }
  debounceRef.current = setTimeout(async () => {
    try {
      setLoading(true)
      const res = await api.get(`/api/users/search?q=${encodeURIComponent(query)}&page=1&limit=20`)
      setResults(res.data.users)
    } catch (_) {
      setApiError('Erro ao buscar usuários.')
    } finally {
      setLoading(false)
    }
  }, 300)
  return () => clearTimeout(debounceRef.current)
}, [query])
```

---

### `MINIREDESOCIAL_FRONT/src/App.jsx` (modify)

**Current structure** (App.jsx lines 38-57 — replace PostLoginShell with AppShell):
```jsx
export default function App() {
  return (
    <BrowserRouter>
      <Routes>
        <Route path="/invite/:token" element={<AcceptInvite />} />
        <Route path="/register" element={<Register />} />
        <Route path="/login" element={<Login />} />
        <Route
          path="/"
          element={
            <RequireAuth>
              <PostLoginShell />
            </RequireAuth>
          }
        />
        <Route path="*" element={<Navigate to="/login" replace />} />
      </Routes>
    </BrowserRouter>
  )
}
```

**After modification** — remove `PostLoginShell` function (lines 15-36) and replace the `"/"` Route with nested AppShell routes:
```jsx
import AppShell from './components/AppShell/AppShell'
import ProfilePage from './pages/ProfilePage/ProfilePage'
import SearchPage from './pages/SearchPage/SearchPage'

// Inside App():
<Route element={<RequireAuth><AppShell /></RequireAuth>}>
  <Route path="/" element={<div>Feed em breve...</div>} />
  <Route path="/search" element={<SearchPage />} />
  <Route path="/notifications" element={<div>Notificações em breve...</div>} />
  <Route path="/profile/me" element={<ProfilePage />} />
  <Route path="/profile/:userId" element={<ProfilePage />} />
</Route>
```

**RequireAuth stays unchanged** (App.jsx lines 7-13 — do not touch):
```jsx
function RequireAuth({ children }) {
  const token = localStorage.getItem('fademy_token')
  if (!token) {
    return <Navigate to="/login" replace />
  }
  return children
}
```

---

### `package.json` (modify — add test files)

**Current scripts.test** (package.json line 11):
```json
"test": "node --test src/seed/seed.test.js src/middlewares/auth.middleware.test.js src/middlewares/role.middleware.test.js src/controllers/invite.controller.test.js src/controllers/media.controller.test.js src/controllers/registration.controller.test.js"
```

**Add three new test files** (append to end of the space-delimited list):
```
src/controllers/user.controller.test.js src/controllers/follow.controller.test.js src/controllers/block.controller.test.js
```

---

## Shared Patterns

### Authentication
**Source:** `src/middlewares/auth.middleware.js` (lines 12-32)
**Apply to:** All route files for Phase 3 (user.routes.js, follow.routes.js)

```javascript
// Import:
const authenticate = require('../middlewares/auth.middleware');

// Usage in route:
router.get('/search', authenticate, searchUsers);

// What authenticate injects into req.user:
req.user = {
  id: decoded.sub,            // string
  role: decoded.role,         // string
  institutionId: decoded.institutionId,  // string
}
```

### Multi-tenant scoping (PLAT-01)
**Source:** `src/controllers/admin.controller.js` (lines 12-14) and `src/controllers/invite.controller.js` (lines 22-23)
**Apply to:** user.controller.js searchUsers, getProfile; all controllers that query User

```javascript
// Always from JWT — never from req.body or req.params:
const { institutionId } = req.user;

// Query pattern:
await User.find({ institutionId, ... }).lean();
```

### Error handling
**Source:** `src/controllers/invite.controller.js` (lines 65-67) and `src/controllers/admin.controller.js` (lines 28-30)
**Apply to:** All controller functions

```javascript
// In every async controller — outer try/catch:
} catch (err) {
  return res.status(500).json({ error: 'Internal server error' });
}

// Duplicate key (MongoDB error code 11000):
if (err.code === 11000) {
  return res.status(409).json({ error: '...' });
}
throw err;  // re-throw inside inner try, caught by outer

// Not found:
if (!resource) return res.status(404).json({ error: 'User not found' });
```

### CSS Module co-location
**Source:** Every frontend component (AuthShell.jsx + AuthShell.module.css, PrimaryButton.jsx + PrimaryButton.module.css)
**Apply to:** All new frontend files

```jsx
// Each .jsx file imports its own .module.css sibling:
import styles from './ComponentName.module.css'

// Usage:
<div className={styles.wrapper}>
```

### Design tokens
**Source:** `MINIREDESOCIAL_FRONT/src/styles/tokens.css` (lines 1-40)
**Apply to:** All new CSS Module files

Key tokens for Phase 3 components:
- `--color-dominant: #083C5D` — dark blue (AppShell background, bottom nav bg)
- `--color-accent: #8C6E4F` — tobacco brown (active tab, FollowButton primary)
- `--color-surface: #D6CFC1` — beige (text on dark backgrounds)
- `--color-destructive: #FD8080` — block/danger actions
- `--touch-min: 44px` — minimum height for all tappable elements
- `--radius-button: 8px` — button border-radius

### api.js axios instance
**Source:** `MINIREDESOCIAL_FRONT/src/lib/api.js` (lines 1-15)
**Apply to:** All frontend pages and components that call the API

```jsx
import api from '../lib/api'

// All API calls go through this instance — JWT header added automatically:
const res = await api.get('/api/users/search?q=...')
const res = await api.post(`/api/users/${id}/follow`)
const res = await api.delete(`/api/users/${id}/follow`)
```

---

## No Analog Found

| File | Role | Data Flow | Reason |
|------|------|-----------|--------|
| `MINIREDESOCIAL_FRONT/src/components/UserResultCard/UserResultCard.jsx` | component | request-response | No list-item / card components exist yet. Use PrimaryButton.jsx prop conventions and tokens.css for styling. Minimal: avatar img + name + role badge + compact FollowButton. |

---

## Metadata

**Analog search scope:** `src/` (backend), `/home/maceloalexandre/Documentos/projetos/MINIREDESOCIAL_FRONT/src/` (frontend)
**Files scanned:** 29 backend files, 27 frontend files
**Pattern extraction date:** 2026-06-03

**Critical path note:** The frontend directory is NOT inside the backend repo root. It lives at `/home/maceloalexandre/Documentos/projetos/MINIREDESOCIAL_FRONT/`. All CONTEXT.md references to `MINIREDESOCIAL_FRONT/src/...` resolve to that absolute path.
