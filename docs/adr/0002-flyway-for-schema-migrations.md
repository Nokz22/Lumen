# ADR-0002 — Flyway para migrações de esquema

**Status:** Aceite
**Data:** 2026-07-06
**Decisores:** Nuno (autor/mantenedor)

## Contexto

A Fase 1 introduz as primeiras tabelas (`users`, `consent_records`, `mood_check_ins`).
O esquema vai crescer em todas as fases seguintes (instrumentos, risco, exercícios,
wearable, conversas) e precisa de evoluir de forma controlada, versionada e reprodutível
em três ambientes distintos (local, CI com Testcontainers, produção).

## Problema

Como garantir que o esquema da base de dados é sempre o mesmo, na mesma ordem, em
qualquer ambiente — sem depender de alguém correr scripts SQL manualmente ou de o
Hibernate "adivinhar" o esquema a partir das entidades?

## Opções consideradas

1. **Hibernate `ddl-auto: update`.** Rápido para prototipar, mas perigoso: gera SQL
   implícito, não versionado, não revisível em PR, e já tem histórico de corromper
   dados em produção ao "adivinhar" alterações (ex.: apagar uma coluna renomeada em vez
   de a migrar). Incompatível com "auditável e correto" como critério de topo.
2. **Liquibase.** Alternativa madura, formato XML/YAML mais verboso; equivalente em
   capacidades ao Flyway para este caso de uso.
3. **Flyway com SQL puro versionado (`V1__...sql`, `V2__...sql`).** Migrações são SQL
   real, revisível em PR como qualquer outro código, aplicado automaticamente no arranque
   e testado a sério pelos testes de integração (Testcontainers sobe um Postgres limpo e
   corre todas as migrações antes de qualquer teste).

**Decisão:** opção 3.

## Decisão

Flyway com `ddl-auto: validate` no Hibernate (nunca `update` nem `create`) — o Hibernate
só confirma que o mapeamento das entidades corresponde ao esquema que o Flyway já criou;
nunca gera DDL. Migrações em `db/migration/V{n}__description.sql`, incrementais, nunca
alteradas depois de mergeadas (uma alteração de esquema é sempre uma nova migração).

**Seed de dados de demonstração** (`db/dev-seed/V3__seed_demo_user.sql`) fica numa
localização Flyway separada, só incluída via `spring.flyway.locations` no perfil `dev` —
os testes (perfil `test`) e produção nunca veem dados sintéticos.

## Consequências

**Positivas:**
- Todo o histórico do esquema vive no repositório, revisível em PR.
- Os testes de integração (Testcontainers) validam as migrações reais, não um esquema
  "imaginado" pelo Hibernate — se uma migração está errada, os testes falham.
- Consistente com o standard "zero segredos, tudo versionado, nada mágico".

**Negativas / custos aceites:**
- Uma migração escrita incorretamente e já mergeada não se "corrige" — precisa de uma
  nova migração corretiva. Mais disciplina do que `ddl-auto: update`, de propósito.
- Descoberto nesta fase: os tipos SQL têm de bater certo com os tipos Java (ex.:
  `SMALLINT` vs Java `int` mapeia para `INTEGER` no Hibernate, não `SMALLINT`) — a
  validação do Hibernate falha ruidosamente se não baterem, o que é o comportamento
  correto (falha cedo, em vez de silenciosamente incompatível).

## Trade-offs (como eu defenderia isto numa entrevista)

`ddl-auto: update` parece mais rápido no dia 1, mas esta app lida com dados de saúde —
um esquema que "aparece sozinho" nunca é aceitável quando a auditabilidade da estrutura
de dados importa tanto quanto a dos dados em si. Um Tech Lead vai perguntar "como sabes
que o esquema em produção é o que pensas que é?" — a resposta é: o histórico do Flyway
mostra exatamente isso, e o CI corre-o do zero a cada build.
