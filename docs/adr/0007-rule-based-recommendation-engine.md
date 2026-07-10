# ADR-0007 — Motor de recomendação baseado em regras, não em ML

**Status:** Aceite
**Data:** 2026-07-07
**Decisores:** Nuno (autor/mantenedor)

## Contexto

A Fase 4 introduz o "coração da experiência" (brief §5): o check-in diário passa a
gerar sugestões concretas de auto-cuidado (respiração, caminhada, alongamento, higiene
de sono, ativação comportamental, grounding). O brief pede explicitamente **regras
transparentes e explicáveis, nunca uma caixa-preta**, e pede um ADR a justificar
porque não se usa ML aqui. Esta é a decisão a documentar.

## Decisão — Mapeamento estado→ação é um `switch`/mapa determinístico, não um modelo

`RecommendationService.determineRecommendations()` é uma função pura: dado o
`MoodCheckInSubmittedEvent` (emoção, energia, sono), devolve um mapa
`ExerciseCategory → reason` através de ~4 regras fixas (energia baixa/sono curto →
ativação comportamental + higiene de sono; ansioso → respiração + caminhada;
frustrado/zangado → grounding + caminhada; triste → ativação comportamental). Nenhum
modelo treinado, nenhuma pontuação de confiança, nenhum A/B test de pesos.

**Porquê isto e não ML:**
1. **Explicabilidade não-negociável num domínio sensível.** Cada `Recommendation`
   guarda o `reason` exato que a gerou — a pessoa vê sempre *porque* algo foi sugerido,
   nunca "o sistema achou". Um modelo (mesmo simples, tipo regressão logística) não dá
   esta garantia sem trabalho extra de explicabilidade que aqui seria pura
   sobre-engenharia.
2. **Sem dados para treinar nada a sério.** Não há histórico de "esta sugestão
   ajudou" (não existe feedback loop nesta fase — ver dívida técnica abaixo), por isso
   qualquer modelo seria treinado com dados sintéticos ou não seria treinado de todo,
   o que anula a vantagem de usar ML em primeiro lugar.
3. **Testabilidade direta.** `RecommendationServiceTest` testa cada regra isoladamente
   com `assertEquals`-style verificação — impossível de fazer com a mesma clareza
   sobre pesos de um modelo.
4. **Simplicidade é o desempate por defeito** (docs/constitution.md) — 4 regras se leem numa
   função, não justificam a complexidade operacional de servir/versionar um modelo.

**Alternativa rejeitada:** um motor de regras "configurável" (ex.: regras carregadas
de BD, DSL própria). Rejeitada por sobre-engenharia — com 4 regras, uma DSL não paga o
seu custo; se a biblioteca de regras crescer muito, extrair isso é um refactor
localizado, não uma decisão a antecipar agora (YAGNI).

## Consequências

**Positivas:**
- Qualquer pessoa a rever o código consegue prever exatamente que recomendação sai de
  que check-in, sem executar nada.
- Zero infraestrutura extra (sem serving de modelo, sem feature store).

**Negativas / dívida técnica conscientemente aceite:**
- **Sem personalização real** — duas pessoas com o mesmo padrão de check-in recebem
  sempre as mesmas categorias de exercício. A rotação de exercício dentro de uma
  categoria é determinística (hash do `moodCheckInId`), não aprendida.
- **Sem feedback loop** — a pessoa não pode dizer "esta sugestão não ajudou"; não há
  sinal nenhum a fechar o ciclo além de `ExerciseCompletion` (fez/não fez), que hoje
  não influencia recomendações futuras.
- Se o produto crescer para precisar de personalização real, a integração natural é
  usar `ExerciseCompletion` como sinal de treino — mas isso é trabalho de uma fase
  futura, não desta.

## Trade-offs (como eu defenderia isto numa entrevista)

Um Tech Lead vai perguntar "não é regredir, usar regras em vez de ML em 2026?" — a
resposta é que a escolha certa depende do domínio, não da moda: aqui a explicabilidade
supera a sofisticação porque é bem-estar mental, onde uma sugestão que a pessoa não
percebe *porquê* recebeu é pior do que nenhuma sugestão. O motor de regras não é uma
limitação temporária à espera de ML — é a decisão de arquitetura correta para este
produto, documentada para não ser "corrigida" sem essa discussão acontecer primeiro.
