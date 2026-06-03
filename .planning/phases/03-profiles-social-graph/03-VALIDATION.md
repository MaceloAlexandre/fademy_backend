---
phase: 3
slug: profiles-social-graph
status: draft
nyquist_compliant: false
wave_0_complete: false
created: 2026-06-03
---

# Phase 3 — Validation Strategy

> Per-phase validation contract for feedback sampling during execution.

---

## Test Infrastructure

| Property | Value |
|----------|-------|
| **Framework** | node:test (built-in Node.js 18+) + mongodb-memory-server |
| **Config file** | none — test files listed explicitly in package.json scripts.test |
| **Quick run command** | `node --test src/controllers/follow.controller.test.js` |
| **Full suite command** | `npm test` |
| **Estimated runtime** | ~15 seconds |

---

## Sampling Rate

- **After every task commit:** Run `node --test src/controllers/<controller>.test.js` (whichever controller was changed)
- **After every plan wave:** Run `npm test`
- **Before `/gsd:verify-work`:** Full suite must be green
- **Max feedback latency:** ~15 seconds

---

## Per-Task Verification Map

| Task ID | Plan | Wave | Requirement | Threat Ref | Secure Behavior | Test Type | Automated Command | File Exists | Status |
|---------|------|------|-------------|------------|-----------------|-----------|-------------------|-------------|--------|
| 3-01-01 | 01 | 1 | PROF-01 | — | PATCH /api/users/me toggles isPrivate | unit | `node --test src/controllers/user.controller.test.js` | ❌ W0 | ⬜ pending |
| 3-01-02 | 01 | 1 | PROF-02 | — | GET /api/users/:id returns reduced data for private profile non-follower | unit | `node --test src/controllers/user.controller.test.js` | ❌ W0 | ⬜ pending |
| 3-01-03 | 01 | 1 | PROF-03 | ReDoS | GET /api/users/search excludes blocked users; regex input escaped | unit | `node --test src/controllers/user.controller.test.js` | ❌ W0 | ⬜ pending |
| 3-01-04 | 01 | 1 | SOCL-01 | — | follow() creates Follow{status:accepted} for public profile | unit | `node --test src/controllers/follow.controller.test.js` | ❌ W0 | ⬜ pending |
| 3-01-05 | 01 | 1 | SOCL-02 | — | follow() creates Follow{status:pending} for private profile | unit | `node --test src/controllers/follow.controller.test.js` | ❌ W0 | ⬜ pending |
| 3-01-06 | 01 | 1 | SOCL-03 | IDOR | acceptFollow() returns 403 for non-owner; rejectFollow() same | unit | `node --test src/controllers/follow.controller.test.js` | ❌ W0 | ⬜ pending |
| 3-01-07 | 01 | 1 | SOCL-04 | — | block() removes both Follow docs and creates Block | unit | `node --test src/controllers/block.controller.test.js` | ❌ W0 | ⬜ pending |
| 3-01-08 | 01 | 1 | SOCL-05 | — | search excludes users who blocked viewer or whom viewer blocked | unit | `node --test src/controllers/user.controller.test.js` | ❌ W0 | ⬜ pending |
| 3-04-01 | 04 | 2 | FRNT-05 | — | AppShell renders bottom nav; FollowButton shows correct state | manual smoke | open browser, navigate tabs | — | ⬜ pending |

*Status: ⬜ pending · ✅ green · ❌ red · ⚠️ flaky*

---

## Wave 0 Requirements

- [ ] `src/controllers/user.controller.test.js` — stubs for PROF-01, PROF-02, PROF-03, SOCL-05
- [ ] `src/controllers/follow.controller.test.js` — stubs for SOCL-01, SOCL-02, SOCL-03
- [ ] `src/controllers/block.controller.test.js` — stubs for SOCL-04
- [ ] Update `package.json` `scripts.test` to include the three new test files

---

## Manual-Only Verifications

| Behavior | Requirement | Why Manual | Test Instructions |
|----------|-------------|------------|-------------------|
| AppShell bottom nav renders; tab switching works; scroll-aware header hides/shows | FRNT-05 | CSS/React interaction — not covered by node:test unit tests | Open app on mobile viewport, navigate tabs, scroll in profile/search screen |
| FollowButton state machine cycles correctly (null → pending → cancel → null; null → accepted → unfollow) | FRNT-05 | E2E user interaction required | Log in as two users, test follow flow, block flow |

---

## Validation Sign-Off

- [ ] All tasks have `<automated>` verify or Wave 0 dependencies
- [ ] Sampling continuity: no 3 consecutive tasks without automated verify
- [ ] Wave 0 covers all MISSING references
- [ ] No watch-mode flags
- [ ] Feedback latency < 30s
- [ ] `nyquist_compliant: true` set in frontmatter

**Approval:** pending
