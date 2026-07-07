# Máquina de estados — Fase 3 (instrumentos + fluxo de crise)

## `Assessment`

```mermaid
stateDiagram-v2
    [*] --> SCHEDULED: new Assessment(user, type)
    SCHEDULED --> IN_PROGRESS: start()
    IN_PROGRESS --> COMPLETED: complete()
    COMPLETED --> SCORED: score()
    SCORED --> [*]
```

## `RiskEvent`

```mermaid
stateDiagram-v2
    [*] --> DETECTED: new RiskEvent(userId, assessmentId, PHQ9_ITEM9)
    DETECTED --> RESOURCES_PRESENTED: presentResources()
    RESOURCES_PRESENTED --> ACKNOWLEDGED: acknowledge()
    ACKNOWLEDGED --> [*]

    note right of DETECTED
        ACKNOWLEDGED nunca é atingido
        diretamente a partir de DETECTED
        (invariante CLAUDE.md)
    end note
```

## Como as duas se encaixam num pedido de submissão

```mermaid
sequenceDiagram
    actor U as Utilizador
    participant C as AssessmentController
    participant S as AssessmentService
    participant R as RiskEvent
    participant A as Assessment

    U->>C: POST /assessments/PHQ9 {responses}
    C->>S: submit(userId, PHQ9, responses)
    S->>A: new Assessment + start() + complete()
    alt item 9 > 0
        S->>R: new RiskEvent() + presentResources()
        S-->>C: CrisisTriggeredResult(riskEventId, resources)
        Note over A: fica em COMPLETED — SCORED nunca é atingido aqui
    else item 9 == 0
        S->>A: score()
        S-->>C: ScoredAssessmentResult(totalScore, wellbeingBand)
    end
    C-->>U: resposta (nunca ambos os campos ao mesmo tempo)
```

## Notas

- Todas as transições até `COMPLETED`/`RESOURCES_PRESENTED` acontecem dentro do mesmo
  pedido HTTP — ver ADR-0006, Decisão 1, para o porquê deste fluxo síncrono.
- Um `Assessment` só sai de `COMPLETED` para `SCORED` de duas formas: normalmente, no
  mesmo pedido de submissão (quando não há crise); ou depois, em
  `AssessmentService.scoreAfterCrisisAcknowledgment()`, chamado só quando o
  `RiskEvent` associado atinge `ACKNOWLEDGED` (ver ADR-0006, Decisão 2).
- `RiskEvent.assessmentId` é opcional — um `Assessment` sempre cria um `RiskEvent` com
  `assessmentId` preenchido, mas o desenho permite que uma fonte futura sem
  `Assessment` associado (ex.: um classificador de risco no chat da Fase 6) crie um
  `RiskEvent` com `assessmentId = null`.
