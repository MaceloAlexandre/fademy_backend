# Phase 5: Notifications (Polling) - Context

**Gathered:** 2026-06-07
**Status:** Ready for planning

<domain>
## Phase Boundary

Usuários recebem e gerenciam notificações in-app para eventos sociais (follow, comentário,
convite usado), disparadas por um serviço de notificação centralizado (já existe e já é usado
em Phase 3/4 para `follow_request`, `follow_accepted`, `new_follower`, `post_comment`) e
exibidas no mobile via polling. O ícone de sino no header (adicionado inerte na Phase 4,
D-10/D-11) é conectado a uma tela de notificações, com badge de contagem de não-lidas.

Requirements: NOTF-01, NOTF-02, NOTF-03, NOTF-04, FRNT-06

In scope: endpoint de listagem de notificações (paginado), endpoint de marcar como lida
(individual e via ação inline), wiring do tipo `invite_received` (ainda não disparado em
nenhum lugar); frontend: NotificationsPage, NotificationCard, conexão do bell icon + badge,
hook de polling.

Out of scope: `post_like` (ENGM-01, v2); `message_received` — o tipo já existe no enum do
model `Notification`, mas seu disparo só faz sentido quando o chat existir (Phase 6) — essa
fase não precisa "forçar" um disparo sem evento real. Notificações push/email (fora do
escopo do produto). Agrupamento por data na lista (MVP usa lista simples).

</domain>

<decisions>
## Implementation Decisions

### Exibição e navegação da lista de notificações
- **D-01:** Cada card de notificação mostra **avatar + nome do ator + texto da ação +
  timestamp** — espelha o estilo já estabelecido em `PostCard`/`UserResultCard`, mantendo
  consistência visual.
- **D-02:** Tocar em uma notificação **navega para o conteúdo relacionado** (tela de detalhe
  do post para `post_comment`, perfil do usuário para tipos relacionados a follow/convite)
  **e marca como lida** na mesma ação — padrão mobile padrão (Instagram/Twitter).
- **D-03:** Notificações **não lidas** recebem **fundo destacado + indicador de ponto**;
  lidas ficam no estilo padrão — permite escaneamento rápido.
- **D-04:** Lista em **ordem cronológica reversa simples** (mais recentes primeiro), sem
  agrupamento por data — consistente com o padrão do feed, mais simples de implementar e de
  fazer merge com resultados de polling.

### Ações de solicitação de follow (resolve a decisão adiada da Phase 3 — D-07 lá)
- **D-05:** Notificações do tipo `follow_request` exibem **botões inline de Aceitar/Rejeitar**
  diretamente no card — sem precisar sair da tela de notificações. Os endpoints backend
  `PATCH /api/follows/:id/accept` e `PATCH /api/follows/:id/reject` **já existem** desde a
  Phase 3 (D-07 lá).
- **D-06:** Após aceitar/rejeitar, **o card é atualizado in-place** — os botões são
  substituídos por um rótulo de status ("Aceito" / "Rejeitado"); a notificação permanece na
  lista como registro histórico (não desaparece).
- **D-07:** Aceitar ou rejeitar a partir da notificação **também marca a notificação como
  lida** — a ação é um sinal mais forte que a simples visualização; evita rastrear dois
  estados independentes.

### Polling e badge de não-lidas
- **D-08:** Intervalo de polling de **5 segundos** — escolha explícita do usuário,
  priorizando responsividade em demonstrações práticas sobre redução de carga no servidor
  ("precisa ser mais curto, coisa de 5 segundos, para demonstrações práticas").
- **D-09:** Polling ativo **em todo o app, sempre que aberto** (não só na tela de
  notificações) — mantém o badge do bell icon atualizado em qualquer tela.
- **D-10:** O bell icon do header exibe **badge numérico com contagem de não-lidas** (ex:
  "3", "9+") — conecta o ícone que ficou inerte desde a Phase 4 (`onClick={() => {}}`,
  D-10/D-11 daquela fase).

