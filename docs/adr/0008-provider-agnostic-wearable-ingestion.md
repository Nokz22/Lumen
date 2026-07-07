# ADR-0008 — Ingestão de wearable provider-agnostic: simulador primeiro

**Status:** Aceite
**Data:** 2026-07-08
**Decisores:** Nuno (autor/mantenedor)

## Contexto

O brief (§7) pede para mostrar competência de ingestão e correlação de sinais
fisiológicos **sem depender de hardware real nem de app nativa no MVP**, deixando a
porta aberta para um wearable real mais tarde ("Fitbit / Garmin / Apple Health / Health
Connect encaixam sem reescrever o domínio"). `ConsentType.WEARABLE_INGESTION` já
existia desde a Fase 1, sem uso até agora. Esta fase teve de decidir como modelar dados
de série temporal de forma que um adaptador real, quando existir, não obrigue a mexer
no domínio nem no pipeline de ingestão.

## Decisão 1 — Série temporal genérica `(type, value, recordedAt)`, não tabela larga

`WearableReading` tem uma linha por leitura, com um `WearableReadingType` (enum) a
identificar a métrica, em vez de uma tabela com uma coluna `heartRate`, uma
`hrv`, uma `sleepHours`, etc. É o padrão normal de série temporal (à
Prometheus/InfluxDB) e é o que torna a ingestão **realmente** provider-agnostic: um
adaptador Fitbit e um adaptador Garmin produzem exatamente a mesma forma de linha, só
`source` muda. Uma tabela larga obrigaria a adicionar uma coluna sempre que um novo
provider trouxesse uma métrica que os outros não têm.

**Alternativa rejeitada:** uma tabela por métrica (`heart_rate_readings`,
`hrv_readings`, ...). Mais tipada, mas multiplica migrações e repositórios por cada
métrica nova — o oposto de "provider-agnostic".

## Decisão 2 — `WearableSource` é uma porta de dados, não de persistência

`WearableSource.fetch(userId, since, until)` devolve leituras; não as grava. O único
adaptador hoje é o `SIMULATOR`; um adaptador Fitbit real implementaria a mesma
interface chamando a API do Fitbit em vez de gerar dados sintéticos —
`WearableIngestionService` e tudo o resto do pipeline não sabem nem precisam de saber
a diferença (Composition over Inheritance na fronteira externa, tal como o brief
pede). O simulador é apenas mais um `WearableSource`, não um caso especial.

## Decisão 3 — Simulador com correlação suave e ruidosa, nunca perfeita

`SimulatorWearableSource` empurra HRV/frequência cardíaca/passos de cada dia com base
no défice de sono da noite anterior, mais ruído gaussiano — nunca uma relação
determinística. Um simulador perfeito ("dormir menos SEMPRE reduz a energia em X")
seria desonesto sobre o que "correlação" quer dizer e tornaria o mecanismo de deteção
trivial e pouco convincente de demonstrar; um simulador puramente aleatório nunca
teria nada de coerente para mostrar. O ruído é a escolha certa: o motor de correlação
(Decisão 4) tem de encontrar um padrão real, não decorado.

## Decisão 4 — Correlação de Pearson à mão, sobre pares diários, com limiar e amostra mínima

`WearableInsightService.pearson()` é uma função pura de ~15 linhas — não se justifica
puxar uma biblioteca de estatística para isto. Um insight só é devolvido quando
`|r| ≥ 0.3` **e** há pelo menos 5 pares de dias — evita apresentar como "padrão" o que
é só ruído de uma amostra pequena. O texto do insight é sempre uma observação
("nos dias com mais sono, tendes a reportar mais energia"), nunca uma frase de causa —
mesma fronteira ética do resto do projeto (ADR-0001): dado fisiológico + humor
autorreportado não vira "diagnóstico" nem "veredicto", fica em "insight".

## Decisão 5 — Sem integração com o motor de recomendação nesta fase

O brief menciona HRV como possível gatilho futuro de regras ("ansioso + HRV baixa →
respiração"), mas o DoD da Fase 5 só pede ingestão + correlação no dashboard.
Estender `RecommendationService` (Fase 4) para consumir sinais de wearable é uma
extensão natural e pequena — os dois motores já partilham o mesmo espírito (regras
simples e explicáveis) — mas fica fora de escopo aqui, para não misturar duas fases
num único conjunto de commits.

## Consequências

**Positivas:**
- Um adaptador real (Fitbit/Garmin) só precisa de implementar `WearableSource` — zero
  mudanças no domínio, na persistência, no serviço de correlação ou nos endpoints.
- A correlação é testável com dados construídos à mão (não o simulador) onde o
  resultado é conhecido, exatamente como o scoring do PHQ-9/GAD-7 e as regras de
  recomendação nas fases anteriores.

**Negativas / dívida técnica conscientemente aceite:**
- Nenhum adaptador real chega a ser escrito nesta fase — a porta existe, mas a prova
  de que aguenta um Fitbit/Garmin real fica por demonstrar, não só por afirmar.
- Pearson é uma correlação linear simples; não capta padrões não-lineares nem
  desfasamentos variáveis (ex.: o efeito de uma má noite podia demorar dois dias a
  aparecer, não um).
- Sem integração com o motor de recomendação (Decisão 5) — aceite como escopo da
  próxima iteração natural, não desta fase.

## Trade-offs (como eu defenderia isto numa entrevista)

Um Tech Lead vai perguntar "o simulador não é só um mock, uma forma fácil de fugir ao
trabalho difícil de integrar um wearable real?" — a resposta é que o valor
demonstrado aqui não é "liguei a uma API externa", é "desenhei uma fronteira onde
ligar a uma API externa não obriga a tocar em mais nada". O simulador prova
exatamente essa fronteira: gera dados pela mesma porta, passa pelo mesmo pipeline de
ingestão, alimenta o mesmo motor de correlação. Trocar `SIMULATOR` por `FITBIT`
depois é, por desenho, um exercício de implementar uma interface — não um refactor.
