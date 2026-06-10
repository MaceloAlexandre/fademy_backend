---
quick_id: 260610-dv1
status: complete
---

# Summary: Fix desktop full-width fixed-position elements

## What changed

In `MINIREDESOCIAL_FRONT` (sibling repo), replaced `left: 0; right: 0;` with `left: 50%; transform: translateX(-50%); width: 100%; max-width: 480px;` (the same pattern already used by `AppShell.module.css` `.header`/`.bottomNav`) in 4 rules:

1. `src/pages/PostDetailPage/PostDetailPage.module.css` — `.submitErrorWrap`
2. `src/pages/PostDetailPage/PostDetailPage.module.css` — `.inputBar`
3. `src/components/ChatComposer/ChatComposer.module.css` — `.inputBar`
4. `src/pages/ConversationThreadPage/ConversationThreadPage.module.css` — `.submitErrorWrap`

CSS-only change. No JS/logic touched.

## Why

User reported the comment-input bar on PostDetailPage spanning the full desktop browser width while the rest of the app stays in the 480px mobile-simulated column. Same root-cause pattern existed in 3 other fixed-position bars/banners (chat composer + both error banners), fixed for consistency per user's choice.

## Applied directly (no executor subagent)

Session was at ~70% context when this task was initialized; user opted to apply the fix directly (lightweight) instead of spawning planner/executor subagents with cross-repo worktree isolation.

## Verification

Not yet visually verified in browser (desktop). User should reload the frontend and check:
- Open a post → comment input bar at bottom is now ~480px wide and centered, not full-width
- Open a chat thread → composer bar at bottom is now ~480px wide and centered