### Disparo de `invite_received` (recém-definido — spec original era ambígua)
- **D-11:** Convites são **links de token compartilháveis e anônimos** (`createInvite` não
  recebe `invitedUserId` — qualquer um com o link pode se cadastrar). Não existe um
  destinatário conhecido no momento da criação do convite. Por isso, o gatilho foi
  **redefinido**: `invite_received` dispara quando **alguém se cadastra usando o link de
  convite**, e o **destinatário da notificação é quem criou o convite** (`invite.invitedById`)
  — ex: "[Nome] se cadastrou usando seu convite". É o único momento do fluxo de convite com
  dois usuários reais envolvidos.
- **D-12:** `refId`/`refModel` apontam para **o novo usuário** (`refModel: 'User'`) — mantém
  o padrão de tocar-para-navegar (D-02) consistente: tocar na notificação leva ao perfil do
  novo membro.
- **Ponto de disparo:** fluxo de aceitação de convite/registro (`registration.controller.js`
  ou onde o `Invite` é resolvido) — precisa ler `invite.invitedById` para identificar o
  destinatário da notificação.

### Esclarecimento de escopo — `message_received`
- O tipo `message_received` já está no enum do model `Notification` (nenhuma alteração de
  schema necessária). Seu disparo real só ocorrerá naturalmente na **Phase 6** quando o chat
  existir — essa fase **não** precisa criar um evento artificial só para popular esse tipo.
  Isso esclarece a aparente contradição no critério de sucesso #3 do roadmap (lista
  `message_received` entre os tipos que "geram notificações quando esses eventos ocorrem").

### Claude's Discretion
- Estrutura interna dos componentes React (`NotificationsPage`, `NotificationCard`)
- Estratégia de paginação do endpoint `GET /api/notifications` (cursor-based, espelhando o
  padrão do feed da Phase 4 — D-02 lá — ou paginação simples)
