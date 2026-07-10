# ADR-0001 — Fronteira ética: ferramenta de bem-estar, não-diagnóstica

**Status:** Aceite
**Data:** 2026-07-05
**Decisores:** Nuno (autor/mantenedor)

## Contexto

O Lumen cruza engenharia de software com um domínio sensível: saúde mental. Antes de
qualquer entidade de domínio, endpoint ou linha de UI existir, é preciso fixar o que o
produto **é** e o que **nunca é**, porque essa fronteira condiciona toda a decisão
técnica que se segue — nomes de campos da API, vocabulário devolvido pelo motor de
scoring, o system prompt do LLM (Fase 6), e até que dados podem ser recolhidos de
menores (não podem — 18+).

Sem esta decisão registada e verificável, cada feature futura teria de reinterpretar a
fronteira caso a caso, com risco real de o produto deslizar para linguagem diagnóstica
"por conveniência" (ex.: "a tua pontuação indica depressão moderada" em vez de "a tua
pontuação de bem-estar está na faixa X").

## Problema

Como garantir que o Lumen nunca se comporta como dispositivo médico ou ferramenta de
diagnóstico — nem no dia 1, nem depois de 6 fases de features a acumularem-se — quando
o domínio (PHQ-9, GAD-7, deteção de risco) é literalmente clínico por natureza?

## Opções consideradas

1. **Sem fronteira formal, só "bom senso" ao escrever cada feature.**
   Falha previsível: bom senso não é verificável, não sobrevive a 8 fases de trabalho
   nem a decisões apressadas sob pressão de prazo. Não há nada para um Tech Lead auditar.

2. **Fronteira documentada apenas no README, informalmente.**
   Melhor que (1), mas um README é prosa — não força consistência de vocabulário na API
   nem é referenciável como decisão de arquitetura datada e com trade-offs explícitos.

3. **ADR formal (esta opção) + regra de vocabulário aplicada em três camadas: UI, nomes
   de campos da API, prompts do LLM.**
   Torna a fronteira uma decisão de arquitetura citável, com consequências e trade-offs
   explícitos, e testável (ex.: "AssessmentScore nunca serializado com vocabulário
   diagnóstico" é um invariante com teste próprio, standards §3).

**Decisão:** opção 3.

## Decisão

O Lumen é uma ferramenta de **bem-estar e auto-cuidado**. Nunca é:
- um **dispositivo médico** (sem screening diagnóstico regulado, sem marcação FDA/CE
  médica);
- uma ferramenta de **diagnóstico** (nunca "tens depressão", nunca "estás em burnout");
- um **substituto de cuidado profissional** (reforça sempre o encaminhamento humano);
- destinado a **menores** (apenas adultos, 18+; verificação etária na Fase 2).

O que o Lumen **é**: um espaço para a pessoa se observar (humor, sono, energia, sinais
fisiológicos), aprender ações concretas de auto-cuidado, e ver as suas tendências ao
longo do tempo. Ilumina caminhos — não decide por ninguém.

**Regra de vocabulário, vinculativa em todo o código:**
- nunca "pontuação de depressão" → sempre "pontuação de bem-estar / de auto-monitorização";
- nunca "estás ansioso" → "registaste sentir-te ansioso";
- a app descreve o que a pessoa reporta; nunca interpreta clinicamente.

Esta regra aplica-se a três superfícies concretas, cada uma com o seu próprio mecanismo
de verificação nas fases seguintes:
1. **Textos de UI** — revisão manual + glossário (`docs/glossary.md`, a partir da Fase 1).
2. **Nomes de campos da API / DTOs** — revisão de código; nenhum campo devolve rótulo
   diagnóstico (ex.: `wellbeingScore`, nunca `depressionScore`).
3. **Prompts do LLM** (Fase 6) — system prompt proíbe explicitamente diagnóstico.

## Consequências

**Positivas:**
- Cada decisão futura (scoring, chat, UI) tem um critério de arbitragem claro: "isto
  aproxima-se de diagnóstico?" Se sim, para e questiona-se — como já fixado no
  `docs/constitution.md` ("se eu pedir algo que aproxime a app de diagnóstico... para e avisa").
- Dá ao projeto um argumento de entrevista forte e verificável: a fronteira ética não é
  uma frase de marketing, é uma decisão datada com trade-offs assumidos.
- Cria a obrigação de recursos de crise por região desde o desenho (Fase 3), porque o
  produto explicitamente não resolve crises — encaminha.

**Negativas / custos aceites:**
- Vocabulário mais longo e menos "direto" nalgumas telas (ex.: "registaste sentir-te
  ansioso" é mais comprido que "estás ansioso"). Aceite: precisão clínica > concisão.
- Não podemos usar linguagem diagnóstica mesmo quando o instrumento clínico subjacente
  (PHQ-9) é, ele próprio, um instrumento de rastreio de depressão. Isto exige tradução
  cuidadosa do scoring oficial para "faixa de bem-estar" sem perder a utilidade do
  instrumento — trabalho de Fase 3, não trivial.

## Trade-offs (como eu defenderia isto numa entrevista)

A alternativa óbvia seria "deixar a app falar como as pessoas falam" (mais direta, mais
parecida com um chatbot de wellness genérico). Rejeitei-a porque o diferencial do Lumen
é precisamente **não** ser isso — a segurança clínica está no topo dos quality
attributes (standards §1) e não é negociável por UX. Um Tech Lead de healthtech vai
perguntar "como garantes que isto não vira conselho médico não regulado?" — a resposta
é este ADR, mais os testes de invariante que vêm nas fases seguintes (ex.:
`shouldNeverSerializeAssessmentScoreWithDiagnosticVocabulary`).
