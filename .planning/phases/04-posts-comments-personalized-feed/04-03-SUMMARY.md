---
phase: 04-posts-comments-personalized-feed
plan: "03"
subsystem: frontend-feed
tags: [react, feed, infinite-scroll, pull-to-refresh, intersection-observer, post-card, appshell]
dependency_graph:
  requires:
    - phase: 04-01
      provides: GET /api/posts/feed cursor-paginated endpoint
    - phase: 04-02
      provides: comment backend (commentCount on posts)
    - phase: 03-profiles-social-graph
      provides: AppShell, CSS Modules pattern, lucide-react, axios interceptor
  provides:
    - Updated AppShell (+ tab to /compose, Bell icon in header, no /notifications nav tab)
    - PostCard component (avatar, text, image strip, link, category chips, comment count)
    - FeedPage with cursor-based infinite scroll (IntersectionObserver) and pull-to-refresh
    - App.jsx routes: / -> FeedPage, /posts/:postId -> PostDetailPage stub, /compose -> ComposePage stub
    - Stub PostDetailPage and ComposePage so dev server starts before Plan 04
  affects: [04-04-PLAN (PostDetailPage/ComposePage replace stubs)]

tech-stack:
  added: []
  patterns:
    - IntersectionObserver sentinel div for infinite scroll (no library)
    - Pull-to-refresh via touchstart/touchmove/touchend on list container
    - isDetailView prop pattern for reusing card component at detail-view top without tap-navigation
    - Cursor ref pattern (useRef mirrors useState cursor) to avoid stale closures in async fetchPage

key-files:
  created:
    - MINIREDESOCIAL_FRONT/src/components/PostCard/PostCard.jsx
    - MINIREDESOCIAL_FRONT/src/components/PostCard/PostCard.module.css
    - MINIREDESOCIAL_FRONT/src/pages/FeedPage/FeedPage.jsx
    - MINIREDESOCIAL_FRONT/src/pages/FeedPage/FeedPage.module.css
    - MINIREDESOCIAL_FRONT/src/pages/PostDetailPage/PostDetailPage.jsx
    - MINIREDESOCIAL_FRONT/src/pages/ComposePage/ComposePage.jsx
  modified:
    - MINIREDESOCIAL_FRONT/src/components/AppShell/AppShell.jsx
    - MINIREDESOCIAL_FRONT/src/components/AppShell/AppShell.module.css
    - MINIREDESOCIAL_FRONT/src/App.jsx

key-decisions:
  - "cursor ref pattern: useRef mirrors cursor state to give async fetchPage the current cursor without stale closure"
  - "TABS label null for + tab, uses nullish coalescing to render aria-label=Nova publicação"
  - "stub FeedPage (null export) created in Task 1 commit, replaced atomically in Task 3 commit"

requirements-completed: [FRNT-04, FEED-01, FEED-03]

duration: ~15min
completed: "2026-06-07"
---

# Phase 04 Plan 03: Feed Frontend Slice Summary

**AppShell updated (+ tab + Bell header), PostCard with all D-07 rows, and FeedPage with IntersectionObserver infinite scroll + pull-to-refresh against the real /api/posts/feed endpoint.**

## Performance

- **Duration:** ~15 min
- **Started:** 2026-06-07
- **Completed:** 2026-06-07
- **Tasks:** 3 of 4 automated tasks completed (Task 4 is a human-verify checkpoint — pending)
- **Files modified:** 9

## Accomplishments

- AppShell updated: nav tab 2 changed from /notifications (Bell) to /compose (Plus); Bell icon added to header between wordmark and Sair, with 44x44 touch target, muted color, no-op onClick, and focus ring
- PostCard built with all D-07 rows: author avatar (40px) + name + relative timestamp; full post text; horizontal scroll image strip (80px thumbs); external link (noopener noreferrer, accent tinted); category chips (accent 15% tint); comment count button (MessageCircle, navigates to /posts/:id)
- FeedPage built with cursor-based infinite scroll (IntersectionObserver on sentinel div), pull-to-refresh (touch handlers, resets cursor, min 300ms spinner), and all 6 UI states
- App.jsx routes wired: / -> FeedPage, /posts/:postId -> PostDetailPage (stub), /compose -> ComposePage (stub); /notifications route removed
- Stub PostDetailPage/ComposePage land here so App.jsx imports resolve and dev server starts before Plan 04

## Task Commits

