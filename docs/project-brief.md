# PROMPT — LUMEN · Plataforma de Bem-Estar Corpo-Mente

> Copia tudo abaixo desta linha e cola no teu assistente de código (Claude Code, etc.).
> Este documento define **o quê** e **porquê**. O documento `lumen-engineering-standards.md`
> (standards de engenharia) define **como** — ambos se aplicam, e em caso de conflito
> sobre segurança clínica, **este documento prevalece**.
> Trabalha **uma fase de cada vez**. Nunca peças o projeto todo de uma vez.

---

## 0. QUEM SOU E O QUE ESTOU A CONSTRUIR

Sou developer júnior com foco em **Java 17 + Spring Boot 3** e uma licenciatura em
Psicologia. O Lumen é o meu projeto-bandeira: cruza engenharia de software séria com
o meu domínio clínico. Trata-me como um par — explica cada decisão como farias a um
colega que quer *defender o código numa entrevista técnica*, não copiar-cola cego.

Constrói por fases, com commits pequenos e incrementais (Conventional Commits), e no
fim de cada bloco lógico sugere a mensagem de commit. Eu faço o commit manualmente.

**Regra de trabalho:** se eu pedir algo que contrarie a fronteira ética da secção 1
ou as boas práticas dos standards, **para e avisa-me antes de implementar.** Neste
projeto, "o utilizador pediu" nunca é justificação suficiente.

---

## 1. FRONTEIRA ÉTICA — A ESPINHA DE TODO O PROJETO

Isto não é um detalhe de compliance; é a decisão de arquitetura número um, e o que
distingue este projeto de um chatbot de wellness genérico. **Vai para o ADR-001,
escrito na Fase 0, antes de qualquer código de domínio.**

O Lumen é uma ferramenta de **bem-estar e auto-cuidado**. Nunca é:

- um **dispositivo médico** (não faz screening diagnóstico regulado, não é FDA Class II
  nem marcação CE médica);
- uma ferramenta de **diagnóstico** (nunca diz "tens depressão", "estás em burnout");
- um **substituto de cuidado profissional** (reforça sempre o encaminhamento humano);
- destinado a **menores** (apenas adultos, 18+; consentimento etário na Fase 2).

O que o Lumen **é**: um espaço para a pessoa se observar a si própria (humor, sono,
energia, sinais fisiológicos), aprender a cuidar do corpo e da mente com ações
concretas, e ver as suas próprias tendências ao longo do tempo. Ilumina caminhos —
não decide por ninguém. (Daí o nome.)

**Consequência de linguagem, aplicada em todo o produto e código:**
nunca "pontuação de depressão" → sempre "pontuação de bem-estar / de auto-monitorização".
Nunca "estás ansioso" → "registaste sentir-te ansioso". A app descreve o que a pessoa
reporta; nunca interpreta clinicamente. Esta regra vive nos textos de UI, nos nomes de
campos da API, e nos prompts do LLM.

---

## 2. STACK

Idêntica em fundamentos ao meu projeto anterior — quero **reaproveitar aprendizagem**,
não reaprender ferramentas. O que muda é o domínio, não o esqueleto.

**Backend** (o meu foco principal)
- Java 17 + Spring Boot 3 (Gradle)
- PostgreSQL + Flyway
- Spring Security + JWT (access + refresh, rotation)
- RabbitMQ (eventos assíncronos)
- WebSocket / STOMP (dashboard em tempo real + streaming do chat)
- MapStruct, Bean Validation
- Testes: JUnit 5, Mockito, Testcontainers (Postgres + RabbitMQ reais), MockMvc
- springdoc-openapi (Swagger)
- Spring Boot Actuator + Micrometer

**Frontend**
- React 18 + TypeScript strict + Vite, **web responsiva primeiro** (mobile nativa é
  outra escala — fica como future work)
- Tailwind CSS, TanStack Query, react-i18next (**EN principal, PT-PT como opção alternável**)
- STOMP.js, Vitest + React Testing Library

**Infra**
- Docker multi-stage, docker-compose (dev/prod)
- GitHub Actions CI (build + testes + lint), branch protection no `main`

---

## 3. ROLES E ACESSO

MVP com dois roles, mas o modelo de autorização é desenhado **extensível desde o
início** (enum de roles + verificação de escopo de dados por role, sem refactor futuro):

- **USER** — a pessoa que se cuida a si própria. Vê e gere apenas os seus próprios dados.
- **ADMIN** — administração técnica (gestão da biblioteca de exercícios, recursos de
  crise, utilizadores). **Nunca** lê conteúdo emocional/conversacional de um USER.

