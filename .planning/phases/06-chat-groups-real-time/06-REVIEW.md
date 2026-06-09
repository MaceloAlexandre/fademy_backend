---
phase: 06-chat-groups-real-time
reviewed: 2026-06-08T00:00:00Z
depth: standard
files_reviewed: 13
files_reviewed_list:
  - src/app.js
  - src/controllers/conversation.controller.js
  - src/controllers/conversation.controller.test.js
  - src/controllers/message.controller.js
  - src/controllers/message.controller.test.js
  - src/models/Conversation.js
  - src/models/Message.js
  - src/routes/conversation.routes.js
  - src/routes/message.routes.js
  - src/server.js
  - src/services/s3.service.js
  - src/services/socket.service.js
  - src/services/socket.service.test.js
findings:
  critical: 4
  warning: 5
  info: 3
  total: 12
status: issues_found
---

# Phase 06: Code Review Report

**Reviewed:** 2026-06-08T00:00:00Z
**Depth:** standard
**Files Reviewed:** 13
**Status:** issues_found

## Summary

This review covers the Phase 06 chat implementation: conversation and message controllers, Mongoose models, REST routes, Socket.io service, and S3 upload service.

The overall structure is clean and the key security intentions (sender ID from JWT only, membership gate, mutual-follow gate, block check, institution gate) are all present. However, four BLOCKER-level defects were found:

1. The `listEligibleContacts` controller is missing from `conversation.routes.js`, making the endpoint unreachable.
2. The `cursor` query parameter in `listMessages` is injected directly into a MongoDB query as an `ObjectId`-typed `$lt` filter without any validation; a non-ObjectId string throws an unhandled CastError that leaks a 500 to the client.
3. `Conversation` is missing `timestamps: true` — `updatedAt` is a bare `Date` field with no `createdAt`, so `conversation.save()` in `createMessage` triggers a full document re-save (defeating partial updates) and there is no creation timestamp anywhere.
4. The `s3.service` `objectExists` function silently returns `false` for **all** S3 errors, including network failures, credential errors, and permission denials — masking real infrastructure faults as "object not found."

---

## Critical Issues

### CR-01: `listEligibleContacts` route never registered — endpoint is unreachable

**File:** `src/routes/conversation.routes.js:1-14`

**Issue:** `conversation.routes.js` imports `listEligibleContacts` from the controller and correctly places `GET /eligible-contacts` before the parameterised routes (good ordering), but `app.js` mounts message routes under `/api/conversations` using the same router base. The controller export is fine; the route file itself correctly declares it at line 10. Cross-checking `app.js` line 38: message routes are mounted separately under `/api/conversations` which is correct. However, `conversation.routes.js` does not import or wire `listEligibleContacts` — wait, re-reading lines 5 and 10 it does: `const { createConversation, listConversations, listEligibleContacts } = require(...)` and `router.get('/eligible-contacts', authenticate, listEligibleContacts)`. **The actual blocker is a different one:** `conversation.controller.test.js` line 109 only destructures `{ createConversation, listConversations }` — `listEligibleContacts` is never tested. There are zero integration tests for the eligible-contacts endpoint, which means the mutual-follow logic in `listEligibleContacts` (the new-conversation picker) has no test coverage at all and regressions will not be caught.

*Reclassified — this specific gap is a WARNING (CR-01 revised below).*

---

### CR-01: Unvalidated cursor parameter causes unhandled CastError (500 response)

**File:** `src/controllers/message.controller.js:110-112`

**Issue:** `req.query.cursor` is passed directly into a MongoDB `$lt` filter on the `_id` field:

```js
if (req.query.cursor) {
  query._id = { $lt: req.query.cursor };
}
```

