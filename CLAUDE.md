# Fademy API — contexto para o Claude

API da Fademy, rede social acadêmica só por convite e multi-instituição (primeira
instituição: Faculdade Católica da Paraíba). Projeto da disciplina de Programação Orientada
a Objetos. Autores: Macelo Alexandre, Wacelys Nunes e Arthur Casimiro.

Repo `MaceloAlexandre/fademy_backend`, branch `main`. Frontend React em `../fademy_frontend`
(`MaceloAlexandre/fademy_frontend`), que **não muda**: a API mantém o contrato do backend Node
antigo, que está na branch `legado-node` (só consulta). O exemplo de Singleton/Adapter do
professor e a análise de POO do grupo estão em `docs/referencias/`.

## Documentos (leia antes de mexer)

- `docs/SPEC.md` — regras de negócio (RN01...) e contrato da API. **Fonte da verdade.**
- `docs/ARQUITETURA.md` — camadas, pacotes, Singleton, convenções de código.
- `.claude/skills/github-issue-implementation/SKILL.md` — como implementar uma issue.

## Stack e comandos

Java 25 · Spring Boot 4.1 · PostgreSQL 17 + Flyway · MongoDB 8 · SeaweedFS (S3) · socket.io-server-java ·
JUnit 5 + Mockito + Testcontainers + ArchUnit. Build pelo wrapper, sem Maven instalado.
Precisa do **JDK** 25 (o JRE não compila): `sudo apt install openjdk-25-jdk-headless`, ou um JDK
em `~/.jdks/` com `JAVA_HOME` apontando para ele.

```bash
docker compose up -d       # postgres, mongo, seaweedfs (S3)
./mvnw spring-boot:run     # API em :3000
./mvnw verify              # todos os testes (Docker rodando)
```

## Regras que não se negociam

- **Clean Architecture:** `domain` e `application` sem Spring, JDBC ou Mongo. Todo caso de uso
  é interface (`application/usecase`) + implementação (`application/service`); todo acesso
  externo é interface (`application/gateway`) + implementação (`infrastructure/`).
- **Orientação a objetos:** regra sobre um objeto é método dele. Variação por papel é
  polimorfismo na hierarquia de `Usuario`, nunca `if (papel.equals("aluno"))`.
- **Singleton:** só o `DatabaseConfig` chama `DatabaseManager.getInstance()`, na subida.
- **Contrato:** rotas, status, mensagens de erro e nomes de campo (`_id`) seguem o SPEC seção 5.
- **Migrations Flyway:** nunca editar uma já commitada; sempre uma nova.

## Fluxo de trabalho: issues + SDD

Todo trabalho nasce de uma issue com critérios de aceite (Dado / Quando / Então), agrupada
por milestone (etapa) e acompanhada no Project **Fademy**. Para implementar, use a skill
`github-issue-implementation`: branch `feature/<id>` ou `bugfix/<id>` a partir da `main`,
teste para cada critério, SPEC atualizado, commit `feat #<id>: ...` / `fix #<id>: ...`,
PR com `Refs #<id>`. O merge é do usuário. Não use `.planning/` nem comandos `/gsd-*`.

Onde houver bifurcação real (schema, regra de negócio, contrato), **pergunte com uma
recomendação**; não decida sozinho.

---

## Diretrizes de código

**Tradeoff:** estas diretrizes favorecem cautela em vez de velocidade. Em tarefa trivial, use o bom senso.

### 1. Pense antes de codar

- Explicite suposições. Se estiver em dúvida, pergunte.
- Se houver mais de uma interpretação, apresente-as; não escolha em silêncio.
- Se existir um caminho mais simples, diga. Discorde quando fizer sentido.

### 2. Simplicidade primeiro

- Só o que a issue pede. Nada especulativo.
- Sem abstração para um único uso além das que a arquitetura exige (use case, gateway).
- Sem tratamento de erro para cenário impossível.

### 3. Mudanças cirúrgicas

- Não "melhore" código vizinho, comentários ou formatação.
- Siga o estilo existente.
- Código morto não relacionado: mencione, não apague.
- Toda linha alterada deve se ligar ao pedido.

### 4. Execução guiada por objetivo

- Transforme a tarefa em critério verificável ("adicionar validação" → "teste com entrada
  inválida, depois fazer passar").
- Repita até verificar. Critério fraco ("fazer funcionar") exige esclarecimento.
