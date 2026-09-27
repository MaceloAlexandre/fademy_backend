# Backlog — reescrita da Fademy API em Java

Plano de etapas. Cada etapa é um **milestone** do `fademy_backend`; cada item é uma issue no
Project **Fademy**. Issues e PRs dividem a mesma numeração no GitHub, então os números das
etapas E3 em diante são os reais, atribuídos quando a etapa foi publicada.

As issues da E1 e da E2 estão completas abaixo. As das etapas seguintes estão resumidas; o
corpo completo (critérios Dado/Quando/Então) fica na própria issue do GitHub, escrito quando
a etapa anterior termina.

| Etapa | Issues | Resultado ao final |
|---|---|---|
| E1 — Esqueleto | #1–#3 | Projeto compila, Singleton conectado aos dois bancos, `/health` e erros no padrão |
| E2 — Identidade | #4–#6 | Hierarquia de usuários, carga inicial, login funcionando no frontend |
| E3 — Entrada | #13–#16 | Fluxo completo de convite → cadastro (o diagrama de sequência) |
| E4 — Social | #21–#26 | Notificações no Mongo, perfil, busca, seguir, bloquear |
| E5 — Conteúdo | #33–#36 | Posts, feed, comentários |
| E6 — Chat | a publicar | Conversas e mensagens no Mongo, tempo real |
| E7 — Fechamento | a publicar | Painel admin, diagramas UML novos, roteiro da apresentação |

---

## E1 — Esqueleto

### #1 — Projeto Spring Boot com as camadas da Clean Architecture

`enhancement` · milestone E1

## Contexto
Primeira peça da reescrita. Cria o projeto Java vazio com os três pacotes da Clean Architecture
e um teste que impede que alguém quebre a regra de dependência sem perceber. Sem isso, a
arquitetura vira só intenção.

## Critérios de aceite
- [ ] **CA1** — Dado o repositório clonado numa máquina com Java 25 e sem Maven, quando rodo `./mvnw verify`, então o projeto compila com Java 25 e os testes passam.
- [ ] **CA2** — Dado o projeto, então existem os pacotes `br.com.fademy.domain`, `br.com.fademy.application` (`usecase`, `service`, `gateway`) e `br.com.fademy.infrastructure`, como no `docs/ARQUITETURA.md` seção 3.
- [ ] **CA3** — Dada uma classe em `domain` ou `application` que importa `org.springframework`, `jakarta.persistence`, `org.bson` ou `com.mongodb`, quando rodo `./mvnw verify`, então um teste ArchUnit falha citando a classe.
- [ ] **CA4** — Dada uma classe em `domain` que importa `application` ou `infrastructure`, ou uma classe em `application` que importa `infrastructure`, quando rodo `./mvnw verify`, então um teste ArchUnit falha citando a classe.
- [ ] **CA5** — Dado um PR aberto para a `main`, então o GitHub Actions roda `./mvnw verify` e o resultado aparece no PR.
- [ ] **CA6** — `target/`, `.idea/`, `*.iml` e `.env` estão no `.gitignore`.

