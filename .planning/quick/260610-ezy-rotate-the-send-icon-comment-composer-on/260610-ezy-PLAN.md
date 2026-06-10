---
phase: quick-260610-ezy
plan: 01
type: execute
wave: 1
depends_on: []
files_modified:
  - /home/maceloalexandre/Documentos/projetos/MINIREDESOCIAL_FRONT/src/pages/PostDetailPage/PostDetailPage.jsx
  - /home/maceloalexandre/Documentos/projetos/MINIREDESOCIAL_FRONT/src/components/ChatComposer/ChatComposer.jsx
autonomous: true
requirements: [QUICK-260610-EZY]

must_haves:
  truths:
    - "The comment composer send button on PostDetailPage shows a horizontal (right-pointing) paper-plane icon"
    - "The chat composer send button shows the same horizontal (right-pointing) paper-plane icon"
    - "`npm run build` in MINIREDESOCIAL_FRONT succeeds with no unresolved-import errors"
  artifacts:
    - path: "/home/maceloalexandre/Documentos/projetos/MINIREDESOCIAL_FRONT/src/pages/PostDetailPage/PostDetailPage.jsx"
      provides: "Comment submit button using SendHorizontal"
      contains: "SendHorizontal"
    - path: "/home/maceloalexandre/Documentos/projetos/MINIREDESOCIAL_FRONT/src/components/ChatComposer/ChatComposer.jsx"
      provides: "Message send button using SendHorizontal"
      contains: "SendHorizontal"
  key_links:
    - from: "PostDetailPage.jsx import"
      to: "lucide-react SendHorizontal export"
      via: "named import"
      pattern: "import \\{[^}]*SendHorizontal[^}]*\\} from 'lucide-react'"
---

<objective>
Replace the `Send` icon (default ~45-60deg upward diagonal paper plane) with lucide-react's
`SendHorizontal` icon (paper plane drawn pointing horizontally to the right) in the two send
buttons: the comment composer on PostDetailPage and the chat message composer.

Purpose: The current send icon points up-and-to-the-right diagonally; the owner wants it
horizontal/sideways pointing right. `SendHorizontal` is the purpose-built icon for this — no
CSS rotation hacks needed.
Output: Two edited JSX files, one commit inside the MINIREDESOCIAL_FRONT repo.
</objective>

<execution_context>
@$HOME/.claude/get-shit-done/workflows/execute-plan.md
@$HOME/.claude/get-shit-done/templates/summary.md
</execution_context>

<context>
**CROSS-REPO TASK.** All edits and the commit happen in the SEPARATE sibling repo at
`/home/maceloalexandre/Documentos/projetos/MINIREDESOCIAL_FRONT` (NOT this repo, NOT a submodule).
There is NO worktree isolation — edit files in place on that repo's current `main` branch and
commit there with `git -C /home/maceloalexandre/Documentos/projetos/MINIREDESOCIAL_FRONT ...`.

Confirmed state (verified, do not re-research):
- `lucide-react` is installed and exports `SendHorizontal` (declared in
  `node_modules/lucide-react/dist/lucide-react.d.ts`). No dependency changes needed.
- No CSS rotate transform exists on these icons/buttons — nothing to remove.

PostDetailPage.jsx (current):
- Line 3: `import { Send, ArrowLeft } from 'lucide-react'`
- Line 181: `<Send size={20} aria-hidden="true" />`

ChatComposer.jsx (current):
- Line 2: `import { Camera, X, Send } from 'lucide-react'`
- Line 141: `{submitting ? <Spinner size={16} /> : <Send size={20} aria-hidden="true" />}`
</context>

<tasks>

<task type="auto">
  <name>Task 1: Swap Send -> SendHorizontal in both composers and commit</name>
  <files>
    /home/maceloalexandre/Documentos/projetos/MINIREDESOCIAL_FRONT/src/pages/PostDetailPage/PostDetailPage.jsx
    /home/maceloalexandre/Documentos/projetos/MINIREDESOCIAL_FRONT/src/components/ChatComposer/ChatComposer.jsx
  </files>
  <action>
    In PostDetailPage.jsx: change the import on line 3 from `{ Send, ArrowLeft }` to
    `{ SendHorizontal, ArrowLeft }`, and change the JSX usage on line 181 from
    `<Send size={20} aria-hidden="true" />` to `<SendHorizontal size={20} aria-hidden="true" />`.
    Preserve `size={20}` and `aria-hidden="true"` exactly.

    In ChatComposer.jsx: change the import on line 2 from `{ Camera, X, Send }` to
    `{ Camera, X, SendHorizontal }`, and change the JSX usage on line 141 from
    `<Send size={20} aria-hidden="true" />` to `<SendHorizontal size={20} aria-hidden="true" />`
    (inside the existing `submitting ? <Spinner size={16} /> : ...` ternary). Preserve the ternary,
    `size={20}`, and `aria-hidden="true"` exactly.

    Touch nothing else in either file — only these two import lines and two JSX usages.
    After editing, run the build verification, then stage and commit ONLY these two files inside
    the MINIREDESOCIAL_FRONT repo using `git -C /home/maceloalexandre/Documentos/projetos/MINIREDESOCIAL_FRONT`.
  </action>
  <verify>
    <automated>! grep -rn "\bSend\b" /home/maceloalexandre/Documentos/projetos/MINIREDESOCIAL_FRONT/src/pages/PostDetailPage/PostDetailPage.jsx /home/maceloalexandre/Documentos/projetos/MINIREDESOCIAL_FRONT/src/components/ChatComposer/ChatComposer.jsx | grep -v SendHorizontal | grep -E "from 'lucide-react'|<Send " && grep -c "SendHorizontal" /home/maceloalexandre/Documentos/projetos/MINIREDESOCIAL_FRONT/src/pages/PostDetailPage/PostDetailPage.jsx /home/maceloalexandre/Documentos/projetos/MINIREDESOCIAL_FRONT/src/components/ChatComposer/ChatComposer.jsx</automated>
    <automated>cd /home/maceloalexandre/Documentos/projetos/MINIREDESOCIAL_FRONT && npm run build</automated>
  </verify>
  <done>
    Both files import and render `SendHorizontal` (size 20, aria-hidden) in place of `Send`;
    no lingering `Send` named import or `<Send ` usage remains in either file; `npm run build`
    succeeds; one commit exists inside the MINIREDESOCIAL_FRONT repo containing exactly these two files.
  </done>
</task>

</tasks>

<verification>
- `grep "SendHorizontal"` matches in both files (import line + JSX usage = 2 hits each).
- No remaining `<Send ` JSX or `Send` named import from 'lucide-react' in either file.
- `npm run build` in MINIREDESOCIAL_FRONT exits 0.
- `git -C /home/maceloalexandre/Documentos/projetos/MINIREDESOCIAL_FRONT log -1 --stat` shows the two edited files in the latest commit.
</verification>

<success_criteria>
The send buttons on PostDetailPage's comment composer and the chat ChatComposer both display the
horizontal right-pointing paper-plane icon (`SendHorizontal`), the frontend builds cleanly, and a
single commit recording the change exists inside the MINIREDESOCIAL_FRONT repo.
</success_criteria>

<output>
Create `.planning/quick/260610-ezy-rotate-the-send-icon-comment-composer-on/260610-ezy-SUMMARY.md` when done.
</output>
