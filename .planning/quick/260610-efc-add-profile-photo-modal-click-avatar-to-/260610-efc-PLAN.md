---
phase: quick-260610-efc
plan: 01
type: execute
wave: 1
depends_on: []
files_modified:
  - /home/maceloalexandre/Documentos/projetos/MINIREDESOCIAL_FRONT/src/pages/ProfilePage/ProfilePage.jsx
  - /home/maceloalexandre/Documentos/projetos/MINIREDESOCIAL_FRONT/src/pages/ProfilePage/ProfilePage.module.css
  - /home/maceloalexandre/Documentos/projetos/MINIREDESOCIAL_FRONT/src/components/PostCard/PostCard.jsx
  - /home/maceloalexandre/Documentos/projetos/MINIREDESOCIAL_FRONT/src/components/PostCard/PostCard.module.css
  - /home/maceloalexandre/Documentos/projetos/MINIREDESOCIAL_FRONT/src/pages/PostDetailPage/PostDetailPage.jsx
  - /home/maceloalexandre/Documentos/projetos/MINIREDESOCIAL_FRONT/src/pages/PostDetailPage/PostDetailPage.module.css
  - /home/maceloalexandre/Documentos/projetos/MINIREDESOCIAL_FRONT/src/components/NotificationCard/NotificationCard.jsx
  - /home/maceloalexandre/Documentos/projetos/MINIREDESOCIAL_FRONT/src/components/NotificationCard/NotificationCard.module.css
  - /home/maceloalexandre/Documentos/projetos/MINIREDESOCIAL_FRONT/src/components/ConversationListItem/ConversationListItem.jsx
  - /home/maceloalexandre/Documentos/projetos/MINIREDESOCIAL_FRONT/src/components/ConversationListItem/ConversationListItem.module.css
  - /home/maceloalexandre/Documentos/projetos/MINIREDESOCIAL_FRONT/src/pages/ConversationThreadPage/ConversationThreadPage.jsx
  - /home/maceloalexandre/Documentos/projetos/MINIREDESOCIAL_FRONT/src/pages/ConversationThreadPage/ConversationThreadPage.module.css
autonomous: true
requirements: [QUICK-260610-efc]

must_haves:
  truths:
    - "Clicking the avatar on a profile page opens a modal showing the full-size photo; clicking the X or outside closes it without navigating"
    - "In the feed (PostCard), clicking an author avatar or name navigates to that author's profile and does NOT open the post detail"
    - "In a post's comment thread, clicking a commenter avatar or name navigates to that commenter's profile"
    - "In a notification card, clicking the actor avatar or actor name navigates to that actor's profile (even for post_comment / message_received notifications where the card body navigates elsewhere)"
    - "In the conversations list, clicking the other user's avatar or name navigates to their profile and does NOT open the chat thread"
    - "In a chat thread, clicking the other user's avatar or name in the top profile card navigates to their profile"
  artifacts:
    - path: "/home/maceloalexandre/Documentos/projetos/MINIREDESOCIAL_FRONT/src/pages/ProfilePage/ProfilePage.jsx"
      provides: "Photo-viewer modal state + overlay markup mirroring MessageBubble lightbox"
      contains: "photoOpen"
  key_links:
    - from: "PostCard avatar/name"
      to: "/profile/:userId"
      via: "navigate with stopPropagation"
      pattern: "navigate\\(`/profile/"
    - from: "ConversationListItem avatar/name"
      to: "/profile/:userId"
      via: "Link/navigate with stopPropagation (row is itself a Link to /chat)"
      pattern: "/profile/"
---

<objective>
Add a profile-photo viewer modal (click avatar on ProfilePage → full-size image lightbox with X close, click-outside to dismiss, no navigation) and make user avatars + names clickable links to that user's profile (`/profile/:id`) everywhere they appear: feed post cards, comment threads, notification cards, the conversations list, and the chat thread header card.

