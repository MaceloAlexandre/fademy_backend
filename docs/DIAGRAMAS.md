# Diagramas — Fademy API

Diagramas do projeto em Java, em Mermaid (o GitHub desenha direto nesta página). Todos os
nomes de classe e de método são os do código. Nos diagramas de classe, os métodos de acesso
(`id()`, `nome()`...) foram omitidos para caber.

A análise de POO do projeto antigo (`docs/referencias/analise-sobre-poo.txt`) apontava que o
diagrama de lá era de **dados**. Estes são de **classes**: mostram comportamento, herança,
polimorfismo e as dependências entre as camadas.

1. [Casos de uso](#1-casos-de-uso)
2. [Camadas e pacotes](#2-camadas-e-pacotes)
3. [Domínio](#3-domínio)
4. [Use case, service e gateway](#4-use-case-service-e-gateway)
5. [Singleton e Adapters](#5-singleton-e-adapters)
6. [Sequência: cadastro via convite](#6-sequência-cadastro-via-convite)

---

## 1. Casos de uso

Cada caso de uso do diagrama original virou uma interface em `application/usecase`.

```mermaid
flowchart LR
    Convidado([Convidado])
    Aluno([Aluno])
    Equipe([Coordenador / Professor / Funcionário])
    Admin([Admin])

    subgraph Fademy
        EA[EnviarAvatarUseCase]
        CC[ConsultarConviteUseCase]
        RC[RegistrarViaConviteUseCase]
        AU[AutenticarUseCase]
        VP[VerPerfilUseCase / EditarPerfilUseCase]
        BU[BuscarUsuariosUseCase / SugerirUsuariosUseCase]
        SE[SeguirUsuarioUseCase / DeixarDeSeguirUseCase]
        AR[AceitarPedidoUseCase / RejeitarPedidoUseCase]
        BL[BloquearUsuarioUseCase / DesbloquearUsuarioUseCase]
        PP[PublicarPostUseCase]
        FE[VerFeedUseCase / VerPostUseCase / ListarPostsDoUsuarioUseCase]
        CO[ComentarUseCase]
        NO[ListarNotificacoesUseCase / LerNotificacaoUseCase]
        CV[IniciarConversaUseCase / ListarConversasUseCase / MensagensUseCase]
        GC[GerarConviteUseCase / RevogarConviteUseCase]
        PA[VerPainelAdminUseCase]
    end

    Convidado --> EA & CC & RC
    Aluno --> AU & VP & BU & SE & AR & BL & PP & FE & CO & NO & CV
    Equipe --> AU & VP & BU & SE & AR & BL & PP & FE & CO & NO & CV & GC
    Admin --> AU & GC & PA
```

A equipe faz tudo o que o aluno faz e também gera convites e publica links; o admin, além
disso, acessa o painel. Essa diferença não é `if` no código: é a hierarquia da seção 3.

## 2. Camadas e pacotes

As setas são dependências de código. Elas só apontam para dentro, e o teste
`ArquiteturaTest` (ArchUnit) quebra o build se alguém inverter uma delas.

```mermaid
flowchart TB
    subgraph infrastructure["infrastructure (Spring, JDBC, MongoDB, S3, Socket.IO)"]
        web["web<br/>controllers, DTOs, TratadorDeErros, Autenticacao"]
        config["config<br/>UseCaseConfig, CargaInicial"]
        database["database<br/>DatabaseManager (Singleton), DatabaseConfig"]
        postgres["postgres<br/>Postgres*Gateway"]
        mongo["mongo<br/>Mongo*Gateway"]
        armazenamento["armazenamento<br/>S3ArmazenamentoGateway"]
        seguranca["seguranca<br/>JwtTokenGateway, BcryptSenhaGateway"]
        temporeal["temporeal<br/>SocketIoConfig, SalasSocketIo, SocketIoTempoRealGateway"]
    end
    subgraph application["application (Java puro)"]
        usecase["usecase<br/>interfaces dos casos de uso"]
        service["service<br/>implementações"]
        gateway["gateway<br/>interfaces do mundo externo"]
    end
    subgraph domain["domain (Java puro)"]
        entidades["usuario, convite, social, conteudo,<br/>conversa, notificacao, instituicao, midia, excecao"]
    end

    web --> usecase
    config --> service
    postgres & mongo & armazenamento & seguranca & temporeal --> gateway
    postgres & mongo --> database
    service --> usecase
    service --> gateway
    usecase --> entidades
    gateway --> entidades
    service --> entidades
```

## 3. Domínio

### 3.1 Usuários: herança e polimorfismo

`Usuario` responde `false` para todas as permissões; cada subclasse libera o que o papel
permite. `Papel.criarUsuario` é a fábrica que monta a subclasse certa a partir do papel gravado.

```mermaid
classDiagram
    class Usuario {
        <<abstract>>
        -String id
        -String instituicaoId
        -String nome
        -String email
        -String senhaHash
        -String avatarUrl
        -boolean privado
        -boolean ativo
        -Instant criadoEm
        +papel()* Papel
        +ehDaEquipe() boolean
        +podeGerarConvite() boolean
        +podeGerarConvitePara(Papel alvo) boolean
        +podePublicarLink() boolean
        +podeAcessarPainelAdmin() boolean
        +mostraConteudoPara(String visualizadorId, boolean seguidoPeloVisualizador) boolean
        +mesmaInstituicao(Usuario outro) boolean
        +definirPrivacidade(boolean privado)
        +trocarAvatar(String avatarUrl)
    }
    class Aluno {
        +papel() Papel
    }
    class MembroDaEquipe {
        <<abstract>>
        +ehDaEquipe() true
        +podeGerarConvite() true
        +podePublicarLink() true
    }
    class Professor {
        +papel() Papel
    }
    class Coordenador {
        +papel() Papel
    }
    class Funcionario {
        +papel() Papel
    }
    class Administrador {
        +papel() Papel
        +podeGerarConvitePara(Papel alvo) true
        +podeAcessarPainelAdmin() true
    }
    class Papel {
        <<enumeration>>
        ADMIN
        COORDENADOR
        PROFESSOR
        FUNCIONARIO
        ALUNO
        +texto() String
        +vagasMaximasPorConvite() int
        +deTexto(String texto)$ Papel
        +criarUsuario(DadosUsuario dados) Usuario
    }
    class DadosUsuario {
        <<record>>
    }
    class Visualizador {
        <<record>>
        String id
        Papel papel
        String instituicaoId
    }

    Usuario <|-- Aluno
    Usuario <|-- MembroDaEquipe
    MembroDaEquipe <|-- Professor
    MembroDaEquipe <|-- Coordenador
    MembroDaEquipe <|-- Funcionario
    MembroDaEquipe <|-- Administrador
    Papel ..> Usuario : cria
    Papel ..> DadosUsuario : usa
    Visualizador --> Papel
```

### 3.2 Entidades com comportamento

Cada regra de negócio sobre um objeto é método dele. O service só orquestra.

```mermaid
classDiagram
    class Convite {
        -Papel papelAlvo
        -String token
        -int maxUsos
        -int usos
        -StatusConvite status
        -Instant expiraEm
        +gerar(Usuario autor, String papelAlvo, Integer maxUsos, Instant expiraEm, Instant agora)$ Convite
        +situacao(Instant agora) SituacaoConvite
        +garantirValido(Instant agora)
        +vagasRestantes() int
        +revogar()
        +daInstituicao(String instituicaoId) boolean
    }
    class StatusConvite {
        <<enumeration>>
        ACTIVE
        REVOKED
        EXPIRED
    }
    class SituacaoConvite {
        <<enumeration>>
        VALIDO
        REVOGADO
        EXPIRADO
        CHEIO
        +valido() boolean
        +motivo() String
    }
    class Seguir {
        -String seguidorId
        -String seguidoId
        -StatusSeguir status
        +solicitar(Usuario seguidor, Usuario alvo, Instant agora)$ Seguir
        +notificacaoParaOSeguido() TipoNotificacao
        +garantirQueEhOSeguido(String usuarioId)
        +aceitarPor(String usuarioId)
    }
    class StatusSeguir {
        <<enumeration>>
        PENDING
        ACCEPTED
    }
    class Post {
        -String texto
        -List~String~ imagens
        -String linkUrl
        -List~String~ categoriaIds
        -int comentarios
        +publicar(Usuario autor, String texto, String linkUrl, List~String~ categoriaIds, Instant agora)$ Post
        +anexarImagem(String url)
        +temLink() boolean
        +daInstituicao(String instituicaoId) boolean
        +notificaAutorSobreComentarioDe(Usuario comentarista) boolean
    }
    class Comentario {
        <<record>>
        +escrever(Usuario autor, String postId, String texto, Instant agora)$ Comentario
    }
    class Conversa {
        <<record>>
        List~String~ membros
        +entre(Usuario criador, Usuario outro, Instant agora)$ Conversa
        +temMembro(String usuarioId) boolean
        +garantirMembro(String usuarioId)
        +outroMembro(String usuarioId) String
    }
    class Mensagem {
        <<record>>
        String texto
        List~String~ midias
        +escrever(String conversaId, String remetenteId, String texto, int quantidadeDeImagens, Instant agora)$ Mensagem
        +comMidias(List~String~ urls) Mensagem
    }
    class Notificacao {
        <<record>>
        TipoNotificacao tipo
        boolean lida
        +nova(TipoNotificacao tipo, String destinatarioId, String atorId, String refId, Instant agora)$ Notificacao
        +garantirDestinatario(String usuarioId)
    }
    class TipoNotificacao {
        <<enumeration>>
        NEW_FOLLOWER
        FOLLOW_REQUEST
        FOLLOW_ACCEPTED
        POST_COMMENT
        INVITE_RECEIVED
        MESSAGE_RECEIVED
        +texto() String
        +modeloReferencia() String
    }
    class Imagem {
        <<record>>
        +aceita(String tipo)$ boolean
        +novaChave(String pasta) String
    }
    class Categoria {
        <<record>>
        +fixas()$ List~Categoria~
    }

    Convite --> StatusConvite
    Convite ..> SituacaoConvite
    Convite ..> Usuario : autor
    Seguir --> StatusSeguir
    Seguir ..> TipoNotificacao
    Seguir ..> Usuario
    Post ..> Usuario : autor
    Post "1" o-- "*" Categoria : categoriaIds
    Comentario ..> Post : postId
    Conversa "1" o-- "*" Mensagem : conversaId
    Notificacao --> TipoNotificacao
```

## 4. Use case, service e gateway

O caso de uso mais completo, o cadastro via convite, com todas as peças. O controller
conhece só a interface do use case; o service conhece só interfaces de gateway; quem liga
cada service às implementações é o `UseCaseConfig`.

```mermaid
classDiagram
    direction LR
    class AuthController {
        +cadastrar(CadastroRequest corpo) UsuarioCadastradoResponse
    }
    class RegistrarViaConviteUseCase {
        <<interface>>
        +executar(Cadastro cadastro) Usuario
    }
    class RegistrarViaConviteService {
        +executar(Cadastro cadastro) Usuario
    }
    class ConviteGateway {
        <<interface>>
        +buscarPorToken(String token) Optional~Convite~
        +consumirVaga(String conviteId) boolean
    }
    class UsuarioGateway {
        <<interface>>
        +criar(Usuario usuario) Usuario
    }
    class ArmazenamentoGateway {
        <<interface>>
        +existe(String chave) boolean
    }
    class SenhaGateway {
        <<interface>>
        +gerarHash(String senha) String
    }
    class TransacaoGateway {
        <<interface>>
        +executar(Supplier acao) T
    }
    class NotificacaoGateway {
        <<interface>>
        +criar(Notificacao notificacao)
    }
    class PostgresConviteGateway
    class PostgresUsuarioGateway
    class S3ArmazenamentoGateway
    class BcryptSenhaGateway
    class SpringTransacaoGateway
    class MongoNotificacaoGateway

    AuthController --> RegistrarViaConviteUseCase
    RegistrarViaConviteUseCase <|.. RegistrarViaConviteService
    RegistrarViaConviteService --> ConviteGateway
    RegistrarViaConviteService --> UsuarioGateway
    RegistrarViaConviteService --> ArmazenamentoGateway
    RegistrarViaConviteService --> SenhaGateway
    RegistrarViaConviteService --> TransacaoGateway
    RegistrarViaConviteService --> NotificacaoGateway
    ConviteGateway <|.. PostgresConviteGateway
    UsuarioGateway <|.. PostgresUsuarioGateway
    ArmazenamentoGateway <|.. S3ArmazenamentoGateway
    SenhaGateway <|.. BcryptSenhaGateway
    TransacaoGateway <|.. SpringTransacaoGateway
    NotificacaoGateway <|.. MongoNotificacaoGateway
```

O mesmo desenho se repete nos outros 29 casos de uso: `XxxUseCase` (interface) ←
`XxxService` (implementação) → `YyyGateway` (interface) ← `PostgresYyyGateway` /
`MongoYyyGateway` / ... (implementação).

## 5. Singleton e Adapters

### 5.1 Exemplo do professor × Fademy

| Exemplo do professor (`docs/referencias/exemplo-singleton-adapter/`) | Fademy |
|---|---|
| `DatabaseManager.getInstance()` guarda os adapters de Postgres e Mongo | igual: `getPostgresAdapter()` e `getMongoAdapter()` (#61) |
| `DataBaseAdapter` (interface: `conectar`, `desconectar`) | igual |
| `PostgresAdapter`, `MongoAdapter` | igual; guardam o `DataSource` e o `MongoClient`/`MongoDatabase` |
| `salvarUsuario` no adapter | uma interface por necessidade (`UsuarioGateway`, `ConversaGateway`, ...), implementada por `Postgres*Gateway` e `Mongo*Gateway` com a conexão do adapter |
| `PostgresClient`, `MongoClient` (simulados) | `DataSource` (Hikari) e `MongoClient` do driver oficial, reais |
| `UsuarioService` recebe o adapter | cada `*Service` recebe as interfaces de gateway |
| `Main` chama `getInstance()` | só o `DatabaseConfig` chama `getInstance()`, na subida (regra verificada por ArchUnit) |

### 5.2 Diagrama

```mermaid
classDiagram
    class DatabaseManager {
        <<Singleton>>
        -DatabaseManager instance$
        -PostgresAdapter postgresAdapter
        -MongoAdapter mongoAdapter
        -DatabaseManager()
        +getInstance()$ DatabaseManager
        +getPostgresAdapter() PostgresAdapter
        +getMongoAdapter() MongoAdapter
    }
    class DataBaseAdapter {
        <<interface>>
        +conectar()
        +desconectar()
    }
    class PostgresAdapter {
        +getDataSource() DataSource
    }
    class MongoAdapter {
        +getCliente() MongoClient
        +getBanco() MongoDatabase
    }
    class ConfiguracaoBancos {
        <<record>>
        +doAmbiente()$ ConfiguracaoBancos
    }
    class DatabaseConfig {
        +postgresAdapter() PostgresAdapter
        +mongoAdapter() MongoAdapter
        +dataSource(PostgresAdapter a) DataSource
        +mongoClient(MongoAdapter a) MongoClient
        +mongoDatabase(MongoAdapter a) MongoDatabase
    }
    class UsuarioGateway {
        <<interface>>
    }
    class ConversaGateway {
        <<interface>>
    }
    class PostgresUsuarioGateway {
        <<Adapter>>
    }
    class MongoConversaGateway {
        <<Adapter>>
    }

    DatabaseManager ..> ConfiguracaoBancos : lê
    DatabaseManager *-- PostgresAdapter : cria
    DatabaseManager *-- MongoAdapter : cria
    DataBaseAdapter <|.. PostgresAdapter
    DataBaseAdapter <|.. MongoAdapter
    DatabaseConfig ..> DatabaseManager : getInstance() na subida
    UsuarioGateway <|.. PostgresUsuarioGateway
    ConversaGateway <|.. MongoConversaGateway
    PostgresUsuarioGateway ..> PostgresAdapter : DataSource do adapter
    MongoConversaGateway ..> MongoAdapter : MongoDatabase do adapter
```

`getInstance()` é o do professor, sem `synchronized`. É seguro porque a primeira chamada
acontece na subida, numa única thread, antes de o servidor aceitar requisições
(`docs/ARQUITETURA.md`, seção 6).

### 5.3 O que fica em cada banco

```mermaid
flowchart LR
    subgraph Postgres["PostgreSQL (via DataSource do Singleton)"]
        instituicao
        categoria
        usuario
        convite
        seguir
        bloqueio
        post
        post_categoria
        comentario
    end
    subgraph Mongo["MongoDB (via MongoDatabase do Singleton)"]
        notificacoes
        conversas
        mensagens
    end
    notificacoes -.->|destinatarioId, atorId| usuario
    conversas -.->|membros| usuario
    mensagens -.->|remetenteId| usuario
```

## 6. Sequência: cadastro via convite

A versão Java do diagrama de sequência do projeto antigo. Os passos 1 a 5 e o upload são os
mesmos para o frontend; muda o que acontece dentro da API.

```mermaid
sequenceDiagram
    autonumber
    actor Convidado
    participant React as Frontend (React)
    participant CC as ConviteController
    participant MC as MidiaController
    participant AC as AuthController
    participant S as RegistrarViaConviteService
    participant C as Convite
    participant PG as PostgreSQL
    participant S3 as SeaweedFS (S3)
    participant MG as MongoDB

    Convidado->>React: abre /invite/:token
    React->>CC: GET /api/invites/:token
    CC->>PG: ConviteGateway.buscarPorToken
    CC->>C: situacao(agora)
    CC-->>React: { roleTarget, institutionName, remainingSlots, valid, reason }

    Convidado->>React: escolhe a foto
    React->>MC: POST /api/media/upload
    MC->>S3: ArmazenamentoGateway.enviar("avatars/<uuid>.jpg")
    MC-->>React: 201 { key, objectUrl }

    Convidado->>React: envia o cadastro
    React->>AC: POST /api/auth/register
    AC->>S: executar(Cadastro)
    S->>PG: ConviteGateway.buscarPorToken
    S->>C: garantirValido(agora)
    S->>S3: ArmazenamentoGateway.existe(avatarKey)
    Note over S: convite.papelAlvo().criarUsuario(...)<br/>papel e instituição vêm do convite
    rect rgba(120, 160, 255, 0.15)
        Note over S,PG: TransacaoGateway.executar (uma transação)
        S->>PG: UsuarioGateway.criar
        S->>PG: ConviteGateway.consumirVaga (UPDATE atômico)
    end
    alt e-mail já usado
        PG-->>S: ConflitoException
        S-->>AC: 409 "Este e-mail já está em uso" (vaga não consumida)
    else sem vaga no instante da gravação
        S-->>AC: 400 "Invite has no remaining slots" (usuário desfeito)
    else criado
        S->>MG: NotificacaoGateway.criar(invite_received) depois do commit
        S-->>AC: Usuario
        AC-->>React: 201 { id, name, email, role, institutionId, avatarUrl }
        React-->>Convidado: redireciona para /login?registered=1
    end
```
