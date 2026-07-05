# LUMEN — STANDARDS DE ENGENHARIA (documento único, OBRIGATÓRIO)

Complementa `project-brief.md`. Define o **como**; o brief define o **quê/porquê**.
Em conflito sobre **segurança clínica**, o brief prevalece.

Filosofia: construir ao nível de uma equipa de engenharia experiente, e distinguir-se
tanto pelo que se acrescenta como pelo que se **recusa** acrescentar (secção 15).
Complexidade tem de ser ganha, nunca assumida.

---

## 0. DOIS NÍVEIS DE EXIGÊNCIA (as fundações)

**Nível 0 — desde a Fase 0, em todo o código, sempre:**
arquitetura em camadas; testes de comportamento; logging SLF4J; zero segredos;
Conventional Commits pequenos; timestamps em UTC; input validation; zero warnings;
e a **fronteira ética** (linguagem não-diagnóstica) do brief.

**Nível 1 — incremental, ancorado a fases:**

| Standard | Fase |
|---|---|
| Optimistic locking (`@Version`) | 1 |
| Health/readiness/liveness (Actuator) | 1 |
| ArchUnit fitness functions no CI | 1 |
| Dependabot + secret scanning | 2 |
| Cifra em repouso + audit log + consentimento | 2 |
| Refresh token rotation | 2 |
| Máquina de estados do RiskEvent + fluxo de crise | 3 |
| Correlation IDs | 3 |
| Resilience4j na chamada ao LLM | 6 |
| Métricas Micrometer + Prometheus | 7 |
| Rate limiting | 7 |
| C4 completo | 7 (Context em rascunho na Fase 0) |

**Regra de ouro do scope:** cada fase tem uma Definition of Done (secção 14). Fechada,
não reabre — melhorias vão para o roadmap.

---

## 1. QUALITY ATTRIBUTES (ordem de desempate)

Critério consistente quando duas soluções são ambas corretas:

1. **Segurança clínica e Privacidade** — no topo, inseparáveis.
2. **Clareza / Legibilidade**
3. **Testabilidade**
4. **Evolutibilidade** (provider-agnostic, enums extensíveis, fronteiras limpas)
5. **Observabilidade**
6. **Performance**
7. **Escalabilidade**

**Simplicidade** não está na lista — é a regra de desempate por defeito. Entre duas
opções equivalentes, a mais simples ganha.

---

## 2. ARQUITETURA + FITNESS FUNCTIONS

Clean Architecture, SOLID, Separation of Concerns, Composition over Inheritance,
Dependency Injection, baixo acoplamento, alta coesão. Camadas:
Presentation → Application → Domain; Infrastructure implementa portas do Domain.
Nunca o inverso. Entidades JPA nunca cruzam a fronteira da API (sempre DTOs); sem
lógica de negócio em controllers.

As regras não são um diagrama — são **testes ArchUnit no CI** (Fase 1):

```
- ..domain.. NÃO depende de org.springframework..
- @Entity NÃO é referenciada em ..web../..controller..
- *Dto NÃO existe em ..domain..
- *Controller NÃO depende de *Repository
- *Repository só acedido a partir de ..application../..service..
- Regra de camadas (layered architecture) respeitada em ambos os sentidos
```

Alteração arquitetural → explica motivo, alternativas, decisão, impacto; regista em ADR.

---

## 3. UBIQUITOUS LANGUAGE + INVARIANTES DE DOMÍNIO

Fazemos DDD onde ele paga e evitamos cerimónia (há **um** bounded context).

**Glossário** (`docs/glossary.md`): termos do domínio com significado único partilhado
entre código, API, UI e prompts. Fixa a linguagem ética: "pontuação de bem-estar" e
nunca "de depressão"; "registou sentir-se" e nunca "está".

**Invariantes** (cada um com teste com o seu nome):

```
- Resposta > 0 ao item 9 do PHQ-9 cria SEMPRE um RiskEvent ANTES de a pontuação
  ser devolvida.
- Um RiskEvent NUNCA atinge ACKNOWLEDGED sem passar por RESOURCES_PRESENTED.
- Um AssessmentScore NUNCA é serializado com vocabulário diagnóstico.
- Conteúdo emocional, respostas de instrumentos e mensagens de chat NUNCA em logs.
- O LLM NUNCA é invocado quando o classificador de risco de entrada dispara.
- A deteção de crise é independente da disponibilidade do LLM.
- Retirar consentimento desativa a funcionalidade dependente no mesmo ciclo de pedido.
```