Purpose: Lets users tap any face/name to view a profile, and inspect their own (or others') profile photo at full size — a core social-network affordance currently missing.
Output: One photo modal in ProfilePage + click-to-profile wiring across 5 surfaces, each committed separately in the MINIREDESOCIAL_FRONT repo.
</objective>

<execution_context>
@$HOME/.claude/get-shit-done/workflows/execute-plan.md
@$HOME/.claude/get-shit-done/templates/summary.md

**CROSS-REPO TASK — read carefully:**
- All code edits happen in the SEPARATE git repo at `/home/maceloalexandre/Documentos/projetos/MINIREDESOCIAL_FRONT` (independent repo on its own `main` branch, NOT a submodule of the current repo).
- Do NOT use worktree isolation — edit files directly in place.
- Each task below ends with its OWN commit made INSIDE the frontend repo using `git -C /home/maceloalexandre/Documentos/projetos/MINIREDESOCIAL_FRONT ...`.
- Do NOT add dependencies. `lucide-react` (icons) and `react-router-dom` (`useNavigate`) are already present.
- No backend changes.
</execution_context>

<context>
@/home/maceloalexandre/Documentos/projetos/miniredesocial/CLAUDE.md

<interfaces>
<!-- Verified from the codebase — executor should use these directly. -->

Routing (src/App.jsx):
- Profile route: `/profile/:userId`
- Own profile is reached via the literal `/profile/me` (AppShell User tab); other users use their id.

User identity field names (as populated by the API):
- Avatar image src: `avatarUrl`
- Id on a populated user sub-doc: `_id` (sometimes `.id` on top-level profile objects)

Per-surface id sources (confirmed):
- PostCard:           `post.authorId._id`,  `post.authorId.name`,  `post.authorId.avatarUrl`
- PostDetailPage:     `comment.authorId._id`, `comment.authorId.name`, `comment.authorId.avatarUrl`
- NotificationCard:   `actorId._id`,        `actorId.name`,        `actorId.avatarUrl`
- ConversationListItem: `otherUser._id`,    `otherUser.name`,      `otherUser.avatarUrl`
- ConversationThreadPage profile card: `otherUser.id`, `otherUser.name`, `otherUser.avatarUrl`

EXISTING MODAL PATTERN to mirror — src/components/MessageBubble/MessageBubble.jsx (lightbox, lines 74-97) + MessageBubble.module.css (.lightbox / .lightboxClose / .lightboxImg, lines 103-140):
- Overlay: `position: fixed; inset: 0; background: rgba(0,0,0,0.85); z-index: 200;` flex-centered.
- Overlay `onClick` closes; the `<img>` has `onClick={(e) => e.stopPropagation()}` so clicks on the photo don't close.
- Close button top-right (`top/right: var(--space-md)`), 44x44 circle, `aria-label="Fechar..."`, renders an X (MessageBubble uses the `✕` glyph; you may use lucide `X` for consistency with NotificationCard).
- `role="dialog" aria-modal="true" aria-label="..."` on the overlay.

EXISTING stopPropagation precedent — src/components/PostCard/PostCard.jsx: the whole card is clickable (`handleCardClick` → `/posts/:id`); inner interactive elements already call `e.stopPropagation()` (see `handleCommentClick`, `handleLinkClick`). New avatar/name clicks MUST do the same.
</interfaces>
</context>

<tasks>

<task type="auto">
  <name>Task 1: Profile photo viewer modal on ProfilePage</name>
  <files>
    /home/maceloalexandre/Documentos/projetos/MINIREDESOCIAL_FRONT/src/pages/ProfilePage/ProfilePage.jsx
    /home/maceloalexandre/Documentos/projetos/MINIREDESOCIAL_FRONT/src/pages/ProfilePage/ProfilePage.module.css
  </files>
  <action>
Add a `photoOpen` boolean state (default false) to ProfilePage. Make the avatar open a full-size photo modal:
- For the NON-owner branch (currently a bare `<img className={styles.avatar}>`, ~line 182): wrap it so clicking the avatar sets `photoOpen=true`. Use a `<button type="button">` styled to look identical to the current avatar (no visible button chrome) so the layout is unchanged; add `aria-label="Ver foto de perfil"`.
- For the OWNER branch (currently a button that triggers the file picker to change the avatar, ~lines 168-180): do NOT hijack the existing change-photo behavior. The owner already taps to change their photo. Leave the owner avatar tap as-is (opens file picker). The photo-VIEW modal applies to the non-owner avatar tap. (Rationale: the constraint says clicking the profile avatar opens a viewer; for the owner the established behavior is "tap to change photo" — keep it consistent, do not regress it.)
- Render the modal overlay at the end of the component's returned JSX, only when `photoOpen` is true, mirroring the MessageBubble lightbox exactly: overlay div with `role="dialog" aria-modal="true" aria-label="Foto de perfil"` and `onClick={() => setPhotoOpen(false)}`; a close `<button>` top-right with lucide `X` icon and `aria-label="Fechar foto"` that also closes; and the `<img src={profile.avatarUrl} alt={profile.name}>` with `onClick={(e) => e.stopPropagation()}`. The modal must NOT navigate anywhere.

In ProfilePage.module.css add `.photoModalOverlay`, `.photoModalClose`, `.photoModalImg` copied from MessageBubble.module.css `.lightbox` / `.lightboxClose` / `.lightboxImg` (same fixed full-screen dark overlay, 44px circular close button top-right, contained image max 90vw/85vh). Also add an `.avatarViewBtn` rule (background:none; border:none; padding:0; cursor:pointer; line-height:0; display:block) so the wrapping button is visually invisible and the avatar img keeps its existing size/shape.

Import the lucide `X` icon (the file already imports `MessageCircle, LogOut, Camera` from 'lucide-react' — add `X`).
  </action>
  <verify>
    <automated>cd /home/maceloalexandre/Documentos/projetos/MINIREDESOCIAL_FRONT && grep -q "photoOpen" src/pages/ProfilePage/ProfilePage.jsx && grep -q "photoModalOverlay" src/pages/ProfilePage/ProfilePage.module.css && npm run build 2>&1 | tail -5</automated>
  </verify>
  <done>npm run build succeeds; ProfilePage renders a dark full-screen modal showing profile.avatarUrl when a non-owner avatar is clicked; X button and click-outside close it; no route change occurs. Committed in MINIREDESOCIAL_FRONT.</done>
</task>

<task type="auto">
  <name>Task 2: Clickable author in PostCard (feed) + comment authors in PostDetailPage</name>
  <files>
    /home/maceloalexandre/Documentos/projetos/MINIREDESOCIAL_FRONT/src/components/PostCard/PostCard.jsx
    /home/maceloalexandre/Documentos/projetos/MINIREDESOCIAL_FRONT/src/components/PostCard/PostCard.module.css
    /home/maceloalexandre/Documentos/projetos/MINIREDESOCIAL_FRONT/src/pages/PostDetailPage/PostDetailPage.jsx
    /home/maceloalexandre/Documentos/projetos/MINIREDESOCIAL_FRONT/src/pages/PostDetailPage/PostDetailPage.module.css
  </files>
  <action>
PostCard.jsx: In the author row (lines 45-55), make BOTH the avatar `<img>` and the name `<span>` navigate to `/profile/${post.authorId._id}` on click. Add a handler `handleAuthorClick(e)` that calls `e.stopPropagation()` then `navigate('/profile/' + post.authorId._id)` — stopPropagation is REQUIRED because the whole card already navigates to the post detail (matching existing `handleCommentClick`/`handleLinkClick`). Attach `onClick={handleAuthorClick}` to the avatar img and to the name span. Give the name span `role="link"`/`tabIndex={0}` and a keydown handler (Enter/Space → handleAuthorClick) for accessibility, OR wrap the name+avatar in `<button type="button">`s with no chrome — pick whichever keeps the existing `.authorRow` flex layout intact. Add CSS in PostCard.module.css: a `cursor: pointer` rule for the clickable avatar and name (e.g. `.authorLink { cursor: pointer; }`) without changing fonts/sizes.

PostDetailPage.jsx: In each comment row (lines 110-129), make the comment avatar `<img>` and the comment name `<span>` clickable, navigating to `/profile/${comment.authorId?._id}`. There is no card-level click here so stopPropagation is not strictly required, but only navigate when `comment.authorId?._id` exists (guard against null author). The page already imports `useNavigate` (line 2). Add a `cursor: pointer` style to the clickable comment avatar/name in PostDetailPage.module.css.
  </action>
  <verify>
    <automated>cd /home/maceloalexandre/Documentos/projetos/MINIREDESOCIAL_FRONT && grep -q "stopPropagation" src/components/PostCard/PostCard.jsx && grep -c "navigate(\`/profile/\|navigate('/profile/\|/profile/\${" src/components/PostCard/PostCard.jsx src/pages/PostDetailPage/PostDetailPage.jsx && npm run build 2>&1 | tail -5</automated>
  </verify>
  <done>npm run build succeeds; clicking a feed post's author avatar or name goes to that author's profile WITHOUT opening the post detail; clicking a commenter's avatar or name goes to their profile. Committed in MINIREDESOCIAL_FRONT.</done>
</task>

<task type="auto">
  <name>Task 3: Clickable actor in NotificationCard</name>
  <files>
    /home/maceloalexandre/Documentos/projetos/MINIREDESOCIAL_FRONT/src/components/NotificationCard/NotificationCard.jsx
    /home/maceloalexandre/Documentos/projetos/MINIREDESOCIAL_FRONT/src/components/NotificationCard/NotificationCard.module.css
  </files>
  <action>
NotificationCard.jsx: The whole card already has `role="button"` and an `onClick={handleClick}` that for `post_comment` navigates to `/posts/:refId`, for `message_received` to `/chat/:refId`, and otherwise to the actor profile. Make the actor avatar `<img>` (lines 105-109) AND the actor name `<strong className={styles.actorName}>` (line 112) ALWAYS navigate to the actor's profile regardless of notification type. Add `handleActorClick(e)` that calls `e.stopPropagation()` then `navigate('/profile/' + actorId._id)` (stopPropagation REQUIRED so the card-level handler doesn't also fire — same pattern as the existing dismiss button at lines 97-104 which already uses `e.stopPropagation()`). Attach to the avatar img and the actor name. Add keyboard accessibility on the name (tabIndex/role/keydown) or leave the name as a styled inline button — keep the existing `.actorName` bold styling. Add `cursor: pointer` for the clickable actor avatar/name in NotificationCard.module.css.
  </action>
  <verify>
    <automated>cd /home/maceloalexandre/Documentos/projetos/MINIREDESOCIAL_FRONT && grep -q "handleActorClick\|/profile/" src/components/NotificationCard/NotificationCard.jsx && grep -c "stopPropagation" src/components/NotificationCard/NotificationCard.jsx && npm run build 2>&1 | tail -5</automated>
  </verify>
  <done>npm run build succeeds; in the notifications list, tapping an actor avatar or actor name always opens that actor's profile, even for post_comment/message_received cards whose body navigates elsewhere. Committed in MINIREDESOCIAL_FRONT.</done>
