# ADR-0010 — Guardrails do companheiro LLM: três camadas, segurança nunca confiada ao modelo

**Status:** Aceite
**Data:** 2026-07-09
**Decisores:** Nuno (autor/mantenedor)

## Contexto

Esta é a fase que o brief chama "a mais sensível e mais diferenciadora" do projeto —
um LLM real a conversar com alguém sobre o seu bem-estar. docs/constitution.md define os
invariantes que não podem falhar: o LLM nunca é invocado quando o classificador de
risco de entrada dispara; a deteção de crise é independente da disponibilidade do
LLM; conteúdo de chat nunca aparece em logs. Um modelo de linguagem generativo não é
determinístico, pode alucinar, pode ser manipulado (prompt injection) e pode falhar
ou ficar indisponível — nenhuma dessas propriedades é aceitável para a peça que
decide se alguém em crise recebe os recursos de apoio certos. A decisão central desta
fase é: **a segurança nunca pode depender do modelo se comportar bem.**

## Decisão 1 — Três camadas de defesa, cada uma independente da anterior

1. **Classificador de risco de entrada** (`ChatRiskClassifier`) — corre sempre, antes
   de qualquer chamada ao LLM. Se disparar, o `LlmClient` nunca é tocado.
2. **System prompt clínico rígido** (`CompanionSystemPrompt`) — instrui o modelo a
   nunca diagnosticar, nunca validar pensamento distorcido como facto, nunca
   recomendar medicação, e explica que a deteção de crise já está tratada por um
   sistema separado antes de o modelo alguma vez ser chamado.
3. **Verificação de saída** (`ChatOutputVerifier`) — revê o texto completo devolvido
   pelo modelo antes de chegar à pessoa; se falhar, substitui por uma mensagem segura
   + recursos.

Nenhuma camada depende de as outras terem funcionado bem — é defesa em profundidade a
sério, não uma verificação redundante disfarçada de três.

## Decisão 2 — Classificador de entrada é uma lista de frases, não um segundo modelo

`ChatRiskClassifier.isHighRisk()` normaliza o texto (minúsculas, remove acentos) e
compara contra ~28 frases PT+EN de risco de autoagressão/suicídio conhecidas — a
mesma filosofia do resto do projeto (regras simples e explicáveis, nunca caixa-preta,
tal como o scoring do PHQ-9). A alternativa óbvia — usar o próprio LLM, ou um segundo
modelo, para classificar risco — foi rejeitada por três razões: (1) um LLM pode estar
em baixo, e a deteção de crise tem de sobreviver a isso (invariante do docs/constitution.md); (2)
uma chamada de classificação teria a mesma latência e custo que a conversa em si, a
dobrar por turno; (3) uma decisão de "isto é uma crise ou não" tem de ser auditável e
determinística — a mesma frase tem de disparar sempre, não depender de uma amostragem
não-determinística de um modelo generativo. Testado explicitamente: um teste
(`shouldDetectCrisisEvenWhenTheCompanionResponseServiceWouldFail`) simula o
`CompanionResponseService` a falhar sempre e confirma que a deteção de crise continua
a funcionar — porque estruturalmente nunca chega a essa peça.

## Decisão 3 — O caminho de crise nunca toca o `LlmClient`, por desenho de fluxo, não por verificação

`ConversationService.submitMessage()` grava a mensagem do utilizador, corre o
classificador e, se disparar, devolve logo um `ConversationCrisisResult` —
`CompanionResponseService.generateResponseAsync()` nunca chega a ser chamado. Não há
um `if` a verificar "já disparou crise, não chames o LLM"; a chamada ao LLM está
literalmente noutro ramo do código, inatingível a partir do ramo de crise. Isto é
deliberado: uma verificação pode ser esquecida numa alteração futura; a ausência de
um caminho de código não pode.

## Decisão 4 — Verificação de saída é outro denylist, não um segundo LLM a rever o primeiro

