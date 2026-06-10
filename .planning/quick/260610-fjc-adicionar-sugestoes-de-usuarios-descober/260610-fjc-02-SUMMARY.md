---
phase: quick-260610-fjc
plan: 02
subsystem: ui
tags: [react, search, suggestions, follow]

# Dependency graph
requires:
  - phase: quick-260610-fjc-01
    provides: "GET /api/users/suggestions endpoint returning { users: [{ _id, name, avatarUrl, role, isPrivate }] }"
provides:
  - "Reusable SuggestionCard component (avatar + name + role + FollowButton, click-to-profile, list/carousel variants)"
  - "SearchPage suggestions list shown on empty query, fetched from /api/users/suggestions"
affects: [quick-260610-fjc-03]

# Tech tracking
tech-stack:
  added: []
  patterns:
    - "SuggestionCard variant prop ('list' | 'carousel') controls layout via composed CSS module classes"
    - "onFollowed(userId) callback removes a card from its parent list when follow status becomes accepted/pending"

key-files:
  created:
    - "MINIREDESOCIAL_FRONT: src/components/SuggestionCard/SuggestionCard.jsx"
    - "MINIREDESOCIAL_FRONT: src/components/SuggestionCard/SuggestionCard.module.css"
  modified:
    - "MINIREDESOCIAL_FRONT: src/pages/SearchPage/SearchPage.jsx"
    - "MINIREDESOCIAL_FRONT: src/pages/SearchPage/SearchPage.module.css"

key-decisions:
  - "SuggestionCard modeled directly on UserResultCard (same stopPropagation pattern for FollowButton wrapper) to keep follow/navigate behavior consistent"
  - "Carousel variant pre-built (column layout, 96px width, avatar 56px, role hidden) so Plan 03's FeedPage carousel can reuse SuggestionCard without further CSS work"
  - "Removed extra .suggestions wrapper padding — AppShell content area already provides horizontal padding (var(--space-lg)), matching how UserResultCard rows are laid out"

requirements-completed: [FJC-01, FJC-03, FJC-05]

# Metrics
duration: 8min
completed: 2026-06-10
---

# Phase quick-260610-fjc Plan 02: SuggestionCard + SearchPage Suggestions Summary

**Reusable SuggestionCard (avatar/name/role/Seguir, click-to-profile, list+carousel variants) wired into SearchPage to show "Sugestões para você" before the user types.**

## Performance

- **Duration:** ~8 min
- **Started:** 2026-06-10T14:22:00Z
- **Completed:** 2026-06-10T14:30:22Z
- **Tasks:** 3
- **Files modified:** 4

## Accomplishments
- Created `SuggestionCard` component (avatar, name, role, FollowButton) with click-to-profile navigation and `variant` prop ('list' default, 'carousel' for Plan 03's FeedPage)
- SearchPage now fetches `/api/users/suggestions` on mount and renders a "Sugestões para você" list when the search query is empty
- Following a suggested user removes their card from the list via `onFollowed` callback, without navigating
- Existing search-on-type behavior (debounced search, pagination, "Carregar mais") left unchanged

## Task Commits

Each task was committed atomically (frontend repo: MINIREDESOCIAL_FRONT):

1. **Task 1: Create SuggestionCard component** - `527fa3b` (feat)
2. **Task 2 + 3: SearchPage suggestions integration + commit** - `358e685` (feat)

_Note: Tasks 2 and 3 (implementation + commit) were combined into a single commit per the plan's Task 3 instructions, since SuggestionCard was already committed in Task 1._

## Files Created/Modified
- `MINIREDESOCIAL_FRONT/src/components/SuggestionCard/SuggestionCard.jsx` - New reusable card: avatar, name, role, FollowButton (compact), click-to-profile, `variant` prop ('list' | 'carousel'), `onFollowed` callback
- `MINIREDESOCIAL_FRONT/src/components/SuggestionCard/SuggestionCard.module.css` - `.card` (row layout), `.cardCarousel` (column layout, 96px width, 56px avatar, role hidden), avatar/info/name/role/followWrap styles
- `MINIREDESOCIAL_FRONT/src/pages/SearchPage/SearchPage.jsx` - Added `suggestions`/`suggestionsLoading` state, mount-time fetch of `/api/users/suggestions`, "Sugestões para você" section rendered on empty query using `SuggestionCard`
- `MINIREDESOCIAL_FRONT/src/pages/SearchPage/SearchPage.module.css` - Added `.suggestionsHeading` style

## Decisions Made
- SuggestionCard built as a near-identical structural clone of UserResultCard (same stopPropagation pattern around FollowButton, same navigate-on-card-click) to keep behavior consistent and predictable.
- Pre-implemented the `carousel` variant CSS now (column layout, fixed 96px width, larger avatar, no role text) so Plan 03 can drop it into the FeedPage carousel without additional styling work.
- Reused the existing `.emptyState` style for the "Nenhuma sugestão no momento" message rather than adding a new class.

## Deviations from Plan

None - plan executed exactly as written.

## Known Stubs

None.

## Issues Encountered
None.

## User Setup Required
None - no external service configuration required.

## Next Phase Readiness
- SuggestionCard is ready for reuse in Plan 03 (FeedPage suggestions carousel) via `variant="carousel"`.
- SearchPage suggestions flow is fully wired to the live `/api/users/suggestions` endpoint from Plan 01.
- No blockers.

---
*Phase: quick-260610-fjc*
*Completed: 2026-06-10*

## Self-Check: PASSED

- FOUND: MINIREDESOCIAL_FRONT/src/components/SuggestionCard/SuggestionCard.jsx
- FOUND: MINIREDESOCIAL_FRONT/src/components/SuggestionCard/SuggestionCard.module.css
- FOUND: commit 527fa3b
- FOUND: commit 358e685
- FOUND: SUMMARY.md
