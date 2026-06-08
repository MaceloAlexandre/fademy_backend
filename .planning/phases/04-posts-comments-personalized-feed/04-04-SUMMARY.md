---
phase: 04-posts-comments-personalized-feed
plan: "04"
subsystem: frontend-compose-comment
tags: [react, compose, post-creation, multipart, formdata, comment, post-detail, fixed-input]
dependency_graph:
  requires:
    - phase: 04-01
      provides: POST /api/posts, GET /api/categories, GET /api/posts/:id endpoints
    - phase: 04-02
      provides: GET/POST /api/posts/:postId/comments endpoints
    - phase: 04-03
      provides: App.jsx routes, stub ComposePage/PostDetailPage, PostCard component
  provides:
    - ComposePage: multipart post composer (text required, up to 4 images, optional link, category chips, Publicar)
    - PostDetailPage: post detail at top + full comment thread + fixed bottom comment input
  affects: [Phase 4 core loop complete — compose + comment end-to-end]

tech-stack:
  added: []
  patterns:
    - FormData multipart submit via axios (Content-Type set automatically)
    - URL.createObjectURL for local image thumbnail preview
    - Promise.all for parallel fetch of post + comments
    - Fixed position bottom bar (position fixed, bottom 56px) above bottom nav
    - aria-pressed on category chips for accessibility

key-files:
  created:
    - MINIREDESOCIAL_FRONT/src/pages/ComposePage/ComposePage.module.css
    - MINIREDESOCIAL_FRONT/src/pages/PostDetailPage/PostDetailPage.module.css
  modified:
    - MINIREDESOCIAL_FRONT/src/pages/ComposePage/ComposePage.jsx
    - MINIREDESOCIAL_FRONT/src/pages/PostDetailPage/PostDetailPage.jsx

key-decisions:
  - "text is always required regardless of images — Publicar disabled when text empty; mirrors backend schema required:true on text field"
  - "URL.createObjectURL used for image thumbnail preview without uploading prematurely"
  - "Promise.all for post + comments parallel fetch — single loading state covers both"
  - "Fixed input bar uses position:fixed bottom 56px (above nav), z-index 90; error banner above at z-index 91"

requirements-completed: [FRNT-04, CONT-01, CONT-02, CONT-03, CONT-04, CONT-05]

duration: ~10min
completed: "2026-06-08"
---

# Phase 04 Plan 04: ComposePage and PostDetailPage Summary

**ComposePage with FormData multipart post (text required + up to 4 images + optional link + category chips) and PostDetailPage with full comment thread + fixed bottom input — completing the Phase 4 core loop.**

## Performance

- **Duration:** ~10 min
- **Started:** 2026-06-08
- **Completed:** 2026-06-08 (Tasks 1-2 automated; Task 3 human-verify pending)
- **Tasks:** 2 of 3 automated tasks completed; Task 3 is a human-verify checkpoint
- **Files modified:** 4

## Accomplishments

- ComposePage replaces null stub: fetches categories on mount, renders up to 4 image slots (Camera icon + dashed border when empty, thumbnail + X remove when filled), category chip multi-select with aria-pressed, FormField for optional link, builds FormData on submit (text always required), 403 aluno link gate shows "Sua função não permite postar links.", redirects to / on success
- PostDetailPage replaces null stub: parallel fetch of post + comments, PostCard at top with isDetailView, full comment thread (32px avatar + name + timestamp + text), "Seja o primeiro a comentar." empty state, fixed bottom input bar (position fixed, bottom 56px, 56px tall), send button with 16px Spinner while submitting
- npm run build passes (1608 modules, 0 errors)

## Task Commits

1. **Task 1: ComposePage** - `211d463` (feat) - ComposePage.jsx + ComposePage.module.css
2. **Task 2: PostDetailPage** - `f4a06da` (feat) - PostDetailPage.jsx + PostDetailPage.module.css
3. **Task 3: Human smoke test** - PENDING (checkpoint:human-verify)

## Files Created/Modified

- `MINIREDESOCIAL_FRONT/src/pages/ComposePage/ComposePage.jsx` - Full implementation replacing null stub
- `MINIREDESOCIAL_FRONT/src/pages/ComposePage/ComposePage.module.css` - Co-located styles (created)
- `MINIREDESOCIAL_FRONT/src/pages/PostDetailPage/PostDetailPage.jsx` - Full implementation replacing null stub
- `MINIREDESOCIAL_FRONT/src/pages/PostDetailPage/PostDetailPage.module.css` - Co-located styles (created)

## Decisions Made

- **Text always required:** `Publicar` disabled whenever `text.trim() === ''`, regardless of attached images. The backend schema marks `text` as `required: true` — permitting submit with only images would always fail at the API. Frontend validation stays in lockstep.
- **URL.createObjectURL for previews:** Local file thumbnails rendered via `URL.createObjectURL(file)` without pre-uploading. The actual upload happens on form submit via FormData.
- **Promise.all for parallel load:** Post detail and comments fetched simultaneously. A single `pageState` covers both — if either fails, state goes to `error`.
- **Fixed input bar z-index layering:** Input bar at z-index 90 (matches UI-SPEC); submit error banner at z-index 91 so it renders above the bar.

## Deviations from Plan

None — plan executed exactly as written.

## Threat Model Coverage

- T-04D-01 (Elevation of Privilege — aluno link): Client surfaces backend 403 as "Sua função não permite postar links."; role gate is authoritative server-side.
- T-04D-02 (XSS — comment/post text): All comment and post fields rendered as React text children (JSX auto-escapes). No dangerouslySetInnerHTML used.
- T-04D-03 (DoS — oversized image): Client-side 5MB-per-file check in `handleFileChange` mirrors backend MAX_UPLOAD_BYTES; shows "Imagem muito grande. Máximo 5 MB por arquivo."
- T-04D-04 (Tampering — category id injection): Only IDs from the fetched /api/categories list are selectable.
- T-04D-SC: No new frontend packages installed.

## Checkpoint Status

**Task 3 (Human smoke test)** — awaiting human verification. Automated tasks 1 and 2 are complete and committed. Dev server can be started now for manual verification of the full core loop.

## Known Stubs

None — all stubs from Plan 03 have been replaced with real implementations.

## Self-Check: PASSED

Files modified:
- MINIREDESOCIAL_FRONT/src/pages/ComposePage/ComposePage.jsx: FOUND (replaced null stub)
- MINIREDESOCIAL_FRONT/src/pages/ComposePage/ComposePage.module.css: FOUND (created)
- MINIREDESOCIAL_FRONT/src/pages/PostDetailPage/PostDetailPage.jsx: FOUND (replaced null stub)
- MINIREDESOCIAL_FRONT/src/pages/PostDetailPage/PostDetailPage.module.css: FOUND (created)

Commits verified:
- 211d463 (Task 1 ComposePage): FOUND
- f4a06da (Task 2 PostDetailPage): FOUND

Build: npm run build exits 0, 1608 modules transformed.
