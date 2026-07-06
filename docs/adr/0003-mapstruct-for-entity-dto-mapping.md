# ADR-0003 — MapStruct para mapear Entity ↔ DTO

**Status:** Aceite
**Data:** 2026-07-06
**Decisores:** Nuno (autor/mantenedor)

## Contexto

As entidades JPA (`MoodCheckIn`, e as que se seguem nas próximas fases) nunca podem
cruzar a fronteira da API — os standards exigem sempre DTOs (`docs/standards.md` §2).
Isso significa escrever, para cada endpoint, código que converte entre a entidade de
domínio e a forma que a API expõe.

## Problema

Escrever esses conversores à mão é repetitivo e um local fácil de introduzir bugs
silenciosos (um campo esquecido, um `null` mal tratado) sem que o compilador avise.

## Opções consideradas

1. **Mapeamento manual** (um método `toResponse(Entity): Dto` escrito à mão por
   entidade). Zero dependências novas, mas cresce linearmente com o número de
   entidades/DTOs e não apanha campos esquecidos em tempo de compilação — só em runtime
   ou nunca.
2. **ModelMapper / reflection em runtime.** Mais "mágico", erros só aparecem em runtime,
   overhead de reflection em todos os pedidos.
3. **MapStruct** — gera o código de mapeamento em *compile-time* via annotation
   processing. Se um campo do destino não tiver correspondência de origem, é um aviso
   (ou erro) de compilação, não uma surpresa em produção.

**Decisão:** opção 3.

## Decisão

Um `interface *Mapper` anotado com `@Mapper(componentModel = "spring")` por par
entidade/DTO relevante, gerado como bean Spring injetável. Neste projeto, o mapper vive
na camada `application` (não em `presentation`) — é lá que a entidade de domínio é
legitimamente conhecida; o Controller nunca importa nem vê o tipo da entidade, só o DTO
que o `Service` já devolve mapeado.

## Consequências

**Positivas:**
- Erros de mapeamento (campo em falta, tipo incompatível) aparecem no build, não em
  produção.
- Zero overhead de reflection — o código gerado é Java simples, tão rápido como escrito
  à mão.
- Descoberto ao escrever o `ArchUnit` desta fase: colocar o mapper em `application` (e
  não em `presentation`) foi o que tornou a regra "entidades nunca referenciadas na
  camada de apresentação" verdadeira em vez de decorativa — o `Controller` original
  passava a entidade da `Service` diretamente e só depois mapeava; mover o mapeamento
  para dentro do `Service` fechou essa fuga.

**Negativas / custos aceites:**
- Mais um annotation processor no build (`mapstruct-processor`), mais um passo mental ao
  ler o código pela primeira vez ("onde está a implementação desta interface?" — é
  gerada, `*MapperImpl`, em `build/generated`).

## Trade-offs (como eu defenderia isto numa entrevista)

A alternativa de mapear à mão é perfeitamente válida para uma ou duas entidades — deixa
de o ser a partir de umas 4-5, que é onde este projeto vai estar já na Fase 3. Escolher
MapStruct agora, ainda com só uma entidade mapeada, é decidir a ferramenta antes de a
dor aparecer — mas ao contrário de abstrações especulativas (que os standards rejeitam
explicitamente, §15), esta é uma ferramenta *madura e estável*, não uma camada de
indireção que só eu invento.
