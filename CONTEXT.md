# Contexto do Projeto — Fademy

## Visão geral

Mini rede social voltada para alunos, professores, funcionários e coordenadores de instituições de ensino. O sistema deve ser construído inicialmente para a **Faculdade Católica da Paraíba**, mas arquitetado para suportar múltiplas instituições desde o primeiro dia.

---

## Stack

### Backend
- **Runtime**: Node.js
- **Framework**: Express
- **Banco de dados**: MongoDB
- **ODM**: Mongoose
- **Variáveis de ambiente**: dotenv
- **Autenticação**: JWT (a implementar)
- **Upload de imagens**: a definir (sugestão: Cloudinary ou S3)

### Frontend
- **Framework**: React — **mobile-first** (a interface é desenhada primeiro para celular)
- **Paleta de marca**: azul escuro, preto, bege, marrom tabaco
- App nativo é um milestone posterior; o frontend web mobile-first vem primeiro

---

## Regras de negócio principais

### Cadastro e autenticação
- Cadastro **somente por convite**. Convites são gerados por professores, coordenadores, funcionários e admins.
- O convite é **vinculado à instituição** de quem convidou. Ao aceitar, o usuário já entra naquela instituição.
- O convite define o `role` do novo usuário.
- O usuário escolhe a instituição na tela de cadastro (UI), mas o sistema valida pelo token do convite.
- Foto de perfil obrigatória no cadastro.

### Roles e permissões

| Role | Pode convidar | Pode postar link | Permissões extras |
|---|---|---|---|
| `admin` | Sim | Sim | Gestão da instituição no app |
| `coordenador` | Sim | Sim | Moderação de conteúdo |
| `professor` | Sim | Sim | — |
| `funcionario` | Sim | Sim | — |
| `aluno` | Não | Não | — |

### Perfil
- Pode ser **público** ou **privado** (campo `isPrivate`).
- Perfil público: qualquer usuário pode seguir diretamente.
- Perfil privado: o follow fica com status `pending` até o dono aceitar.

### Posts
- Suporta texto, imagens e links (links só para roles permitidos).
- Ao criar um post, o autor escolhe uma ou mais **categorias**.
- Campo `hasLink` deve ser validado no backend conforme o role do autor.

### Categorias (fixas no sistema)
Divididas em dois tipos:

**Tipo `content`** (natureza do post):
- Projetos
- Aulas
- Dia a dia
- Meme

**Tipo `area`** (área do conhecimento/curso):
- Ciência da Computação
- Direito
- Marketing
- Filosofia
- Educação Física
- NEPA

### Sistema de follows
- Um usuário pode seguir outro (ou solicitar, se privado).
- `FOLLOWS.status`: `pending | accepted`
- Feed personalizado é baseado em quem o usuário segue (status `accepted`).

### Bloqueio
- Ao bloquear: remove o follow dos dois lados automaticamente.
- Usuário bloqueado não pode mandar mensagem, não vê posts, não aparece em buscas.

### Notificações
- Somente **in-app** (sem push ou email por ora).
- Armazenadas no banco com campo `read` (boolean).
- Usar padrão polimórfico: `refId` + `refModel` para referenciar a entidade origem.

Tipos de notificação sugeridos:
- `follow_request` — solicitação de follow
- `follow_accepted` — follow aceito
- `new_follower` — novo seguidor (perfil público)
- `post_comment` — comentário no post
- `post_like` — curtida no post (se implementado)
- `invite_received` — convite recebido
- `message_received` — nova mensagem

### Chat e grupos
- Chat **1:1** disponível apenas entre usuários que se seguem mutuamente.
- Adicionar uma terceira pessoa a uma conversa transforma em **grupo**.
- Grupos podem ser **públicos** (pesquisáveis) ou **privados**.
- Mensagens suportam texto e mídia (imagens, arquivos) — não são posts.
- `CONVERSATIONS.isGroup` distingue 1:1 de grupo.

---

## Collections do MongoDB

### `institutions`
```js
{
  _id: ObjectId,
  name: String,           // "Faculdade Católica da Paraíba"
  slug: String,           // "catolica-pb"
  logoUrl: String,
  active: Boolean,
  createdAt: Date
}
```

### `users`
```js
{
  _id: ObjectId,
  institutionId: ObjectId,  // ref: institutions
  name: String,
  email: String,            // único
  passwordHash: String,
  avatarUrl: String,
  role: String,             // enum: admin | coordenador | professor | funcionario | aluno
  isPrivate: Boolean,       // default: false
  active: Boolean,
  createdAt: Date
}
```

