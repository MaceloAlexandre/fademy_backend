---
phase: 03-profiles-social-graph
plan: "04"
subsystem: frontend
tags: [react, css-modules, social-graph, profile, follow, block]
dependency_graph:
  requires: [03-01, 03-02, 03-03]
  provides: [FollowButton, BlockButton, UserResultCard, ProfilePage]
  affects: [03-05]
tech_stack:
  added: []
  patterns: [CSS Modules, useState loading pattern, inline confirmation (no modals), useParams, useNavigate]
key_files:
  created:
    - MINIREDESOCIAL_FRONT/src/components/FollowButton/FollowButton.jsx
    - MINIREDESOCIAL_FRONT/src/components/FollowButton/FollowButton.module.css
    - MINIREDESOCIAL_FRONT/src/components/BlockButton/BlockButton.jsx
    - MINIREDESOCIAL_FRONT/src/components/BlockButton/BlockButton.module.css
    - MINIREDESOCIAL_FRONT/src/components/UserResultCard/UserResultCard.jsx
    - MINIREDESOCIAL_FRONT/src/components/UserResultCard/UserResultCard.module.css
    - MINIREDESOCIAL_FRONT/src/pages/ProfilePage/ProfilePage.jsx
    - MINIREDESOCIAL_FRONT/src/pages/ProfilePage/ProfilePage.module.css
  modified: []
decisions:
  - "Private profile non-follower view renders conditionally to null (not CSS display:none) — satisfies T-03-16 threat mitigation"
  - "BlockButton not shown in private non-follower view — user can FollowButton but block requires full profile visibility; consistent with D-05"
  - "isOwner determined solely by userId === 'me' route param — avoids JWT parsing in components, per 02-CONTEXT D-14"
  - "UserResultCard initial followStatus is null — card context quick-follow is MVP acceptable; full status shown on ProfilePage"
  - "onBlocked in ProfilePage uses window.location.reload() — simplest MVP approach for Phase 3"
metrics:
  duration: "~8 min"
  completed: "2026-06-03T23:58:19Z"
  tasks_completed: 2
  files_created: 8
  files_modified: 0
---

# Phase 03 Plan 04: Frontend Profile & Social Graph Components Summary

FollowButton (4-state machine with inline confirmations), BlockButton, UserResultCard, and ProfilePage built with CSS Modules — no modals, no hardcoded hex colors, private profile content conditionally rendered to null.

## Tasks Completed

| Task | Description | Files Created |
|------|-------------|---------------|
| 1 | FollowButton.jsx + BlockButton.jsx + CSS Modules | 4 files |
| 2 | ProfilePage.jsx + UserResultCard.jsx + CSS Modules | 4 files |

## What Was Built

### FollowButton (`src/components/FollowButton/`)

State machine button reflecting the follow relationship:

- `null + !isPrivate` — "Seguir" (accent fill PrimaryButton style)
- `null + isPrivate` — "Solicitar" (accent fill PrimaryButton style)
- `'pending'` — "Solicitação enviada" (ghost, surface border), tap shows inline "Cancelar solicitação?" + [Cancelar] [Confirmar]; Confirmar calls DELETE /api/users/:targetId/follow
- `'accepted'` — "Seguindo" (ghost, accent border), tap shows inline "Deixar de seguir?" + [Cancelar] [Confirmar]; Confirmar calls DELETE /api/users/:targetId/follow

Loading state: Spinner (16px) replaces label, button disabled. Error state: ErrorBanner below button. No modals — inline expansion below button. `compact` prop reduces min-height to 36px for UserResultCard context.

### BlockButton (`src/components/BlockButton/`)

- `!isBlockedByMe` — "Bloquear" (ghost, rgba(253,128,128,0.4) border, --color-destructive text), tap shows inline "Bloquear este usuário?" + "Seguidores mútuos serão removidos." + [Cancelar] [Bloquear]
- `isBlockedByMe` — "Bloqueado" (muted, opacity 0.5, cursor default, disabled)

### ProfilePage (`src/pages/ProfilePage/`)

- `useParams({ userId })` — `'me'` route signals isOwner=true
- Loading: centered Spinner (32px). Error: ErrorBanner "Erro ao carregar perfil. Tente novamente." Blocked (404): ErrorBanner "Usuário não encontrado."
- Always rendered: avatar (80px circle, rgba(214,207,193,0.3) border), name (heading/600), role badge (label/muted)
- isOwner only: privacy toggle (checkbox, PATCH /api/users/me on change); no FollowButton or BlockButton
- Full content (isOwner or not private non-follower): counters row, posts placeholder
- Other user: action row with FollowButton + BlockButton; BlockButton omitted for private non-follower view
- Private profile threat mitigation: conditional render to null (not CSS display:none) — T-03-16

### UserResultCard (`src/components/UserResultCard/`)

- Horizontal flex: avatar (40px circle) | name (body/600) + role (label/muted) | compact FollowButton (right-aligned)
- Card div onClick navigates to `/profile/:userId` via useNavigate
- FollowButton wrapper has stopPropagation to prevent card navigation when interacting with button
- Separator: border-bottom 1px solid rgba(214,207,193,0.1)
- `onFollowStatusChange` callback propagates status updates to parent (SearchPage)

## Deviations from Plan

None — plan executed exactly as written.

## Known Stubs

| Stub | File | Reason |
|------|------|--------|
| Posts placeholder "Posts aparecem aqui em breve" | ProfilePage.jsx | Phase 4 delivers posts; intentional per plan scope |
| UserResultCard initial `followStatus = null` | UserResultCard.jsx | MVP: quick-follow from card without knowing current status; acceptable per plan spec |
| `onBlocked` uses `window.location.reload()` | ProfilePage.jsx | Simplest MVP for Phase 3; block naturally removes user from reachable profiles |

## Threat Surface Scan

No new surface beyond the plan's threat model:
- T-03-16: Private profile content conditionally rendered to null — mitigated
- T-03-17: Block POST idempotent in backend — accepted
- T-03-15: Self-follow/follow-blocked-user enforced in backend — accepted

## Self-Check: PASSED

- [x] FollowButton.jsx exists at MINIREDESOCIAL_FRONT/src/components/FollowButton/
- [x] FollowButton.module.css exists (no hardcoded hex)
- [x] BlockButton.jsx exists at MINIREDESOCIAL_FRONT/src/components/BlockButton/
- [x] BlockButton.module.css exists (no hardcoded hex)
- [x] UserResultCard.jsx exists at MINIREDESOCIAL_FRONT/src/components/UserResultCard/
- [x] UserResultCard.module.css exists (no hardcoded hex)
- [x] ProfilePage.jsx exists at MINIREDESOCIAL_FRONT/src/pages/ProfilePage/ — imports FollowButton, BlockButton, Spinner, ErrorBanner, api
- [x] ProfilePage.module.css exists (no hardcoded hex)
- [x] All inline confirmations — no modal dialogs anywhere
- [x] Private profile non-follower: conditional render to null for counters/posts
- [x] Own profile: privacy toggle shown; no FollowButton/BlockButton
