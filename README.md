# Lumen

![CI](https://github.com/Nokz22/Lumen/actions/workflows/ci.yml/badge.svg)

Plataforma de **bem-estar corpo-mente**: auto-observação diária (humor, energia, sono),
instrumentos validados (PHQ-9, GAD-7) em linguagem de bem-estar, um motor de
recomendação transparente, ingestão de sinais fisiológicos (wearable) e, mais adiante,
um companheiro conversacional com guardrails de segurança em três camadas.

Projeto de portfólio construído fase a fase, com o nível de rigor de uma equipa de
engenharia experiente — arquitetura em camadas, testes de comportamento, ADRs, CI,
e uma fronteira ética que nenhuma feature futura pode contornar.

## Fronteira ética (ver [ADR-0001](docs/adr/0001-ethical-boundary-non-diagnostic-wellbeing-tool.md))

O Lumen **nunca** é um dispositivo médico, nunca diagnostica, e nunca substitui cuidado
profissional — reforça sempre o encaminhamento humano. É apenas para adultos (18+).
A linguagem em toda a app é não-diagnóstica: "pontuação de bem-estar", nunca "de
depressão"; "registaste sentir-te", nunca "estás". Esta regra vive nos textos de UI,
nos nomes de campos da API e nos prompts do LLM.

## Fluxo de crise (ver [ADR-0006](docs/adr/0006-clinical-safety-model-crisis-flow.md))

Uma resposta positiva ao item 9 do PHQ-9 interrompe o instrumento antes de qualquer
pontuação existir e mostra os recursos de crise da região. Dois avisos importantes:

- **Contactos de crise** (`db/migration/V11__create_crisis_resources.sql`): semeados
  com os números reais mencionados no brief (SNS 24, SOS Voz Amiga, 112). **Confirma
  que estão atuais antes de qualquer demonstração pública** — números e horários de
  linhas de apoio mudam.
- **Tradução PT-PT dos itens do PHQ-9/GAD-7** (`frontend/src/i18n/locales/pt.json`):
  melhor esforço a partir de traduções de referência conhecidas, mas **não revista por
  um profissional**. Não usar em contexto real sem essa revisão.

## Motor de recomendação (ver [ADR-0007](docs/adr/0007-rule-based-recommendation-engine.md))

Regras determinísticas e explicáveis (nunca ML) mapeiam o check-in diário para
sugestões de auto-cuidado — check-in → evento RabbitMQ → `RecommendationService` →
push por WebSocket para o dashboard. O texto do "porquê" psicoeducativo de cada
exercício (`db/migration/V12__create_exercises.sql`) é a minha melhor tentativa em
linguagem de bem-estar, não revisto clinicamente — mesmo aviso da tradução PT-PT acima.

## Ingestão de wearable (ver [ADR-0008](docs/adr/0008-provider-agnostic-wearable-ingestion.md))

**Nenhum dispositivo real está ligado.** A porta `WearableSource` é provider-agnostic
por desenho (um adaptador Fitbit/Garmin real implementaria a mesma interface sem
tocar no domínio), mas o único adaptador existente hoje é o `SIMULATOR` — todos os
sinais fisiológicos que aparecem na app são sintéticos, gerados para demonstrar o
mecanismo de ingestão e correlação, nunca dados reais de ninguém.

## Companheiro LLM (ver [ADR-0009](docs/adr/0009-conversation-memory-window-plus-summary.md) e [ADR-0010](docs/adr/0010-llm-guardrails-three-layer-defense.md))

O chat em `/companion` usa a Anthropic API real quando `ANTHROPIC_API_KEY` está
definida (`app.llm.provider=anthropic`); sem ela, cai automaticamente para
`CannedLlmClient`, um mock que nunca chama a rede e nunca custa nada — o perfil
`test` força sempre o mock, independentemente do ambiente, por isso os testes
automatizados (incluindo CI) nunca atingem a API real. A segurança nunca é confiada
ao modelo: um classificador de risco de entrada (lista de frases PT+EN) dispara o
fluxo de crise **antes** de o LLM ser sequer chamado, e continua a funcionar mesmo
com o provider de LLM completamente indisponível (ver ADR-0010).

Nota de contexto: a Sword desenvolveu o [MindEval](https://www.sword.health/), um
framework para avaliar sistematicamente modelos de linguagem em diálogo de saúde
mental. Os testes desta fase provam comportamento pontual contra frases conhecidas,
não uma avaliação adversarial sistemática — MindEval é o padrão de rigor que uma
versão de produção desta funcionalidade teria de perseguir.

## Stack

**Backend:** Java 17, Spring Boot 3 (Gradle), PostgreSQL + Flyway, Spring Security + JWT,
RabbitMQ, WebSocket/STOMP, MapStruct, Resilience4j, JUnit 5 + Mockito + Testcontainers.

**Frontend:** React 18 + TypeScript strict + Vite, Tailwind CSS, TanStack Query,
react-i18next (EN principal, PT-PT como opção).

**Infra:** Docker Compose (dev), GitHub Actions (CI).

## Arquitetura

Presentation → Application → Domain; Infrastructure implementa portas do Domain (nunca
o inverso). O domínio não depende do Spring; entidades JPA nunca cruzam a fronteira da
API (sempre DTOs). Ver [C4 Context (rascunho)](docs/diagrams/c4-context.md).

## Como correr

### Com Docker (recomendado)

```bash
cp .env.example .env
docker compose up -d
```

Sobe PostgreSQL (`localhost:5432`) e RabbitMQ (`localhost:5672`, management UI em
`localhost:15672`).

### Backend

```bash
cd backend
./gradlew bootRun
```

Health check: `curl http://localhost:8080/actuator/health`. CORS já aceita
`http://localhost:5173` (o frontend em dev) — configurável via `app.cors.allowed-origins`.

> **Se o backend não conseguir ligar-se ao Postgres** ("role does not exist" ou
> semelhante): confirma que não tens outro Postgres local a ocupar a porta 5432
> (`lsof -i:5432`). Um Postgres instalado via Homebrew, por exemplo, ganha a ligações a
> `localhost:5432` mesmo com o do Docker publicado na mesma porta — pára-o
> (`brew services stop postgresql@15`) ou muda `POSTGRES_PORT` no teu `.env`.

### Frontend

```bash
cd frontend
npm install
npm run dev
```

Regista uma conta em `/register`, ou usa o **utilizador de demonstração** já semeado
(perfil `dev`): `demo@lumen.dev` / `Demo1234!` — já tem consentimento de dados de saúde
concedido, por isso já consegue fazer check-in imediatamente após login.

## Variáveis de ambiente

Ver [`.env.example`](.env.example) — credenciais de PostgreSQL e RabbitMQ para
desenvolvimento local. Nunca versionar um `.env` real.

Para o companheiro LLM usar a Anthropic API real em vez do mock, define
`LLM_PROVIDER=anthropic` e `ANTHROPIC_API_KEY=<a-tua-chave>` (opcionalmente
`ANTHROPIC_MODEL`, por omissão `claude-sonnet-5`) antes de arrancar o backend. Sem
isto, o chat funciona na mesma — só usa respostas fixas do `CannedLlmClient`.

## Testes e qualidade

```bash
cd backend && ./gradlew build   # JUnit + Testcontainers + ArchUnit + Checkstyle + gate JaCoCo (80% linha)
cd frontend && npm run lint     # ESLint
cd frontend && npm run build    # type-check (tsc) + build
```

## Estrutura

```
backend/    Spring Boot 3, Gradle
frontend/   React 18 + TypeScript, Vite
docs/       project-brief, standards, ADRs, diagramas
```

## Documentação

- [`docs/constitution.md`](docs/constitution.md) — regras não-negociáveis do projeto (fronteira ética, invariantes clínicos, quality attributes).
- [`docs/project-brief.md`](docs/project-brief.md) — o quê e porquê, fases, domínio, fluxo de crise.
- [`docs/standards.md`](docs/standards.md) — standards de engenharia, plano de commits e Definition of Done por fase.
- [`docs/adr/`](docs/adr/) — Architecture Decision Records.
- [`docs/glossary.md`](docs/glossary.md) — linguagem única partilhada entre código, API e UI.
- [`docs/diagrams/domain-model-phase1.md`](docs/diagrams/domain-model-phase1.md) — modelo de domínio (Mermaid).
- [`docs/diagrams/crisis-flow-state-machine.md`](docs/diagrams/crisis-flow-state-machine.md) — máquina de estados do `Assessment`/`RiskEvent` (Mermaid).
- [`docs/diagrams/domain-model-phase4.md`](docs/diagrams/domain-model-phase4.md) — modelo de domínio `Exercise`/`Recommendation` (Mermaid).
- [`docs/threat-model.md`](docs/threat-model.md) — ativo → ameaça → mitigação.

## Roadmap (fases)

- [x] **Fase 0** — Fundações + fronteira ética
- [x] **Fase 1** — Domínio + check-in diário + dashboard base
- [x] **Fase 2** — Autenticação, roles e base de RGPD
- [x] **Fase 3** — Instrumentos + fluxo de crise
- [x] **Fase 4** — Motor de recomendação + biblioteca de exercícios
- [x] **Fase 5** — Ingestão de wearable (provider-agnostic)
- [x] **Fase 6** — Companheiro LLM com guardrails + memória
- [ ] Fase 7 — Polimento, deploy e documentação
