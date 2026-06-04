# Phase 4: Posts, Comments & Personalized Feed - Context

**Gathered:** 2026-06-04
**Status:** Ready for planning

<domain>
## Phase Boundary

O loop central se completa — usuários criam posts categorizados (texto/imagens/link por role),
comentam em posts de outros, e veem um feed personalizado e paginado das pessoas que seguem.
A aba Feed (placeholder na Phase 3) é ativada. A AppShell é atualizada: aba de Notificações
sai da bottom nav e entra no header; aba de Criar post (+) ocupa o terceiro slot.

Requirements: CONT-01, CONT-02, CONT-03, CONT-04, CONT-05, FEED-01, FEED-02, FEED-03, FRNT-04

In scope: modelos Post e Comment; endpoints de criação de post (multipart/form-data + S3),
listagem de feed (cursor-based, paginado), criação de comentário, listagem de comentários;
frontend: FeedPage, PostDetailPage, ComposePage, PostCard; atualização da AppShell (nav + header).

Out of scope: likes em posts (v2); notificações frontend (Phase 5); paginação de comentários
(deferido); edição/deleção de posts.

</domain>

<decisions>
## Implementation Decisions

### Feed scroll behavior
- **D-01:** Feed usa **infinite scroll** — `IntersectionObserver` detecta quando o usuário se
  aproxima do final da lista e dispara o fetch da próxima página automaticamente.
- **D-02:** Paginação **cursor-based** — API aceita `?cursor=<lastPostId>&limit=20`. Backend
  usa `_id < cursor` para buscar a página seguinte. Garante estabilidade quando novos posts
  são inseridos durante o scroll.
- **D-03:** **Pull-to-refresh** disponível no feed — usuário puxa para baixo no topo da lista
  para recarregar do início (reseta o cursor para `null` e re-fetcha a primeira página).

### Post image upload
- **D-04:** Máximo de **4 imagens por post**.
- **D-05:** Upload via **server-proxy** (multipart/form-data) — o cliente envia um único
  `POST /api/posts` com campos de texto + imagens no mesmo request. O servidor faz o upload
  para S3 e só então salva o documento Post. Consistente com o padrão `uploadAvatar` já
  existente em `src/services/s3.service.js`.
- **D-06:** Campo **`linkUrl` explícito** no compositor — input dedicado para URL. Backend
  define `hasLink: true` quando `linkUrl` é não-vazio. Roles sem permissão (`aluno`) recebem
  403 (CONT-03). Detecção de link não é feita por parsing do texto livre.

### Post card & comment entry
- **D-07:** Card no feed exibe: **avatar + nome do autor**, **texto do post**, **thumbnails
  de imagens** (até 4), **chips de categorias**, **botão com contagem de comentários**. Sem
  preview inline de comentários no feed — contagem abre a tela de detalhes.
- **D-08:** Comentários abrem em **tela dedicada `/posts/:id`** — thread completa com input
  fixo no rodapé para escrever um novo comentário. Navegação: tap no post card ou no botão
  de comentários vai para essa rota.
- **D-09:** Tela `/posts/:id` carrega **todos os comentários de uma vez** — sem paginação
  de comentários no MVP. Pode ser adicionada em fase futura se o volume crescer.

### Compositor & atualização da bottom nav
- **D-10:** **Bottom nav atualizada** (sobrescreve D-01 da Phase 3):
  - Antes: Feed / Busca / **Notif** / Perfil
  - Agora: Feed / Busca / **+ (Criar post)** / Perfil
  - Ícone de Notificações sai da nav e vai para o **header** (bell icon ao lado do wordmark
    ou do botão "Sair"). Phase 5 conectará esse ícone ao centro de notificações.
- **D-11:** `AppShell.jsx` é atualizado nesta fase para refletir a nova nav e adicionar o
  bell icon de notificações no header.
- **D-12:** Tap no **+** navega para `/compose` — rota dedicada, tela cheia com compositor
  completo (texto, upload de até 4 imagens, campo de link, seleção de categorias). Após
  publicar com sucesso, redireciona de volta ao feed.

### Claude's Discretion
- Estrutura interna dos componentes React (PostCard, FeedPage, ComposePage, PostDetailPage)
- Nome dos campos na resposta da API e estrutura do payload de criação de post
- Tratamento de loading states e error boundaries nas telas
- Estratégia de cache/invalidação do feed após publicar um post
- Key do S3 para imagens de post (sugestão: `posts/<uuid>.<ext>`)

</decisions>

<canonical_refs>
## Canonical References

**Downstream agents MUST read these before planning or implementing.**

### Domain spec (autoritativo)
- `CONTEXT.md` (raiz do repo) — schemas completos de `posts` e `comments` (incluindo campos
  `imageUrls`, `categoryIds`, `hasLink`, `linkUrl`), regras de permissão por role, regras de
  feed e bloqueio. **Fonte de verdade para os modelos Mongoose.**
- `erd_mini_rede_social.html` (raiz do repo) — ERD visual do banco

### Foundations (fases anteriores — reusar, não recriar)
- `.planning/phases/03-profiles-social-graph/03-CONTEXT.md` — padrões de AppShell (D-01
  sobrescrito por D-10 desta fase), CSS Modules, multi-tenant scoping, padrão de controllers
- `.planning/phases/02-entry-loop-invite-register-login/02-CONTEXT.md` — axios interceptor
  (D-17), JWT em localStorage (D-14), CSS Modules (D-18), React + Vite, React Router
