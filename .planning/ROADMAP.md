# Roadmap: Fademy

## Overview

Fademy is built as a sequence of vertical MVP slices so there is always a deployable, demo-able product. We lay the multi-tenant foundation and seed first, then deliver the core entry loop (invite → register → login) on mobile, then the social graph, then the heart of the product (posts → personalized feed). Notifications follow via polling. Chat/groups — the highest-risk, real-time piece — lands last so it can be cut without breaking the product if the 3-week window tightens. A final phase hardens for VPS deployment.

## Phases

**Phase Numbering:**

- Integer phases (1, 2, 3): Planned milestone work
- Decimal phases (2.1, 2.2): Urgent insertions (marked with INSERTED)

Decimal phases appear between their surrounding integers in numeric order.

- [x] **Phase 1: Foundation & Multi-Tenant Seed** - Backend scaffolding, institution scoping, seed (Católica PB + categories + bootstrap admin), auth middleware skeleton
- [x] **Phase 2: Entry Loop — Invite, Register, Login** - End-to-end on mobile: an invited user registers with a mandatory photo and logs in (completed 2026-05-28)
- [x] **Phase 3: Profiles & Social Graph** - View/configure profiles, search, follow public/private, accept/reject, block (completed 2026-06-04)
- [ ] **Phase 4: Posts, Comments & Personalized Feed** - Create posts (text/images/links by role), categorize, comment, see a personalized paginated feed (core loop completes)
- [ ] **Phase 5: Notifications (Polling)** - In-app notifications dispatched by a service; users fetch and mark read
- [ ] **Phase 6: Chat & Groups (Real-Time)** - 1:1 chat between mutual followers via Socket.io, media messages, group conversations
- [ ] **Phase 7: Production Deployment** - Env config, production build, deploy to VPS

## Phase Details

### Phase 1: Foundation & Multi-Tenant Seed

**Goal**: A running Express/MongoDB backend with institution scoping baked in, the system seeded with Católica PB + all fixed categories + a bootstrap admin, and the role/auth middleware skeleton in place
**Mode:** mvp
**Depends on**: Nothing (first phase)
**Requirements**: PLAT-01, PLAT-02, AUTH-03
**Success Criteria** (what must be TRUE):

  1. Running the seed creates the Faculdade Católica da Paraíba institution, all fixed `content` and `area` categories, and a bootstrap admin user
  2. Every data model carries an `institutionId` and queries are scoped by institution
  3. Role-based authorization middleware exists and rejects unauthorized roles before reaching controllers
  4. The server connects to MongoDB and responds to a health check

**Plans**: TBD

### Phase 2: Entry Loop — Invite, Register, Login

**Goal**: An invited person can open Fademy on their phone, accept an invite, register with a mandatory profile photo, and log in — the first deployable, demo-able slice
**Mode:** mvp
**Depends on**: Phase 1
**Requirements**: INVT-01, INVT-02, INVT-03, INVT-04, AUTH-01, AUTH-02, AUTH-04, PLAT-03, FRNT-01, FRNT-02, FRNT-03
**Success Criteria** (what must be TRUE):

  1. An admin/coordenador/professor/funcionario can generate an invite bound to their institution carrying the new user's role; an aluno attempting to invite gets 403
  2. A user can accept a valid invite token and is created in the inviting institution with the invited role; expired tokens are rejected
  3. Registration requires and uploads a profile photo to S3 before the account is created
  4. A registered user can log in with email/password to receive a JWT and can log out
  5. The React mobile-first frontend (branded dark blue/black/beige/tobacco) runs the invite-acceptance + registration + login flow against the API

**Plans**: 4 plans

  - [x] 02-01-PLAN.md — Invite generation (model, permission matrix, slot caps, revoke)
  - [x] 02-02-PLAN.md — S3 presigned profile-photo upload slice
  - [x] 02-03-PLAN.md — Invite-bound registration (consumes invite + S3 photo) + CORS
  - [x] 02-04-PLAN.md — React frontend: accept-invite, register, login/logout

**UI hint**: yes

### Phase 3: Profiles & Social Graph

**Goal**: Users can find each other, view profiles, control profile privacy, follow (directly or by request), accept/reject requests, and block — all usable on the phone
**Mode:** mvp
**Depends on**: Phase 2
**Requirements**: PROF-01, PROF-02, PROF-03, SOCL-01, SOCL-02, SOCL-03, SOCL-04, SOCL-05, FRNT-05
**Success Criteria** (what must be TRUE):

  1. A user can set their profile public or private and view another user's profile
  2. A user can search for users, and blocked relationships are excluded from results
  3. A user can follow a public profile directly (accepted) and request to follow a private profile (pending)
  4. The owner of a private profile can accept or reject follow requests
  5. Blocking a user removes follows on both sides and prevents the blocked user from messaging, seeing posts, or appearing in search — all reachable from the mobile profile view with follow/request/block controls

**Plans**: 5 plans

**Wave 1** (parallel)

  - [x] 03-01-PLAN.md — Follow & Block backend (models, notification stub, controllers, routes, tests)
  - [x] 03-02-PLAN.md — User profile & search backend (getProfile, updateMe, searchUsers, user.controller.test)

**Wave 2** *(blocked on Wave 1 completion)*

  - [x] 03-03-PLAN.md — AppShell navigation shell (replaces PostLoginShell, bottom nav + scroll-aware header)

