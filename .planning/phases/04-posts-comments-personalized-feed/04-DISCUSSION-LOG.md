# Phase 4: Posts, Comments & Personalized Feed - Discussion Log

> **Audit trail only.** Do not use as input to planning, research, or execution agents.
> Decisions are captured in CONTEXT.md — this log preserves the alternatives considered.

**Date:** 2026-06-04
**Phase:** 4-posts-comments-personalized-feed
**Areas discussed:** Feed scroll behavior, Post image upload UX, Post card & comment entry, Composer entry point

---

## Feed scroll behavior

| Option | Description | Selected |
|--------|-------------|----------|
| Infinite scroll | Posts load automatically via IntersectionObserver as user nears bottom | ✓ |
| Load-more button | User taps to fetch next page; simpler, consistent with search (D-10 Phase 3) | |
| Pull-to-refresh only | No pagination; just latest N posts | |

**User's choice:** Infinite scroll

---

| Option | Description | Selected |
|--------|-------------|----------|
| Cursor-based (?cursor=lastPostId) | Stable during inserts; backend uses _id < cursor | ✓ |
| Offset-based (?page=&limit=) | Simpler; risks duplicates when new posts inserted mid-scroll | |

**User's choice:** Cursor-based pagination

---

| Option | Description | Selected |
|--------|-------------|----------|
| Yes — pull-to-refresh | Pull down to reload from top; resets cursor | ✓ |
| No — manual refresh | Tap Feed tab again to reload | |

**User's choice:** Pull-to-refresh included

---

## Post image upload UX

| Option | Description | Selected |
|--------|-------------|----------|
| Up to 4 images | Instagram-style limit; reasonable S3 cost | ✓ |
| Up to 1 image | Simplest | |
| Unlimited | Flexible but complex | |

**User's choice:** Up to 4 images per post

---

| Option | Description | Selected |
|--------|-------------|----------|
| Upload on POST (server-proxied) | Single multipart/form-data request; consistent with uploadAvatar | ✓ |
| Presigned URL flow | Client uploads to S3 then POSTs URLs; reduces server bandwidth but adds round-trips | |

**User's choice:** Server-proxied multipart upload

---

| Option | Description | Selected |
|--------|-------------|----------|
| Explicit link field | Dedicated linkUrl input; hasLink: true when non-empty; 403 for aluno | ✓ |
| Links embedded in text | hasLink detected by parsing text; fuzzy | |

**User's choice:** Explicit linkUrl field

---

## Post card & comment entry

| Option | Description | Selected |
|--------|-------------|----------|
| Author + text + images + categories + comment count | Standard social card; comment count opens detail screen | ✓ |
| Author + text + images + categories + first 2 comments inline | More content per card; heavier feed query | |
| Compact — author + text + comment count only | No images in feed; stripped down | |

**User's choice:** Full card with comment-count button (no inline comments)

---

| Option | Description | Selected |
|--------|-------------|----------|
| Dedicated comment screen (/posts/:id) | Full-screen thread; fixed comment input at bottom | ✓ |
| Bottom sheet | Slide-up over feed; needs new sheet component | |
| Inline expand | Expands in-place; complex scroll behavior | |

**User's choice:** Dedicated /posts/:id screen

---

| Option | Description | Selected |
|--------|-------------|----------|
| Load all comments (MVP) | Fetch all in GET /posts/:id; fine for low volumes | ✓ |
| Paginate comments (load-more) | Safer for high volumes; adds complexity now | |

**User's choice:** Load all comments — no pagination for MVP

---

## Composer entry point

| Option | Description | Selected |
|--------|-------------|----------|
| FAB on feed screen | + button bottom-right; navigates to /compose | |
| Fixed button at top of feed | Always visible at top of feed column | |
| Inside header | Edit icon in scroll-aware header | |
| Other (freeform) | Move notification icon to header; create post icon in bottom nav where notification was | ✓ |

**User's choice (freeform):** "Na verdade acho que ficava melhor mover o ícone de notificação para o topo da página e o ícone de criar postagem na navbar inferior onde agora está o ícone de notificação"

**Translation:** Move notification icon to the top of the page; put the create-post icon in the bottom nav where notification was.

**Notes:** This updates bottom nav from Phase 3 D-01 (Feed/Busca/Notif/Perfil → Feed/Busca/+/Perfil). Notification bell goes into the AppShell header. Phase 5 will wire up the bell badge count.

---

| Option | Description | Selected |
|--------|-------------|----------|
| /compose route (full-screen) | Full-screen composer; after posting → back to feed | ✓ |
| Modal/sheet over feed | Slide-up sheet; needs new component | |

**User's choice:** Dedicated /compose route

---

## Claude's Discretion

- Internal structure of React components (PostCard, FeedPage, ComposePage, PostDetailPage)
- API response field names and payload structure
- Loading states and error boundaries
- Feed cache invalidation strategy after posting
- S3 key prefix for post images

## Deferred Ideas

- Comment pagination — deferred to future phase if volume grows
- Post likes (ENGM-01) — v2 backlog
- Post editing/deletion — not in Phase 4 scope
- Notification bell badge count — icon appears in Phase 4; real count connected in Phase 5