Um invariante sem teste é uma intenção, não uma garantia.

---

## 4. TESTES

- Unit (JUnit 5 + Mockito) + Integração (Testcontainers: Postgres e RabbitMQ reais;
  MockMvc) + Segurança (por role — provar o que cada role NÃO pode fazer).
- Guardrails do LLM e fluxo de crise testados por **comportamento** (disparou?),
  nunca por strings exatas — o LLM é não-determinístico.
- Nomes descritivos: `shouldTriggerCrisisFlowWhenPhq9Item9IsPositive()`.
- Cada teste prova um comportamento relevante; não escrever testes só para cobertura.
- **Gate JaCoCo no CI:** falha o build abaixo de 80% de cobertura de linha em código novo.

---

## 5. GIT — CONVENÇÕES E PLANO DE COMMITS POR FASE

**Regras:** GitHub Flow; `main` protegido (merge só com CI verde), branch protection
desde a Fase 0. Branch por funcionalidade. Conventional Commits em inglês. 1 commit =
1 unidade lógica (se a mensagem precisa de "and", são dois). Mínimo 8–15 commits por
fase; nada de push gigante no fim. Corpo de commit para o "porquê" quando não é óbvio.

Sequências de referência (adaptar, não copiar cegamente — o histórico conta a história):

**Fase 0 — Fundações + fronteira ética** (`chore/project-foundation`)
```
chore: initialize monorepo structure with backend and frontend dirs
chore(backend): bootstrap Spring Boot 3 project with Gradle
chore(frontend): bootstrap React 18 + TypeScript with Vite
chore: add docker-compose with PostgreSQL and RabbitMQ services
chore(backend): configure Checkstyle with zero-warnings policy
chore(frontend): configure ESLint and Prettier
ci: add GitHub Actions workflow for build, lint and tests
docs: add README with overview, CI badge and ethical positioning
docs(adr): add ADR-001 ethical boundary (non-diagnostic wellbeing tool)
docs: add C4 context diagram draft (Mermaid)
```

**Fase 1 — Domínio + check-in diário + dashboard base** (`feature/core-domain`)
```
feat(domain): add User and ConsentRecord entities with Flyway baseline
feat(domain): add MoodCheckIn entity with self-reported mood scale
feat(domain): add optimistic locking via @Version on mutable entities
feat(api): add daily check-in endpoints with DTOs and MapStruct
feat(api): add global exception handler with RFC 7807
feat(api): add Bean Validation on all request DTOs
feat(frontend): add base dashboard showing mood history
test(api): add integration tests for check-in with Testcontainers
test(arch): add ArchUnit rules enforcing layer boundaries
chore(ci): add JaCoCo coverage gate at 80% on new code
docs(adr): add ADR-003 Flyway and ADR-004 MapStruct
docs: add domain model diagram (Mermaid) and glossary draft
```

**Fase 2 — Autenticação, roles e base de RGPD** (`feature/authentication`)
```
feat(security): add Spring Security config with JWT access tokens
feat(security): add refresh token issuance and rotation
feat(security): add registration with 18+ age gate and login
feat(security): add role-based authorization (USER, ADMIN) extensible for CLINICIAN
feat(privacy): add granular revocable consent lifecycle
feat(privacy): add audit log for access to sensitive data
feat(privacy): add encryption at rest for sensitive fields
test(security): prove ADMIN cannot read USER emotional content
test(security): add token rotation and expiration tests
feat(frontend): add login, route guards and silent refresh interceptor
ci: add Dependabot config and secret scanning (gitleaks)
docs(adr): add ADR-002 JWT vs sessions and ADR privacy-by-design
```

