---
phase: 6
slug: chat-groups-real-time
status: draft
nyquist_compliant: true
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
| 06-01-T2 | 01 | 1 | CHAT-01 | T-06-01 / T-06-02 / SOCL-05 | Conversation create/open gated on mutual-follow + block check; non-mutual/blocked/self → 403/403/400 | controller (integration, mongodb-memory-server) | `node --test src/controllers/conversation.controller.test.js` | ❌→W0 (created in 06-01 T2) | ⬜ pending |
| 06-01-T3 | 01 | 1 | CHAT-01 | T-06-03 | Socket handshake accepts valid JWT (HS256-pinned `verifyToken`), rejects invalid/missing token | unit/integration (socket.io-client vs in-process server) | `node --test src/services/socket.service.test.js` | ❌→W0 (created in 06-01 T3) | ⬜ pending |
| 06-01-T3 | 01 | 1 | CHAT-01 | T-06-04 | Sent message delivered live to recipient's room via `message:new` (two `socket.io-client` instances) | integration | `node --test src/services/socket.service.test.js` | ❌→W0 (created in 06-01 T3) | ⬜ pending |
| 06-02-T2 | 02 | 2 | CHAT-02 | T-06-08 | Message persists `text` and/or `mediaUrls`; image upload mirrors `uploadPostImage` S3 flow (allowlist + size cap) | controller (integration, mongodb-memory-server + mocked S3) | `node --test src/controllers/message.controller.test.js` | ❌→W0 (created in 06-02 T2) | ⬜ pending |
| 06-02-T2 | 02 | 2 | CHAT-02 | NOTF-02 | `message_received` notification dispatched on message creation (spy on `notification.service`) | controller (integration, call-counting spy) | `node --test src/controllers/message.controller.test.js` | ❌→W0 (created in 06-02 T2) | ⬜ pending |
| 06-03-* / 06-04-* | 03, 04 | 3, 4 | FRNT-07 | — | Conversations list renders avatar+name+preview+timestamp+unread dot; "new message" picker shows mutual followers only; thread live send/receive | manual / component-level | manual verification (`human_verify_mode: end-of-phase`) | ❌ no frontend test infra | ⬜ pending |

*Wave 0 test files are created inside the Wave 1/2 plans (06-01 Tasks 2-3, 06-02 Task 2) — this repo's convention is to author the failing-then-passing test alongside the controller/service it covers (matching the established `*.controller.test.js` per-controller pattern), and register each in `package.json`'s explicit test list within the same plan. There is no separate standalone Wave 0 plan because the test harness (mongodb-memory-server + makeRes + Module._load spy) is copied per-file from existing analogs.*

---

## Wave 0 Requirements

- [ ] `src/controllers/conversation.controller.test.js` — CHAT-01 (mutual-follow gate, block gate, find-or-create, list) — authored in 06-01 Task 2
- [ ] `src/controllers/message.controller.test.js` — CHAT-02 (text+media persistence, S3 mock, notification dispatch spy) — authored in 06-02 Task 2
- [ ] `src/services/socket.service.test.js` — CHAT-01 real-time delivery (handshake auth accept/reject, room join, cross-socket emit) — **new test pattern**: spins up an in-process `http.Server` + `socket.io` server + `socket.io-client` connections; no precedent in repo — authored in 06-01 Task 3
- [ ] Append all three new test file paths to the `"test"` script string in `package.json` (06-01 appends conversation + socket; 06-02 appends message)
- [ ] Framework install: `npm install socket.io@^4.8.3` (06-01 Task 1, backend) and `npm install socket.io-client@^4.8.3` (06-03 Task 1, in `MINIREDESOCIAL_FRONT/`; backend devDependency for socket.service.test in 06-01 Task 3)

---

## Manual-Only Verifications

| Behavior | Requirement | Why Manual | Test Instructions |
|----------|-------------|------------|-------------------|
| Conversations list UI (avatar/name/preview/timestamp/unread dot) and "new message" picker (mutual followers only) | FRNT-07 | No frontend test framework exists anywhere under `MINIREDESOCIAL_FRONT/`; consistent with how FRNT-04/05/06 were handled | Open the chat tab on mobile viewport, confirm list renders all required fields and unread state; open "new message" picker, confirm only mutual-follow users appear |
| Real-time message delivery on the actual mobile UI | CHAT-01 / CHAT-02 | Socket lifecycle (handshake, reconnection, optimistic send, image render) is best confirmed against the live React client | Open two browser sessions as mutually-following users, send a text + an image from one, confirm both appear live in the other without refresh |

---

## Validation Sign-Off

- [x] All tasks have `<automated>` verify or Wave 0 dependencies (backend automated; frontend manual per no-test-infra policy)
- [x] Sampling continuity: no 3 consecutive tasks without automated verify (backend tasks each have a test; frontend tasks are manual-verify by documented policy)
- [x] Wave 0 covers all MISSING references (authored within Wave 1/2 plans alongside their controllers/service)
- [x] No watch-mode flags
- [x] Feedback latency < 30s
- [x] `nyquist_compliant: true` set in frontmatter

**Approval:** approved by planner (IDs assigned)