- `src/middlewares/auth.middleware.js` — `authenticateToken` (obrigatório em todas as rotas)
- `src/middlewares/role.middleware.js` — `requireRole` (para bloqueio de `aluno` em link posts)
- `src/services/s3.service.js` — padrão `uploadAvatar` a ser estendido para `uploadPostImage`
- `src/services/notification.service.js` — `create('post_comment', ...)` ao comentar
- `src/models/Category.js` — modelo Category existente (type: 'content' | 'area')
- `src/models/Follow.js` — usado na query de feed (followingIds com status='accepted')
- `src/models/Block.js` — usado para filtrar ambas as direções no feed (FEED-02)
- `MINIREDESOCIAL_FRONT/src/components/AppShell/AppShell.jsx` — atualizar: tab 3 → +;
  bell icon no header
- `MINIREDESOCIAL_FRONT/src/lib/api.js` — axios instance com interceptor JWT

### Requirements & roadmap
- `.planning/REQUIREMENTS.md` — CONT-01..05, FEED-01..03, FRNT-04
- `.planning/ROADMAP.md` — Phase 4 goal + 5 success criteria

</canonical_refs>

<code_context>
## Existing Code Insights

### Reusable Assets (backend)
- `src/services/s3.service.js` → função `uploadAvatar` como modelo para `uploadPostImage`
  (mesmo padrão, prefixo diferente: `posts/<uuid>.<ext>`); `buildObjectUrl` reutilizável
- `src/services/notification.service.js` → `create('post_comment', actorId, postAuthorId,
  commentId, 'Comment')` ao criar um comentário
- `src/models/Notification.js` → enum já inclui `post_comment` — sem alteração necessária
- `src/middlewares/auth.middleware.js` + `src/middlewares/role.middleware.js` — aplicar em
  todas as rotas de posts e comentários
- `src/controllers/*.controller.js` + `src/routes/*.routes.js` — padrão estabelecido de
  controller/router; Post e Comment seguem a mesma estrutura
- `mongodb-memory-server` test pattern (ver `follow.controller.test.js`) — reusar para
  post e comment controllers

### Reusable Assets (frontend)
- `PrimaryButton`, `FormField`, `ErrorBanner`, `Spinner` — reusar no compositor e na tela de
  comentários
- `PhotoUpload.jsx` — referência para o seletor de imagens no compositor (adaptação)
- `UserResultCard.jsx` — referência de card component (PostCard será similar em estrutura)
- CSS Modules co-localizados — continua como padrão

### Established Patterns
- Multi-tenant: todo query filtra por `req.user.institutionId` (PLAT-01)
- `req.user` injetado pelo `authenticateToken` (contém `_id`, `institutionId`, `role`)
- Feed query pattern: buscar followingIds onde `status='accepted'` no modelo Follow, depois
  buscar posts cujo `authorId` está nessa lista, excluindo autores bloqueados em qualquer
  direção (Join com Block model)
- Pasta layout: `src/{models,routes,controllers,services}` — Post.js, Comment.js em models;
  post.routes.js, comment.routes.js em routes
- Frontend: pages em `pages/`, componentes em `components/`, CSS Module co-localizado

### Integration Points
- `src/app.js` — montar `post.routes` e `comment.routes`
- `MINIREDESOCIAL_FRONT/src/App.jsx` — ativar rota `/feed`, adicionar `/posts/:id` e
  `/compose`; passar por `AppShell`
- `MINIREDESOCIAL_FRONT/src/components/AppShell/AppShell.jsx` — atualizar tab 3 de Notif
  para + (Compose), adicionar bell icon de notificações no header

</code_context>

<specifics>
## Specific Ideas

- **Feed query:** `Follow.find({ followerId: req.user._id, status: 'accepted' })` → array de
  followingIds; `Block.find({ $or: [...] })` → array de blockedIds em ambas as direções;
  `Post.find({ authorId: { $in: followingIds, $nin: blockedIds }, _id: { $lt: cursor } })`
  com `.sort({ _id: -1 }).limit(21)` (fetch 21 para detectar hasMore)
- **Infinite scroll:** `IntersectionObserver` em um elemento sentinela no final da lista —
  quando visível e `hasMore === true`, dispara fetch da próxima página com o cursor do
  último post
- **Pull-to-refresh:** evento `touchstart`/`touchmove`/`touchend` ou lib leve (verificar se
  há algo instalado antes de adicionar nova dependência)
- **Bottom nav update:** tab index 2 (Notif) muda para ícone + rota `/compose`; bell icon
  no header com badge de contagem (badge fica como `0` ou sem badge nesta fase — Phase 5
  conecta o número real)
- **Compositor:** após `POST /api/posts` com sucesso → `navigate('/feed')` e invalidar/reset
  o cursor do feed para recarregar do topo

</specifics>

<deferred>
## Deferred Ideas

- **Paginação de comentários** — carregamento de todos os comentários de uma vez é MVP;
  paginação pode ser adicionada em fase futura se o volume crescer.
- **Likes em posts** (`post_like` notification type) — ENGM-01, v2 backlog.
- **Edição e deleção de posts** — não está no scope da Phase 4; pode entrar em refinamento
  de conteúdo futuro.
- **Badge de notificações no header bell icon** — ícone aparece nesta fase mas o número de
  não-lidas só é conectado na Phase 5 (polling de notificações).

None — discussion stayed within phase scope.

</deferred>

---

*Phase: 4-posts-comments-personalized-feed*
*Context gathered: 2026-06-04*