**Preparado para o futuro, documentado mas não implementado:**
- **CLINICIAN** (read-only) — um psicólogo/psiquiatra que, com **consentimento
  explícito do utilizador**, poderia rever tendências e conteúdo. Modela-se o escopo
  de permissões a pensar nisto (ADR sobre acesso clínico e minimização de dados).

Princípio transversal: **minimização de dados**. Cada role vê o mínimo necessário.
O acesso a qualquer dado sensível de um USER gera entrada no audit log (secção 9).

---

## 4. DOMÍNIO E ENTIDADES

Modelar o ciclo de auto-observação e auto-cuidado de um adulto. Entidades centrais:

**Identidade e consentimento**
- **User** — conta (18+, credenciais, locale, região para recursos de crise)
- **ConsentRecord** — consentimentos **granulares e revogáveis** (uso de dados de
  saúde, processamento pelo LLM, ingestão de wearable). Categoria especial RGPD
  (secção 9). Cada consentimento é versionado e datado.

**Auto-observação diária**
- **MoodCheckIn** — check-in **diário**: humor auto-reportado (roda de emoções:
  feliz, triste, zangado, ansioso, frustrado, neutro), energia (escala leve 1–5),
  sono (horas / qualidade), nota livre opcional. Fonte de verdade emocional do sistema.

**Instrumentos estruturados (dados quantitativos)**
- **AssessmentType** — configuração de um instrumento validado. Começa com `PHQ9` e
  `GAD7`, enum/registo **extensível** para acrescentar outros sob supervisão clínica.
- **Assessment** — uma instância respondida por um USER. Cadência **mensal** (nunca
  diária — sobre-medir enviesa e é clinicamente incorreto). Ciclo de vida próprio:
  `SCHEDULED → IN_PROGRESS → COMPLETED → SCORED`.
- **AssessmentResponse** — resposta item a item (0–3 por item, conforme instrumento).
- **AssessmentScore** — pontuação calculada com o **scoring oficial** do instrumento,
  mais a faixa interpretativa **em linguagem de bem-estar** (secção 1). Nunca guardar
  nem devolver rótulos diagnósticos.

**Segurança clínica (ver secção 6 — é uma secção inteira)**
- **RiskEvent** — evento de risco detetado (ex.: PHQ-9 item 9 > 0, ou sinalização no
  chat). Máquina de estados **safety-critical**.
- **CrisisResource** — recurso de apoio, **configurável por região** (nunca hardcoded
  a um país). Nome, tipo, contacto, horário, âmbito geográfico.

**Auto-cuidado**
- **Recommendation** — recomendação gerada a partir do estado atual → ação corporal/mental.
- **Exercise** — item da biblioteca. `ExerciseCategory` extensível: respiração,
  alongamento, caminhada/ativação física, higiene de sono, ativação comportamental,
  grounding. Cada exercício tem duração, intensidade, e um "porquê" psicoeducativo.
- **ExerciseCompletion** — registo de que o USER fez o exercício (fecha o ciclo,
  alimenta o dashboard).

**Sinais fisiológicos (wearable)**
- **WearableReading** — leitura normalizada (frequência cardíaca, HRV, sono, passos),
  em série temporal, **em UTC**.
- **WearableSource** — fonte/adaptador. Arquitetura **provider-agnostic** (secção 7).

**Companheiro conversacional**
- **ConversationMessage** — mensagem do **chat único e contínuo** por utilizador
  (role user/assistant, conteúdo, timestamp UTC).
- **ConversationSummary** — resumo acumulado do histórico, para gestão de janela de
  contexto (secção 8).

**Auditoria**
- **AuditLogEntry** — quem acedeu a que dado sensível, quando, com que fim.

---

## 5. FLUXO DIÁRIO E MOTOR DE RECOMENDAÇÃO

O coração da experiência do utilizador, e a ponte corpo-mente:

1. O USER faz o **check-in diário** (humor + energia + sono).
2. Opcionalmente, há **sinais de wearable** ingeridos (HR/HRV/sono/passos).
3. O **motor de recomendação** mapeia o estado atual → uma ou mais ações concretas,
   com regras **transparentes e explicáveis** (nunca caixa-preta). Exemplos de mapeamento:
   - ansioso + HRV baixa → exercício de respiração + caminhada curta ao ar livre
   - baixa energia + sono curto → ativação comportamental leve + higiene de sono
   - frustrado → grounding + movimento físico
