---
name: github-issue-implementation
description: "Implementa issues do GitHub do Fademy de ponta a ponta quando o usuário pedir para implementar uma issue por número: lê a issue, exige critérios de aceite claros, cria a branch a partir da main, implementa seguindo a Clean Architecture do docs/ARQUITETURA.md, valida com testes, atualiza docs/SPEC.md, faz commit e push, abre PR e move o item no GitHub Project Fademy (In progress → In review → Done após o merge)."
---

# Implementação de issue do GitHub — Fademy

Execute o fluxo completo solicitado. Trate os padrões de branch e commit abaixo como invariantes: não traduza, não acrescente texto e não substitua `/` ou `#`.

Este projeto trabalha com **issues + SDD (spec-driven development)**: a issue define *o quê* (critérios de aceite), `docs/SPEC.md` guarda as regras de negócio vigentes, `docs/ARQUITETURA.md` guarda a estrutura do código, e o código só muda junto com o teste que prova o critério. Não use nem crie `.planning/` (fluxo GSD abandonado).

## Repositórios

| Repositório (`origin`) | Pasta local (a partir de `fademy/`) | Branch-base |
|---|---|---|
| `MaceloAlexandre/fademy_backend` | `fademy_backend` | `main` |
| `MaceloAlexandre/fademy_frontend` | `fademy_frontend` | `main` |

O backend é a API Java (Spring Boot, Clean Architecture). A branch `legado-node` guarda o backend Node original; é só consulta, nunca base de branch. O exemplo de Singleton/Adapter do professor está em `docs/referencias/exemplo-singleton-adapter/`.

GitHub Project: **Fademy** — conta `MaceloAlexandre`. Status: `Todo` → `In progress` → `In review` → `Done`. As etapas do plano são **milestones** do repositório (`E1 — Esqueleto`, `E2 — Identidade`, ...). Resolva os IDs do Project, do item, do campo e das opções a cada execução; não os fixe aqui.

Todo comando git/gh roda na pasta do repositório (`git -C fademy_backend ...`, `gh ... -R MaceloAlexandre/<repo>`). A pasta `fademy/` não é um repositório.

Para qualquer outro repositório, pergunte ao usuário a branch-base antes de qualquer mutação. Nunca suponha.

## Contexto do projeto (vale para toda issue)

- Projeto acadêmico da disciplina de Programação Orientada a Objetos. O que importa: **orientação a objetos de verdade** (entidades com comportamento, herança onde há variação de comportamento, polimorfismo no lugar de `if` por tipo), **Clean Architecture** (use case → service → gateway, regra de dependência) e o **Singleton `DatabaseManager`** com Postgres e Mongo usados de verdade. Não trate hardening de segurança como critério implícito nem amplie escopo por isso.
- O frontend React (`fademy_frontend`) não muda: o **contrato da API** (SPEC seção 5 — rotas, status, mensagens de erro, nomes de campo como `_id`) é invariável, salvo quando a issue disser o contrário.
- A apresentação é na rede local da faculdade, sem depender de internet: nada de serviço externo obrigatório (S3 é MinIO local).
- Idioma: código (classes, métodos), mensagens de commit, PR e SPEC em português (Brasil). Mensagens de erro da API seguem o texto exato do SPEC (herdado do Node, em inglês na maioria).
- Nunca commite `.env` nem segredos.

## Resolver o alvo e a issue

