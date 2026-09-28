# Fademy API

Backend da Fademy, rede social acadêmica só por convite. Projeto da disciplina de
Programação Orientada a Objetos — Macelo Alexandre, Wacelys Nunes e Arthur Casimiro.

Java 25 · Spring Boot · Clean Architecture · PostgreSQL + MongoDB (Singleton `DatabaseManager`).

## Documentação

- [`docs/SPEC.md`](docs/SPEC.md) — regras de negócio e contrato da API
- [`docs/ARQUITETURA.md`](docs/ARQUITETURA.md) — camadas, pacotes e padrões
- [`docs/DIAGRAMAS.md`](docs/DIAGRAMAS.md) — diagramas de casos de uso, classes, Singleton/Adapter e sequência
- [`docs/APRESENTACAO.md`](docs/APRESENTACAO.md) — como subir tudo na rede local e roteiro da demonstração
- [`docs/ISSUES.md`](docs/ISSUES.md) — plano de etapas
- [`docs/referencias/`](docs/referencias/) — exemplo de Singleton/Adapter do professor e análise de POO do projeto antigo

O backend Node original está na branch `legado-node`.

## Montar o ambiente numa máquina nova

Pré-requisitos: **JDK 25** (não só o JRE; no Debian/Ubuntu/Kali: `sudo apt install
openjdk-25-jdk-headless`), Docker com `docker compose` e Node 20+ (para o frontend). Maven não
precisa: o projeto usa o `./mvnw`.

```bash
mkdir fademy && cd fademy
git clone https://github.com/MaceloAlexandre/fademy_backend.git
git clone https://github.com/MaceloAlexandre/fademy_frontend.git

# PostgreSQL e MongoDB (as imagens ficam no GridFS do MongoDB)
cd fademy_backend
docker compose up -d

# API em http://localhost:8080 (com dados de demonstração)
SPRING_PROFILES_ACTIVE=demo ./mvnw spring-boot:run

# frontend em http://localhost:5173 (outro terminal)
cd ../fademy_frontend
npm install
npm run dev
```

Contas: `admin@fademy.com` / `changeme`; com o perfil `demo`, também `ana@demo.fademy`,
`helena@demo.fademy`... com a senha `demo1234` (lista completa em `docs/APRESENTACAO.md`).
Testes: `./mvnw verify` (com o Docker rodando). No IntelliJ, escolha o JDK 25 em *Project
Structure → SDK* e rode a classe `FademyApplication`.

Abra o Claude Code na pasta `fademy/` (acima dos dois repositórios) ou dentro de
`fademy_backend/`: a skill `github-issue-implementation` e o `CLAUDE.md` vêm com o repositório.

## Como trabalhamos: issues + SDD

1. Todo trabalho nasce de uma issue com critérios de aceite verificáveis (Dado / Quando / Então),
   dentro de um milestone (etapa) e no Project **Fademy**.
2. As regras de negócio vigentes ficam em `docs/SPEC.md` e mudam junto com o código.
3. Branch `feature/<id>` ou `bugfix/<id>` a partir da `main`; commit `feat #<id>: ...` ou
   `fix #<id>: ...`; PR para a `main` com `Refs #<id>`.

Com o Claude Code, peça por exemplo `implementa a issue 3 do backend`: a skill
[`github-issue-implementation`](.claude/skills/github-issue-implementation/SKILL.md) conduz o fluxo.