- Design do endpoint de marcar como lida (PATCH individual vs. endpoint de "marcar todas
  como lidas")
- Limite de exibição do badge (ex: "9+" acima de 9) e seu valor exato
- Hook de polling: implementação (setInterval vs. lib), local de montagem (nível AppShell
  para ficar disponível em todas as telas — ver D-09)
- Tratamento de loading/error states na tela de notificações

</decisions>

<canonical_refs>
## Canonical References

**Downstream agents MUST read these before planning or implementing.**

### Domain spec (autoritativo)
- `CONTEXT.md` (raiz do repo) §"Notificações" (linhas ~99-112) — tipos sugeridos e regras
  (`read` flag, padrão polimórfico `refId`/`refModel`, somente in-app)
- `CONTEXT.md` (raiz do repo) §schema `notifications` (linhas ~234-247) — schema autoritativo
  do model
- `erd_mini_rede_social.html` (raiz do repo) — ERD visual do banco

### Foundations (fases anteriores — reusar, não recriar)
- `.planning/phases/04-posts-comments-personalized-feed/04-CONTEXT.md` — D-10/D-11: plano de
  conectar o bell icon nesta fase; padrões de AppShell, CSS Modules
- `.planning/phases/03-profiles-social-graph/03-CONTEXT.md` — D-07: decisão original de
  adiar a UI de aceitar/rejeitar follow request para esta fase (resolvida em D-05..D-07 acima)
- `src/services/notification.service.js` — `create(type, actorId, userId, refId, refModel)`
  já implementado e usado em `follow.controller.js` e `comment.controller.js`. **Reusar, não
  recriar — NOTF-02 já está satisfeito pelo padrão existente.**
- `src/models/Notification.js` — model já existe com schema completo e índice
  `{ userId, read, createdAt }`. **NOTF-01 já está satisfeito — sem alterações de schema.**
- `src/controllers/follow.controller.js` — `acceptFollow`/`rejectFollow` (PATCH
  `/api/follows/:id/accept|reject`) já implementados desde Phase 3; reusar para D-05
- `src/middlewares/auth.middleware.js` — `authenticateToken` (obrigatório em todas as rotas)
- `MINIREDESOCIAL_FRONT/src/components/AppShell/AppShell.jsx` — bell icon já presente
  (`<Bell>` do lucide-react, `onClick={() => {}}`, `aria-label="Notificações"`) — conectar
  navegação + badge aqui (D-10)
- `MINIREDESOCIAL_FRONT/src/lib/api.js` — axios instance com interceptor JWT

### Requirements & roadmap
- `.planning/REQUIREMENTS.md` — NOTF-01..04, FRNT-06
- `.planning/ROADMAP.md` — Phase 5 goal + 4 success criteria

</canonical_refs>

<code_context>
## Existing Code Insights

### Reusable Assets (backend)
- `src/services/notification.service.js` → `create()` — já em uso; basta chamar com o tipo
  `invite_received` no ponto de disparo certo (D-11)
- `src/models/Notification.js` → schema e índice já prontos para queries de listagem
  ordenadas por `createdAt` e filtradas por `userId`/`read`
- `src/controllers/follow.controller.js` → `acceptFollow`/`rejectFollow` — endpoints prontos
  para D-05 (botões inline)
- `src/controllers/*.controller.js` + `src/routes/*.routes.js` — padrão estabelecido de
  controller/router a seguir para `notification.controller.js`/`notification.routes.js`
- `mongodb-memory-server` test pattern (ver `follow.controller.test.js`,
  `comment.controller.test.js`) — reusar para testes do notification controller

### Reusable Assets (frontend)
- `PrimaryButton`, `ErrorBanner`, `Spinner` — reusar na tela de notificações
- `UserResultCard`/`PostCard` — referência de layout para `NotificationCard` (avatar + nome
  + texto + timestamp, D-01)
- `FollowButton`/`BlockButton` — referência de padrão para botões de ação inline (D-05)
- CSS Modules co-localizados — continua como padrão

### Established Patterns
- Multi-tenant: `Notification` não carrega `institutionId` próprio — o escopo vem
  naturalmente de `userId`/`actorId` (ambos `User`, já scoped por instituição). Nenhuma
  alteração de schema necessária para manter PLAT-01.
- `req.user` injetado por `authenticateToken` (`_id`, `institutionId`, `role`)
- Cursor-based pagination (Phase 4, D-02) — candidato natural para `GET /api/notifications`
  se a lista crescer; decisão de design fica a critério do planner/Claude
- Polling é um padrão **novo** neste codebase — primeira feature a usá-lo (chat usa
  Socket.io/real-time, não polling)

### Integration Points
- `src/app.js` — montar `notification.routes`
- Novo: `src/controllers/notification.controller.js`, `src/routes/notification.routes.js`
- `src/controllers/registration.controller.js` (ou onde o `Invite` é resolvido no aceite) —
  adicionar disparo de `invite_received` (D-11)
- `MINIREDESOCIAL_FRONT/src/App.jsx` — adicionar rota `/notifications`
- `MINIREDESOCIAL_FRONT/src/components/AppShell/AppShell.jsx` — conectar `onClick` do bell
  icon (navegar para `/notifications`), renderizar badge de contagem (D-10), montar hook de
  polling no nível do shell (D-09)

</code_context>

<specifics>
## Specific Ideas

- **Intervalo de polling:** 5 segundos — valor explícito do usuário, priorizando
  demonstração ao vivo sobre eficiência de rede/servidor.
- **Textos de notificação sugeridos:**
  - `new_follower`: "[Nome] começou a seguir você"
  - `follow_request`: "[Nome] quer seguir você" + botões Aceitar/Rejeitar
  - `follow_accepted`: "[Nome] aceitou sua solicitação de follow"
  - `post_comment`: "[Nome] comentou no seu post"
  - `invite_received`: "[Nome] se cadastrou usando seu convite"
- **Badge:** contagem numérica no bell icon, com cap sugerido em "9+" para valores acima de 9
  (exato threshold a critério do Claude)
- **Polling hook:** deve viver no nível do `AppShell` (ou contexto equivalente) para que o
  badge fique correto em qualquer tela do app autenticado (D-09)

</specifics>

<deferred>
## Deferred Ideas

- **`post_like` notification** — ENGM-01, v2 backlog; não implementar agora.
- **Disparo de `message_received`** — o tipo permanece no enum (já existe), mas seu disparo
  real só faz sentido quando o chat existir (Phase 6); esta fase não cria um evento
  artificial para ele.
- **Agrupamento da lista de notificações por data** ("Hoje" / "Ontem" / "Esta semana") —
  usuário escolheu lista simples cronológica reversa para o MVP; agrupamento pode ser
  adicionado depois se desejado.

None — discussion stayed within phase scope.

</deferred>

---

*Phase: 5-notifications-polling*
*Context gathered: 2026-06-07*
