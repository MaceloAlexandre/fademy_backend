# SPEC — Fademy API

Regras de negócio vigentes do backend Fademy. Toda mudança de comportamento entra
neste documento na mesma entrega (PR) que a implementa, referenciando a issue.
Em caso de conflito com o `CLAUDE.md` ou com o código, **este documento prevalece**.

- Regras têm identificador estável (`RN01`...). Não renumere; regra removida vira "revogada".
- Estas regras foram extraídas do backend Node original (branch `legado-node`) e dos
  diagramas de casos de uso e de sequência. Onde o Node tinha bug ou buraco, a regra
  corrige e a divergência está listada na seção [7](#7-divergências-em-relação-ao-node).
- A seção [8](#8-status-de-implementação) diz qual issue entrega cada regra.

---

## 1. Escopo

Fademy é uma rede social acadêmica **só por convite** e **multi-instituição**. A primeira
instituição é a Faculdade Católica da Paraíba. Alunos, professores, funcionários,
coordenadores e administradores entram por convite, montam o perfil, publicam posts com
imagens e categorias, seguem uns aos outros, comentam, recebem notificações e conversam
1:1 em tempo real.

O backend é consumido pelo frontend React (`MaceloAlexandre/fademy_frontend`) **sem
alteração**: o contrato REST e Socket.IO da seção 5 é o mesmo do backend Node.

## 2. Glossário

| Termo | Significado |
|---|---|
| Instituição | Tenant. Todo usuário pertence a exatamente uma. |
| Equipe | Usuários com papel `admin`, `coordenador`, `professor` ou `funcionario`. |
| Convite | Link `/invite/<token>` com papel-alvo, número de vagas e validade. |
| Vaga | Um uso do convite (um cadastro). |
| Seguir aceito | Registro de follow com `status = accepted`. |
| Seguidores mútuos | A segue B **e** B segue A, ambos `accepted`. |
| Bloqueio entre A e B | Existe bloqueio de A para B **ou** de B para A. |
| Visualizador | Usuário autenticado que faz a requisição. |

## 3. Onde cada dado vive

| Banco | Dados | Por quê |
|---|---|---|
| PostgreSQL | instituições, usuários, convites, categorias, follows, bloqueios, posts, comentários | Dados relacionais com unicidade e integridade referencial (FKs, `UNIQUE`), transações e consultas com JOIN (feed, busca, sugestões). |
| MongoDB | conversas, mensagens, notificações; **imagens** (GridFS, bucket `imagens`) | Alto volume de escrita, leitura por linha do tempo de um único dono, documento autocontido (mensagem com lista de mídias, notificação com referência polimórfica). As imagens ficam no GridFS, o recurso do MongoDB para arquivos, com a chave como nome e o tipo nos metadados (#62). |

Referências entre os bancos são só por id (ex.: `senderId` de uma mensagem é o id de um
usuário do Postgres). Não há chave estrangeira entre bancos. Regras de consistência na
[seção 6](#6-consistência-entre-bancos).

## 4. Regras de negócio

### 4.1 Instituição e categorias

**RN01 — Isolamento por instituição.** Todo dado lido ou escrito é da instituição do
visualizador. A instituição vem **só** do token (claim `institutionId`), nunca de path,
query ou body. Um recurso de outra instituição é tratado como inexistente (404), exceto
onde a regra disser outro status.

**RN02 — Categorias.** As categorias são globais (não pertencem a uma instituição) e fixas:
- conteúdo (`content`): Projetos, Aulas, Dia a dia, Meme;
- área (`area`): Ciência da Computação, Direito, Marketing, Filosofia, Educação Física, NEPA.

O `slug` é o nome em minúsculas, sem acento, com espaços trocados por `-`.
`GET /api/categories` (autenticado) devolve `{ categories: [ { _id, name, slug, type } ] }`.

**RN03 — Carga inicial.** Na subida da aplicação, de forma idempotente: cria (ou atualiza o
nome de) a instituição `catolica-pb` "Faculdade Católica da Paraíba"; cria as categorias da
RN02; cria o admin inicial com `BOOTSTRAP_ADMIN_EMAIL` (padrão `admin@fademy.com`),
`BOOTSTRAP_ADMIN_PASSWORD` (padrão `changeme`) e `BOOTSTRAP_ADMIN_NAME` (padrão `Admin`)
nessa instituição, com um avatar de iniciais. Rodar de novo não altera a senha de um admin que
já existe; se ele não tem foto, ganha o avatar de iniciais. Se o armazenamento de imagens estiver
fora do ar, o admin é criado sem foto e a aplicação sobe mesmo assim (#54).

### 4.2 Papéis e permissões

**RN04 — Papéis.** Os papéis são `admin`, `coordenador`, `professor`, `funcionario` e `aluno`.
O papel vem do convite usado no cadastro e não muda depois.

**RN05 — Permissões por papel.**

| Ação | admin | coordenador | professor | funcionario | aluno |
|---|:-:|:-:|:-:|:-:|:-:|
| Gerar e revogar convite | ✓ | ✓ | ✓ | ✓ | — |
| Gerar convite com papel-alvo `admin` | ✓ | — | — | — | — |
| Publicar post com link | ✓ | ✓ | ✓ | ✓ | — |
| Painel administrativo | ✓ | — | — | — | — |

Todo o resto (perfil, seguir, bloquear, postar sem link, comentar, conversar) vale para todos.

**RN06 — Vagas máximas por convite.** Papel-alvo `aluno`: até 50 vagas. Demais papéis: até 10.

### 4.3 Autenticação

**RN07 — Login.** `POST /api/auth/login { email, password }`.
- Falta `email` ou `password` → 400 `email and password are required`.
- E-mail inexistente, senha errada ou usuário inativo → 401 `Invalid credentials` (mesma
  mensagem nos três casos, para não revelar quais e-mails existem).
- Sucesso → 200 `{ token }`. JWT HS256 com `sub` (id do usuário), `role` e `institutionId`,
  válido por `JWT_EXPIRES_IN` (padrão 7 dias).

**RN08 — Rotas autenticadas.** Exigem `Authorization: Bearer <token>`.
- Sem cabeçalho → 401 `Authentication required`.
- Token inválido, expirado ou com outro algoritmo → 401 `Invalid or expired token`.
- Papel sem permissão (RN05) → 403 `Insufficient permissions`, antes de qualquer outra validação.

**RN09 — E-mail único.** Um e-mail pertence a no máximo um usuário **em todo o sistema**
(comparação sem diferenciar maiúsculas). Ver D1.

### 4.4 Convites

**RN10 — Gerar convite.** `POST /api/invites { roleTarget, maxUses, expiresAt? }` (equipe).
Validações, nesta ordem:
1. falta `roleTarget` ou `maxUses` → 400 `roleTarget and maxUses are required`;
2. `roleTarget = admin` e o visualizador não é admin → 403 `Only admin can generate admin invites`;
3. `roleTarget` não é um papel da RN04 → 400 `Invalid roleTarget`;
4. `maxUses` menor que 1 → 400 `maxUses must be at least 1` (ver D8);
5. `maxUses` acima do limite da RN06 → 400 `maxUses exceeds cap of <N> for roleTarget <papel>`.

O token é um UUID aleatório. `expiresAt` padrão: 7 dias a partir de agora. O convite nasce
`active`, com `usesCount = 0`, na instituição do visualizador e com `invitedById` = visualizador.
Resposta 201 `{ token, link: "/invite/<token>" }`.

**RN11 — Consultar convite (público).** `GET /api/invites/:token`, sem autenticação.
Token inexistente → 404 `Invite not found`. Senão, 200:
`{ roleTarget, institutionName, remainingSlots, valid, reason }`, em que
`remainingSlots = maxUses − usesCount`, `valid` é verdadeiro só se o convite está `active`,
não expirou e tem vaga, e `reason` é o primeiro que se aplicar: `revoked`, `expired`, `full`,
ou `null` quando válido.

**RN12 — Revogar convite.** `POST /api/invites/:token/revoke` (equipe). Só convites da
instituição do visualizador; qualquer membro da equipe da instituição pode revogar.
Inexistente ou de outra instituição → 404 `Invite not found`. Sucesso → status `revoked`,
200 `{ message: "Invite revoked", token }`.

**RN13 — Consumo de vaga.** Cada cadastro consome uma vaga. O consumo é atômico: dois
cadastros simultâneos nunca passam de `maxUses`. Ao consumir a última vaga, o convite
passa a `expired`.

### 4.5 Imagens

**RN14 — Upload de avatar.** `POST /api/media/upload`, multipart, campo `file`, **sem
autenticação** (acontece antes de a conta existir). Aceita `image/jpeg`, `image/png` e
`image/webp` com até `MAX_UPLOAD_BYTES` (padrão 5 MB).
- Sem arquivo → 400 `file is required`; tipo não aceito → 400
  `file must be image/jpeg, image/png, or image/webp`; acima do tamanho → 400 `File upload error`.
- O servidor gera a chave `avatars/<uuid>.<ext>`; o cliente nunca escolhe a chave.
- 201 `{ key, objectUrl }`.

**RN15 — Imagens de post e de mensagem.** Até 4 arquivos no campo multipart `images`, com os
tipos e o tamanho da RN14. Chaves `posts/<uuid>.<ext>` e `messages/<uuid>.<ext>`. Todos os
arquivos são validados **antes** de qualquer envio ao armazenamento; um arquivo inválido
rejeita a requisição inteira (400 `Invalid file type`) sem deixar objeto órfão.

As imagens das RN14 e RN15 são gravadas no MongoDB, pelo GridFS (#62).

**RN49 — Ver imagem.** `GET /api/media/<chave>` (ex.: `/api/media/avatars/<uuid>.jpg`), **sem
autenticação** (o navegador abre a imagem direto pela URL). Chave existente → 200 com os bytes
da imagem e o `Content-Type` dela; inexistente → 404 `Image not found` (#62).

### 4.6 Cadastro via convite

**RN16 — Cadastro.** `POST /api/auth/register { token, name, email, password, avatarUrl, avatarKey }`.
Validações, nesta ordem:
1. qualquer campo ausente → 400 `token, name, email, password, avatarUrl, and avatarKey are required`;
2. convite inexistente → 400 `Invalid invite token`;
3. convite não `active` → 400 `Invite is no longer valid`;
4. convite expirado → 400 `Invite has expired`;
5. convite sem vaga → 400 `Invite has no remaining slots`;
6. `avatarKey` não existe no armazenamento → 400 `Profile photo not found in storage — please upload a photo first`;
7. e-mail já usado (RN09) → 409 `Este e-mail já está em uso`.

Instituição e papel vêm do convite, nunca do body. A foto gravada é a `avatarKey` validada no
item 6; o `avatarUrl` é exigido, mas não é gravado (#53). A senha é guardada com bcrypt
(`BCRYPT_ROUNDS`, padrão 10). A criação do usuário e o consumo da vaga (RN13) acontecem na
mesma transação. Depois, o autor do convite recebe a notificação `invite_received` (ator =
novo usuário). Resposta 201 `{ id, name, email, role, institutionId, avatarUrl }`, **sem
token**: o frontend redireciona para o login.

### 4.7 Perfil

**RN17 — Ver perfil.** `GET /api/users/:id` e `GET /api/users/me`.
- Alvo inexistente, de outra instituição ou que bloqueou o visualizador → 404 `User not found`.
- Se o visualizador bloqueou o alvo, o perfil aparece (para permitir desbloquear) com `isBlockedByMe: true`.
- `followStatus` = status do follow do visualizador para o alvo (`pending`, `accepted` ou `null`).
- Perfil privado, visualizador não é o dono e não segue com `accepted` → 200 com
  `{ id, name, avatarUrl, role, isPrivate: true, followStatus, isBlockedByMe }`.
- Caso contrário → 200 com os campos acima mais `followersCount` e `followingCount`
  (só follows `accepted`).

**RN18 — Editar o próprio perfil.** `PATCH /api/users/me { isPrivate?, avatarUrl?, avatarKey? }`.
- `isPrivate` presente e não booleano → 400 `isPrivate must be a boolean`.
- `avatarUrl` e `avatarKey` vêm juntos e como texto; senão → 400 `avatarUrl and avatarKey must be strings`.
  A chave precisa existir no armazenamento (ver D3), senão 400 como na RN16. Grava-se a chave;
  o `avatarUrl` enviado não é guardado (#53).
- Nenhum campo válido → 400 `No valid fields to update`.
- 200 `{ id, name, avatarUrl, role, isPrivate }`.
- Tornar o perfil público não aceita automaticamente pedidos pendentes.

### 4.8 Busca e sugestões

**RN19 — Busca.** `GET /api/users/search?q=&page=1&limit=20`.
- `q` vazio → 200 `{ users: [], total: 0, page: 1, limit }`.
- Busca por trecho do nome, sem diferenciar maiúsculas nem acentos, com `q` truncado em 100 caracteres.
- Só usuários da instituição do visualizador, sem o próprio visualizador e sem quem tem
  bloqueio com ele. `limit` máximo 50.
- 200 `{ users: [ { _id, name, avatarUrl, role, isPrivate } ], total, page, limit }`.

**RN20 — Sugestões.** `GET /api/users/suggestions?limit=15` (máximo 20).
Candidatos: mesma instituição, sem o visualizador, sem bloqueio com ele e sem quem ele já
segue (`pending` ou `accepted`). Ordem: equipe primeiro, depois alunos; dentro de cada
grupo, cadastro mais recente primeiro. 200 `{ users: [ { _id, name, avatarUrl, role, isPrivate } ] }`.

### 4.9 Seguir

**RN21 — Seguir.** `POST /api/users/:id/follow`.
1. seguir a si mesmo → 400 `Cannot follow yourself`;
2. bloqueio entre os dois → 403 `Forbidden`;
3. alvo inexistente ou de outra instituição → 404 `User not found`;
4. já existe follow (qualquer status) → 409 `Already following or request pending`.

Alvo privado → follow `pending` e notificação `follow_request` para o alvo (antes, apaga
uma `follow_request` anterior do mesmo ator para o mesmo alvo). Alvo público → follow
`accepted` e notificação `new_follower`. 201 `{ status, followId }`.

**RN22 — Deixar de seguir.** `DELETE /api/users/:id/follow`. Remove o follow do visualizador
para o alvo, em qualquer status (também cancela pedido pendente). Idempotente: 200
`{ unfollowed: true }` mesmo sem follow.

**RN23 — Aceitar pedido.** `PATCH /api/follows/:id/accept`.
Follow inexistente → 404 `Follow request not found`; visualizador não é o alvo → 403
`Forbidden`; não está `pending` → 400 `No pending request`. Sucesso → `accepted`,
notificação `follow_accepted` para quem pediu (ator = visualizador), 200 `{ status: "accepted" }`.

**RN24 — Rejeitar pedido.** `PATCH /api/follows/:id/reject`. Mesmos 404 e 403 da RN23.
Sucesso → o follow é removido, 200 `{ rejected: true }`.

### 4.10 Bloqueio

**RN25 — Bloquear.** `POST /api/users/:id/block`.
Bloquear a si mesmo → 400 `Cannot block yourself`; alvo inexistente ou de outra instituição
→ 404 `User not found`. Sucesso: remove os follows nos dois sentidos, remove as
notificações `follow_request` entre os dois (nos dois sentidos) e registra o bloqueio.
Idempotente. 200 `{ blocked: true }`.

**RN26 — Desbloquear.** `DELETE /api/users/:id/block`. Desbloquear a si mesmo → 400
`Cannot unblock yourself`. Remove o bloqueio do visualizador para o alvo; não restaura
follows. Idempotente. 200 `{ unblocked: true }`.

**RN27 — Efeitos do bloqueio entre A e B.** Enquanto houver bloqueio entre A e B:
- quem foi bloqueado não vê o perfil de quem bloqueou (RN17);
- um não aparece na busca nem nas sugestões do outro (RN19, RN20);
- nenhum dos dois pode seguir o outro (RN21);
- os posts de um não aparecem no feed do outro (RN29);
- quem foi bloqueado não vê posts, lista de posts nem comentários de quem bloqueou (RN30, RN31, RN33);
- não se cria conversa entre eles e nenhum dos dois envia mensagem numa conversa já existente (RN38, RN41).

### 4.11 Posts e feed

**RN28 — Publicar.** `POST /api/posts`, multipart `{ text, linkUrl?, categoryIds?, images? }`.
- `text` ausente ou só espaços → 400 `text is required`. O texto é salvo sem espaços nas pontas.
- `linkUrl` não vazio e papel sem permissão (RN05) → 403 `Insufficient permissions`.
- `categoryIds` pode vir como lista ou texto separado por vírgula; ids que não existem → 400
  `Invalid category` (ver D9).
- Imagens pela RN15.
- `hasLink` é verdadeiro quando há `linkUrl`. `commentCount` nasce 0.
- 201 `{ post }` no formato da seção 5.3.

**RN29 — Feed.** `GET /api/posts/feed?cursor=&limit=20`. Posts dos usuários que o visualizador
segue com `accepted`, da instituição dele, sem autores com bloqueio entre eles, do mais novo
para o mais antigo. Os próprios posts do visualizador não entram no feed. Paginação pela
seção 5.4. 200 `{ posts, hasMore, nextCursor }`.

**RN30 — Ver um post.** `GET /api/posts/:id`. Inexistente, de outra instituição ou de autor
que bloqueou o visualizador → 404 `Post not found`. 200 `{ post }`.

**RN31 — Posts de um usuário.** `GET /api/users/:id/posts?cursor=&limit=20` (`:id` pode ser `me`).
- O alvo bloqueou o visualizador → 403 `Forbidden`.
- Perfil privado, visualizador não é o dono e não segue com `accepted` → 403 `Forbidden` (ver D6).
- Autor inexistente ou de outra instituição não tem posts para mostrar: 200 com lista vazia, como no Node (#35).
- Paginação pela seção 5.4. 200 `{ posts, hasMore, nextCursor }`.

### 4.12 Comentários

**RN32 — Comentar.** `POST /api/posts/:postId/comments { text }`.
`text` ausente ou só espaços → 400 `text is required`; post invisível pela RN30 → 404
`Post not found`. O comentário e o incremento de `commentCount` do post acontecem na mesma
transação. Se o comentarista não é o autor do post, o autor recebe `post_comment` (referência
= o post). 201 `{ comment }`.

**RN33 — Listar comentários.** `GET /api/posts/:postId/comments`. Post invisível pela RN30 →
404 `Post not found` (ver D4). Do mais antigo para o mais novo, com o autor populado.
200 `{ comments }`.

### 4.13 Notificações

**RN34 — Tipos.**

| Tipo | Destinatário | Ator | Referência (`refModel`) |
|---|---|---|---|
| `new_follower` | quem foi seguido | quem seguiu | follow (`Follow`) |
| `follow_request` | dono do perfil privado | quem pediu | follow (`Follow`) |
| `follow_accepted` | quem pediu | quem aceitou | follow (`Follow`) |
| `post_comment` | autor do post | comentarista | post (`Post`) |
| `invite_received` | autor do convite | novo usuário | novo usuário (`User`) |
| `message_received` | outro membro da conversa | remetente | conversa (`Conversation`) |

Toda notificação nasce com `read = false`.

**RN35 — Listar.** `GET /api/notifications?cursor=&limit=20`. Só as do visualizador, da mais
nova para a mais antiga, paginação pela seção 5.4, com `actorId` populado `{ _id, name, avatarUrl }`.
Cada `follow_request` traz `followStatus` com o status atual do follow referenciado
(`pending`, `accepted` ou `null` se ele não existe mais). 200
`{ notifications, hasMore, nextCursor, unreadCount }`, em que `unreadCount` conta todas as não
lidas do visualizador.

**RN36 — Marcar como lida e excluir.** `PATCH /api/notifications/:id/read` e
`DELETE /api/notifications/:id`. Inexistente → 404 `Notification not found`; de outro
destinatário → 403 `Forbidden`. 200 `{ read: true }` / `{ deleted: true }`.

**RN37 — Notificação não derruba a ação.** Criar ou apagar notificação é efeito colateral:
se falhar, a ação principal (seguir, comentar, cadastrar, enviar mensagem, bloquear) continua
valendo e responde sucesso; a falha é registrada no log.

### 4.14 Conversas e mensagens

**RN38 — Iniciar conversa.** `POST /api/conversations { userId }`. Só conversas 1:1 (ver D7).
1. com o próprio visualizador → 400 `Cannot message yourself`;
2. alvo inexistente → 404 `User not found`;
3. alvo de outra instituição → 403 `Forbidden`;
4. bloqueio entre os dois → 403 `Forbidden`;
5. não são seguidores mútuos → 403 `Both users must follow each other to start a chat`.

Se já existe conversa entre os dois, devolve ela (200 `{ conversation }`); senão cria
(201 `{ conversation }`). Existe no máximo uma conversa por par de usuários.

**RN39 — Listar conversas.** `GET /api/conversations`. Conversas do visualizador, da atividade
mais recente para a mais antiga (`updatedAt`), com `memberIds` populado `{ _id, name, avatarUrl }`
e `lastMessage` (a última mensagem ou `null`). 200 `{ conversations }`.

**RN40 — Contatos elegíveis.** `GET /api/conversations/eligible-contacts`. Seguidores mútuos
do visualizador. 200 `{ users: [ { _id, name, avatarUrl, role } ] }`.

**RN41 — Enviar mensagem.** `POST /api/conversations/:id/messages`, multipart `{ text?, images? }`.
1. conversa inexistente → 404 `Conversation not found`;
2. visualizador não é membro → 403 `Forbidden`;
3. bloqueio entre os membros → 403 `Forbidden` (ver D10);
4. sem texto (após tirar espaços) e sem imagem → 400 `text or media is required`;
5. imagens pela RN15.

O remetente é sempre o visualizador. Grava a mensagem, atualiza `updatedAt` da conversa,
notifica o outro membro com `message_received` e emite `message:new` para ele (RN43).
201 `{ message }`.

**RN42 — Listar mensagens.** `GET /api/conversations/:id/messages?cursor=&limit=20`. Mesmos
404 e 403 da RN41 (itens 1 e 2). Da mais nova para a mais antiga, paginação pela seção 5.4.
200 `{ messages, hasMore, nextCursor }`.

### 4.15 Tempo real

**RN43 — Socket.IO.** Na mesma porta da API (polling e WebSocket em `/socket.io/`). O cliente
conecta com `auth: { token }` (o JWT do login); sem token ou com token inválido, a conexão é
encerrada logo depois de aberta e não entra em sala nenhuma (a biblioteca Java não tem o
middleware de handshake do Node, que respondia `connect_error`; #42). Cada conexão entra na sala
cujo nome é o id do usuário. O servidor só emite; a escrita é sempre pela API REST. Evento:
`message:new`, com a mensagem no formato da seção 5.3, enviado à sala do destinatário.

### 4.16 Administração e saúde

**RN44 — Painel administrativo.** `GET /api/admin/overview` (só `admin`). 200
`{ institution: { _id, name, slug, logoUrl, active, createdAt }, userCount }` da instituição
do visualizador.

**RN45 — Saúde.** `GET /health`, público. 200 `{ status: "ok", postgres: "up", mongo: "up" }`
quando os dois bancos respondem; senão 503 `{ status: "degraded", postgres, mongo }` com
`up` ou `down` em cada um.

## 5. Contrato da API

### 5.1 Convenções

- Prefixo `/api` (exceto `/health`). JSON em UTF-8. Porta padrão `8080` (#59).
- **Ids:** texto opaco, no campo `_id`. Entidades do Postgres usam UUIDv7 (ordenado pelo
  tempo); entidades do Mongo usam ObjectId em hexadecimal. As respostas de perfil (RN17,
  RN18) e de cadastro (RN16) usam `id`, como no Node.
- Um id de path em formato inválido é tratado como inexistente (404 da rota).
- **Datas:** ISO-8601 em UTC com sufixo `Z`.
- **Imagens:** o banco guarda só a chave no armazenamento (`avatars/…`, `posts/…`,
  `messages/…`). Toda URL de imagem na resposta (`avatarUrl`, `imageUrls`, `mediaUrls`,
  `objectUrl`) é montada na hora como `IMAGENS_URL_PUBLICA` + `/` + chave (padrão
  `http://localhost:8080/api/media`, a rota da RN49), com o endereço da execução atual; trocar o
  endereço não quebra as imagens que já existiam (#53, #62).
- **Erros:** `{ "error": "<mensagem>" }`. Erro inesperado → 500 `Internal server error`,
  sem detalhe interno no corpo. Erros de protocolo HTTP (JSON malformado, rota inexistente,
  método não suportado) mantêm o status padrão (400, 404, 405) com o mesmo formato de corpo (#3).
- CORS liberado para `FRONTEND_ORIGIN` (padrão `http://localhost:5173`).

### 5.2 Endpoints

| Método e rota | Autenticação | Regra |
|---|---|---|
| `GET /health` | — | RN45 |
| `POST /api/auth/login` | — | RN07 |
| `POST /api/auth/register` | — | RN16 |
| `POST /api/media/upload` | — | RN14 |
| `GET /api/media/<chave>` | — | RN49 |
| `GET /api/invites/:token` | — | RN11 |
| `POST /api/invites` | equipe | RN10 |
| `POST /api/invites/:token/revoke` | equipe | RN12 |
| `GET /api/categories` | sim | RN02 |
| `GET /api/users/me` · `PATCH /api/users/me` | sim | RN17 · RN18 |
| `GET /api/users/search` | sim | RN19 |
| `GET /api/users/suggestions` | sim | RN20 |
| `GET /api/users/:id` | sim | RN17 |
| `GET /api/users/:id/posts` | sim | RN31 |
| `POST · DELETE /api/users/:id/follow` | sim | RN21 · RN22 |
| `POST · DELETE /api/users/:id/block` | sim | RN25 · RN26 |
| `PATCH /api/follows/:id/accept` · `/reject` | sim | RN23 · RN24 |
| `POST /api/posts` | sim | RN28 |
| `GET /api/posts/feed` | sim | RN29 |
| `GET /api/posts/:id` | sim | RN30 |
| `POST · GET /api/posts/:postId/comments` | sim | RN32 · RN33 |
| `GET /api/notifications` | sim | RN35 |
| `PATCH /api/notifications/:id/read` · `DELETE /api/notifications/:id` | sim | RN36 |
| `POST · GET /api/conversations` | sim | RN38 · RN39 |
| `GET /api/conversations/eligible-contacts` | sim | RN40 |
| `POST · GET /api/conversations/:id/messages` | sim | RN41 · RN42 |
| `GET /api/admin/overview` | admin | RN44 |

### 5.3 Formatos

```jsonc
// post (RN28–RN31)
{ "_id", "authorId": { "_id", "name", "avatarUrl" }, "institutionId", "text",
  "imageUrls": [], "linkUrl"?, "hasLink", "categoryIds": [ { "_id", "name", "slug", "type" } ],
  "commentCount", "createdAt" }
// comentário (RN32, RN33)
{ "_id", "postId", "authorId": { "_id", "name", "avatarUrl" }, "institutionId", "text", "createdAt" }
// notificação (RN35)
{ "_id", "userId", "actorId": { "_id", "name", "avatarUrl" }, "type", "refId", "refModel",
  "read", "createdAt", "followStatus"? }
// conversa (RN38, RN39)
{ "_id", "memberIds": [ ... ], "isGroup": false, "createdById", "createdAt", "updatedAt",
  "lastMessage"? }
// mensagem (RN41–RN43)
{ "_id", "conversationId", "senderId", "text"?, "mediaUrls": [], "createdAt" }
```

Em RN38 `memberIds` é a lista de ids; em RN39 cada membro vem populado `{ _id, name, avatarUrl }`.
No post criado (RN28) `authorId` e `categoryIds` vêm como ids, sem popular. O comentário criado
(RN32) já vem com `authorId` populado, para a tela mostrar nome e foto sem recarregar (no Node
vinha só o id; #36).

### 5.4 Paginação por cursor

Usada em RN29, RN31, RN35 e RN42. Parâmetros `cursor` (opcional) e `limit` (padrão 20,
máximo 50; valor inválido ou menor que 1 vira 20). Os itens vêm do mais novo para o mais
antigo; com `cursor`, só itens com id menor que ele. A resposta traz `hasMore` e
`nextCursor` (id do último item da página, ou `null` quando `hasMore` é falso). Cursor em
formato inválido → 400 `Invalid cursor`.

## 6. Consistência entre bancos

**RN46 — Transação no Postgres.** Todas as escritas no Postgres de um mesmo caso de uso
acontecem numa única transação (ex.: cadastro + consumo de vaga; comentário + contador).

**RN47 — Ordem das escritas.** Quando um caso de uso escreve nos dois bancos, a escrita no
Postgres é confirmada primeiro e a do Mongo vem depois. A escrita no Mongo que for efeito
colateral segue a RN37. Não existe transação entre os dois bancos.

**RN48 — Referência a usuário.** Ao popular um usuário referenciado no Mongo (ator,
remetente, membro), os dados vêm do Postgres no momento da leitura; o Mongo guarda só o id.
Assim, trocar a foto do perfil aparece em notificações e conversas antigas.

## 7. Divergências em relação ao Node

Pontos em que o Node tinha bug ou não definia o comportamento. Cada um precisa de aprovação
antes de virar regra.

| # | Node | Regra nova | Onde |
|---|---|---|---|
| D1 | E-mail único por instituição, mas o login busca só por e-mail: dois usuários com o mesmo e-mail em instituições diferentes tornam o login ambíguo. | E-mail único no sistema todo. | RN09 |
| D2 | Perfil, seguir, bloquear e listar comentários não conferem a instituição. | Recurso de outra instituição = 404. | RN01, RN17, RN21, RN25 |
| D3 | `PATCH /users/me` aceita qualquer `avatarUrl` sem conferir o armazenamento. | A chave precisa existir no armazenamento, como no cadastro. | RN18 |
| D4 | Lista comentários de qualquer post, de qualquer instituição. | Mesma visibilidade do post. | RN33 |
| D5 | `GET /posts/:id` ignora bloqueio. | Post de quem bloqueou o visualizador = 404. | RN30 |
| D6 | `GET /users/:id/posts` devolve posts de perfil privado a quem não segue. | 403 para quem não segue com `accepted`. | RN31 |
| D7 | O modelo tinha campos de grupo (`isGroup`, `isPublic`, `name`), mas só o 1:1 foi feito. | Só 1:1; `isGroup` sempre `false` na resposta, por contrato. | RN38 |
| D8 | `maxUses` 0 ou negativo era aceito. | Mínimo 1. | RN10 |
| D9 | `categoryIds` inexistentes eram salvos. | 400 `Invalid category`. | RN28 |
| D10 | Depois de um bloqueio, a conversa existente continuava aceitando mensagens. | 403. | RN41 |
| D11 | Cadastro e consumo de vaga não eram atômicos (dois cadastros simultâneos podiam passar do limite). | Transação + consumo atômico. | RN13, RN16 |
| D12 | `/health` só olhava o Mongo. | Olha os dois bancos. | RN45 |

## 8. Status de implementação

Cada issue, ao ser entregue, registra aqui as regras que cobre.

| Regras | Issue | Status |
|---|---|---|
| RN45 | #2 | entregue |
| Seção 5.1: erros, datas, CORS | #3 | entregue |
| RN04, RN05, RN06 (no domínio) | #4 | entregue |
| RN02 (dados), RN03, RN09 | #5 | entregue |
| RN02 (endpoint), RN07, RN08 (401) | #6 | entregue |
| RN14, RN15 (tipos e tamanho) | #13 | entregue |
| RN08 (403), RN10, RN12 | #14 | entregue |
| RN11 | #15 | entregue |
| RN13, RN16, RN34 (`invite_received`), RN37, RN46, RN47 | #16 | entregue |
| RN21, RN22, RN34 (`new_follower`, `follow_request`) | #21 | entregue |
| RN23, RN24, RN34 (`follow_accepted`) | #22 | entregue |
| RN25, RN26; RN27 para seguir | #23 | entregue |
| RN17, RN18 (D2, D3); RN27 para perfil | #24 | entregue |
| RN19, RN20; RN27 para busca e sugestões | #25 | entregue |
| RN35, RN36, RN48; seção 5.4 (paginação por cursor) | #26 | entregue |
| RN28, RN15 (imagens de post), D9 | #33 | entregue |
| RN29; RN27 no feed | #34 | entregue |
| RN30, RN31 (D5, D6); RN27 em posts | #35 | entregue |
| RN32, RN33 (D4), RN34 (`post_comment`), RN46 | #36 | entregue |
| RN38 (D7), RN39 (sem última mensagem), RN40 | #41 | entregue |
| RN43 (conexão, autenticação e salas) | #42 | entregue |
| RN39 (última mensagem), RN41, RN42, RN43 (`message:new`), RN34 (`message_received`), D10 | #43 | entregue |
| RN44 (e o painel da RN05) | #47 | entregue |
| RN01 (isolamento por instituição) | em cada recurso: #14, #21–#26, #33–#36, #41 | entregue |
| Seção 5.1: imagens por chave; RN16 e RN18 gravam a chave | #53 | entregue |
| RN03 (avatar de iniciais do admin) | #54 | entregue |
| Seção 5: porta padrão 8080 | #59 | entregue |
| Seção 3, RN14, RN15 (imagens no MongoDB/GridFS), RN49 | #62 | entregue |

Com a #47, todas as regras RN01–RN48 estão implementadas.