</task>

<task type="auto">
  <name>Task 4: Clickable other-user in ConversationListItem + ConversationThreadPage header</name>
  <files>
    /home/maceloalexandre/Documentos/projetos/MINIREDESOCIAL_FRONT/src/components/ConversationListItem/ConversationListItem.jsx
    /home/maceloalexandre/Documentos/projetos/MINIREDESOCIAL_FRONT/src/components/ConversationListItem/ConversationListItem.module.css
    /home/maceloalexandre/Documentos/projetos/MINIREDESOCIAL_FRONT/src/pages/ConversationThreadPage/ConversationThreadPage.jsx
    /home/maceloalexandre/Documentos/projetos/MINIREDESOCIAL_FRONT/src/pages/ConversationThreadPage/ConversationThreadPage.module.css
  </files>
  <action>
ConversationListItem.jsx: The whole row is a `<Link to={`/chat/${conversation._id}`}>` (line 34). Make the avatar `<img>` (lines 35-39) and the name `<span className={styles.name}>` (line 41) navigate to `/profile/${otherUser._id}` INSTEAD of opening the chat, when clicked. Because the row is a `<Link>` (not a div with onClick), an inner click both navigates the Link AND any inner handler — so use `useNavigate` from react-router-dom and an `onClick` that calls `e.preventDefault()` AND `e.stopPropagation()` then `navigate('/profile/' + otherUser._id)`. (preventDefault stops the parent Link navigation; stopPropagation stops bubbling.) Add the `useNavigate` import. Add `cursor: pointer` styling for the clickable avatar/name (the row already shows pointer, so this is mostly to keep hit areas clear). Keep the existing layout.

