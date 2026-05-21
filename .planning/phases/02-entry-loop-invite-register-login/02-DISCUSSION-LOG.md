# Phase 2: Entry Loop — Invite, Register, Login - Discussion Log

> **Audit trail only.** Do not use as input to planning, research, or execution agents.
> Decisions are captured in CONTEXT.md — this log preserves the alternatives considered.

**Date:** 2026-05-21
**Phase:** 2-entry-loop-invite-register-login
**Areas discussed:** Invite delivery, Invite expiry/slots, S3 photo upload, Logout & session

---

## Invite delivery

| Option | Description | Selected |
|--------|-------------|----------|
| Link/token manual | Backend generates token + link; inviter sends manually. No email infra. | ✓ |
| Email automático | Backend sends email automatically (SMTP/SES). More scope/risk. | |

**User's choice:** Link/token manual
**Notes:** Aligns with root CONTEXT.md "sem email por ora" and the 3-week timeline.

---

## Invite expiry & slots (free-text — model changed)

| Option | Description | Selected |
|--------|-------------|----------|
| 7 dias | Balance of convenience and security | (basis) |
| 24 horas | More secure, may expire too soon | |
| 30 dias | Convenient, long-lived token | |

**User's choice:** Free-text — "make the invite time configurable, and invites usable by a number of members, not just 1 fixed person."
**Notes:** Pivoted the invite model to multi-use links. Confirmed: default expiry 7 days
(configurable); slot caps by target role — professor/funcionário max 10, aluno max 50
(admin/coordenador assumed 10, to confirm); each invite has its own role; a user can run
multiple active invites in parallel; revoke logic is in scope (status: revoked); invited
person registers with their own email (invite fixes institution + role only).

---

## S3 photo upload

| Option | Description | Selected |
|--------|-------------|----------|
| Presigned URL | Backend signs; browser uploads directly to S3. Less server load. | ✓ |
| Proxy pelo backend | Browser → backend (multipart) → S3. Simpler validation, more server traffic. | |

**User's choice:** Presigned URL
**Notes:** Photo uploaded before account creation (success criterion 3).

---

## Logout & session

| Option | Description | Selected |
|--------|-------------|----------|
| Stateless + localStorage | JWT in localStorage; logout discards token client-side. | ✓ |
| Stateless + cookie httpOnly | JWT in httpOnly cookie; CSRF/CORS care needed. | |
| Server-side (blacklist/refresh) | Server tracks revoked/refresh tokens. Overkill for MVP. | |

**User's choice:** Stateless + localStorage
**Notes:** Accepted XSS trade-off, mitigated by React escaping.

---

## Claude's Discretion

- Frontend stack: React + Vite, axios, React Router, brand palette applied (user did not
  select this area — deferred to Claude per the spec; UI specifics defer to /gsd:ui-phase).
- API route shapes, S3 key structure, presigned URL TTL, image size/type limits, form UX.

## Deferred Ideas

- Automatic email delivery of invites (SMTP/SES).
- `invite_received` notification (Phase 5).
- Server-side token revocation / refresh tokens.
- Profiles, follows, posts, feed, chat (later phases).
- Confirm slot cap for admin/coordenador invites (assumed 10).
