# Phase 3: Profiles & Social Graph - Context

**Gathered:** 2026-06-03
**Status:** Ready for planning

<domain>
## Phase Boundary

Usuários encontram outros usuários, visitam perfis, controlam privacidade e gerem o grafo
social (follow, cancel follow, request follow, block) — tudo acessível pelo mobile. Entrega
também a shell de navegação autenticada (bottom nav + header scroll-aware) que todas as
fases seguintes aproveitam.

Requirements: PROF-01, PROF-02, PROF-03, SOCL-01, SOCL-02, SOCL-03, SOCL-04, SOCL-05, FRNT-05.

In scope: User model já tem `isPrivate`; criar modelos Follow e Block; endpoints de perfil,
follow, unfollow, cancel-request, accept-request, reject-request, block, search; tela de
perfil com controles; busca de usuários; shell de navegação.

Out of scope: frontend de aceitar/rejeitar solicitações (Phase 5 via notificações); posts e
feed (Phase 4); chat e grupos (Phase 6).

</domain>

<decisions>
## Implementation Decisions

### Navegação pós-login (shell autenticada)
- **D-01:** Shell autenticada usa **bottom nav bar** com 4 abas fixas: **Feed / Busca / Notif / Perfil** —
  padrão mobile (Instagram/TikTok). Phase 3 entrega Busca e Perfil ativos; Feed e Notif ficam
  como placeholders até as phases 4 e 5, respectivamente.
- **D-02:** **Header scroll-aware**: esconde ao rolar para baixo (content reading), reaparece ao rolar
  para cima — independente de quão longe está do topo. Não usa `position: sticky` simples;
  requer lógica de scroll direction detection no React.
- **D-03:** **Logout fica no header**, não na aba de Perfil. Header exibe wordmark "Fademy" + botão "Sair"
  (mantendo o padrão já estabelecido em `PostLoginShell`).
- **D-04:** O componente `PostLoginShell` atual em `App.jsx` é **substituído** pela nova `AuthShell`
  com bottom nav + header scroll-aware. As rotas autenticadas passam a ser aninhadas nessa
  nova shell.

### Visibilidade do perfil privado
- **D-05:** Perfil privado visitado por **não-seguidor**: exibe apenas nome, avatarUrl e botão
  "Solicitar follow". Posts, bio e contadores ficam ocultos. (`GET /api/users/:id` retorna
  dados reduzidos quando `isPrivate=true` e o visitante não tem follow `accepted`.)
- **D-06:** Quando há solicitação pendente (follow com `status: pending`): botão exibe
  **"Solicitação enviada"** com opção de **cancelar** (DELETE no follow pending).
- **D-07:** **Aceitar/rejeitar solicitações de follow** (SOCL-03): a API backend (`PATCH
  /api/follows/:id/accept` e `PATCH /api/follows/:id/reject`) é implementada em Phase 3,
  mas a **interface frontend** de aceitar/rejeitar fica para Phase 5 (via centro de
  notificações). A tela de Perfil no Phase 3 **não** exibe lista de requests pendentes para
  o próprio dono.

### Busca de usuários (PROF-03)
- **D-08:** Busca scoped para **mesma `institutionId`** do usuário logado — alinhado com o
  modelo multi-tenant. Usuários de outras instituições não aparecem nos resultados.
- **D-09:** Query por **nome parcial, case-insensitive** (`name` field) via MongoDB regex
  (`new RegExp(query, 'i')`). Resultados **excluem** usuários que bloquearam o pesquisador
  ou foram bloqueados por ele (SOCL-05 / PROF-03).
- **D-10:** Resultados **paginados** — API aceita `?q=&page=1&limit=20`. Frontend usa
  paginação simples (não infinite scroll nessa fase).
- **D-11:** UX da busca: **aba "Busca" no bottom nav** com barra de pesquisa. Usuário toca
  na aba, digita, resultados aparecem na mesma tela (sem nova rota). Debounce de ~300ms.

### Controles de follow no perfil (FRNT-05)
- **D-12:** Botão de follow/block no profile view reflete o estado atual via query ao backend:
  - `null` (não segue) + perfil público → "Seguir"
  - `null` + perfil privado → "Solicitar"
  - `pending` → "Solicitação enviada" + opção cancelar
  - `accepted` → "Seguindo" + opção unfollow
  - Bloqueado (blocker ou blocked) → nenhum botão; usuário bloqueado não vê o perfil do
    bloqueador
- **D-13:** Block flow: botão "Bloquear" visível no perfil de outro usuário (fora do estado
  bloqueado). Bloquear remove os follows bilaterais automaticamente (SOCL-04) — tratado na
  controller, não em middleware separado nessa fase.

### Claude's Discretion
- Estrutura interna dos componentes React do profile view e da busca
- Naming dos endpoints (sugerido: `/api/users/:id`, `/api/users/:id/follow`,
  `/api/users/:id/block`, `/api/users/search?q=`)