4. As recomendações **tiram a pessoa de casa e ensinam auto-cuidado** — é o objetivo
   explícito do produto. Preferir sempre ações corporais e comportamentais concretas
   a "conselhos" abstratos.
5. Cada ação sugerida liga a um **Exercise** da biblioteca, com o seu "porquê"
   psicoeducativo (a tua formação clínica dá autoridade a estes textos).

Tecnicamente: submissão de check-in → evento no RabbitMQ → o motor consome → gera
`Recommendation` → WebSocket empurra para o dashboard. Regras de recomendação
começam simples e determinísticas; um ADR documenta porque *não* usamos ML aqui
(explicabilidade > sofisticação num domínio sensível).

---

## 6. SEGURANÇA CLÍNICA — FLUXO DE CRISE (SECÇÃO CRÍTICA)

**Esta é a secção mais importante do projeto. Um tech lead vai procurá-la primeiro.**
A partir do momento em que existe qualquer sinalização de risco, a segurança não é
opcional nem "best effort" — é o comportamento por defeito.

### 6.1 Onde o risco é detetado

- **Instrumentos:** o **item 9 do PHQ-9** aborda pensamentos de morte/auto-dano.
  **Qualquer** resposta acima de zero é o gatilho crítico. É tratado de forma
  diferente dos outros oito itens — precisamente porque a minha formação me diz que é.
- **Chat com o LLM:** um classificador de risco na **entrada** (antes do modelo) e na
  **saída** (antes de mostrar), independente do próprio LLM (secção 8).
- **Padrões (future work, documentado):** deterioração acentuada de scores, quebra
  prolongada de check-ins.

### 6.2 O que acontece quando dispara (a regra de ouro)

O sistema **escala imediatamente para recursos humanos e linhas de apoio. Nunca
tenta "resolver" a crise via chat, nem continua o fluxo normal.**

Sequência, quando o gatilho é o item 9:
1. **Interrompe** o fluxo do instrumento — não mostra a pontuação como se fosse um
   resultado normal.
2. Mostra, de forma calma e não-alarmista, os **recursos de crise da região** do
   utilizador (`CrisisResource`).
3. Cria um `RiskEvent` e exige **acknowledgment** explícito de que a pessoa viu os
   recursos.
4. Regista tudo (audit log), sem nunca expor este conteúdo a ADMIN.

### 6.3 Máquina de estados do RiskEvent (safety-critical, validada no backend)

```
DETECTED → RESOURCES_PRESENTED → ACKNOWLEDGED
```

Transições inválidas rejeitadas com erro. Um `RiskEvent` **nunca** salta direto para
`ACKNOWLEDGED` sem passar por `RESOURCES_PRESENTED`. Testes obrigatórios cobrem cada
transição e o caminho de erro. Este é o fluxo onde a robustez importa mais do que em
qualquer outro sítio do sistema.

### 6.4 Recursos de crise (configuráveis, verificados)

- Registo `CrisisResource` **por região**, nunca hardcoded a um número.
- Para Portugal, referências como o **SNS 24 (808 24 24 24)** e linhas de apoio
  emocional (ex.: SOS Voz Amiga). **Verificar sempre os contactos atuais e válidos
  antes de qualquer demo pública** — números e horários mudam, e mostrar um recurso
  errado num contexto de crise é inaceitável.
- A app deixa claro que estes recursos são a via correta, e que o Lumen é apoio ao
  bem-estar, não resposta a emergências.

---

## 7. INGESTÃO DE WEARABLE (PROVIDER-AGNOSTIC)

Objetivo: mostrar competência de ingestão e correlação de sinais fisiológicos **sem**
depender de hardware nem de app nativa no MVP — e deixar a porta aberta para wearable
real depois.

- Endpoint recebe séries temporais **normalizadas** (HR, HRV, sono, passos) num
  formato interno teu, em UTC.
- **Padrão de adaptador:** uma porta `WearableSource` com implementações por fonte.
  O primeiro adaptador é o **simulador** (gera dados realistas para a demo).
- Mais tarde, adaptadores Fitbit / Garmin / Apple Health / Health Connect encaixam
  **sem reescrever o domínio** — é Composition over Inheritance numa fronteira externa.
- Valor de produto: **correlação** entre sinal fisiológico e humor auto-reportado
  ("nas noites em que dormiste menos, no dia seguinte registaste mais ansiedade").
  Insight, nunca veredicto clínico.

ADR documenta a decisão de simulador-primeiro e o desenho provider-agnostic.

---

## 8. COMPANHEIRO CONVERSACIONAL COM GUARDRAILS (A JÓIA — FASE FINAL)