**Fase 3 — Instrumentos + FLUXO DE CRISE (núcleo)** (`feature/assessments-crisis`)
```
feat(assessment): add PHQ-9 and GAD-7 with official scoring
feat(assessment): express scores in wellbeing language, never diagnostic
feat(assessment): schedule instruments monthly, not daily
feat(crisis): add RiskEvent state machine (DETECTED -> RESOURCES_PRESENTED -> ACKNOWLEDGED)
feat(crisis): trigger crisis flow when PHQ-9 item 9 is positive, before showing score
feat(crisis): add region-configurable CrisisResource registry
feat(frontend): add calm non-alarmist crisis screen with resources and acknowledgment
test(crisis): prove item 9 positive creates RiskEvent before score is returned
test(crisis): cover every valid and invalid RiskEvent transition
test(assessment): verify no diagnostic vocabulary in serialized scores
feat(observability): add correlation id filter
docs(adr): add ADR clinical safety model; add state machine diagram
```

**Fase 4 — Motor de recomendação + biblioteca de exercícios** (`feature/self-care`)
```
feat(domain): add Exercise with extensible ExerciseCategory enum
feat(recommendation): add transparent rules mapping state to body-mind action
feat(recommendation): publish check-in event to RabbitMQ; consumer generates recommendation
feat(recommendation): push recommendations to dashboard over WebSocket
feat(domain): add ExerciseCompletion tracking
feat(frontend): add exercise library with psychoeducational rationale
feat(frontend): add loading, empty and error states to every screen
test(recommendation): cover state-to-action mapping rules
test(messaging): prove idempotent consumer and DLQ behavior
docs(adr): add ADR why rule-based (explainability) over ML here
```

**Fase 5 — Ingestão de wearable (provider-agnostic)** (`feature/wearable-ingestion`)
```
feat(wearable): add normalized WearableReading time-series model in UTC
feat(wearable): add WearableSource port with simulator adapter
feat(wearable): correlate physiological signals with self-reported mood
feat(frontend): add correlation view on dashboard (insight, not verdict)
test(wearable): verify normalization and correlation logic
docs(adr): add ADR provider-agnostic wearable design
```

**Fase 6 — Companheiro LLM com guardrails + memória** (`feature/companion`)
```
feat(companion): add LlmClient port with mockable implementation
feat(companion): add single continuous conversation per user with persistence
feat(companion): add context window + ConversationSummary strategy
feat(companion): add input risk classifier before the model
feat(companion): add clinical system prompt forbidding diagnosis
feat(companion): add output verification with safe fallback
feat(companion): add resilience (timeout, limited retry, fallback) via Resilience4j
feat(privacy): add explicit consent for LLM processing; app works without chat if denied
test(companion): prove crisis flow fires from chat before LLM is called
test(companion): prove crisis detection works when LLM is unavailable
docs(adr): add ADR conversation memory and ADR LLM guardrails
```

**Fase 7 — Polimento, deploy e documentação** (`chore/production-readiness`)
```
feat(observability): add Micrometer metrics and Prometheus endpoint
feat(security): add rate limiting on public and LLM endpoints
chore(docker): add production multi-stage builds for both apps
chore(deploy): deploy backend to Render and frontend to Vercel
docs: complete C4 container and component diagrams
docs: finalize README with clinical-safety and ethical-boundary sections
docs: add roadmap and future improvements (CLINICIAN role, real wearable)
```

---

## 6. LOGGING, CONFIGURAÇÃO, TEMPO

- SLF4J sempre; níveis DEBUG/INFO/WARN/ERROR apropriados; sem redundância. **Nunca**
  logar conteúdo emocional, respostas de instrumentos, mensagens de chat, tokens ou PII.
- Perfis `dev`/`test`/`demo`/`prod`; `.env.example` documentado; zero segredos no repo.
- **UTC** em toda a persistência (`Instant`/`timestamptz`); hora local só na UI.
- API sob `/api/v1`; README explica coexistência de versões futuras.

---

## 7. PRIVACY & SECURITY BY DESIGN (calibrado)

Para o Lumen isto é core. Calibrado, não STRIDE exaustivo: uma tabela
`ativo → ameaça → mitigação` em `docs/threat-model.md`:

| Ativo | Ameaça | Mitigação |
|---|---|---|
| JWT / refresh | Roubo, replay | Expiração curta, rotation, revogação; decisão de storage em ADR |
| Conteúdo de chat | Fuga / acesso indevido | Sem logs; ADMIN nunca lê; consentimento próprio; cifra em repouso |
| Respostas de instrumentos | Reidentificação | Pseudonimização; minimização; audit log |
| Consentimentos | Manipulação | Versionados, datados, revogáveis; verificados a cada uso |
| Egress para o LLM | Exposição a terceiros | Consentimento explícito; app funciona sem chat se negado |
| Logs | PII acidental | Regra dura: zero conteúdo sensível; correlation ID |

**Privacy by Design:** minimização, retenção definida, cifra em trânsito e repouso,
pseudonimização, exportação, direito ao esquecimento, ciclo de vida do consentimento.
**Security by Design:** OWASP Top 10, authz/authn testadas, secrets fora do repo, CORS,
input validation, output encoding, rate limiting no público, BCrypt.
**Não fazemos** (e dizemos porquê): STRIDE exaustivo, KMS, pentest — práticas de equipa
com produto em produção real.

---

## 8. RESILIÊNCIA — SÓ ONDE HÁ DEPENDÊNCIA EXTERNA REAL

Padrões de resiliência não se espalham "porque parece profissional". Aplicam-se onde
falha de facto: a chamada ao LLM (Fase 6), com **Resilience4j**: timeout explícito,
retry limitado com backoff (sem duplicar cobranças), fallback para resposta segura +
recursos. **Invariante acoplado:** a deteção de crise é independente do LLM estar vivo.
O RabbitMQ tem a sua resiliência (DLQ, idempotência, publisher confirms). Para o
Postgres, o pool de conexões chega — sem circuit breakers inventados.

---

## 9. OBSERVABILIDADE (staged)

Fase 1: Actuator (health/readiness/liveness). Fase 3: logs estruturados + correlation
IDs. Fase 7: Micrometer + Prometheus; nota de integração futura com Grafana. Sem
OpenTelemetry/distributed tracing — não há nada distribuído (secção 15).

---

## 10. PERFORMANCE

Em cada funcionalidade: índices (FKs, colunas de filtro), N+1 (fetch joins/@EntityGraph),
lazy por defeito, paginação em toda a listagem, cache só quando justificado (ADR),
complexidade algorítmica. Orçamentos de performance como alvos internos medidos
("dashboard sente-se instantâneo"), nunca SLOs inventados (secção 15).

---

## 11. FRONTEND

Arquitetura: `components/` (apresentação pura), `features/` (mood, assessments, crisis,
companion, wearable), `hooks/`, `services/`+`api/`, `types/`, `utils/`, `layouts/`.
Composição sobre componentes gigantes.

**Estados de UX obrigatórios em todos os ecrãs:** loading (skeleton, sem layout shift),
empty (mensagem útil + ação), error (mensagem clara + retry), e conexão em tempo real
(indicador + reconexão com backoff) nos ecrãs com WebSocket.

**UI calma:** dark por defeito + light real, contraste suave; cor com significado e sem
dramatizar; corpo ≥ 15px; check-in rápido; dashboard como insight, não veredicto; ecrã
de crise com tratamento próprio, não-alarmista.

**Acessibilidade WCAG 2.1 AA:** teclado, focus, contraste nos dois temas, labels, ARIA
(timeline com `role="list"` + `aria-live="polite"`).

---

## 12. DOCUMENTAÇÃO, ADRs, DIAGRAMAS

ADRs em `docs/adr/` (Contexto, Problema, Opções, Decisão, Consequências, Trade-offs),
escritos **quando** a decisão é tomada (~10–12 no total, não à cabeça). Diagramas
Mermaid mantidos no mesmo PR que altera o fluxo: modelo de domínio (F1), auth (F2),
máquina de estados do RiskEvent + mensageria (F3), WebSocket (F4/6), C4 Context→
Container→Component (F0 rascunho, F7 completo). README evolui até conter: visão,
screenshots, arquitetura, C4, índice de ADRs, **secção de segurança clínica e fronteira
ética**, estrutura, como correr (com/sem Docker), variáveis de ambiente, testes,
cobertura, CI/CD, roadmap.

