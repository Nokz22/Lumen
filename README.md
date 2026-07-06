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

## Stack

**Backend:** Java 17, Spring Boot 3 (Gradle), PostgreSQL + Flyway, Spring Security + JWT,
RabbitMQ, WebSocket/STOMP, MapStruct, JUnit 5 + Mockito + Testcontainers.

**Frontend:** React 18 + TypeScript strict + Vite, Tailwind CSS, TanStack Query,
react-i18next (PT-PT + EN).

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

Health check: `curl http://localhost:8080/actuator/health`.

### Frontend

```bash
cd frontend
npm install
npm run dev
```

## Variáveis de ambiente

Ver [`.env.example`](.env.example) — credenciais de PostgreSQL e RabbitMQ para
desenvolvimento local. Nunca versionar um `.env` real.

## Testes e qualidade

```bash
cd backend && ./gradlew build   # testes JUnit + Checkstyle (zero-warnings)
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

- [`docs/project-brief.md`](docs/project-brief.md) — o quê e porquê, fases, domínio, fluxo de crise.
- [`docs/standards.md`](docs/standards.md) — standards de engenharia, plano de commits e Definition of Done por fase.
- [`docs/adr/`](docs/adr/) — Architecture Decision Records.

## Roadmap (fases)

- [x] **Fase 0** — Fundações + fronteira ética
- [ ] Fase 1 — Domínio + check-in diário + dashboard base
- [ ] Fase 2 — Autenticação, roles e base de RGPD
- [ ] Fase 3 — Instrumentos + fluxo de crise
- [ ] Fase 4 — Motor de recomendação + biblioteca de exercícios
- [ ] Fase 5 — Ingestão de wearable (provider-agnostic)
- [ ] Fase 6 — Companheiro LLM com guardrails + memória
- [ ] Fase 7 — Polimento, deploy e documentação
