# Modelo de domínio — Fase 4 (motor de recomendação + biblioteca de exercícios)

```mermaid
erDiagram
    USER ||--o{ MOOD_CHECK_IN : "logs"
    MOOD_CHECK_IN ||--o{ RECOMMENDATION : "triggers (async, via RabbitMQ)"
    EXERCISE ||--o{ RECOMMENDATION : "is suggested as"
    EXERCISE ||--o{ EXERCISE_COMPLETION : "is completed as"
    RECOMMENDATION |o--o{ EXERCISE_COMPLETION : "optionally led to"

    EXERCISE {
        uuid id PK
        enum category
        string name
        int durationMinutes
        enum intensity
        string rationale
        instant createdAt
    }

    RECOMMENDATION {
        uuid id PK
        uuid userId FK
        uuid moodCheckInId FK
        uuid exerciseId FK
        string reason
        instant createdAt
    }

    EXERCISE_COMPLETION {
        uuid id PK
        uuid userId FK
        uuid exerciseId FK
        uuid recommendationId "nullable, not a JPA relation"
        instant completedAt
    }
```

## Notas

- `EXERCISE` é conteúdo de referência semeado numa migração normal
  (`db/migration/V12`), não `db/dev-seed` — tal como `CrisisResource` na Fase 3, é
  necessário em qualquer ambiente para a app funcionar, não é dado de demonstração.
- `RECOMMENDATION` não tem `@Version` — é um registo append-only de "isto foi
  sugerido", nunca mutado depois de criado.
- `EXERCISE_COMPLETION.recommendationId` é opcional e não é uma relação JPA (UUID
  solto, mesmo padrão de `RiskEvent.assessmentId`/`AuditLogEntry`) — completar um
  exercício não exige ter vindo de uma recomendação; a pessoa pode ir direto à
  biblioteca.
- A seta `MOOD_CHECK_IN → RECOMMENDATION` é assíncrona: um `MoodCheckInSubmittedEvent`
  publicado no RabbitMQ é o que liga as duas, não uma foreign key criada na mesma
  transação do check-in (ver ADR-0007 e `docs/diagrams/crisis-flow-state-machine.md`
  para o padrão equivalente de máquina de estados usado na Fase 3).