`ChatOutputVerifier.isSafe()` procura linguagem diagnóstica ("you have depression",
"generalized anxiety disorder") e conselhos de medicação ("you should take",
"increase your dose") na resposta do modelo. Se falhar, substitui por
`SAFE_FALLBACK_MESSAGE` — mas **não abre um `RiskEvent`**. A distinção importa: isto
apanha o modelo a falhar uma regra (drift, jailbreak, alucinação), não
necessariamente a pessoa estar em risco — são preocupações diferentes com respostas
diferentes.

## Decisão 5 — Streaming: buffer completo, verificação, depois "reenvio" simulado

`CompanionResponseService` acumula o texto completo do modelo antes de o mostrar a
quem quer que seja — só depois de `ChatOutputVerifier` aprovar (ou substituir) é que o
texto é dividido em palavras e reenviado via WebSocket, simulando o efeito de
streaming ao vivo. Isto é um desvio deliberado de "streaming literal": com reenvio
token-a-token real, uma falha na verificação só seria apanhada depois de a pessoa já
ter visto texto potencialmente inseguro no ecrã — o guardrail chegaria tarde demais.
Segundo a ordem de desempate do docs/constitution.md ("Segurança clínica e Privacidade >
Clareza > ... > Performance"), um atraso perceptível de alguns segundos antes do
"início" visual da resposta vale sempre mais do que a garantia de segurança.

## Alternativas rejeitadas

- **Confiar no próprio LLM para se autopolicar** (ex.: pedir-lhe para recusar
  responder a mensagens de risco). Modelos podem ser manipulados a ignorar
  instruções do sistema; nunca seria suficiente sozinho como única linha de defesa.
- **Um segundo LLM como "juiz"** a rever a entrada e a saída do primeiro. Adiciona
  custo, latência e mais uma fonte de comportamento não-determinístico exatamente
  onde se precisa do oposto.

## Consequências

**Positivas:**
- Os invariantes do docs/constitution.md são garantidos pela estrutura do código, não pela
  disciplina de quem o edita no futuro — testável sem nunca chamar um LLM real.
- A app funciona com segurança clínica intacta mesmo com o provider de LLM
  completamente indisponível.

**Negativas / dívida técnica conscientemente aceite:**
- Um denylist de frases não apanha risco expresso de forma indireta, metafórica, ou
  numa língua não prevista (só PT+EN) — falsos negativos são possíveis. Um Tech Lead
  perguntaria exatamente isto; a resposta honesta é que é uma primeira camada
  auditável, não uma garantia de deteção universal — e é precisamente por isso que
  as outras duas camadas (system prompt + verificação de saída) existem em paralelo,
  não como substitutas.
- O verificador de saída também é um denylist — pode ser evadido por paráfrase que
  não contenha as frases exatas procuradas.
- Nenhuma avaliação sistemática de segurança do LLM (tipo MindEval, da Sword, para
  diálogo de saúde mental) foi aplicada aqui — os testes desta fase provam
  comportamento pontual (frases conhecidas disparam/não disparam), não cobertura
  estatística contra um conjunto adversarial. Fica documentado como o passo natural
  a seguir para levar isto a um nível de rigor de produção.

## Trade-offs (como eu defenderia isto numa entrevista)

Um Tech Lead vai perguntar "porque não deixar o modelo mais recente e mais capaz
tratar disto, já que os LLMs de hoje são bons a reconhecer risco?" — a resposta é que
"bom a reconhecer" não é "garantido a reconhecer sempre", e esta é precisamente a
categoria de decisão onde uma falha silenciosa é inaceitável. Uma regra simples,
determinística e auditável que apanha 90% dos casos de forma **previsível** vale mais
aqui do que um modelo que apanha 98% dos casos de forma **imprevisível** — porque os
2% que falham são exatamente os que mais importam, e com uma regra simples sei
exatamente onde e porquê falha; com um modelo, não sei até acontecer.