O componente mais sensível e mais diferenciador. Um LLM real via API, mas **a
segurança nunca é confiada ao próprio modelo.**

### 8.1 Modelo conversacional

- **Um único chat contínuo por utilizador** (não há múltiplas sessões).
- Estado persistente: guardam-se todas as `ConversationMessage` na BD, mas ao LLM
  envia-se apenas uma **janela de contexto** (últimas N mensagens) + um
  **`ConversationSummary`** do que veio antes. ADR sobre esta estratégia — histórico
  cresce infinitamente, o modelo tem limite de tokens, cada token custa.

### 8.2 Arquitetura de guardrails (defesa em profundidade)

O LLM fica **atrás de uma interface** (`LlmClient`), para poder ser mockado nos testes
e trocado de provider sem tocar no domínio. À volta dele, três camadas:

1. **Pré-modelo — classificador de risco na entrada.** Antes de a mensagem chegar ao
   LLM, é avaliada. Se sinalizar risco → **fluxo de crise (secção 6)**, o LLM nem é
   chamado.
2. **O modelo — system prompt clínico rígido.** Instruções que proíbem diagnóstico,
   proíbem validar pensamento distorcido, impõem tom de apoio e psicoeducação, e
   obrigam a encaminhar para humano quando apropriado. É apoio reflexivo, não terapia.
3. **Pós-modelo — verificação na saída.** A resposta do LLM é reavaliada antes de ser
   mostrada (linguagem de diagnóstico, conselho perigoso). Se falhar → substitui por
   resposta segura + recursos.

### 8.3 Problemas que este componente traz (e que quero saber tratar)

- **Latência/custo:** streaming de resposta, timeouts, rate limiting por utilizador.
- **Não-determinismo:** os testes verificam **comportamento** (o guardrail disparou?),
  nunca strings exatas.
- **Privacidade:** enviar texto emocional a uma API de terceiros é dado de categoria
  especial → exige **consentimento explícito e revogável** para este fluxo (secção 9),
  e a app funciona sem o chat se o consentimento não for dado.

Nota de contexto: a Sword desenvolveu o **MindEval**, um framework para avaliar modelos
de linguagem em diálogo de saúde mental. Vale a pena referenciar no README a ideia de
avaliação sistemática de segurança do LLM — mostra que penso neste problema ao nível
da indústria.

---

## 9. DADOS DE CATEGORIA ESPECIAL (RGPD)

Dados de saúde mental são categoria especial no RGPD — o tratamento é mais exigente e
isso, bem feito, é um diferencial de engenharia enorme para qualquer healthtech europeia.

- **Consentimento** explícito, granular e **revogável** (`ConsentRecord`); a app
  degrada graciosamente quando um consentimento é retirado (ex.: sem chat, mas o resto
  funciona).
- **Minimização:** cada role e cada componente acede ao mínimo necessário.
- **Pseudonimização** nos logs; **nunca** registar conteúdo emocional, respostas de
  instrumentos, ou mensagens de chat em texto claro nos logs.
- **Audit log** de todo o acesso a dados sensíveis.
- **Direito ao esquecimento:** exportação e eliminação de dados do utilizador.
- **UTC** em toda a persistência temporal; conversão para hora local só na apresentação.
- **Dados 100% sintéticos** em desenvolvimento e demo — nunca dados reais de pessoas.

---

## 10. UI — REGRAS DE DESIGN

Uma app de bem-estar consultada frequentemente, muitas vezes em momentos de baixa
energia ou stress. A UI tem de ser **calma, clara e sem fricção**.

- **Dark mode por defeito + light mode real**, ambos completos, contraste suave
  (nada de preto/branco puros — reduz fadiga).
- **Cor com significado**, com moderação; a paleta base é serena e neutra. Estados
  emocionais podem ter cor, mas sem dramatizar (evitar vermelho alarmista).
- Tipografia legível, tamanhos generosos (corpo ≥ 15px), muito espaço em branco.
- O **check-in diário** deve ser rápido e sem esforço — poucos toques, nunca um
  formulário longo.
- O **dashboard de tendências** mostra padrões pessoais ao longo do tempo como
  **insight, não veredicto** — gráficos calmos, linguagem de auto-observação.
- O **fluxo de crise** tem um tratamento visual próprio: calmo, não-alarmista, com os
  recursos em destaque claro e acessível.
- Acessibilidade **WCAG 2.1 AA** (secção própria nos standards): teclado, focus,
  contraste, labels, `aria-live` para atualizações.

---

