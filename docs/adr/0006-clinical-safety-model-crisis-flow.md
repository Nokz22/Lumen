# ADR-0006 — Modelo de segurança clínica: fluxo de crise e gate de pontuação

**Status:** Aceite
**Data:** 2026-07-06
**Decisores:** Nuno (autor/mantenedor)

## Contexto

A Fase 3 introduz o PHQ-9 e o GAD-7 e, com eles, o invariante mais crítico do projeto
(`CLAUDE.md`): uma resposta positiva ao item 9 do PHQ-9 (pensamentos de morte ou
auto-dano) tem de interromper o fluxo normal, mostrar recursos de crise, e nunca
mostrar uma pontuação como se fosse um resultado normal. Isto exige decisões de
modelação que vão além do CRUD habitual — como é que "não mostrar a pontuação" se
torna uma garantia do código, e não só uma intenção documentada.

## Decisão 1 — Fluxo síncrono, máquina de estados testada ao nível da entidade

`Assessment` percorre `SCHEDULED → IN_PROGRESS → COMPLETED → SCORED`; `RiskEvent`
percorre `DETECTED → RESOURCES_PRESENTED → ACKNOWLEDGED`. Um utilizador real do Lumen
responde ao instrumento todo de uma vez (como acontece na maioria das apps de
bem-estar), por isso todas as transições até `COMPLETED`/`RESOURCES_PRESENTED`
acontecem dentro do mesmo pedido HTTP — não há um "instrumento em progresso" que
persista entre sessões. Cada transição continua a validar o estado anterior
(`Assessment.start()`/`complete()`/`score()`, `RiskEvent.presentResources()`/
`acknowledge()` lançam `InvalidAssessmentStateException`/
`InvalidRiskEventTransitionException` se chamadas fora de ordem) e é testada
isoladamente (`AssessmentTest`, `RiskEventTest`).

**Alternativa rejeitada:** expor cada transição como o seu próprio endpoint HTTP,
modelando um percurso assíncrono de vários dias (ex.: instrumento enviado por email,
respondido mais tarde). Mais fiel a alguns fluxos clínicos reais, mas nenhum caso de
uso concreto do produto precisa disso agora, e obrigaria o frontend a gerir estado
entre sessões sem ganho proporcional. A máquina de estados fica pronta para essa
extensão — só não é usada dessa forma ainda.

## Decisão 2 — A pontuação fica gated atrás do acknowledgment, não do cálculo

Quando o item 9 dispara, `AssessmentService.submit()` cria o `Assessment` em
`COMPLETED` (nunca `SCORED`) e devolve um `CrisisTriggeredResult` — sem `totalScore`
nem `wellbeingBand` no corpo da resposta. A pontuação só é calculada e persistida em
`AssessmentService.scoreAfterCrisisAcknowledgment()`, chamada por
`CrisisService.acknowledge()` depois de o `RiskEvent` transitar validamente para
`ACKNOWLEDGED`. Isto torna "não mostra a pontuação como se fosse um resultado normal"
(brief §6.2) uma propriedade verificável: não existe nenhum caminho de código em que a
pontuação exista antes do acknowledgment.

**Alternativa rejeitada:** calcular a pontuação sempre, e só filtrar o campo na
serialização da resposta quando há crise. Rejeitada porque é uma garantia mais fraca —
depende de nunca esquecer o filtro num serializer futuro (ex.: um export de dados, um
endpoint de admin). Não persistir o `AssessmentScore` até ao acknowledgment remove essa
classe de erro inteira: não há dado para vazar.

## Decisão 3 — Cinco faixas de bem-estar para o PHQ-9, quatro para o GAD-7

`WellbeingBand` é um único enum partilhado (`MINIMAL/MILD/MODERATE/PRONOUNCED/
ELEVATED`) para os dois instrumentos, mas o PHQ-9 usa as cinco faixas (cortes oficiais
0–4/5–9/10–14/15–19/20–27) e o GAD-7 usa só quatro (0–4/5–9/10–14/15–21), nunca
produzindo `PRONOUNCED`. A alternativa de dois enums separados foi rejeitada por
duplicar sem necessidade — os nomes das faixas já são genéricos e não-clínicos por
natureza (ADR-0001), por isso não há razão para não os partilhar; só a lógica de corte
por instrumento (`AssessmentService.bandFor`) difere, e essa lógica só tem sentido
vivendo junto ao resto do scoring.

## Decisão 4 — `CrisisResource` é dado de referência, não dado de demonstração

Ao contrário do utilizador de demo (`db/dev-seed`), os contactos de crise (SNS 24, SOS
Voz Amiga, 112) são semeados numa migração normal (`V11__create_crisis_resources.sql`)
porque são dados públicos necessários em qualquer ambiente — sem eles, o fluxo de
crise não tem o que mostrar em produção. Ficam explicitamente marcados (no comentário
da migração e no README) como precisando de verificação de atualidade antes de
qualquer demonstração pública: mostrar um contacto errado num contexto de crise é
inaceitável (brief §6.4), e eu não tenho forma de garantir que os números que usei
continuam corretos no momento em que este código for lido.

## Consequências

**Positivas:**
- O invariante central do produto (item 9 → crise → nunca pontuação direta) está
  provado por teste de integração real (`AssessmentIntegrationTest`,
  Testcontainers + MockMvc), não só por teste unitário com mocks.
- A máquina de estados de ambas as entidades é extensível: um `RiskEvent` disparado
  por outra fonte (ex.: classificador de chat na Fase 6) só precisa de um novo
  `TriggerSource`, sem tocar nas transições.

**Negativas / dívida técnica conscientemente aceite:**
- Sem lembrete proativo de reavaliação mensal — a cadência de 30 dias é só um bloqueio
  reativo (403 se o utilizador tentar cedo demais), não uma notificação "já podes
  refazer". Fica para uma fase futura com infraestrutura de notificações.
- A tradução PT-PT dos itens do PHQ-9/GAD-7 (`frontend/src/i18n/locales/pt.json`) é a
  minha melhor tentativa a partir de traduções de referência conhecidas, mas **não foi
  revista clinicamente** — é o único ponto desta fase onde a validação de um
  profissional (a minha formação em Psicologia, fora do código) é a fonte de verdade,
  não eu.

## Trade-offs (como eu defenderia isto numa entrevista)

Um Tech Lead vai perguntar porque é que a pontuação não é simplesmente "escondida" na
UI quando há crise, em vez de nunca ser calculada. A resposta é a mesma filosofia da
Decisão 2: uma garantia de segurança clínica não pode depender de a camada de
apresentação se lembrar de a aplicar sempre. Se amanhã existir um segundo cliente
(app mobile, endpoint de export, painel futuro de `CLINICIAN`), cada um teria de
reimplementar o mesmo filtro — e um deles, mais cedo ou mais tarde, vai esquecer-se.
Não persistir a pontuação até ao acknowledgment move a garantia para o único sítio
onde ela não pode ser contornada: o facto de o dado não existir.
