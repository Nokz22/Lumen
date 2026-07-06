# C4 — Nível 1: Context (rascunho, Fase 0)

> Rascunho. Refinado a cada fase à medida que os sistemas externos (wearable, LLM)
> deixam de ser "future work" e passam a ter adaptador real. Versão completa (Context +
> Container + Component) fica para a Fase 7.

```mermaid
C4Context
    title Lumen — System Context (rascunho)

    Person(user, "USER", "Adulto (18+) que se auto-observa e pratica auto-cuidado")
    Person(admin, "ADMIN", "Administração técnica: biblioteca de exercícios, recursos de crise, utilizadores")

    System(lumen, "Lumen", "Plataforma de bem-estar corpo-mente. Nunca diagnostica; encaminha para recursos humanos em caso de risco.")

    System_Ext(wearable, "Fonte de wearable", "Simulador na Fase 0-5; Fitbit/Garmin/Apple Health/Health Connect depois (adaptador provider-agnostic)")
    System_Ext(llm, "Provider de LLM", "Fase 6. Atrás da interface LlmClient; guardrails em três camadas antes/depois da chamada")
    System_Ext(crisisResources, "Recursos de crise regionais", "Linhas de apoio e serviços de emergência configuráveis por região (ex.: SNS 24 em PT)")

    Rel(user, lumen, "Check-in diário, instrumentos, chat, vê o seu dashboard")
    Rel(admin, lumen, "Gere biblioteca de exercícios e recursos de crise; nunca lê conteúdo emocional de um USER")
    Rel(lumen, wearable, "Ingere séries temporais normalizadas (HR, HRV, sono, passos)", "HTTPS")
    Rel(lumen, llm, "Envia janela de contexto + resumo, só com consentimento explícito", "HTTPS")
    Rel(lumen, crisisResources, "Apresenta ao USER quando um RiskEvent é detetado", "referência estática, não integração")
```

## Notas desta fase

- **Wearable e LLM** existem no diagrama porque já são decisões de arquitetura tomadas
  (portas `WearableSource` e `LlmClient`), mas o código só chega nas Fases 5 e 6.
- **Recursos de crise** não são uma integração técnica — é um registo (`CrisisResource`)
  configurável por região, apresentado ao USER. Modelado aqui porque é sistema externo
  do ponto de vista do produto (o Lumen não presta esse serviço, encaminha para ele).
- Container diagram (Fase 1+) vai detalhar: API Spring Boot, PostgreSQL, RabbitMQ,
  frontend React, WebSocket/STOMP.
