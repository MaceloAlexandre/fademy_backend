# CLAUDE.md

Behavioral guidelines to reduce common LLM coding mistakes. Merge with project-specific instructions as needed.

**Tradeoff:** These guidelines bias toward caution over speed. For trivial tasks, use judgment.

## 1. Think Before Coding

**Don't assume. Don't hide confusion. Surface tradeoffs.**

Before implementing:

- State your assumptions explicitly. If uncertain, ask.
- If multiple interpretations exist, present them - don't pick silently.
- If a simpler approach exists, say so. Push back when warranted.
- If something is unclear, stop. Name what's confusing. Ask.

## 2. Simplicity First

**Minimum code that solves the problem. Nothing speculative.**

- No features beyond what was asked.
- No abstractions for single-use code.
- No "flexibility" or "configurability" that wasn't requested.
- No error handling for impossible scenarios.
- If you write 200 lines and it could be 50, rewrite it.

Ask yourself: "Would a senior engineer say this is overcomplicated?" If yes, simplify.

## 3. Surgical Changes

**Touch only what you must. Clean up only your own mess.**

When editing existing code:

- Don't "improve" adjacent code, comments, or formatting.
- Don't refactor things that aren't broken.
- Match existing style, even if you'd do it differently.
- If you notice unrelated dead code, mention it - don't delete it.

When your changes create orphans:

- Remove imports/variables/functions that YOUR changes made unused.
- Don't remove pre-existing dead code unless asked.

The test: Every changed line should trace directly to the user's request.

## 4. Goal-Driven Execution

**Define success criteria. Loop until verified.**

Transform tasks into verifiable goals:

- "Add validation" → "Write tests for invalid inputs, then make them pass"
- "Fix the bug" → "Write a test that reproduces it, then make it pass"
- "Refactor X" → "Ensure tests pass before and after"

For multi-step tasks, state a brief plan:

```

1. [Step] → verify: [check]
2. [Step] → verify: [check]
3. [Step] → verify: [check]

```

Strong success criteria let you loop independently. Weak criteria ("make it work") require constant clarification.

---

**These guidelines are working if:** fewer unnecessary changes in diffs, fewer rewrites due to overcomplication, and clarifying questions come before implementation rather than after mistakes.

<!-- GSD:project-start source:PROJECT.md -->

## Project

**Fademy**

Fademy is a mobile-first, invite-only mini social network for educational institutions — built first for the **Faculdade Católica da Paraíba** but multi-tenant from day one. Students, professors, staff, and coordinators join by invitation, build profiles, post content (text/images/links) tagged by category, follow each other, comment, get in-app notifications, and chat 1:1 or in groups. It ships as an Express/MongoDB REST API plus a React web frontend, and is intended to go to production on the owner's VPS.

**Core Value:** A student opens Fademy on their phone, gets in via an invite, and immediately sees a personalized feed of posts from people they follow — and can post their own. The invite→register→post→feed loop is the heart; if that works flawlessly on mobile, Fademy feels real.

### Constraints

- **Tech stack (backend)**: Node.js, Express, MongoDB, Mongoose, dotenv, JWT — fixed by the existing spec.
- **Tech stack (frontend)**: React, mobile-first — chosen by owner; native app is a later milestone.
- **Design**: Color palette dark blue, black, beige, tobacco brown — owner-specified brand for Fademy.
- **Real-time**: Socket.io for chat messages; polling for notifications — balances demo impact against the owner's real-time experience and the 3-week window.
- **Media**: Uploads to S3 (owner has a bucket).
- **Deployment**: VPS owned by the user.
- **Timeline**: ~3 weeks until institutional presentation — slice work as vertical MVP so there's always a deployable, demo-able product; highest-risk features (chat) land last.
- **Multi-tenancy**: institution scoping required from day one, even though only Católica PB seeds initially.

<!-- GSD:project-end -->

<!-- GSD:stack-start source:STACK.md -->

## Technology Stack

Technology stack not yet documented. Will populate after codebase mapping or first phase.
<!-- GSD:stack-end -->

<!-- GSD:conventions-start source:CONVENTIONS.md -->

## Conventions

Conventions not yet established. Will populate as patterns emerge during development.
<!-- GSD:conventions-end -->

<!-- GSD:architecture-start source:ARCHITECTURE.md -->

## Architecture

Architecture not yet mapped. Follow existing patterns found in the codebase.
<!-- GSD:architecture-end -->

<!-- GSD:skills-start source:skills/ -->

## Project Skills

No project skills found. Add skills to any of: `.claude/skills/`, `.agents/skills/`, `.cursor/skills/`, `.github/skills/`, or `.codex/skills/` with a `SKILL.md` index file.
<!-- GSD:skills-end -->

<!-- GSD:workflow-start source:GSD defaults -->

## GSD Workflow Enforcement

Before using Edit, Write, or other file-changing tools, start work through a GSD command so planning artifacts and execution context stay in sync.

Use these entry points:

- `/gsd:quick` for small fixes, doc updates, and ad-hoc tasks
- `/gsd:debug` for investigation and bug fixing
- `/gsd:execute-phase` for planned phase work

Do not make direct repo edits outside a GSD workflow unless the user explicitly asks to bypass it.
<!-- GSD:workflow-end -->

<!-- GSD:profile-start -->

## Developer Profile

> Profile not yet configured. Run `/gsd:profile-user` to generate your developer profile.
> This section is managed by `generate-claude-profile` -- do not edit manually.
<!-- GSD:profile-end -->