- Estrutura de resposta da API para o estado do follow (retornar junto com GET /users/:id ou
  endpoint separado `/api/users/:id/follow-status`)
- Tratamento de erro e loading states nos componentes

</decisions>

<canonical_refs>
## Canonical References

**Downstream agents MUST read these before planning or implementing.**

### Domain spec (autoritativo)
- `CONTEXT.md` (raiz do repo) — schemas completos de `follows` e `blocks`, regras de negócio
  de privacidade, follow, block e notificações. **Tratar como fonte de verdade para os modelos
  Mongoose.**
- `erd_mini_rede_social.html` (raiz do repo) — ERD visual do banco de dados

### Foundations (Phase 1 + 2 — reusar, não recriar)
- `.planning/phases/02-entry-loop-invite-register-login/02-CONTEXT.md` — decisões locked:
  axios interceptor (D-17), JWT em localStorage (D-14), CSS Modules (D-18), React + Vite,
  React Router
- `src/middlewares/auth.middleware.js` — `authenticateToken` (reuse em todas as rotas)
- `src/middlewares/role.middleware.js` — `requireRole` factory
- `src/models/User.js` — modelo User com `isPrivate: Boolean` já presente
- `src/config/env.js` — padrão de config (adicionar vars se necessário)
- `MINIREDESOCIAL_FRONT/src/App.jsx` — estrutura atual de rotas (a substituir pelo novo shell)
- `MINIREDESOCIAL_FRONT/src/lib/api.js` — axios instance com interceptor JWT

### Requirements & roadmap
- `.planning/REQUIREMENTS.md` — PROF-01..03, SOCL-01..05, FRNT-05
- `.planning/ROADMAP.md` — Phase 3 goal + 5 success criteria

</canonical_refs>

<code_context>
## Existing Code Insights

### Reusable Assets (backend)
- `authenticateToken` middleware — aplica em todas as rotas de perfil/follow/block/search
- `requireRole(...)` — não necessário nesta fase (todas as ações são do próprio usuário)
- Padrão de controller: `src/controllers/*.controller.js` + `src/routes/*.routes.js`
- `mongodb-memory-server` test pattern — reusar para Follow e Block controllers

### Reusable Assets (frontend)
- `PrimaryButton`, `FormField`, `ErrorBanner`, `Spinner` — reusar nos novos componentes
- `AuthShell` (componente wrapper das telas de auth) — não é o mesmo que a nova shell de
  navegação; nomes diferentes necessários
- CSS Modules por componente — continua como padrão

### Established Patterns
- Multi-tenant: todo query filtra por `req.user.institutionId` (PLAT-01)
- `req.user` injetado pelo `authenticateToken` middleware (contém `_id`, `institutionId`, `role`)
- Folder layout: `src/{models,routes,controllers,services}` — Follow.js, Block.js entram em models;
  follow.routes.js, block.routes.js em routes; etc.
- Frontend: pages em `pages/`, componentes em `components/`, CSS Module co-localizado

### Integration Points
- `src/app.js` — novos routers montados aqui (follow.routes, block.routes, user.routes para search/profile)
- `MINIREDESOCIAL_FRONT/src/App.jsx` — PostLoginShell substituído por nova AppShell com bottom nav;
  novas rotas `/search`, `/profile/:userId`, `/profile/me` aninhadas na AppShell

</code_context>

<specifics>
## Specific Ideas

- Scroll-aware header: detectar direção do scroll (via `useRef` + `useEffect` listener) para
  adicionar/remover classe CSS que controla `transform: translateY(-100%)` com `transition`.
  Independe de proximidade com o topo — só a direção do scroll importa.
- Bottom nav: ícones simples (sem lib externa se possível), fundo dark blue `#083C5D`,
  aba ativa com cor tobacco brown `#8C6E4F`.
- Cancelar solicitação pendente: botão "Solicitação enviada" abre confirmação simples antes
  de DELETE.

</specifics>

<deferred>
## Deferred Ideas

- **Interface de aceitar/rejeitar requests no perfil** — fica para Phase 5 (via notificações).
  A API backend para accept/reject é criada agora, o frontend é Phase 5.
- **Block middleware centralizado** (`block.middleware.js`) para verificar bloqueios antes de
  posts/chat — o check em controllers cobre esta fase; middleware centralizado pode ser
  extraído em Phase 4/6.
- **Notificações de follow** (`new_follower`, `follow_request`, `follow_accepted`) — os
  registros podem ser criados no banco nesta fase via service stub, mas a decisão foi
  deixada em aberto (não foi discutida — Claude decide: **criar os registros** para que Phase 5
  já tenha dados; custo mínimo, retorno alto).

None — discussion stayed within phase scope.

</deferred>

---

*Phase: 3-profiles-social-graph*
*Context gathered: 2026-06-03*