ConversationThreadPage.jsx: In the top profile card (lines 276-282) the avatar img and `<p className={styles.profileName}>` are currently non-clickable (there is already a "Ver perfil" button below them). Make the avatar img and the name navigate to `/profile/${otherUser.id}` on click using the page's existing `navigate` (line 24). No stopPropagation needed (the profile card itself has no click handler). Add `cursor: pointer` for them in ConversationThreadPage.module.css. Leave the existing "Ver perfil" button untouched.

Self-link note: chat message bubbles (MessageBubble) render NO sender avatar/name, so there is no own-message self-link to worry about — do not add anything there.
  </action>
  <verify>
    <automated>cd /home/maceloalexandre/Documentos/projetos/MINIREDESOCIAL_FRONT && grep -q "preventDefault" src/components/ConversationListItem/ConversationListItem.jsx && grep -q "/profile/" src/components/ConversationListItem/ConversationListItem.jsx src/pages/ConversationThreadPage/ConversationThreadPage.jsx && npm run build 2>&1 | tail -5</automated>
  </verify>
  <done>npm run build succeeds; in the conversations list, tapping the other user's avatar or name opens their profile (not the chat); in a chat thread, tapping the other user's avatar or name in the top card opens their profile. Committed in MINIREDESOCIAL_FRONT.</done>
