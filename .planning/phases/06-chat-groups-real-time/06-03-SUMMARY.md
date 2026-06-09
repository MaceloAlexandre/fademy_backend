---
phase: 06-chat-groups-real-time
plan: 03
subsystem: frontend-chat-entry
tags: [frontend, chat, socket.io-client, react, css-modules]
dependency_graph:
  requires: [06-01, 06-02]
  provides: [socket.io-client singleton, useChatSocket hook, ConversationListItem, ConversationsListPage, NewConversationPage, AppShell chat icon, chat routes]
  affects: [MINIREDESOCIAL_FRONT/src/App.jsx, MINIREDESOCIAL_FRONT/src/components/AppShell/AppShell.jsx, src/controllers/conversation.controller.js, src/routes/conversation.routes.js]
tech_stack:
  added: [socket.io-client@^4.8.3 (frontend dependency)]
  patterns: [socket singleton with autoConnect:false, JWT auth:{token} object form, Notification-derived unread state, CSS Modules co-location, stub-then-replace pattern]
key_files:
  created:
    - MINIREDESOCIAL_FRONT/src/lib/socket.js
    - MINIREDESOCIAL_FRONT/src/hooks/useChatSocket.js
    - MINIREDESOCIAL_FRONT/src/components/ConversationListItem/ConversationListItem.jsx
    - MINIREDESOCIAL_FRONT/src/components/ConversationListItem/ConversationListItem.module.css
    - MINIREDESOCIAL_FRONT/src/pages/ConversationsListPage/ConversationsListPage.jsx
    - MINIREDESOCIAL_FRONT/src/pages/ConversationsListPage/ConversationsListPage.module.css
    - MINIREDESOCIAL_FRONT/src/pages/NewConversationPage/NewConversationPage.jsx
    - MINIREDESOCIAL_FRONT/src/pages/NewConversationPage/NewConversationPage.module.css
    - MINIREDESOCIAL_FRONT/src/pages/ConversationThreadPage/ConversationThreadPage.jsx (stub)
  modified:
    - MINIREDESOCIAL_FRONT/src/App.jsx (add /chat, /chat/new, /chat/:conversationId routes)
    - MINIREDESOCIAL_FRONT/src/components/AppShell/AppShell.jsx (add chat icon + badge)
    - MINIREDESOCIAL_FRONT/src/components/AppShell/AppShell.module.css (add .chatBtn)
    - MINIREDESOCIAL_FRONT/src/components/NotificationCard/NotificationCard.jsx (message_received branch)
    - src/controllers/conversation.controller.js (add listEligibleContacts)
    - src/routes/conversation.routes.js (add GET /eligible-contacts route)
    - MINIREDESOCIAL_FRONT/package.json (socket.io-client dependency)
decisions:
  - "socket singleton uses auth:{token} object form (Pitfall 4) — re-sent on reconnect, no token refresh needed"
  - "useChatSocket disconnects on unmount — acceptable since only chat pages use it this phase"
  - "Unread state derived from message_received notifications (read:false, refId=conversationId) — zero schema changes, locked contract"
  - "listEligibleContacts added to conversation.controller.js (Rule 2 deviation) — no existing mutual-follower endpoint, required for picker basic operation"
  - "NotificationCard message_received branch surgical fix — now-active type must navigate to /chat/:refId not actor profile (UI-SPEC cross-phase requirement)"
  - "ConversationThreadPage stub created (null export) for App.jsx import — replaced atomically by Plan 04"
metrics:
  duration: "~15 min"
  completed: "2026-06-08"
  tasks_completed: 3
  files_created: 9
  files_modified: 7
  tests_added: 0
---

# Phase 6 Plan 3: Frontend Chat Entry — Socket Client + Conversations List + New-Message Picker Summary

**One-liner:** Socket.io-client singleton with JWT auth, AppShell chat icon (Notification-derived unread badge), ConversationListItem D-05 row, /chat list page (unread dots from message_received notifications), and /chat/new mutual-follower picker — the Instagram-DM entry flow fully wired to the Plans 01-02 backend.

## Tasks Completed

