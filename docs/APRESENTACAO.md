# Roteiro de apresentação — rede local da faculdade

Tudo roda num notebook: bancos (as imagens ficam no MongoDB), API e frontend. Os celulares da
turma acessam o React pela rede Wi-Fi da sala. Não precisa de internet depois que as imagens
Docker e as dependências já foram baixadas (faça isso em casa, antes).

## 1. Antes do dia (com internet)

```bash
# Backend
cd fademy_backend
docker compose pull                 # postgres, mongo
./mvnw -q -DskipTests package       # baixa as dependências do Maven

# Frontend
cd ../fademy_frontend
npm install
```

Pré-requisitos no notebook: Docker (com `docker compose`), **JDK 25** e Node 20+.

## 2. No dia

### 2.1 Descobrir o IP do notebook na rede

```bash
ip -4 addr show scope global | grep inet
```

Os exemplos abaixo usam `192.168.0.10`; troque pelo IP que aparecer. Use **sempre o IP**,
inclusive no próprio notebook, porque o CORS da API libera uma origem só.

### 2.2 Subir bancos e armazenamento

```bash
cd fademy_backend
docker compose up -d
docker compose ps          # postgres e mongo "running"
```

### 2.3 Subir a API com os dados de demonstração

```bash
FRONTEND_ORIGIN=http://192.168.0.10:5173 \
IMAGENS_URL_PUBLICA=http://192.168.0.10:8080/api/media \
SPRING_PROFILES_ACTIVE=demo \
./mvnw spring-boot:run
```

- `FRONTEND_ORIGIN`: de onde o React chama a API (CORS).
- `IMAGENS_URL_PUBLICA`: endereço das fotos (a rota `/api/media` da própria API); com `localhost`, o celular não conseguiria abrir as imagens.
  O banco guarda só a chave de cada imagem, então os dados criados em casa, com outro IP,
  aparecem certos na faculdade: basta subir a API com o IP do dia.
- `SPRING_PROFILES_ACTIVE=demo`: cria as pessoas, follows, posts e a conversa da seção 3 na
  primeira subida. Nas próximas subidas não cria nada de novo.

Conferir: `curl http://192.168.0.10:8080/health` → `{"status":"ok","postgres":"up","mongo":"up"}`.

### 2.4 Subir o frontend acessível pela rede

```bash
cd ../fademy_frontend
VITE_API_URL=http://192.168.0.10:8080 npm run dev -- --host
```

`--host` faz o Vite aceitar conexões de outros aparelhos. No notebook e nos celulares:
**http://192.168.0.10:5173**.

Se o celular não abrir, confira se o firewall do notebook libera as portas 5173 (frontend),
8080 (API e fotos).

## 3. Contas de demonstração

Todas as contas de demonstração têm a senha **`demo1234`**.

| Pessoa | E-mail | Papel | Observação |
|---|---|---|---|
| Admin | `admin@fademy.com` (senha `changeme`) | admin | Criado pela carga inicial |
| Profa. Helena Duarte | `helena@demo.fademy` | professor | Tem um post com link e uma conversa com a Ana |
| Coord. Marcos Lima | `marcos@demo.fademy` | coordenador | |
| Ana Souza | `ana@demo.fademy` | aluno | Segue a Helena e a Helena a segue (podem conversar) |
| Bruno Costa | `bruno@demo.fademy` | aluno | **Perfil privado**; tem um pedido de seguir pendente da Carla |
| Carla Nunes | `carla@demo.fademy` | aluno | |

Todos entraram na rede **por convite**, pelos mesmos casos de uso que o aplicativo usa
(`GerarConviteUseCase` → `EnviarAvatarUseCase` → `RegistrarViaConviteUseCase`). Ver
`infrastructure/config/DadosDeDemonstracao.java`.

## 4. Roteiro da demonstração

| # | O que mostrar | Onde está no código |
|---|---|---|
| 1 | **Entrar por convite.** No notebook, entrar como admin, abrir *Convites*, gerar um convite de aluno e mostrar o QR code. Um colega lê o QR no celular, envia uma foto e se cadastra. | Diagrama de sequência em `docs/DIAGRAMAS.md` (seção 6); `RegistrarViaConviteService` |
| 2 | **Dois bancos no mesmo caso de uso.** O cadastro grava o usuário e a vaga no PostgreSQL (uma transação) e a notificação "convite usado" no MongoDB. Mostrar a notificação chegando para o admin. | `RegistrarViaConviteService`; `PostgresUsuarioGateway`, `PostgresConviteGateway`, `MongoNotificacaoGateway` |
| 3 | **Singleton.** Mostrar o `DatabaseManager` ao lado do exemplo do professor e explicar por que o `getInstance()` sem `synchronized` é seguro aqui. | `infrastructure/database/DatabaseManager.java`, `DatabaseConfig.java`; `docs/referencias/exemplo-singleton-adapter/`; `docs/ARQUITETURA.md` seção 6 |
| 4 | **Adapter.** Cada gateway é um adapter de um banco atrás de uma interface; o service não sabe qual banco está do outro lado. | `application/gateway/*` × `infrastructure/postgres/*`, `infrastructure/mongo/*`; `docs/DIAGRAMAS.md` seção 5 |
| 5 | **Herança e polimorfismo.** Entrar como aluna (Ana) e tentar publicar com link: 403. Entrar como a Helena: publica. Nenhum `if` compara papel por texto. | `domain/usuario/`: `Usuario`, `MembroDaEquipe`, `Administrador`; `Post.publicar` |
| 6 | **Perfil privado.** Como Carla, abrir o perfil do Bruno: sem números nem posts. Como Bruno, ver o pedido nas notificações e aceitar. | `Usuario.mostraConteudoPara`, `Seguir.aceitarPor` |
| 7 | **Feed e comentários.** Como Ana, ver o feed (posts de quem ela segue), comentar o post da Helena; a Helena recebe a notificação. | `VerFeedService`, `ComentarService` (comentário + contador na mesma transação) |
| 8 | **Chat em tempo real.** Ana e Helena, uma em cada aparelho, na mesma conversa: a mensagem aparece na hora no outro. | `MensagensService`, `SocketIoTempoRealGateway`, `infrastructure/temporeal/` |
| 9 | **Bloqueio nos dois bancos.** Bloquear alguém: somem os follows (Postgres) e os pedidos de seguir entre os dois (MongoDB); a conversa deixa de aceitar mensagens. | `BloquearUsuarioService` |
| 10 | **Clean Architecture sob teste.** Rodar `./mvnw verify`: cerca de 300 testes, incluindo o `ArquiteturaTest`, que quebra se o domínio ou a aplicação importarem Spring ou banco. | `src/test/java/br/com/fademy/ArquiteturaTest.java` |

## 5. Se algo der errado

| Sintoma | Causa provável | O que fazer |
|---|---|---|
| `/health` com `"mongo":"down"` ou `"postgres":"down"` | Container parado | `docker compose up -d` e `docker compose ps` |
| Celular abre a tela, mas login falha | `VITE_API_URL` ou `FRONTEND_ORIGIN` com `localhost` | Subir de novo com o IP (seções 2.3 e 2.4) |
| Fotos não aparecem no celular | `IMAGENS_URL_PUBLICA` com `localhost` ou com o IP antigo | Subir a API de novo com o IP do dia (as fotos já gravadas passam a sair com ele) |
| QR code abre `localhost` no celular | O notebook abriu o React por `localhost` | Abrir `http://<IP>:5173` também no notebook |
| Quero começar do zero | Dados antigos | `docker compose down -v` (apaga os volumes) e subir de novo com `demo` |