</task>

</tasks>

<verification>
After all 4 tasks:
- `cd /home/maceloalexandre/Documentos/projetos/MINIREDESOCIAL_FRONT && npm run build` succeeds with no errors.
- `git -C /home/maceloalexandre/Documentos/projetos/MINIREDESOCIAL_FRONT log --oneline -4` shows 4 new commits (one per task).
- Manual smoke (human, post-deploy): open a profile → tap avatar → full photo modal with X + click-outside close, no navigation. Tap author in feed → profile (post does not open). Tap commenter, notification actor, conversation-list user, chat-header user → each opens the correct profile.
</verification>

<success_criteria>
- Profile photo modal opens on avatar tap, closes on X or outside click, never navigates.
- Avatar + name are clickable → `/profile/:id` in feed, comments, notifications, conversation list, and chat header.
- Card-body / Link parents are not triggered by avatar/name clicks (stopPropagation / preventDefault applied where a clickable parent exists: PostCard, NotificationCard, ConversationListItem).
- No new dependencies; mobile-first; existing palette/CSS-module conventions preserved.
- 4 separate commits in the MINIREDESOCIAL_FRONT repo.
</success_criteria>

<output>
Create `/home/maceloalexandre/Documentos/projetos/miniredesocial/.planning/quick/260610-efc-add-profile-photo-modal-click-avatar-to-/260610-efc-SUMMARY.md` when done.
</output>
