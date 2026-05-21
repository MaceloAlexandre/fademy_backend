# Requirements: Fademy

**Defined:** 2026-05-21
**Core Value:** A student gets in via invite and immediately sees a personalized mobile feed of posts from people they follow — and can post their own.

## v1 Requirements

Full spec is v1 (competition piece). Each maps to a roadmap phase.

### Platform & Infrastructure

- [x] **PLAT-01**: All data is scoped by institution (multi-tenant from day one) — institutionId required on User model; pattern established in Phase 01 Plan 01
- [x] **PLAT-02**: Seed creates Faculdade Católica da Paraíba, all fixed categories, and a bootstrap admin — idempotent runSeed() implemented and tested in Phase 01 Plan 01
- [ ] **PLAT-03**: Media (avatars, post images, message media) uploads to S3
- [ ] **PLAT-04**: App is deployable to a VPS (env config, production build)

### Authentication

- [x] **AUTH-01**: User can log in with email and password and receive a JWT — POST /api/auth/login implemented in Phase 01 Plan 02 (minimal slice; full logout in Phase 2)
- [ ] **AUTH-02**: User can log out
- [x] **AUTH-03**: Role-based authorization is enforced in middleware before controllers — requireRole factory implemented in Phase 01 Plan 02
- [ ] **AUTH-04**: Registration requires a mandatory profile photo

### Invites

- [ ] **INVT-01**: admin/coordenador/professor/funcionario can generate an invite bound to their institution, carrying the new user's role
- [ ] **INVT-02**: User can accept an invite via token and is created in that institution with the invited role
- [ ] **INVT-03**: Invites expire — `expiresAt` is checked on accept
- [ ] **INVT-04**: aluno cannot generate invites (403)

### Profiles

- [ ] **PROF-01**: User can set their profile public or private (`isPrivate`)
- [ ] **PROF-02**: User can view another user's profile
- [ ] **PROF-03**: User can search for users (excluding blocked relationships)

### Social Graph

- [ ] **SOCL-01**: User can follow a public profile directly (status `accepted`)
- [ ] **SOCL-02**: User can request to follow a private profile (status `pending`)
- [ ] **SOCL-03**: Owner of a private profile can accept or reject follow requests
- [ ] **SOCL-04**: User can block another user, which removes follows on both sides
- [ ] **SOCL-05**: Blocked users cannot message, see posts, or appear in search

### Content

- [ ] **CONT-01**: User can create a post with text
- [ ] **CONT-02**: User can attach images to a post (stored in S3)
- [ ] **CONT-03**: Permitted roles can post links; `aluno` gets 403 (`hasLink` validated by role)
- [ ] **CONT-04**: User tags a post with one or more categories (`content` and `area` types)
- [ ] **CONT-05**: User can comment on a post

### Feed

- [ ] **FEED-01**: User sees a personalized feed of posts from accepted follows
- [ ] **FEED-02**: Feed excludes posts from users blocked in either direction
- [ ] **FEED-03**: Feed is paginated

### Notifications

- [ ] **NOTF-01**: In-app notifications are stored with a `read` flag and polymorphic `refId`/`refModel`
- [ ] **NOTF-02**: Notifications are dispatched via a notification service (never inline in controllers)
- [ ] **NOTF-03**: Supported types: follow_request, follow_accepted, new_follower, post_comment, invite_received, message_received
- [ ] **NOTF-04**: User can fetch notifications and mark them read (polling)

### Chat & Groups

- [ ] **CHAT-01**: Mutually-following users can chat 1:1 in real time (Socket.io)
- [ ] **CHAT-02**: Messages support text and media (images/files)
- [ ] **CHAT-03**: Adding a third person to a conversation turns it into a group
- [ ] **CHAT-04**: Groups can be public (searchable) or private

### Frontend (React, mobile-first)

- [ ] **FRNT-01**: React mobile-first app in sibling folder `MINIREDESOCIAL_FRONT`, talking to the API
- [ ] **FRNT-02**: Brand applied — palette dark blue, black, beige, tobacco brown
- [ ] **FRNT-03**: Invite-acceptance + registration flow with mandatory photo upload
- [ ] **FRNT-04**: Feed view and post composer
- [ ] **FRNT-05**: Profile view with follow / request / block controls
- [ ] **FRNT-06**: Notifications view
- [ ] **FRNT-07**: Chat / conversations view

## v2 Requirements

Deferred to a future milestone.

### Mobile

- **MOBL-01**: Native mobile app

### Engagement

- **ENGM-01**: Post likes (and `post_like` notification)

## Out of Scope

| Feature | Reason |
|---------|--------|
| Push notifications & email | In-app only for v1 (per spec) |
| Real-time notifications (websockets) | Notifications use polling; only chat is real-time, to limit real-time surface area |
| Open / self-serve registration | Registration is invite-only by design |
| Native mobile app | Web is mobile-first now; native app is a future milestone |

## Traceability

| Requirement | Phase | Status |
|-------------|-------|--------|
| PLAT-01 | Phase 1 | Pending |
| PLAT-02 | Phase 1 | Pending |
| AUTH-03 | Phase 1 | Pending |
| INVT-01 | Phase 2 | Pending |
| INVT-02 | Phase 2 | Pending |
| INVT-03 | Phase 2 | Pending |
| INVT-04 | Phase 2 | Pending |
| AUTH-01 | Phase 2 | Pending |
| AUTH-02 | Phase 2 | Pending |
| AUTH-04 | Phase 2 | Pending |
| PLAT-03 | Phase 2 | Pending |
| FRNT-01 | Phase 2 | Pending |
| FRNT-02 | Phase 2 | Pending |
| FRNT-03 | Phase 2 | Pending |
| PROF-01 | Phase 3 | Pending |
| PROF-02 | Phase 3 | Pending |
| PROF-03 | Phase 3 | Pending |
| SOCL-01 | Phase 3 | Pending |
| SOCL-02 | Phase 3 | Pending |
| SOCL-03 | Phase 3 | Pending |
| SOCL-04 | Phase 3 | Pending |
| SOCL-05 | Phase 3 | Pending |
| FRNT-05 | Phase 3 | Pending |
| CONT-01 | Phase 4 | Pending |
| CONT-02 | Phase 4 | Pending |
| CONT-03 | Phase 4 | Pending |
| CONT-04 | Phase 4 | Pending |
| CONT-05 | Phase 4 | Pending |
| FEED-01 | Phase 4 | Pending |
| FEED-02 | Phase 4 | Pending |
| FEED-03 | Phase 4 | Pending |
| FRNT-04 | Phase 4 | Pending |
| NOTF-01 | Phase 5 | Pending |
| NOTF-02 | Phase 5 | Pending |
| NOTF-03 | Phase 5 | Pending |
| NOTF-04 | Phase 5 | Pending |
| FRNT-06 | Phase 5 | Pending |
| CHAT-01 | Phase 6 | Pending |
| CHAT-02 | Phase 6 | Pending |
| CHAT-03 | Phase 6 | Pending |
| CHAT-04 | Phase 6 | Pending |
| FRNT-07 | Phase 6 | Pending |
| PLAT-04 | Phase 7 | Pending |

**Coverage:**
- v1 requirements: 43 total (enumerated IDs; supersedes the earlier "38" estimate)
- Mapped to phases: 43
- Unmapped: 0

---
*Requirements defined: 2026-05-21*
*Last updated: 2026-05-21 after roadmap creation (traceability populated)*