| Task | Name | Commit (frontend) | Commit (backend) | Files |
|------|------|-------------------|------------------|-------|
| 1 | socket.io-client install, socket singleton, useChatSocket hook | e7df20a | — | socket.js, useChatSocket.js, package.json |
| 2 | ConversationListItem + AppShell chat icon + App.jsx routes | 582e20a | — | ConversationListItem.jsx/.css, AppShell.jsx/.css, App.jsx, stubs |
| 3 | ConversationsListPage + NewConversationPage (+ eligible-contacts backend) | 5949904 | d48caa2 | 5 frontend files, 2 backend files |

## Verification

- All Task 1-3 automated grep gates: PASS
- Backend test suite after eligible-contacts addition: 98/98 tests, 0 failures
- `socket.io-client` present in MINIREDESOCIAL_FRONT/package.json
- `auth:` object form in socket.js
- `message:new` in useChatSocket.js
- `navigate('/chat')` in AppShell.jsx
- `ConversationsListPage` in App.jsx
- `chat/new` route in App.jsx
- `message_received` filter in ConversationsListPage.jsx
- `api.post('/api/conversations'` in NewConversationPage.jsx

## Deviations from Plan

### Auto-fixed Issues

**1. [Rule 2 - Missing Critical Functionality] Added GET /api/conversations/eligible-contacts backend endpoint**
- **Found during:** Task 3 — no existing endpoint to list mutual followers for the picker
- **Issue:** Plan says "pragmatic approach: use existing endpoints" but no "list accepted following" endpoint exists; only searchUsers (requires a query string) is available — not usable as a contact picker
- **Fix:** Added `listEligibleContacts` function to `conversation.controller.js` + route in `conversation.routes.js`. Queries Follow both ways to build mutual set, returns `User.find()` projection. Declared before `/:id` param to avoid route-capture issue.
- **Files modified:** `src/controllers/conversation.controller.js`, `src/routes/conversation.routes.js`
- **Commit:** d48caa2

**2. [Rule 2 - Missing Critical Functionality] Fixed NotificationCard.jsx message_received branch**
- **Found during:** Task 3 review — UI-SPEC explicitly flags this as a cross-phase impact
- **Issue:** `message_received` was a dormant notification type in Phase 5; this phase activates it, making the existing fallback `navigate('/profile/${actorId._id}')` incorrect — users tapping a message notification would land on the sender's profile instead of the conversation thread
- **Fix:** Added `message_received: 'enviou uma mensagem para você'` to ACTION_TEXT map; added `type === 'message_received'` branch in `handleClick` → `navigate('/chat/${refId}')` (refId IS the conversationId per Plan 02 contract)
- **Files modified:** `MINIREDESOCIAL_FRONT/src/components/NotificationCard/NotificationCard.jsx`
- **Commit:** 5949904 (included in Task 3 commit)

## Known Stubs

- `ConversationThreadPage.jsx` exports a null placeholder. Plan 04 replaces it atomically. This is intentional — stub-then-replace pattern (mirrors 04-03 stub pattern, documented in decisions).

## Threat Flags

No new threat surface introduced beyond what the plan covers. The `listEligibleContacts` endpoint uses `req.user.id` exclusively (from JWT), never from request body. No cross-institution exposure: `Follow` entries are implicitly institution-scoped since users can only follow within their institution (Phase 3 social graph gate).

## Self-Check: PASSED

Files created:
- FOUND: MINIREDESOCIAL_FRONT/src/lib/socket.js
- FOUND: MINIREDESOCIAL_FRONT/src/hooks/useChatSocket.js
- FOUND: MINIREDESOCIAL_FRONT/src/components/ConversationListItem/ConversationListItem.jsx
- FOUND: MINIREDESOCIAL_FRONT/src/pages/ConversationsListPage/ConversationsListPage.jsx
- FOUND: MINIREDESOCIAL_FRONT/src/pages/NewConversationPage/NewConversationPage.jsx
- FOUND: MINIREDESOCIAL_FRONT/src/pages/ConversationThreadPage/ConversationThreadPage.jsx

Commits:
- FOUND: e7df20a (frontend — Task 1)
- FOUND: 582e20a (frontend — Task 2)
- FOUND: 5949904 (frontend — Task 3)
- FOUND: d48caa2 (backend — eligible-contacts endpoint)
