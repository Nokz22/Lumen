# ADR-0009 — Memória de conversa: janela fixa + resumo, nunca histórico completo

**Status:** Aceite
**Data:** 2026-07-09
**Decisores:** Nuno (autor/mantenedor)

## Contexto

Um LLM tem uma janela de contexto finita e cada token custa dinheiro e latência. Uma
conversa que dura semanas não pode ser reenviada na íntegra a cada mensagem — cresce
sem limite e eventualmente estoira o limite do modelo. Ao mesmo tempo, o companheiro
precisa de "lembrar-se" de coisas ditas há muitas mensagens atrás para a conversa não
parecer amnésica. `ConversationMessage` grava uma linha por mensagem (utilizador e
assistente); a questão desta fase é **o que exatamente se envia ao `LlmClient` a cada
turno**, não o que se guarda.

## Decisão 1 — Janela das últimas 20 mensagens em bruto + um resumo persistente

`ConversationContextBuilder.buildChatPrompt()` monta o prompt como: resumo atual (se
existir, como uma mensagem sintética) + as últimas 20 `ConversationMessage` ainda não
resumidas. Todas as mensagens continuam gravadas para sempre em
`conversation_messages` — a janela só limita o que é **enviado ao modelo**, nunca o
que é mostrado ao utilizador no histórico da UI nem o que existe na base de dados
(RGPD: a pessoa tem sempre acesso ao histórico completo, direito de exportação e
apagamento intactos).

## Decisão 2 — Resumo dispara ao ultrapassar 30 mensagens não resumidas, não a cada turno

`planSummarizationIfNeeded()` só devolve um plano quando a contagem de mensagens não
resumidas ultrapassa 30 — nesse momento, tudo exceto as últimas 20 é dobrado para
dentro de `ConversationSummary` via `LlmClient.complete()` (chamada síncrona, sem
streaming, porque não é visível ao utilizador). A folga entre o tamanho da janela (20)
e o limiar de resumo (30) é histerese deliberada: sem ela, ultrapassar o limiar por uma
mensagem obrigaria a resumir de novo no turno seguinte, e a seguir, repetidamente —
uma chamada extra ao LLM por turno, sem necessidade.

## Decisão 3 — Resumo é uma única linha por utilizador, sobrescrita, não versionada

`ConversationSummary` tem uma linha por `userId` (`user_id UNIQUE`), com
`summarizedThroughMessageId` a marcar até onde já está resumido. Cada vez que o
resumo é atualizado, o texto anterior é substituído — não existe histórico de resumos
anteriores. Só o resumo mais recente importa para construir o próximo prompt; guardar
versões anteriores não traria valor funcional, só complexidade.

## Decisão 4 — Contagem de mensagens, não tokens reais

O limiar de 20/30 é contagem de mensagens, não uma contagem real de tokens do modelo.
É simples, auditável e não obriga a depender de um tokenizador específico do provider
(que mudaria se o modelo mudasse). O trade-off é que uma mensagem muito longa (até ao
limite de 4000 caracteres validado em `SendMessageRequest`) pesa o mesmo que uma
mensagem de uma palavra — em teoria, 20 mensagens no limite máximo ainda podem ser
substanciais. Aceite como dívida técnica: ver secção de Consequências.

## Decisão 5 — `ConversationMessage.content` e `ConversationSummary.summaryText` cifrados

Reaproveita o `EncryptedStringConverter` já existente de `domain.moodcheckin`
(AES-GCM, `@Convert` na entidade) — conteúdo emocional de chat é tão sensível quanto
uma nota de check-in ou uma resposta de PHQ-9 (docs/constitution.md: "conteúdo emocional...
nunca aparecem em logs" aplica-se aqui da mesma forma).

## Alternativas rejeitadas

- **Reenviar sempre o histórico completo.** Funciona para conversas curtas, mas
  degrada-se com o tempo — o oposto do que este mecanismo tem de garantir.
- **Retrieval sobre embeddings/vector store.** Traria memória "semântica" mais
  sofisticada, mas obriga a uma nova peça de infraestrutura (base vetorial) sem
  justificação no brief para o âmbito deste projeto de portfólio — complexidade não
  ganha aqui (docs/constitution.md: "simplicidade é o desempate por defeito").

## Consequências

**Positivas:**
- O custo por turno é previsível e limitado — nunca cresce com o comprimento total da
  conversa.
- Nada se perde: o histórico completo continua acessível na UI e sujeito aos mesmos
  direitos RGPD que qualquer outro dado; só o que se envia ao modelo é que é limitado.

**Negativas / dívida técnica conscientemente aceite:**
- Contagem de mensagens em vez de tokens reais (Decisão 4) — em conversas com
  mensagens muito longas, o prompt enviado ao modelo pode ainda assim ser maior do
  que o ideal.
- Perder a capacidade de auditar resumos anteriores (Decisão 3) — só o resumo atual
  existe; não há forma de ver "o que o sistema achava que sabia" numa data passada.

## Trade-offs (como eu defenderia isto numa entrevista)

Um Tech Lead vai perguntar "porque não usar uma biblioteca de memória conversacional
pronta (ex.: LangChain memory)?" — a resposta é que estas bibliotecas resolvem
exatamente este problema, mas trazem uma abstração e uma dependência extra para uma
lógica que, a esta escala, é ~50 linhas de Java claras e testáveis à mão (janela +
resumo + limiar). Nada aqui é específico de um provider de LLM — trocar `CannedLlmClient`
por `AnthropicLlmClient` não obriga a tocar em `ConversationContextBuilder`. Preferir
código simples e próprio a uma dependência pesada, quando o problema cabe em poucas
linhas, é a mesma filosofia da correlação de Pearson à mão na Fase 5 (ADR-0008).