1. **Task 1: AppShell update + App.jsx routes + stub pages** - `094fa58` (feat)
2. **Task 2: PostCard component** - `4f96143` (feat)
3. **Task 3: FeedPage infinite scroll + pull-to-refresh** - `a9b1886` (feat)
4. **Task 4: Human smoke test** - PENDING (checkpoint:human-verify)

## Files Created/Modified

- `MINIREDESOCIAL_FRONT/src/components/AppShell/AppShell.jsx` - Plus import, TABS index 2 -> /compose, Bell icon button in header
- `MINIREDESOCIAL_FRONT/src/components/AppShell/AppShell.module.css` - headerSpacer, bellBtn classes added
- `MINIREDESOCIAL_FRONT/src/App.jsx` - FeedPage/PostDetailPage/ComposePage imports; routes updated
- `MINIREDESOCIAL_FRONT/src/components/PostCard/PostCard.jsx` - Full PostCard with 6 rows (created)
- `MINIREDESOCIAL_FRONT/src/components/PostCard/PostCard.module.css` - Co-located styles (created)
- `MINIREDESOCIAL_FRONT/src/pages/FeedPage/FeedPage.jsx` - Full FeedPage implementation (created)
- `MINIREDESOCIAL_FRONT/src/pages/FeedPage/FeedPage.module.css` - Co-located styles (created)
- `MINIREDESOCIAL_FRONT/src/pages/PostDetailPage/PostDetailPage.jsx` - Stub null export (created)
- `MINIREDESOCIAL_FRONT/src/pages/ComposePage/ComposePage.jsx` - Stub null export (created)

## Decisions Made

- **Cursor ref pattern:** `useRef` mirrors the `cursor` state so that the async `fetchPage` callback can read the current cursor without stale closure, instead of using complex useCallback deps.
- **TABS label null for + tab:** The TABS array entry for /compose has `label: null`; the NavLink rendering uses `label ?? 'Nova publicação'` to provide the aria-label without a visible text label (per UI-SPEC).
- **Stub FeedPage in Task 1, replaced in Task 3:** Task 1 creates a null-returning FeedPage stub so App.jsx compiles immediately; Task 3 overwrites it with the real implementation. This keeps each commit atomic.

## Deviations from Plan

None — plan executed exactly as written.

## Known Stubs

- `MINIREDESOCIAL_FRONT/src/pages/PostDetailPage/PostDetailPage.jsx` — returns null. Plan 04 replaces with real implementation.
- `MINIREDESOCIAL_FRONT/src/pages/ComposePage/ComposePage.jsx` — returns null. Plan 04 replaces with real implementation.

These stubs are intentional per plan design: they exist to allow App.jsx imports to resolve and the dev server to start before Plan 04 runs. They are not blocking the plan's goal (feed viewing).

## Threat Model Coverage

- T-04F-01 (XSS): All post fields rendered as React text children (JSX auto-escapes). No dangerouslySetInnerHTML used.
- T-04F-02 (tabnabbing): linkUrl anchor uses `rel="noopener noreferrer"` and `target="_blank"`.
- T-04F-SC: No new frontend packages installed. lucide-react already in package.json.

## Threat Flags

None — no new network endpoints or auth paths introduced. Frontend only reads from existing /api/posts/feed endpoint via the pre-existing axios interceptor.

## Checkpoint Status

**Task 4 (Human smoke test)** — awaiting human verification. The automated tasks (1-3) are complete and committed. The dev server can be started now for manual verification.

## Self-Check: PASSED

Files created:
- MINIREDESOCIAL_FRONT/src/components/PostCard/PostCard.jsx: FOUND
- MINIREDESOCIAL_FRONT/src/components/PostCard/PostCard.module.css: FOUND
- MINIREDESOCIAL_FRONT/src/pages/FeedPage/FeedPage.jsx: FOUND
- MINIREDESOCIAL_FRONT/src/pages/FeedPage/FeedPage.module.css: FOUND
- MINIREDESOCIAL_FRONT/src/pages/PostDetailPage/PostDetailPage.jsx: FOUND
- MINIREDESOCIAL_FRONT/src/pages/ComposePage/ComposePage.jsx: FOUND

Files modified:
- MINIREDESOCIAL_FRONT/src/components/AppShell/AppShell.jsx: FOUND
- MINIREDESOCIAL_FRONT/src/components/AppShell/AppShell.module.css: FOUND
- MINIREDESOCIAL_FRONT/src/App.jsx: FOUND

Commits verified:
- 094fa58 (Task 1): FOUND
- 4f96143 (Task 2): FOUND
- a9b1886 (Task 3): FOUND

Build: `npm run build` exits 0, 1606 modules transformed.
