---
phase: 03-profiles-social-graph
plan: 03
subsystem: ui
tags: [react, react-router-dom, lucide-react, css-modules, appshell, nav]

requires:
  - phase: 02-entry-loop-invite-register-login
    provides: Frontend scaffold with React, Vite, CSS Modules, tokens.css, PrimaryButton, login/register pages

provides:
  - AppShell component with scroll-aware fixed header ("Fademy" wordmark + "Sair" logout) and bottom nav bar (4 tabs)
  - useScrollDirection hook (passive scroll listener, direction-based, transform-driven)
  - Bottom nav with Feed/Busca/Notif/Perfil tabs wired to NavLink with active styling via --color-accent
  - App.jsx updated: PostLoginShell removed, authenticated routes nested inside AppShell as layout route
  - Routes defined for /, /search, /notifications, /profile/me, /profile/:userId (placeholders for Plans 03-04/05)

affects:
  - 03-04 (ProfilePage, SearchPage will be inserted into existing AppShell routes)
  - 03-05 (follow/block controls render inside AppShell layout)
  - All future frontend phases build inside AppShell

tech-stack:
  added: []
  patterns:
    - "AppShell as layout route: <Route element={<RequireAuth><AppShell /></RequireAuth>}> wraps all authenticated child routes"
    - "useScrollDirection hook: passive scroll listener on window, useRef tracks lastScrollY, useState drives headerHidden boolean"
    - "CSS Modules with two separate classes (header + headerHidden) applied via template literal: `${styles.header} ${hidden ? styles.headerHidden : ''}`"
    - "NavLink className as function: ({isActive}) => isActive ? styles.tabActive : styles.tab"

key-files:
  created:
    - /home/maceloalexandre/Documentos/projetos/MINIREDESOCIAL_FRONT/src/components/AppShell/AppShell.jsx
    - /home/maceloalexandre/Documentos/projetos/MINIREDESOCIAL_FRONT/src/components/AppShell/AppShell.module.css
  modified:
    - /home/maceloalexandre/Documentos/projetos/MINIREDESOCIAL_FRONT/src/App.jsx

key-decisions:
  - "headerHidden implemented as additive class (styles.header + styles.headerHidden) rather than conditional swap — transition defined on .header, transform override in .headerHidden"
  - "TABS array defined as module-level constant — avoids re-creation on render, keeps JSX declarative"
  - "NavLink children as render prop used to conditionally add sr-only aria label for accessibility"
  - "App.jsx no longer imports App.module.css styles (previously only used by PostLoginShell which was removed)"

patterns-established:
  - "Layout route pattern: parentless <Route element={...}> wraps children, AppShell renders <Outlet />"
  - "useScrollDirection hook lives inside AppShell.jsx file — not exported, not a separate file; single-use hook"
  - "No hardcoded hex values in CSS — all colors via var(--color-*) tokens from tokens.css"

requirements-completed:
  - FRNT-05

duration: 8min
completed: 2026-06-03
---

# Phase 03 Plan 03: AppShell Navigation Shell Summary

**Scroll-aware fixed header with "Fademy" wordmark and 4-tab bottom nav (Feed/Busca/Notif/Perfil) delivered as a React Router layout route, replacing PostLoginShell in App.jsx**

## Performance

- **Duration:** ~8 min
- **Started:** 2026-06-03T23:51:02Z
- **Completed:** 2026-06-03T23:59:00Z
- **Tasks:** 2
- **Files modified:** 3

## Accomplishments

- Created `AppShell.jsx` with `useScrollDirection` hook (passive window scroll listener, direction-based: hides when scrollY increases past 10px, reappears when scrollY decreases)
- Created `AppShell.module.css` with `.header`, `.headerHidden`, `.wordmark`, `.logoutBtn`, `.main`, `.bottomNav`, `.tab`, `.tabActive` classes — all using CSS variable tokens, zero hardcoded hex values
- Updated `App.jsx`: removed `PostLoginShell` function, removed unused `App.module.css` import, added `AppShell` import, restructured authenticated routes as nested children of a layout `<Route>` element — routes exist for `/`, `/search`, `/notifications`, `/profile/me`, `/profile/:userId`
- Vite build passes cleanly (1.54s, no errors or warnings)

## Task Commits

Tasks were executed in the frontend project (outside the backend git repo). Frontend files are written directly to `/home/maceloalexandre/Documentos/projetos/MINIREDESOCIAL_FRONT/` and are not tracked in this backend git repo. Per parallel execution context, only the SUMMARY.md is committed here.

1. **Task 1: AppShell.jsx + AppShell.module.css** - frontend files written directly (not committed to backend repo)
2. **Task 2: Update App.jsx** - frontend file updated directly (not committed to backend repo)

**Plan metadata:** committed to backend worktree (this SUMMARY.md)

## Files Created/Modified

- `/home/maceloalexandre/Documentos/projetos/MINIREDESOCIAL_FRONT/src/components/AppShell/AppShell.jsx` — Authenticated navigation shell: useScrollDirection hook, fixed header with logout, fixed bottom nav with 4 NavLink tabs (Home/Search/Bell/User icons), `<Outlet />` for nested route content
- `/home/maceloalexandre/Documentos/projetos/MINIREDESOCIAL_FRONT/src/components/AppShell/AppShell.module.css` — CSS Modules for AppShell: all classes using CSS variable tokens
- `/home/maceloalexandre/Documentos/projetos/MINIREDESOCIAL_FRONT/src/App.jsx` — PostLoginShell removed; AppShell imported; authenticated routes nested as layout route children; App.module.css import removed (was only used by PostLoginShell)

## Decisions Made

- `headerHidden` implemented as an additive class (`${styles.header} ${headerHidden ? styles.headerHidden : ''}`) — the `.header` class carries the `transition: transform 0.2s ease` so the animation works in both show and hide directions; `.headerHidden` only adds `transform: translateY(-56px)`
- `TABS` array defined at module level as a constant to avoid re-creation on each render and keep JSX clean
- NavLink `children` used as a render prop to conditionally render a screen-reader-only `<span>` indicating the active page — this satisfies `aria-current` accessibility intent without misusing the attribute on a non-anchor element in a nested render prop context

## Deviations from Plan

None - plan executed exactly as written. The `useNavigate` import in `App.jsx` was already removed since `PostLoginShell` (the sole user) was deleted and `RequireAuth` only uses `Navigate` (the component), not the hook.

## Issues Encountered

None.

## User Setup Required

None - no external service configuration required. Vite dev server (`npm run dev` in `MINIREDESOCIAL_FRONT/`) suffices for manual smoke testing.

## Next Phase Readiness

- AppShell is live as the authenticated layout shell — Plans 03-04 and 03-05 replace placeholder `<div>` route elements with real `SearchPage` and `ProfilePage` components by importing them into `App.jsx`
- All 5 routes (`/`, `/search`, `/notifications`, `/profile/me`, `/profile/:userId`) are registered and render placeholder text — no route wiring needed in 03-04/05, only import replacement

---
*Phase: 03-profiles-social-graph*
*Completed: 2026-06-03*