---

## 13. RITUAIS POR FASE (custo zero, sinal alto)

**Antes de cada fase — Tech Lead Review:** riscos, trade-offs, complexidade,
alternativas, impacto futuro. Só depois implementa (combina com o Plan Mode).

**Depois de cada fase — Devil's Advocate:** o agente assume um Staff Engineer que
discorda — onde parte em 2 anos? o que está acoplado/complexo demais? o que foi cedo
demais? onde violamos YAGNI? Saída: lista de **dívida técnica conscientemente aceite**.
(Isto substitui scorecards de auto-notas, que são anti-sinal.)

**Transversal — Engineering Learning:** para cada decisão, o agente explica porquê,
porque não a alternativa, o trade-off, e como eu a defenderia numa entrevista.

---

## 14. DEFINITION OF DONE POR FASE

Fechada, não reabre — melhorias vão para o roadmap.

**Fase 0:** `docker compose up` sobe Postgres + RabbitMQ; CI verde (build+lint dos dois
lados); branch protection ativo; README + ADR-001 ético + Context Diagram rascunho.

**Fase 1:** entidades migradas com `@Version`; check-in diário funcional; dashboard base;
RFC 7807; Swagger; testes de integração verdes; ArchUnit + gate JaCoCo ativos; glossário
iniciado.

**Fase 2:** login/registo com 18+ e refresh rotation; endpoints protegidos por role com
testes negativos; consentimento granular revogável; audit log; cifra em repouso;
Dependabot + gitleaks; frontend com guards.

**Fase 3:** PHQ-9/GAD-7 com scoring oficial e linguagem de bem-estar; **máquina de
estados do RiskEvent e fluxo de crise testados exaustivamente**; item 9 dispara antes
da pontuação; recursos por região; correlation IDs; ADR de segurança clínica.

**Fase 4:** motor de recomendação transparente estado→ação; biblioteca de exercícios com
"porquê"; eventos RabbitMQ com idempotência + DLQ; recomendações por WebSocket;
loading/empty/error em todos os ecrãs.

**Fase 5:** ingestão normalizada em UTC; porta `WearableSource` + adaptador simulador;
correlação sinal↔humor no dashboard; ADR provider-agnostic.

**Fase 6:** `LlmClient` mockável; chat único com janela de contexto + resumo; guardrails
em três camadas; consentimento próprio para o LLM; **crise dispara a partir do chat e
funciona com o LLM em baixo (testado)**; Resilience4j na chamada.

**Fase 7:** deploy live nos dois lados com variáveis documentadas; métricas + rate
limiting; C4 completo; README final com secção de segurança clínica; um estranho clona,
corre e percebe o projeto em 15 minutos só com o README.

---

## 15. NÃO-OBJETIVOS (YAGNI LEDGER)

Rejeitamos de propósito — cada rejeição é uma decisão, e mostrá-la é sinal sénior:

- **Scorecards de auto-notas** → substituídos pelo Devil's Advocate.
- **Event Storming + DDD tático completo** → glossário + invariantes (secção 3).
- **OpenTelemetry / distributed tracing** → Actuator + Micrometer; não há nada distribuído.
- **Circuit breakers genéricos, Redis, feature flags** → não estão no stack; resiliência
  só onde há falha externa real (LLM).
- **NFRs com SLOs inventados (p95, 99.9%)** → orçamentos de performance medidos, não
  garantias sobre um free-tier.
- **15 ADRs à cabeça** → escritos quando a decisão é tomada.
- **C4 nível Code + Deployment elaborado** → o nível Code é o código; Context+Container+
  Component chegam.
- **STRIDE exaustivo, KMS, pentest** → versão calibrada (secção 7), com a fronteira dita
  honestamente.

Princípio único: **complexidade tem de ser ganha.**

---

## 16. CRITÉRIO MÁXIMO

Entre duas soluções corretas, escolhe a que melhor demonstra engenharia numa entrevista
técnica **e** respeita a fronteira ética, sem complexidade desnecessária. O objetivo:
que um Tech Lead de uma healthtech olhe cinco minutos e pense — *"percebe de engenharia
séria e de domínio clínico por dentro. É raro."*