## Fora de escopo
- Conexão com bancos, endpoints, Docker Compose (#2).

## Regras do SPEC afetadas
- nenhuma (estrutura: `docs/ARQUITETURA.md` seções 2 e 3)

## Classes envolvidas
- `FademyApplication`, `ArquiteturaTest` (ArchUnit)

## Dependências
- nenhuma

## Como validar
- `./mvnw verify` verde; para CA3/CA4, criar localmente uma classe que viola a regra e ver o teste falhar (não commitar).

---

### #2 — Singleton `DatabaseManager` com PostgreSQL e MongoDB

`enhancement` · milestone E1

## Contexto
Coração do requisito da disciplina: um Singleton, no formato do exemplo do professor, que
guarda a conexão com o Postgres e com o Mongo, usadas pelo resto da aplicação. Também sobe o
ambiente local com Docker e o `/health`, que prova que os dois bancos estão de pé.

## Critérios de aceite
- [ ] **CA1** — Dado o repositório, quando rodo `docker compose up -d`, então sobem PostgreSQL 17, MongoDB 8 e SeaweedFS (S3), e o bucket `fademy` é criado com leitura pública.
- [ ] **CA2** — Dado `DatabaseManager`, quando chamo `getInstance()` duas vezes, então recebo o mesmo objeto.
- [ ] **CA3** — Dada a aplicação no ar, então o `DataSource` e o `MongoClient` que o Spring injeta são exatamente os objetos devolvidos por `DatabaseManager.getInstance().getPostgres()` e `.getMongo()`.
- [ ] **CA4** — Dado que nenhuma classe fora de `DatabaseConfig` chama `DatabaseManager.getInstance()`, então um teste ArchUnit garante isso.
- [ ] **CA5** — Dadas as variáveis `POSTGRES_URL`, `POSTGRES_USER`, `POSTGRES_PASSWORD` e `MONGO_URI` ausentes, quando a aplicação sobe, então conecta nos bancos do `docker-compose` em `localhost`.
- [ ] **CA6** — Dados os dois bancos no ar, quando a aplicação sobe, então o Flyway roda (tabela `flyway_schema_history` existe) e `GET /health` responde 200 `{ "status": "ok", "postgres": "up", "mongo": "up" }` (RN45).
- [ ] **CA7** — Dado o Mongo fora do ar, quando chamo `GET /health`, então responde 503 `{ "status": "degraded", "postgres": "up", "mongo": "down" }`; e o simétrico com o Postgres fora (RN45).
- [ ] **CA8** — Existe uma base de teste de integração que sobe Postgres e Mongo com Testcontainers uma vez por execução e preenche as propriedades lidas pelo `DatabaseManager`.

## Fora de escopo
- Tabelas de negócio (#5 em diante). Gateway de armazenamento S3 (#7).

## Regras do SPEC afetadas
- `docs/SPEC.md` — RN45 entregue

## Classes envolvidas
- `DatabaseManager`, `ConfiguracaoBancos`, `DatabaseConfig`, `VerificarSaudeUseCase`/`VerificarSaudeService`, `SaudeGateway` + `PostgresSaudeGateway` e `MongoSaudeGateway`, `SaudeController`

## Dependências
- Depende de #1

## Como validar
- Testes de CA2–CA8 no `./mvnw verify`; manual: `docker compose up -d`, `./mvnw spring-boot:run`, `curl -i localhost:3000/health`, depois `docker compose stop mongo` e `curl` de novo.

---

### #3 — Formato padrão de erro, datas e CORS

`enhancement` · milestone E1

## Contexto
O frontend lê `{ error }` em toda resposta de erro e espera datas ISO. Centralizar isso agora
evita que cada endpoint trate erro de um jeito, e liga as exceções do domínio aos status HTTP.

## Critérios de aceite
- [ ] **CA1** — Dado um use case que lança `DadosInvalidosException("x")`, `NaoAutenticadoException("x")`, `AcessoNegadoException("x")`, `RecursoNaoEncontradoException("x")` ou `ConflitoException("x")`, quando a rota é chamada, então a resposta é 400, 401, 403, 404 ou 409, respectivamente, com corpo `{ "error": "x" }`.
- [ ] **CA2** — Dada uma exceção inesperada (ex.: `RuntimeException`), então a resposta é 500 `{ "error": "Internal server error" }`, sem stack trace no corpo, e o erro é registrado no log.
- [ ] **CA3** — Dado um `Instant` num DTO de resposta, então ele é serializado como ISO-8601 em UTC com `Z` (ex.: `2026-09-27T18:00:00Z`).
- [ ] **CA4** — Dada uma requisição com `Origin` igual a `FRONTEND_ORIGIN` (padrão `http://localhost:5173`), então a resposta traz `Access-Control-Allow-Origin` com essa origem; com outra origem, não traz.

## Fora de escopo
- Autenticação (#6).

## Regras do SPEC afetadas
- `docs/SPEC.md` — seção 5.1 (erros, datas, CORS) entregue

## Classes envolvidas
- exceções em `domain/excecao`, `TratadorDeErros` (`@RestControllerAdvice`), configuração do Jackson e de CORS

## Dependências
- Depende de #1

## Como validar
- Teste web com um controller de teste que lança cada exceção; teste de serialização de data; teste de CORS com MockMvc.

---

## E2 — Identidade

### #4 — Hierarquia de usuários e permissões por papel

`enhancement` · milestone E2

## Contexto
A análise do grupo apontou que no Node os cinco papéis viraram comparação de texto espalhada
pelas rotas. Aqui eles viram uma hierarquia de classes: cada papel responde o que pode fazer.
É a parte mais "orientação a objetos" do projeto e não depende de banco.

## Critérios de aceite
- [ ] **CA1** — Dado cada papel da RN04, quando `Papel.criarUsuario(...)` é chamado, então devolve a subclasse correspondente (`Aluno`, `Professor`, `Coordenador`, `Funcionario`, `Administrador`), e `usuario.papel()` devolve o papel de origem.
- [ ] **CA2** — Dado um usuário de cada papel, então `podeGerarConvite()`, `podePublicarLink()` e `podeAcessarPainelAdmin()` devolvem exatamente a tabela da RN05.
- [ ] **CA3** — Dado um usuário de cada papel e cada papel-alvo, então `podeGerarConvitePara(alvo)` devolve `true` só quando `podeGerarConvite()` é `true` e (`alvo` não é `admin` ou o usuário é `Administrador`) (RN05).
- [ ] **CA4** — Dado cada papel-alvo, então `papel.vagasMaximasPorConvite()` devolve 50 para `aluno` e 10 para os demais (RN06).
- [ ] **CA5** — Dado o texto `"aluno"`, `"coordenador"`, `"professor"`, `"funcionario"` ou `"admin"`, então `Papel.deTexto(...)` devolve o papel; com outro texto, lança `DadosInvalidosException`.
- [ ] **CA6** — Nenhuma classe fora de `domain/usuario` compara papel por texto ou por `instanceof` para decidir permissão (revisão no PR).

## Fora de escopo
- Persistência de usuário (#5). Login (#6).

## Regras do SPEC afetadas
- `docs/SPEC.md` — RN04, RN05, RN06 entregues (no domínio)

## Classes envolvidas
- `Usuario` (abstrata), `Aluno`, `MembroDaEquipe` (abstrata), `Professor`, `Coordenador`, `Funcionario`, `Administrador`, `Papel`

## Dependências
- Depende de #1

## Como validar
- Testes unitários puros (sem Spring), um teste parametrizado por papel para a tabela da RN05.

---

### #5 — Carga inicial: instituição, categorias e admin

`enhancement` · milestone E2

## Contexto
Primeiras tabelas no Postgres e primeiros gateways de verdade. Ao subir, a aplicação garante
que a Católica PB, as dez categorias e o admin inicial existem, para o grupo conseguir logar
logo em seguida.

## Critérios de aceite
- [ ] **CA1** — Dado um banco vazio, quando a aplicação sobe, então existem a instituição `catolica-pb` "Faculdade Católica da Paraíba" ativa, as 10 categorias da RN02 com `slug` e `type` corretos e um usuário `admin` com e-mail `BOOTSTRAP_ADMIN_EMAIL` nessa instituição (RN03).
- [ ] **CA2** — Dado que a aplicação já subiu uma vez e a senha do admin foi trocada no banco, quando ela sobe de novo, então não há registros duplicados e a senha do admin não muda (RN03).
- [ ] **CA3** — Dado o admin criado, então a senha está guardada com bcrypt e confere com `BOOTSTRAP_ADMIN_PASSWORD`.
- [ ] **CA4** — Dado um usuário com e-mail `Ana@x.com`, quando tento gravar outro com `ana@x.com` (em qualquer instituição), então o gateway lança `ConflitoException` (RN09, D1).
- [ ] **CA5** — Dado um usuário salvo e lido de volta pelo `UsuarioGateway`, então vem como a subclasse do seu papel (#4) e com id UUIDv7.
- [ ] **CA6** — As tabelas são criadas por migrations Flyway (`V1__...`), com `UNIQUE` em `instituicao.slug`, `categoria.slug` e `lower(usuario.email)`.

## Fora de escopo
- `GET /api/categories` (#6). Cadastro por convite (#10).

## Regras do SPEC afetadas
- `docs/SPEC.md` — RN02 (dados), RN03, RN09 entregues

## Classes envolvidas
- `Instituicao`, `Categoria`; `InstituicaoGateway`, `CategoriaGateway`, `UsuarioGateway`, `SenhaGateway` + implementações Postgres/bcrypt; `CarregarDadosIniciaisUseCase`/`Service`; `CargaInicial` (dispara na subida)

## Dependências
- Depende de #2, #4

## Como validar
- Service testado com gateways mockados (idempotência); gateways testados com Testcontainers; subir duas vezes e contar registros.

---

### #6 — Login com JWT e rotas autenticadas

`enhancement` · milestone E2

## Contexto
Fecha a E2 com o primeiro fluxo visível no frontend: o admin inicial entra na tela de login
do React atual. Também cria o filtro que todas as rotas protegidas vão usar, provado na
primeira delas, `GET /api/categories`.

## Critérios de aceite
- [ ] **CA1** — Dado o admin inicial, quando `POST /api/auth/login` com e-mail e senha corretos, então 200 `{ token }`, e o token é um JWT HS256 com `sub`, `role` e `institutionId` válido por `JWT_EXPIRES_IN` (RN07).
- [ ] **CA2** — Dado e-mail inexistente, senha errada ou usuário inativo, quando faço login, então 401 `{ "error": "Invalid credentials" }` nos três casos (RN07).
- [ ] **CA3** — Dado corpo sem `email` ou sem `password`, então 400 `{ "error": "email and password are required" }` (RN07).
- [ ] **CA4** — Dado `GET /api/categories` sem cabeçalho `Authorization`, então 401 `{ "error": "Authentication required" }`; com token inválido, expirado ou assinado com outro algoritmo, 401 `{ "error": "Invalid or expired token" }` (RN08).
- [ ] **CA5** — Dado um token válido, quando `GET /api/categories`, então 200 `{ categories: [ { _id, name, slug, type } ] }` com as 10 categorias (RN02).
- [ ] **CA6** — Dado o frontend React atual apontando para a API Java, quando o admin faz login pela tela, então o token é salvo e ele é redirecionado para o feed (verificação manual; o feed em si ainda falha, porque as rotas dele vêm na E5).

## Fora de escopo
- `GET /api/users/me` (#12). Verificação de permissão por papel nas rotas (entra com a primeira rota restrita, #8).

## Regras do SPEC afetadas
- `docs/SPEC.md` — RN02 (endpoint), RN07, RN08 entregues

## Classes envolvidas
- `AutenticarUseCase`/`Service`, `TokenGateway` + `JwtTokenGateway`, `ListarCategoriasUseCase`/`Service`, filtro de autenticação, `Visualizador` (quem está autenticado), `AuthController`, `CategoriaController`

## Dependências
- Depende de #3, #5

## Como validar
- Service com gateways mockados; testes web dos status e mensagens; passo manual CA6 com `npm run dev` no frontend.

---

## E3 — Entrada (resumo)

| # | Título | Regras | Destaque |
|---|---|---|---|
| 13 | Armazenamento de imagens e upload de avatar | RN14, RN15 (validação) | `ArmazenamentoGateway` + `S3ArmazenamentoGateway` no SeaweedFS; `POST /api/media/upload` |
| 14 | Gerar e revogar convite | RN08 (403), RN10, RN12 | `Convite` com comportamento; primeira rota restrita por papel, decidida por `usuario.podeGerarConvitePara()` |
| 15 | Consultar convite (público) | RN11 | `Convite.situacao(agora)` devolve `revoked`/`expired`/`full`/válido |
| 16 | Cadastro via convite | RN13, RN16, RN34, RN37, RN46, RN47 | Transação usuário + vaga; consumo atômico; notificação `invite_received` já gravada no MongoDB |

## E4 — Social (resumo)

Reordenada pelas dependências: seguir vem primeiro, porque perfil, busca e notificações usam os follows.

| # | Título | Regras | Destaque |
|---|---|---|---|
| 21 | Seguir e deixar de seguir | RN21, RN22 | `Seguir.solicitar()`: privado nasce `pending`; cria também a tabela de bloqueios |
| 22 | Aceitar e rejeitar pedido de seguir | RN23, RN24 | `Seguir.aceitarPor()` / `rejeitarPor()` |
| 23 | Bloquear e desbloquear | RN25, RN26, RN47 | Postgres (follows, bloqueio) + Mongo (limpeza de pedidos) no mesmo caso de uso |
| 24 | Ver e editar perfil | RN17, RN18 | Perfil privado × público; `GET/PATCH /api/users/me` |
| 25 | Busca e sugestões de usuários | RN19, RN20 | Exclusão de bloqueios e follows; equipe primeiro |
| 26 | Listar, ler e excluir notificações | RN35, RN36, RN48 | Mongo + Postgres na leitura; primeira paginação por cursor |

## E5 — Conteúdo (resumo)

| # | Título | Regras | Destaque |
|---|---|---|---|
| 33 | Publicar post | RN28, RN15, D9 | `Post.publicar()` consulta `autor.podePublicarLink()`; imagens pelo #13; tudo validado antes do upload |
| 34 | Feed com paginação por cursor | RN29, seção 5.4 | Cursor sobre UUIDv7 |
| 35 | Ver post e posts de um usuário | RN30, RN31, D5, D6 | Visibilidade por bloqueio e perfil privado (`mostraConteudoPara`) |
| 36 | Comentar e listar comentários | RN32, RN33, RN46, D4 | Comentário + contador na mesma transação; `post_comment` no Mongo |

## E6 — Chat (resumo)

| # | Título | Regras | Destaque |
|---|---|---|---|
| — | Conversas 1:1 | RN38–RN40 | Conversa no Mongo, regra de seguidores mútuos e bloqueio no Postgres |
| — | Enviar e listar mensagens | RN41, RN42 | Mensagem no Mongo + notificação + evento |
| — | Tempo real com Socket.IO | RN43 | `TempoRealGateway` + netty-socketio; o chat do React atual atualiza sozinho |

## E7 — Fechamento (resumo)

| # | Título | Regras | Destaque |
|---|---|---|---|
| — | Painel administrativo | RN44 | `usuario.podeAcessarPainelAdmin()` |
| — | Diagramas UML do novo projeto | — | Diagrama de classes de verdade (métodos, herança, interfaces de use case e gateway), sequência do cadastro atualizada, pacotes |
| — | Roteiro de apresentação na rede local | — | Subir tudo num notebook, frontend acessível pelo celular na rede da faculdade, dados de demonstração |

---

## Labels e milestones a criar

- Labels: `enhancement`, `bug` (padrão do GitHub).
- Milestones: `E1 — Esqueleto`, `E2 — Identidade`, `E3 — Entrada`, `E4 — Social`,
  `E5 — Conteúdo`, `E6 — Chat`, `E7 — Fechamento`.
