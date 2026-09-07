# ADR-0012 — Métricas de domínio e rate limiting

**Status:** Aceite
**Data:** 2026-09-07
**Decisores:** Nuno (autor/mantenedor)

## Contexto

Os standards (§0, §9) ancoram métricas Micrometer + Prometheus e rate limiting à Fase 7.
Nenhum dos dois existia. O Actuator expunha `health` e `info` e nada mais; qualquer pessoa
podia tentar passwords contra `/api/v1/auth/login` sem limite nenhum.

## Decisão 1 — Três contadores escritos à mão, além dos que vêm de borla

O Micrometer traz ~119 séries sem esforço (JVM, pool de conexões, HTTP, Resilience4j). O que
ele não pode saber é para que serve este sistema. Acrescentei três:

- `lumen_risk_event_triggered_total{source}` — fluxos de crise iniciados, por origem da deteção
- `lumen_companion_guardrail_blocked_total{layer}` — mensagens travadas, por camada que travou
- `lumen_companion_fallback_served_total` — respostas seguras servidas porque o modelo falhou

São estas que respondem às perguntas que este produto tem de conseguir responder: *o caminho
de segurança está vivo?* e *os guardrails estão a fazer alguma coisa?*

**Só duas camadas de guardrail têm contador.** A camada do meio (ADR-0010) é o system prompt
clínico — uma instrução ao modelo, não uma verificação com resultado. Não há nada para contar.

## Decisão 2 — Todas as séries registadas a zero no arranque

Um contador que só aparece quando é incrementado pela primeira vez é indistinguível de um
exportador partido. Para o contador de crise, essa diferença é entre *"hoje ninguém esteve
em risco"* e *"o caminho de segurança está a falhar em silêncio desde o deploy"*. As séries
existem a zero desde o primeiro segundo.

## Decisão 3 — Nenhuma métrica identifica ninguém

Todas as etiquetas vêm de enums, nunca de texto livre, e nenhuma leva um id de utilizador.
Uma etiqueta fica guardada indefinidamente e é legível por quem tiver acesso ao endpoint —
um sistema sem nenhuma das garantias de privacidade que o resto deste projeto tem. Mede-se
que um caminho de segurança disparou, nunca para quem. Há um teste com esse nome.

## Decisão 4 — `/actuator/prometheus` exige ADMIN; `/actuator/health` não

O `health` continua público porque a plataforma tem de o alcançar sem credenciais — incluindo
as probes de liveness e readiness por baixo dele.

O `prometheus` fica atrás de ADMIN. O `health` diz "de pé" ou "em baixo". O de métricas
descreve quanto tráfego há, com que frequência o fluxo de crise dispara e quando o modelo
está a falhar: um retrato operacional e, neste domínio, um retrato de quando há pessoas em
sofrimento.

## Decisão 5 — Duas políticas de rate limiting, e uma lista explícita do que nunca é limitado

| Política | Limite | Chave | Porquê |
|---|---|---|---|
| `AUTHENTICATION` | 10 / minuto | endereço do cliente | Não autenticado e alcançável por qualquer um |
| `COMPANION_MESSAGE` | 30 / hora | id do utilizador | Custa dinheiro em cada chamada; a conta é o que se gasta |

**O fluxo de crise nunca é limitado. Em circunstância nenhuma.** Apresentar recursos e
confirmar um `RiskEvent` não têm limite, seja qual for o ritmo, venha de quem vier. Quem
está a repetir o pedido porque a página não carregou é a última pessoa que devia encontrar
um 429, e não há cenário de abuso contra esses dois endpoints que compense essa troca
(project-brief §6.2: a partir do momento em que há sinalização de risco, a segurança é o
comportamento por defeito, não best effort).

Check-ins, instrumentos e histórico também ficam sem limite: são autenticados, restritos aos
dados do próprio, e não custam nada que valha a pena defender.

## Decisão 6 — Baldes em memória, com a fronteira dita em voz alta

Os baldes vivem num JVM. Duas instâncias atrás de um balanceador permitiriam cada uma a quota
inteira. Corrigir isso exige um armazenamento partilhado, e o Redis está na lista de coisas
que este projeto não acrescenta sem as ter merecido (standards §15). Uma instância é o que
está implantado.

Isto está escrito em comentário na própria classe, para que a próxima pessoa saiba que é uma
fronteira conhecida e não um bug que descobriu.

**Caffeine e não um mapa simples:** um mapa indexado por endereço de cliente cresce para
sempre, uma fuga de memória lenta com uma chave controlada por quem ataca.

## Decisão 7 — `getRemoteAddr()`, nunca ler o `X-Forwarded-For` diretamente

Atrás do proxy da plataforma, o endereço do socket é o do proxy — todos os chamadores
partilhariam um balde. É para isso que existe o `server.forward-headers-strategy`, ativado no
perfil `prod`.

Ler o cabeçalho diretamente no filtro seria pior do que não limitar nada: um cabeçalho que o
cliente pode definir é uma chave de rate limit que o cliente pode escolher.

## Consequências

- Três dependências novas: `micrometer-registry-prometheus`, `bucket4j` e `caffeine`. As duas
  últimas são para o rate limiting; escrever um token bucket à mão seria código concorrente
  subtil por poupar uma dependência pequena e testada.
- Um `429` devolve RFC 7807 com `Retry-After`, como todos os outros erros da API.
- Escalar horizontalmente passa a ter um pré-requisito explícito: o rate limiting deixa de
  estar correto até os baldes serem partilhados.