**Wave 3** *(blocked on Wave 2 completion)*

  - [x] 03-04-PLAN.md — ProfilePage + FollowButton + BlockButton + UserResultCard components

**Wave 4** *(blocked on Wave 3 completion)*

  - [x] 03-05-PLAN.md — SearchPage + App.jsx wiring + human smoke test checkpoint

**Cross-cutting constraints:**

- All backend queries scoped by `req.user.institutionId` (PLAT-01)
- `/api/users/search` route registered before `/api/users/:id` (Express route ordering)
- SOCL-03 accept/reject API in Phase 3; frontend UI deferred to Phase 5 (D-07)

**UI hint**: yes

### Phase 4: Posts, Comments & Personalized Feed

**Goal**: The core loop completes — users create categorized posts (text/images/links per role), comment, and see a personalized paginated feed of people they follow on their phone
**Mode:** mvp
**Depends on**: Phase 3
**Requirements**: CONT-01, CONT-02, CONT-03, CONT-04, CONT-05, FEED-01, FEED-02, FEED-03, FRNT-04
**Success Criteria** (what must be TRUE):

  1. A user can create a post with text, attach images (stored in S3), and tag one or more categories
  2. Permitted roles can include links in posts; an aluno attempting a link post gets 403
  3. A user can comment on a post
  4. A user sees a personalized, paginated feed of posts from accepted follows, excluding posts from users blocked in either direction
  5. The feed view and post composer work on the mobile frontend end-to-end

**Plans**: 4 plans

**Wave 1**

  - [x] 04-01-PLAN.md — Posts + personalized feed backend (Post model, S3 image upload, role-gated links, cursor feed, categories endpoint)

**Wave 2** *(blocked on Wave 1)*

  - [x] 04-02-PLAN.md — Comments backend (Comment model, create/list, commentCount, post_comment notification)

**Wave 3** *(blocked on Wave 2)*

  - [x] 04-03-PLAN.md — Feed frontend slice (AppShell nav + Bell, PostCard, FeedPage infinite scroll + pull-to-refresh, App.jsx routes)

**Wave 4** *(blocked on Wave 3)*

  - [ ] 04-04-PLAN.md — Compose + comment frontend (ComposePage multipart composer, PostDetailPage thread + fixed input)

**UI hint**: yes

### Phase 5: Notifications (Polling)

**Goal**: Users receive and manage in-app notifications for social events, dispatched cleanly by a notification service and surfaced via polling on mobile
**Mode:** mvp
**Depends on**: Phase 4
**Requirements**: NOTF-01, NOTF-02, NOTF-03, NOTF-04
**Success Criteria** (what must be TRUE):

  1. Notifications are stored in the DB with a `read` flag and polymorphic `refId`/`refModel`
  2. Notifications are dispatched through a notification service, never inline in controllers
  3. The supported event types (follow_request, follow_accepted, new_follower, post_comment, invite_received, message_received) generate notifications when those events occur
  4. A user can fetch their notifications and mark them read from the mobile notifications view (polling)

**Plans**: TBD
**UI hint**: yes

### Phase 6: Chat & Groups (Real-Time)

**Goal**: Mutually-following users can chat 1:1 in real time, send media, and grow conversations into public or private groups — the highest-risk feature, landed last so it can be cut without breaking the product
**Mode:** mvp
**Depends on**: Phase 5
**Requirements**: CHAT-01, CHAT-02, CHAT-03, CHAT-04, FRNT-07
**Success Criteria** (what must be TRUE):

  1. Two users who follow each other mutually can exchange messages in real time via Socket.io; non-mutual or blocked users cannot start a chat
  2. Messages support text and media (images/files) stored in S3
  3. Adding a third person to a conversation turns it into a group
  4. Groups can be marked public (searchable) or private
  5. The mobile chat/conversations view works end-to-end against the real-time backend

**Plans**: TBD
**UI hint**: yes

### Phase 7: Production Deployment

**Goal**: Fademy runs in production on the owner's VPS with proper environment configuration and a production build
**Mode:** mvp
**Depends on**: Phase 6
**Requirements**: PLAT-04
**Success Criteria** (what must be TRUE):

  1. Backend and frontend build for production with environment-driven configuration (no hardcoded secrets)
  2. The app is deployed and reachable on the owner's VPS
  3. A user can complete the core loop (invite → register → post → feed) against the deployed instance from a phone

**Plans**: TBD

## Progress

**Execution Order:**
Phases execute in numeric order: 1 → 2 → 3 → 4 → 5 → 6 → 7

| Phase | Plans Complete | Status | Completed |
|-------|----------------|--------|-----------|
| 1. Foundation & Multi-Tenant Seed | 2/2 | Complete | 2026-05-21 |
| 2. Entry Loop — Invite, Register, Login | 4/4 | Complete   | 2026-05-28 |
| 3. Profiles & Social Graph | 5/5 | Complete    | 2026-06-04 |
| 4. Posts, Comments & Personalized Feed | 3/4 | In Progress|  |
| 5. Notifications (Polling) | 0/TBD | Not started | - |
| 6. Chat & Groups (Real-Time) | 0/TBD | Not started | - |
| 7. Production Deployment | 0/TBD | Not started | - |
