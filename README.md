# Fademy API

Backend da Fademy, rede social acadêmica só por convite. Projeto da disciplina de
Programação Orientada a Objetos — Macelo Alexandre, Wacelys Nunes e Arthur Casimiro.

Java 25 · Spring Boot · Clean Architecture · PostgreSQL + MongoDB (Singleton `DatabaseManager`).

## Documentação

- [`docs/SPEC.md`](docs/SPEC.md) — regras de negócio e contrato da API
- [`docs/ARQUITETURA.md`](docs/ARQUITETURA.md) — camadas, pacotes e padrões
- [`docs/DIAGRAMAS.md`](docs/DIAGRAMAS.md) — diagramas de casos de uso, classes, Singleton/Adapter e sequência
- [`docs/ISSUES.md`](docs/ISSUES.md) — plano de etapas
- [`docs/referencias/`](docs/referencias/) — exemplo de Singleton/Adapter do professor e análise de POO do projeto antigo

O backend Node original está na branch `legado-node`.

## Como trabalhamos: issues + SDD

1. Todo trabalho nasce de uma issue com critérios de aceite verificáveis (Dado / Quando / Então),
   dentro de um milestone (etapa) e no Project **Fademy**.
2. As regras de negócio vigentes ficam em `docs/SPEC.md` e mudam junto com o código.
3. Branch `feature/<id>` ou `bugfix/<id>` a partir da `main`; commit `feat #<id>: ...` ou
   `fix #<id>: ...`; PR para a `main` com `Refs #<id>`.

Com o Claude Code, peça por exemplo `implementa a issue 3 do backend`: a skill
[`github-issue-implementation`](.claude/skills/github-issue-implementation/SKILL.md) conduz o fluxo.
