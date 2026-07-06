# Modelo de domínio — Fase 1

```mermaid
erDiagram
    USER ||--o{ CONSENT_RECORD : "grants/revokes"
    USER ||--o{ MOOD_CHECK_IN : "logs"

    USER {
        uuid id PK
        string email
        string displayName
        string locale
        string region
        instant createdAt
        long version
    }

    CONSENT_RECORD {
        uuid id PK
        uuid userId FK
        enum consentType
        boolean granted
        int consentVersion
        instant grantedAt
        instant revokedAt
        instant createdAt
    }

    MOOD_CHECK_IN {
        uuid id PK
        uuid userId FK
        enum emotion
        int energyLevel
        decimal sleepHours
        int sleepQuality
        string note
        date checkInDate
        instant createdAt
        long version
    }
```

## Notas

- `MOOD_CHECK_IN` tem uma restrição única `(userId, checkInDate)` — o mecanismo que
  garante "um check-in por dia" ao nível da base de dados, não só da aplicação.
- `CONSENT_RECORD` é *append-only*: revogar um consentimento insere uma nova linha, não
  atualiza a anterior — por isso não tem `@Version` (nunca é mutado em memória).
- `USER` ainda não tem password nem data de nascimento — chegam na Fase 2, quando a
  autenticação e a verificação de idade 18+ forem implementadas (evita colunas sem
  comportamento associado).
