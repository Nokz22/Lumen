# ADR-0005 — Privacy by design: consentimento, audit log, cifra em repouso

**Status:** Aceite
**Data:** 2026-07-06
**Decisores:** Nuno (autor/mantenedor)

## Contexto

Dados de saúde mental são categoria especial no RGPD (`CLAUDE.md`, `docs/standards.md`
§7). A Fase 1 já criou a entidade `ConsentRecord` (append-only) mas sem ciclo de vida
nem ligação a nenhuma feature. A Fase 2 tem de tornar isto real: consentimento que
efetivamente controla acesso, um audit log de acessos, e cifra do único campo de texto
livre sensível que existe hoje.

## Decisão 1 — Consentimento efetivamente aplicado, não só armazenado

`ConsentService.isActive(userId, consentType)` é chamado no **início** de
`MoodCheckInService.checkIn()` e `getHistory()` — sem `HEALTH_DATA_PROCESSING` ativo,
o pedido falha com 403 antes de tocar em qualquer dado. "A app degrada graciosamente
quando um consentimento é retirado" (brief §9) deixa de ser uma frase e passa a ser um
teste (`shouldRejectCheckInWithoutHealthDataConsent`).

**Alternativa rejeitada:** verificar consentimento só na camada de apresentação (ex.:
o frontend simplesmente não mostra o formulário sem consentimento). Rejeitada porque
não protege nada — um pedido direto à API contornava-a por completo. A garantia tem de
viver no `Service`, não na UI.

## Decisão 2 — Audit log mínimo, mas real

`AuditLogService.record(actorUserId, subjectUserId, action)` é chamado em
`getHistory()` — cada vez que alguém vê o histórico de check-ins de um utilizador, fica
registado quem acedeu, a que dados, e quando. Nesta fase, actor e subject são sempre o
próprio utilizador (só existe self-access); a distinção entre os dois campos já está
pronta para quando um `CLINICIAN` (documentado, não implementado) ou um fluxo de
suporte precisar de aceder com consentimento explícito de outrem.

**Não fiz:** logging genérico de "toda a gente que tocou nesta tabela" via triggers de
BD ou aspectos — mais opaco, mais difícil de testar, e a granularidade de
"ação de negócio" (`VIEW_MOOD_HISTORY`) é mais útil para uma auditoria real do que uma
linha de SQL.

## Decisão 3 — Cifra em repouso, escopo mínimo defensável

Só `MoodCheckIn.note` é cifrado (AES-GCM, `AttributeConverter` JPA,
`docs/adr/0004...` explica o padrão de token — aqui o padrão técnico é semelhante: um
conversor sem dependência de Spring, injetado uma vez no arranque). É o único campo de
texto **livre** do sistema hoje — o resto dos campos sensíveis (emoção, energia, sono)
são enums/números estruturados, já minimizados por natureza, e cifrá-los impede
índices e leitura eficiente sem ganho real de privacidade proporcional.

**Alternativa rejeitada:** cifra ao nível do disco/volume gerido (ex.: encryption-at-rest
do provedor cloud). Correta a longo prazo (Fase 7, quando há infraestrutura gerida
real), mas nesta fase o projeto corre em Postgres local/Testcontainers — cifra a nível
de aplicação é o que é demonstrável e testável **agora**, sem depender de infraestrutura
que ainda não existe.

## Consequências

**Positivas:**
- As três garantias (consentimento, auditoria, cifra) têm testes que as provam, não
  só as descrevem.
- O padrão do `EncryptedStringConverter` (chave injetada por `config`, sem o domínio
  depender de Spring) é reutilizável para qualquer campo sensível futuro.

**Negativas / custos aceites:**
- A chave de cifra (`ENCRYPTION_KEY`) vive num campo estático mutável no conversor —
  funcional e testável, mas exige disciplina de teste (ver
  `EncryptedStringConverterTest`, que salva e restaura o valor anterior) para não
  poluir outros testes na mesma JVM.
- Rotação de chave de cifra (trocar `ENCRYPTION_KEY` sem invalidar dados já cifrados
  com a chave antiga) não está resolvida — fica como dívida técnica reconhecida; a
  solução real precisaria de versionar a chave por registo, adiado até haver necessidade
  concreta.

## Trade-offs (como eu defenderia isto numa entrevista)

Um Tech Lead vai perguntar "porque não cifraste tudo?" — a resposta é minimização: o
brief e os standards (§1, "Simplicidade é o desempate por defeito") pedem para não
gastar complexidade onde não há ganho proporcional. Cifrar um enum de emoção não
aumenta privacidade real (o valor já é um de seis possíveis, conhecido pelo esquema);
cifrar texto livre, sim, porque é onde a pessoa pode escrever qualquer coisa.
