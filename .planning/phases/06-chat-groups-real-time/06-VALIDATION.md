---
phase: 6
slug: chat-groups-real-time
status: draft
nyquist_compliant: false
wave_0_complete: false
created: 2026-06-08
---

# Phase 6 — Validation Strategy

> Per-phase validation contract for feedback sampling during execution.

---

## Test Infrastructure

| Property | Value |
|----------|-------|
| **Framework** | Node.js built-in `node:test` + `node:assert/strict`, with `mongodb-memory-server` for DB-backed controller tests |
| **Config file** | none — tests are listed explicitly in `package.json`'s `"test"` script |
| **Quick run command** | `node --test src/controllers/conversation.controller.test.js` (or `message.controller.test.js`) |
| **Full suite command** | `npm test` (append new `*.test.js` paths to the explicit list in `package.json`'s `"test"` script) |
| **Estimated runtime** | ~30 seconds |

---

## Sampling Rate

- **After every task commit:** Run `node --test src/controllers/<name>.controller.test.js` (the single new test file)
- **After every plan wave:** Run `npm test`
- **Before `/gsd:verify-work`:** Full suite must be green; FRNT-07 verified manually (no frontend test infra exists)
- **Max feedback latency:** ~30 seconds

---

## Per-Task Verification Map

| Task ID | Plan | Wave | Requirement | Threat Ref | Secure Behavior | Test Type | Automated Command | File Exists | Status |
|---------|------|------|-------------|------------|-----------------|-----------|-------------------|-------------|--------|
| 06-01-0X | 01 | 0 | CHAT-01 | T-04-05 / SOCL-05 | Conversation create/open gated on mutual-follow + block check; non-mutual/blocked → 403 | controller (integration, mongodb-memory-server) | `node --test src/controllers/conversation.controller.test.js` | ❌ W0 | ⬜ pending |
| 06-0X-0X | TBD | TBD | CHAT-01 | T-02-01 | Socket handshake accepts valid JWT (HS256-pinned `verifyToken`), rejects invalid/missing token | unit/integration (socket.io-client vs in-process server) | `node --test src/services/socket.service.test.js` | ❌ W0 | ⬜ pending |
| 06-0X-0X | TBD | TBD | CHAT-01 | — | Sent message delivered live to recipient's room via `message:new` (two `socket.io-client` instances) | integration | `node --test src/services/socket.service.test.js` | ❌ W0 | ⬜ pending |
| 06-0X-0X | TBD | TBD | CHAT-02 | T-02-08b | Message persists `text` and/or `mediaUrls`; image upload mirrors `uploadPostImage` S3 flow (allowlist + size cap) | controller (integration, mongodb-memory-server + mocked S3) | `node --test src/controllers/message.controller.test.js` | ❌ W0 | ⬜ pending |
| 06-0X-0X | TBD | TBD | CHAT-02 | NOTF-02 | `message_received` notification dispatched on message creation (spy on `notification.service`) | controller (integration, call-counting spy) | `node --test src/controllers/message.controller.test.js` | ❌ W0 | ⬜ pending |
| 06-0X-0X | TBD | TBD | FRNT-07 | — | Conversations list renders avatar+name+preview+timestamp+unread dot; "new message" picker shows mutual followers only | manual / component-level | manual verification (`human_verify_mode: end-of-phase`) | ❌ no frontend test infra | ⬜ pending |

*Plan/Wave/Task IDs are TBD until the planner assigns concrete plan numbers — the planner MUST update this map with real IDs when plans are written.*

---

## Wave 0 Requirements

- [ ] `src/controllers/conversation.controller.test.js` — covers CHAT-01 (mutual-follow gate, block gate, find-or-create, list)
- [ ] `src/controllers/message.controller.test.js` — covers CHAT-02 (text+media persistence, S3 mock, notification dispatch spy)
- [ ] `src/services/socket.service.test.js` — covers CHAT-01 real-time delivery (handshake auth accept/reject, room join, cross-socket emit) — **new test pattern**: spins up an in-process `http.Server` + `socket.io` server + `socket.io-client` connections; no precedent in repo, budget extra time
- [ ] Append all three new test file paths to the `"test"` script string in `package.json` (explicit space-separated list, not a glob)
- [ ] Framework install: `npm install socket.io@^4.8.3` (backend) and `npm install socket.io-client@^4.8.3` (in `MINIREDESOCIAL_FRONT/`)

---

## Manual-Only Verifications

| Behavior | Requirement | Why Manual | Test Instructions |
|----------|-------------|------------|-------------------|
| Conversations list UI (avatar/name/preview/timestamp/unread dot) and "new message" picker (mutual followers only) | FRNT-07 | No frontend test framework exists anywhere under `MINIREDESOCIAL_FRONT/` (no `*.test.jsx`/`vitest.config.*`/`jest.config.*`); consistent with how FRNT-04/05/06 were handled in prior phases | Open the chat tab on mobile viewport, confirm list renders all required fields and unread state; open "new message" picker, confirm only mutual-follow users appear |
| Real-time message delivery on the actual mobile UI (not just the socket-service test harness) | CHAT-01 | Socket connection lifecycle (auth handshake, reconnection, optimistic send) is best confirmed against the live React client, not just an isolated test harness | Open two browser sessions as mutually-following users, send a message from one, confirm it appears live in the other without refresh |

---

## Validation Sign-Off

- [ ] All tasks have `<automated>` verify or Wave 0 dependencies
- [ ] Sampling continuity: no 3 consecutive tasks without automated verify
- [ ] Wave 0 covers all MISSING references
- [ ] No watch-mode flags
- [ ] Feedback latency < 30s
- [ ] `nyquist_compliant: true` set in frontmatter

**Approval:** pending