## 11. DADOS DE DEMONSTRAÇÃO

Para a demo nunca parecer vazia, e para mostrar o produto vivo:

- **Seed** (perfil `demo`): utilizadores fictícios com semanas de check-ins, alguns
  instrumentos respondidos com scores variados, exercícios concluídos, séries de
  wearable coerentes.
- **Simulador** de sinais de wearable a gerar dados realistas ao longo do tempo.
- Um percurso de demonstração que mostra a **correlação** corpo-mente de forma clara.
- **Importante:** os dados de demo **nunca** incluem cenários de crise reais expostos
  publicamente; se demonstrado o item 9, é num contexto controlado que mostra o fluxo
  de segurança a funcionar.

---

## 12. FASES (uma de cada vez, commits incrementais em cada uma)

O plano de commits detalhado por fase está nos standards v2 (secção 12). Aqui fica o
**arco** do projeto. Nota estrutural: o **fluxo de crise (Fase 3) entra no núcleo** —
a partir do item 9, a segurança não é adiável.

**Fase 0 — Fundações + fronteira ética**
Monorepo, Docker Compose (Postgres + RabbitMQ), CI verde, branch protection, README
inicial. **ADR-001 (posicionamento ético/não-diagnóstico) escrito antes de qualquer
feature.** Context Diagram (C4) em rascunho.

**Fase 1 — Domínio + check-in diário + dashboard base**
Entidades core, Flyway, `@Version` onde há estado mutável, CRUD com DTOs e RFC 7807,
Swagger. Check-in diário funcional. Dashboard base a mostrar histórico de humor.
Testes de integração com Testcontainers; gate JaCoCo.

**Fase 2 — Autenticação, roles e base de RGPD**
Spring Security + JWT (rotation), roles USER/ADMIN extensíveis, `@PreAuthorize`,
testes de segurança por role. `ConsentRecord` (granular, revogável), audit log,
verificação etária 18+. Dependabot + gitleaks no CI.

**Fase 3 — Instrumentos + FLUXO DE CRISE (núcleo)**
PHQ-9 e GAD-7 com scoring oficial e linguagem de bem-estar. Cadência mensal.
**Máquina de estados do RiskEvent, deteção do item 9, escalada para `CrisisResource`
por região, acknowledgment.** Testes exaustivos do caminho de risco — é aqui que a
robustez mais importa. ADR do modelo de segurança clínica.

**Fase 4 — Motor de recomendação + biblioteca de exercícios**
Regras transparentes estado → ação corporal/mental. Biblioteca de exercícios
(respiração, caminhada, alongamento, higiene de sono, ativação comportamental,
grounding) com "porquê" psicoeducativo. `ExerciseCompletion`. Eventos via RabbitMQ,
recomendações empurradas por WebSocket.

**Fase 5 — Ingestão de wearable (provider-agnostic)**
Endpoint de ingestão normalizada, porta `WearableSource`, adaptador simulador,
correlação sinal ↔ humor no dashboard. ADR do desenho provider-agnostic.

**Fase 6 — Companheiro LLM com guardrails + memória (a jóia)**
`LlmClient` atrás de interface (real em prod, mock em testes). Chat único contínuo,
janela de contexto + `ConversationSummary`. Guardrails em três camadas (pré/modelo/pós).
Consentimento próprio para o fluxo LLM. Testes de **comportamento** dos guardrails,
incluindo o disparo do fluxo de crise a partir do chat.

**Fase 7 — Polimento, deploy e documentação**
Docker prod, deploy (backend Render/Railway; frontend Vercel), métricas Micrometer +
Prometheus, rate limiting. README completo: visão, arquitetura, diagramas C4, índice
de ADRs, **secção dedicada à segurança clínica e à fronteira ética**, screenshots,
como correr, variáveis de ambiente, cobertura. C4 completo.

---

## 13. CRITÉRIO MÁXIMO

Entre duas soluções tecnicamente corretas, escolhe a que melhor demonstra engenharia
de software numa entrevista técnica **e** respeita a fronteira ética da secção 1, sem
introduzir complexidade desnecessária.

O objetivo não é uma app bonita. É que um Tech Lead — em particular de uma healthtech
como a Sword — olhe para o Lumen durante cinco minutos e pense: *"este candidato
percebe de engenharia séria e percebe o domínio clínico por dentro. É raro."*

---

Começa pela **Fase 0**: mostra-me a estrutura de pastas proposta, o plano de commits
da fase, e o rascunho do **ADR-001 (fronteira ética)** para eu validar **antes** de
escreveres qualquer código.