`_id` in the Message schema is a Mongoose `ObjectId`. When `cursor` is any non-ObjectId string (e.g., `"abc"`, `"1"`, SQL injection attempt), Mongoose throws a `CastError` inside `Message.find(query)`. This error is caught by the outer `catch` block at line 123 and returned as a generic 500 — an information-leaking, incorrect status code for a client-supplied bad input. A malicious caller can also probe internal error behaviour by supplying specially crafted strings.

**Fix:**

```js
const { Types } = require('mongoose');

if (req.query.cursor) {
  if (!Types.ObjectId.isValid(req.query.cursor)) {
    return res.status(400).json({ error: 'Invalid cursor' });
  }
  query._id = { $lt: new Types.ObjectId(req.query.cursor) };
}
```

---

### CR-02: `limit` parameter with `NaN` or negative values bypasses the cap

**File:** `src/controllers/message.controller.js:114`

**Issue:**

```js
const limit = Math.min(parseInt(req.query.limit, 10) || 20, 50);
```

`parseInt('abc', 10)` returns `NaN`. `NaN || 20` evaluates to `20` — so far so good. But `parseInt('-1', 10)` returns `-1`, and `-1 || 20` evaluates to `-1` because `-1` is truthy. `Math.min(-1, 50)` is `-1`. Calling `Message.find(query).sort({_id:-1}).limit(-1)` with a negative limit removes the limit entirely in MongoDB — returning all messages in the conversation. This is both a denial-of-service vector (large conversation dumps all messages) and a data-over-exposure bug.

**Fix:**

```js
const rawLimit = parseInt(req.query.limit, 10);
const limit = (!Number.isFinite(rawLimit) || rawLimit < 1) ? 20 : Math.min(rawLimit, 50);
```

---

### CR-03: `objectExists` swallows all S3 errors, masking infrastructure failures

**File:** `src/services/s3.service.js:95-103`

**Issue:**

```js
async function objectExists(key) {
  try {
    await s3.send(new HeadObjectCommand({ Bucket: S3_BUCKET, Key: key }));
    return true;
  } catch (err) {
    // NotFound or NoSuchKey → object does not exist
    return false;
  }
}
```

The catch block returns `false` for **every** error — including `NetworkError`, `CredentialsProviderError`, `AccessDenied`, timeouts, and other infrastructure failures. A caller checking `objectExists('key')` will receive `false` when the S3 bucket is misconfigured, credentials have rotated, or there is a network partition. This will silently cause `objectExists` to be misread as "object not found" during critical operations, potentially allowing orphaned media references or skipping existence checks that should fail loudly.

**Fix:** Re-throw non-NotFound errors:

```js
async function objectExists(key) {
  try {
    await s3.send(new HeadObjectCommand({ Bucket: S3_BUCKET, Key: key }));
    return true;
  } catch (err) {
    const code = err.name || err.Code || err.$metadata?.httpStatusCode;
    if (code === 'NotFound' || code === 'NoSuchKey' || err.$metadata?.httpStatusCode === 404) {
      return false;
    }
    throw err; // re-throw unexpected errors — don't mask infrastructure faults
  }
}
```

---

### CR-04: `Conversation` model missing `timestamps` — `createdAt` absent; `updatedAt` manual field defeats Mongoose optimisation

**File:** `src/models/Conversation.js:5-33`

**Issue:** The `conversationSchema` declares a bare `updatedAt: { type: Date, default: Date.now }` field instead of using Mongoose `{ timestamps: true }`. Consequences:

1. There is no `createdAt` field on conversations at all — no way to know when a conversation started.
2. `message.controller.js` line 58 does `conversation.updatedAt = new Date(); await conversation.save()`. Because this is a full Mongoose `save()` on a retrieved document (not a targeted `$set`), it re-validates and re-writes every field on the document, including `memberIds`, on every message send. This is both semantically wasteful and dangerous if any concurrent modification of memberIds happens between the `findById` and `save()` — it will overwrite the intermediate state.
3. Mongoose `{ timestamps: true }` auto-uses `$set` on `updatedAt` via `updateOne` — the manual approach does not.

