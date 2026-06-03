# Phase 3: Profiles & Social Graph - Discussion Log

> **Audit trail only.** Do not use as input to planning, research, or execution agents.
> Decisions are captured in CONTEXT.md — this log preserves the alternatives considered.

**Date:** 2026-06-03
**Phase:** 3-profiles-social-graph
**Areas discussed:** Navegação pós-login, Perfil privado, Busca de usuários

---

## Navegação pós-login

| Opção | Descrição | Selecionado |
|-------|-----------|-------------|
| Bottom nav bar | 4 abas, padrão mobile (Instagram/TikTok) | ✓ |
| Header com ícones | Navegação no topo, perde espaço | |

**Abas do bottom nav:**

| Opção | Descrição | Selecionado |
|-------|-----------|-------------|
| Feed / Busca / Notif / Perfil | 4 abas, phases preenchem aos poucos | ✓ |
| Só Busca / Perfil agora | 2 abas expandindo por fase | |

**Header:**

| Opção | Descrição | Selecionado |
|-------|-----------|-------------|
| Header fixo + bottom nav | Padrão Instagram/Twitter | |
| Scroll-aware (custom) | Esconde ao rolar down, aparece ao rolar up | ✓ (freeform) |
| Só bottom nav | Sem header | |

**User's choice:** Header scroll-aware — "eu quero o header não seja fixo, mas caso o user suba a página a página um pouco independente se está perto do topo o header apareça"

**Logout:**

| Opção | Descrição | Selecionado |
|-------|-----------|-------------|
| Na aba Perfil | Mais moderno | |
| No header | Sempre visível | ✓ |

**Notes:** Decisão de navegação é cross-phase — afeta Feed (4), Notif (5) e Chat (6).

---

## Perfil privado

| Opção | Descrição | Selecionado |
|-------|-----------|-------------|
| Nome, foto e botão "Solicitar" | Minimal, padrão Instagram | ✓ |
| Nome, foto, contadores | Meio-termo | |
| Só "Perfil privado" com nome/foto | Mais restritivo | |

**Solicitação pendente:**

| Opção | Descrição | Selecionado |
|-------|-----------|-------------|
| "Solicitação enviada" + cancelar | Feedback completo | ✓ |
| "Solicitação enviada" sem cancelar | Mais simples | |

**Aceitar/rejeitar requests:**

| Opção | Descrição | Selecionado |
|-------|-----------|-------------|
| Seção "Solicitações" na tela de Perfil | UI no Phase 3 | |
| Via notificações (Phase 5) | Frontend de accept/reject fica para a Phase 5 | ✓ |

**Notes:** API backend (accept/reject endpoints) criada em Phase 3; UI somente na Phase 5.

---

## Busca de usuários

| Opção | Descrição | Selecionado |
|-------|-----------|-------------|
| Só mesma instituição | Multi-tenant scoping | ✓ |
| Todas as instituições | Busca global | |

**Query:**

| Opção | Descrição | Selecionado |
|-------|-----------|-------------|
| Nome parcial, case-insensitive | MongoDB regex | ✓ |
| Nome e email | Dois campos | |

**UX frontend:**

| Opção | Descrição | Selecionado |
|-------|-----------|-------------|
| Aba "Busca" no bottom nav | Página dedicada na nav | ✓ |
| Barra de busca no header | Sempre acessível | |

**Paginação:**

| Opção | Descrição | Selecionado |
|-------|-----------|-------------|
| Paginados | page/limit | ✓ |
| Todos de uma vez | Sem paginação | |

---

## Claude's Discretion

- Estrutura interna dos componentes React (profile view, search)
- Naming exato dos endpoints REST
- Formato de resposta da API para estado do follow
- Loading/error states nos componentes
- Stub de notificações (create-on-follow): Claude decidiu criar os registros no banco

## Deferred Ideas

- Interface de aceitar/rejeitar follow requests na tela de Perfil → Phase 5 (notificações)
- Block middleware centralizado → pode ser extraído em Phase 4 ou 6
- Notificações de follow (new_follower, follow_request, follow_accepted) → registros criados via service stub; endpoint de consulta na Phase 5