### `invites`
```js
{
  _id: ObjectId,
  institutionId: ObjectId,  // ref: institutions
  invitedById: ObjectId,    // ref: users
  email: String,
  roleTarget: String,       // enum: role que o convidado vai receber
  token: String,            // uuid único
  status: String,           // enum: pending | accepted | expired
  expiresAt: Date,
  createdAt: Date
}
```

### `follows`
```js
{
  _id: ObjectId,
  followerId: ObjectId,   // ref: users
  followingId: ObjectId,  // ref: users
  status: String,         // enum: pending | accepted
  createdAt: Date
}
```

### `blocks`
```js
{
  _id: ObjectId,
  blockerId: ObjectId,  // ref: users
  blockedId: ObjectId,  // ref: users
  createdAt: Date
}
```

### `categories`
```js
{
  _id: ObjectId,
  name: String,   // "Ciência da Computação"
  slug: String,   // "ciencia-da-computacao"
  type: String    // enum: content | area
}
```

### `posts`
```js
{
  _id: ObjectId,
  authorId: ObjectId,       // ref: users
  text: String,
  imageUrls: [String],
  categoryIds: [ObjectId],  // ref: categories
  hasLink: Boolean,         // validado no backend pelo role
  createdAt: Date
}
```

### `comments`
```js
{
  _id: ObjectId,
  postId: ObjectId,    // ref: posts
  authorId: ObjectId,  // ref: users
  text: String,
  createdAt: Date
}
```

### `notifications`
```js
{
  _id: ObjectId,
  userId: ObjectId,    // destinatário — ref: users
  actorId: ObjectId,   // quem gerou — ref: users
  type: String,        // enum: tipos listados acima
  refId: ObjectId,     // id da entidade relacionada (polimórfico)
  refModel: String,    // nome da collection: "posts" | "follows" | "messages" etc.
  read: Boolean,       // default: false
  createdAt: Date
}
```

### `conversations`
```js
{
  _id: ObjectId,
  memberIds: [ObjectId],  // ref: users
  name: String,           // opcional, para grupos
  isGroup: Boolean,
  isPublic: Boolean,      // grupos públicos são pesquisáveis
  createdById: ObjectId,  // ref: users
  updatedAt: Date
}
```

### `messages`
```js
{
  _id: ObjectId,
  conversationId: ObjectId,  // ref: conversations
  senderId: ObjectId,        // ref: users
  text: String,
  mediaUrls: [String],
  createdAt: Date
}
```

---

## Estrutura de pastas sugerida

```
src/
├── config/
│   └── db.js               # conexão mongoose
├── models/
│   ├── Institution.js
│   ├── User.js
│   ├── Invite.js
│   ├── Follow.js
│   ├── Block.js
│   ├── Category.js
│   ├── Post.js
│   ├── Comment.js
│   ├── Notification.js
│   ├── Conversation.js
│   └── Message.js
├── routes/
│   ├── auth.routes.js
│   ├── user.routes.js
│   ├── invite.routes.js
│   ├── post.routes.js
│   ├── comment.routes.js
│   ├── follow.routes.js
│   ├── feed.routes.js
│   ├── notification.routes.js
│   ├── conversation.routes.js
│   └── message.routes.js
├── controllers/
│   └── (espelha routes/)
├── middlewares/
│   ├── auth.middleware.js   # verifica JWT
│   ├── role.middleware.js   # verifica permissões por role
│   └── block.middleware.js  # verifica bloqueios antes de ações
├── services/
│   ├── notification.service.js
│   └── feed.service.js
└── app.js
```

---

## Decisões técnicas pendentes

- [ ] Biblioteca JWT: `jsonwebtoken` ou `jose`
- [ ] Upload de mídia: Cloudinary, AWS S3 ou armazenamento local (dev)
- [ ] Real-time para chat e notificações: Socket.io ou polling simples no início
- [ ] Seed inicial de categorias e da instituição Católica PB
- [ ] Paginação: cursor-based (recomendado para feed) ou offset

---

## Seed inicial necessário

Ao inicializar o projeto, popular:
1. A instituição `Faculdade Católica da Paraíba` com slug `catolica-pb`
2. Todas as categorias listadas acima com seus respectivos `type`
3. Um usuário `admin` vinculado à instituição para bootstrapping

---

## Observações para o Claude Code

- Sempre validar `role` no middleware antes de chegar no controller
- Ao bloquear um usuário, criar um job/função que remove os follows bilaterais
- Notificações devem ser disparadas via `notification.service.js`, nunca inline no controller
- O feed deve filtrar posts de usuários bloqueados e de usuários que bloquearam o viewer
- Posts com link (`hasLink: true`) só podem ser criados por roles `professor | coordenador | funcionario | admin` — rejeitar com 403 para `aluno`
- Convites expiram — implementar verificação de `expiresAt` ao aceitar