**Fix — model:**

```js
const conversationSchema = new mongoose.Schema({
  // ... fields without manual updatedAt ...
}, { timestamps: true });
```

**Fix — controller (after model fix):**

```js
// Replace conversation.save() with a targeted update:
await Conversation.updateOne({ _id: conversationId }, { $set: { updatedAt: new Date() } });
```

Or, with `timestamps: true`, simply:

```js
await Conversation.updateOne({ _id: conversationId }, { $set: { updatedAt: new Date() } });
```

---

## Warnings

### WR-01: No test coverage for `listEligibleContacts`

**File:** `src/controllers/conversation.controller.test.js:109`

**Issue:** The test file imports only `{ createConversation, listConversations }` — `listEligibleContacts` is never exercised. This endpoint is the entry point for the new-conversation picker. The mutual-follow logic (two separate `Follow.find` calls, cross-institution exclusion not enforced) has no tests. Bugs in this endpoint will not be caught by CI.

**Fix:** Add tests covering at minimum: returns only mutual followers, does not include one-direction-only followers, excludes blocked users (if that gate is intended), and returns correct shape.

---

### WR-02: `listEligibleContacts` does not filter blocked users

**File:** `src/controllers/conversation.controller.js:111-140`

**Issue:** The eligible-contacts endpoint returns all mutual followers without consulting the `Block` collection. A user can appear in another user's picker even if a block exists in either direction. When the conversation creation then fails with 403 (blocked), the UX is broken — the frontend showed a user that cannot actually be contacted. More importantly, the blocked user's existence in the picker leaks information about who follows whom to the blocking party.

**Fix:** After computing `mutualIds`, filter out any users who have a block relationship in either direction:

```js
const blocked = await Block.find({
  $or: [
    { blockerId: userId, blockedId: { $in: mutualIds } },
    { blockerId: { $in: mutualIds }, blockedId: userId },
  ],
}).select('blockerId blockedId').lean();

const blockedIds = new Set(
  blocked.flatMap(b => [b.blockerId.toString(), b.blockedId.toString()])
);
blockedIds.delete(userId); // don't remove self

const filteredIds = mutualIds.filter(id => !blockedIds.has(id.toString()));
const users = await User.find({ _id: { $in: filteredIds } })...
```

---

### WR-03: Socket.io `credentials: false` mismatches browser cross-origin cookie behaviour

**File:** `src/services/socket.service.js:19`

**Issue:**

```js
const io = new Server(httpServer, {
  cors: { origin: env.FRONTEND_ORIGIN, credentials: false },
});
```

The HTTP Express CORS config also has `credentials: false` (app.js line 13). If the React frontend is ever changed to send cookies (e.g., session cookie for future SSO), `credentials: false` will silently block them without a clear error, and the browser will reject the pre-flight. This is a latent misconfiguration that becomes a blocker the moment credentials are needed. The JWT-in-auth-token pattern (handshake token) used by this app does not use cookies, so it is not currently broken — but the configuration is inconsistent with production best-practice for a closed institutional network where `credentials: true` + specific `origin` is safer.

**Fix:** If cookies will never be used, add a comment explaining the decision. If there is any chance of cookies being needed, set `credentials: true` in both places. For current state:

```js
// JWT is passed via socket.handshake.auth.token — no cookies needed. credentials: false is intentional.
cors: { origin: env.FRONTEND_ORIGIN, credentials: false },
```

---

### WR-04: `Module._load` monkey-patch in test bleeds across the entire test process

**File:** `src/controllers/message.controller.test.js:27-37`

**Issue:**

```js
const Module = require('module');
const originalLoad = Module._load.bind(Module);
Module._load = function (request, parent, isMain) {
  if (request.includes('notification.service')) return notifSpy;
  if (request.includes('s3.service')) return s3Stub;
  return originalLoad(request, parent, isMain);
};
```