1. Identifique o repositório pelo argumento do usuário. Se o usuário só disser o número, assuma `fademy_backend` e confirme se houver issue com o mesmo número no frontend.
2. Verifique autenticação com `gh auth status` e leia a issue: título, corpo, estado, labels, milestone, comentários relevantes e vínculo com o Project. Não implemente uma issue fechada sem confirmação explícita.
3. **Porta de critérios de aceite (obrigatória).** Extraia os critérios de aceite da issue. Eles precisam ser:
   - explícitos (seção própria, não implícitos na descrição);
   - verificáveis (teste automatizado ou passo manual objetivo; de preferência "Dado / Quando / Então");
   - sem ambiguidade (status HTTP, mensagem de erro, estado no banco, evento emitido).

   Se faltarem critérios, ou se algum for vago, **pare antes de criar a branch**. Proponha critérios concretos no [formato de issue](#formato-de-issue) e, após aprovação, atualize a issue (`gh issue edit`). Não invente decisões de produto: se o SPEC não responde, pergunte.
4. Extraia áreas afetadas, fora de escopo, dependências (`Depende de #n`) e validações esperadas. Se depender de issue aberta, avise antes de começar. Leia `CLAUDE.md`, `docs/SPEC.md` e `docs/ARQUITETURA.md` antes de editar.
5. **Um repositório por issue.** Se a entrega exigir mudança nos dois repos, pare e proponha dividir em duas issues vinculadas (backend primeiro, com o contrato escrito nas duas).
6. Classifique: feature (`enhancement`) ou bugfix (`bug`). Se ambíguo, pergunte.
7. Confirme que o item está no Project **Fademy** e mova o Status para `In progress`. Registre os IDs do Project, item, campo Status e das opções `In review` e `Done`. Se o vínculo ou as opções forem ambíguos, pare e informe.

Prefira `gh issue view`, `gh project ... --format json` e, só se necessário, `gh api graphql`.

## Preparar a branch

1. Confirme que não há alterações locais que possam ser sobrescritas. Preserve mudança preexistente; se ela impedir a troca de branch, pare e peça orientação.
2. `git -C fademy_backend fetch origin` e confirme que `origin/main` existe.
3. `git -C fademy_backend log --oneline -5 origin/main`: mencione ao usuário commits inesperados de outras pessoas (ex.: migration Flyway nova, mudança de contrato).
4. Crie a branch a partir de `origin/main`, exatamente:
   - feature: `feature/<id>`
   - bugfix: `bugfix/<id>`

Só os dígitos da issue em `<id>`. Se a branch já existir, não reutilize, apague ou sobrescreva sem autorização.

## Implementar e validar

Implemente só o escopo da issue, seguindo `docs/ARQUITETURA.md`:

- regra de negócio no **domínio** (método da entidade) sempre que a regra for sobre o próprio objeto; orquestração no **service**;
- cada caso de uso novo = interface em `application/usecase` + implementação em `application/service`;
- acesso a banco, armazenamento, token, senha ou tempo real = interface em `application/gateway` + implementação em `infrastructure/`;
- nada de Spring, JPA ou Mongo em `domain` e `application` (o teste ArchUnit quebra o build);
- migration Flyway nova `V<n>__<descricao>.sql`; nunca editar uma migration já commitada na `main`.

**Cada critério de aceite deve ser coberto por pelo menos um teste automatizado** que falharia sem a mudança. Regra de domínio → teste unitário; orquestração → teste do service com gateways mockados; gateway → teste de integração com Testcontainers; contrato HTTP (status, corpo, mensagem) → teste web. Quando inviável (ex.: visual no frontend), registre no PR o passo manual e o motivo.

Validações do backend (Docker precisa estar rodando para o Testcontainers):

- `./mvnw verify` — compila, roda unitários, integração e ArchUnit;
- smoke: `docker compose up -d` + `./mvnw spring-boot:run` + `curl localhost:3000/health` quando a issue mexer em configuração, banco ou subida.

Se a infraestrutura de testes ainda não existir, não declare critério coberto por teste: avise que a issue do esqueleto é pré-requisito.

### Documentação (docs/SPEC.md e docs/ARQUITETURA.md)

Compare o comportamento final com o `docs/SPEC.md`. Se a mudança altera ou revela regra de negócio, atualize a regra (ou crie uma nova com o próximo número livre) na mesma entrega, referenciando a issue (`#<id>`), e preencha a seção 8 (Status de implementação) com as regras entregues. Não renumere regras e não invente decisões de produto.

Se a mudança altera a estrutura (pacote novo, novo tipo de gateway, nova convenção), atualize o `docs/ARQUITETURA.md`.

Antes do commit, revise o diff e confirme que:

- todos os critérios de aceite foram atendidos, cada um com teste ou verificação registrada;
- SPEC e ARQUITETURA foram avaliados e atualizados quando preciso;
- não há mudança fora do escopo da issue;
- não há segredos nem artefatos gerados (`target/`, `.idea/`);
- `./mvnw verify` passou.

Não prossiga para commit, push ou Project com falha relevante ou implementação incompleta.

## Commit, envio e Pull Request

Um único commit final quando isso representar bem a mudança:

- feature: `feat #<id>: <mensagem do que foi implementado>`
- bugfix: `fix #<id>: <mensagem do que foi implementado>`

Mensagem curta, específica, em português. Espaço após `feat`/`fix`, `#`, ID e dois-pontos são obrigatórios.

Envie a branch ao `origin` com upstream e confirme que o commit remoto é o local.

Abra o PR com `gh pr create -R MaceloAlexandre/<repo> --base main --head <branch>`:

- título igual à mensagem do commit;
- corpo com: `Refs #<id>` (não `Closes`); resumo; critérios de aceite, cada um com o teste ou verificação que o comprova; regras do SPEC tocadas; validações executadas (comando e resultado); issue vinculada no outro repo, se houver.

Se houver CI, aguarde (`gh pr checks <n> -R MaceloAlexandre/<repo> --watch`). Se falhar, corrija na mesma branch com novo commit no mesmo formato. **Não faça merge**: o merge é do usuário.

## Atualização do Project

1. Com o PR aberto (e CI verde, se houver), mova o item para `In review` e confirme a persistência.
2. Quando o usuário disser que o PR foi mergeado, confirme com `gh pr view <n> -R MaceloAlexandre/<repo> --json state,mergedAt` que está `MERGED`. Só então mova para `Done`, feche a issue com `gh issue close <id> -R MaceloAlexandre/<repo> --comment "Entregue no PR #<n>"` e verifique ambos.

Se commit, push, PR ou mudança de status falhar, não declare conclusão. Não force push nem repita mutações às cegas; relate a etapa pendente e o erro.

## Formato de issue

```markdown
## Contexto
<por que isso importa; 2–4 frases>

## Critérios de aceite
- [ ] **CA1** — Dado <estado inicial>, quando <ação>, então <resultado observável: status, corpo, estado no banco, evento emitido>.
- [ ] **CA2** — ...

## Fora de escopo
- <o que não fazer nesta issue>

## Regras do SPEC afetadas
- `docs/SPEC.md` — RNxx <entregue / alterada / nova>

## Classes envolvidas
- <use case, service, gateways e entidades que a issue cria ou altera>

## Dependências
- Depende de #<n> (ou "nenhuma")

## Como validar
- <teste automatizado esperado e/ou passo manual objetivo>
```

Labels: `enhancement` (feature) ou `bug` (bugfix). Milestone = etapa. Uma issue por repo; sem critério vago.

## Entrega

Ao concluir, informe de forma concisa:

- issue e repositório; classificação;
- branch criada e branch-base;
- resumo da implementação (classes criadas por camada);
- critérios de aceite × teste que cobre cada um;
- regras atualizadas no SPEC (ou por que não precisou);
- hash e mensagem exata do commit; confirmação do push;
- link do PR e resultado do CI (ou "sem CI — validado localmente");
- status do item no Project.
