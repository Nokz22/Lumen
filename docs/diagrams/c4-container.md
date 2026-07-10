# C4 — Nível 2: Container

> Detalha o sistema `Lumen` do [Context diagram](c4-context.md) nos seus containers
> reais, refletindo o estado após a Fase 6 (companheiro LLM incluído).

```mermaid
C4Container
    title Lumen — System Container

    Person(user, "USER", "Adulto (18+) que se auto-observa e pratica auto-cuidado")
    Person(admin, "ADMIN", "Administração técnica")

    System_Boundary(lumen, "Lumen") {
        Container(spa, "Frontend SPA", "React 18, TypeScript, Vite", "Dashboard, check-in, instrumentos, companheiro, biblioteca de exercícios")
        Container(api, "API Backend", "Java 17, Spring Boot 3", "REST (Presentation → Application → Domain), autenticação JWT, guardrails do companheiro")
        Container(ws, "WebSocket/STOMP", "Spring Messaging", "Push em tempo real: recomendações, streaming do companheiro")
        ContainerDb(db, "PostgreSQL", "Flyway-managed", "Dados de domínio; campos sensíveis cifrados (AES-GCM) em repouso")
        Container(mq, "RabbitMQ", "Fila + DLQ", "Eventos de check-in → motor de recomendação, consumidor idempotente")
    }

    System_Ext(llm, "Anthropic API", "Fase 6 — atrás da porta LlmClient; CannedLlmClient mock quando não configurada")
    System_Ext(crisisResources, "Recursos de crise regionais", "Registo estático por região (SNS 24, SOS Voz Amiga, 112)")

    Rel(user, spa, "Usa", "HTTPS")
    Rel(admin, spa, "Usa", "HTTPS")
    Rel(spa, api, "Chama", "HTTPS/JSON, cookie JWT httpOnly")
    Rel(spa, ws, "Subscreve", "WebSocket/STOMP, autenticado no handshake")
    Rel(api, db, "Lê/escreve", "JDBC")
    Rel(api, mq, "Publica/consome", "AMQP")
    Rel(mq, api, "Entrega evento de check-in ao motor de recomendação", "AMQP")
    Rel(api, ws, "Envia push (recomendação, chunk de resposta)", "SimpMessagingTemplate")
    Rel(api, llm, "Envia janela de contexto + resumo, só com consentimento explícito; classificador de risco corre sempre antes", "HTTPS")
    Rel(api, crisisResources, "Apresenta ao USER quando um RiskEvent é detetado", "leitura de tabela")
```

## Notas

- **Sem serviços separados** — `api` é um único deployable Spring Boot; a separação
  Presentation/Application/Domain/Infrastructure é uma fronteira de *código*
  (imposta por ArchUnit), não de processo. Não há razão de escala para microserviços
  neste projeto (ver `docs/constitution.md` — simplicidade é o desempate por defeito).
- **`ws` está desenhado como container lógico separado**, mas corre no mesmo processo
  Spring Boot que `api` — a separação existe aqui só para tornar explícito que o
  companheiro e as recomendações chegam por um canal diferente do REST.
- **Component diagram (nível 3)** fica fora de âmbito — o nível Container já é
  suficiente para comunicar a arquitetura a um Tech Lead numa entrevista; o detalhe
  de componentes está nos diagramas de domínio (`domain-model-phase1.md`,
  `domain-model-phase4.md`) e nas ADRs.