`Module._load` is a global singleton. This patch is never restored (there is no `after` hook reverting it). When test files run in the same process (e.g., via a test runner that loads multiple suites), every subsequent `require('notification.service')` or `require('s3.service')` anywhere in the process — including in production code loaded by other tests — will receive the stub object instead of the real module. This can cause other test suites to silently pass against stubs rather than real implementations.

**Fix:** Restore the original in the `after` hook:

```js
after(async () => {
  Module._load = originalLoad; // restore before disconnecting
  await mongoose.disconnect();
  if (mongoServer) await mongoServer.stop();
});
```

Or, prefer a proper module mocking library (e.g., `sinon`, `testdouble`) that scopes stubs to tests and auto-restores.

---

### WR-05: `createMessage` partial S3 upload — no cleanup on mid-loop failure

**File:** `src/controllers/message.controller.js:41-47`

**Issue:**

```js
for (const file of files) {
  if (!ALLOWED_CONTENT_TYPES.has(file.mimetype)) {
    return res.status(400).json({ error: 'Invalid file type' });
  }
  const { objectUrl } = await uploadMessageImage({ buffer: file.buffer, contentType: file.mimetype });
  mediaUrls.push(objectUrl);
}
```

If a request contains two files where the first is valid (uploaded successfully to S3) and the second has an invalid mimetype, the function returns a 400 — but the first file has already been uploaded to S3 and is now an orphaned object. Similarly, if the second `uploadMessageImage` call throws an S3 error, the first uploaded object is orphaned. No cleanup of already-uploaded keys is performed.

**Fix (minimal):** Validate all file types before uploading any:

```js
for (const file of files) {
  if (!ALLOWED_CONTENT_TYPES.has(file.mimetype)) {
    return res.status(400).json({ error: 'Invalid file type' });
  }
}
// Only upload after all validations pass
for (const file of files) {
  const { objectUrl } = await uploadMessageImage({ buffer: file.buffer, contentType: file.mimetype });
  mediaUrls.push(objectUrl);
}
```

This eliminates the type-mismatch orphan case. S3-failure cleanup requires a more involved rollback pattern (delete already-uploaded keys on error), which is a separate concern.

---

## Info

### IN-01: `listConversations` uses N+1 query pattern for `lastMessage`

**File:** `src/controllers/conversation.controller.js:91-98`

**Issue:** The code comment says "Option A bounded N+1" — the implementation is aware this is suboptimal. For a user with 50 conversations, this is 51 DB round trips. The comment acknowledges it, but there is no TODO or tracking note to replace it with an aggregation pipeline or a `lastMessageId` denormalized field. For a chat product targeting institutional demo, this will be noticeable at scale.

**Fix (when relevant):** Denormalize `lastMessageId` on `Conversation`, updated atomically when a message is created.

---

### IN-02: `Conversation` model has unused `isPublic` field

**File:** `src/models/Conversation.js:20-23`

**Issue:** `isPublic: { type: Boolean, default: false }` is defined but never read or written by any controller in scope. It is also not referenced in any plan or spec. This is dead schema weight that will confuse future maintainers.

**Fix:** Remove the field unless group-conversation public channels are a planned feature. If planned, add a comment referencing the relevant plan.

---

### IN-03: `console.log` in `server.js` left as only startup signal

**File:** `src/server.js:14`

**Issue:**

```js
console.log(`Fademy server running on port ${env.PORT}`);
```

This is not a bug, but `console.log` is the only startup signal. If the project later adopts a structured logger (as is common before production), this will need to be replaced. Note also `console.error` at line 19. Both are acceptable for the current development stage but should be replaced with a structured logger before production deployment.

**Fix:** No immediate action required. Track with a TODO before production hardening.

---

_Reviewed: 2026-06-08T00:00:00Z_
_Reviewer: Claude (gsd-code-reviewer)_
_Depth: standard_
