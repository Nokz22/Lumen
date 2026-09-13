# ADR-0011 — Direitos do titular: exportação e direito ao esquecimento

**Status:** Aceite
**Data:** 2026-08-24
**Decisores:** Nuno (autor/mantenedor)

## Contexto

O brief (§9), os standards (§7) e o `docs/threat-model.md` prometem exportação e direito
ao esquecimento desde a Fase 2. Nenhum dos dois existia: uma pesquisa por
`export|erasure|deleteUser` no `main` devolvia zero resultados.

Numa plataforma que trata dados de categoria especial do RGPD, isto não é uma feature em
falta — é a diferença entre dizer que se faz privacy by design e fazê-lo. O Artigo 15
(acesso), o Artigo 20 (portabilidade) e o Artigo 17 (apagamento) são direitos exercíveis,
não intenções de arquitetura.

## Decisão 1 — A exportação tem forma própria, não reutiliza os DTOs da API

`UserDataExport` é um record dedicado, não uma composição de `MoodCheckInResponse`,
`AssessmentSummaryResponse` e companhia.

Os DTOs da API existem para servir ecrãs e mudam quando um ecrã muda. A exportação existe
para ser **completa**. Se partilhassem forma, o dia em que o dashboard deixasse de mostrar
um campo seria o dia em que esse campo desaparecia silenciosamente da exportação de toda a
gente — e ninguém repararia, porque não há ecrã que mostre uma exportação.

Pelo mesmo motivo o `DataExportService` lê diretamente de cada repositório em vez de
chamar os serviços de cada feature: um filtro acrescentado a uma query de dashboard não
pode encolher o que uma pessoa recebe sobre si própria.

**Rejeitado:** serializar as entidades JPA diretamente. Além de violar a regra de
arquitetura ("entidades JPA nunca cruzam a fronteira da API"), arrastaria proxies lazy,
o `@Version`, e o `passwordHash`.

## Decisão 2 — A exportação não depende de consentimento ativo

Todas as outras leituras destes dados exigem `HEALTH_DATA_PROCESSING`. A exportação, não.

Quem acabou de **retirar** o consentimento é exatamente quem mais provavelmente quer uma
cópia do que foi guardado. Um direito de acesso que evapora quando o consentimento evapora
não é um direito. Retirar consentimento trava tratamento novo; não apaga o histórico, e
não pode trancar a pessoa fora do seu próprio registo.

Está coberto por teste (`shouldStillExportAfterHealthDataConsentIsWithdrawn`): sem
consentimento, o histórico de check-ins devolve 403 e a exportação devolve 200.

## Decisão 3 — O hash da password fica de fora

É uma credencial, não um dado pessoal a que a pessoa tenha direito. Entregá-lo num
ficheiro que vai parar à pasta de downloads seria abrir um problema de segurança através
de uma feature de privacidade.

## Decisão 4 — Apagamento é apagamento, não um sinalizador

`AccountErasureService` faz `DELETE` real em todas as tabelas, numa única transação. Sem
`deleted = true`.

Uma linha que continua lá com um sinalizador não foi apagada — foi escondida, e a
distinção é o objetivo inteiro do Artigo 17. Uma transação só, porque um apagamento
parcial é pior do que nenhum: deixa a pessoa convencida de que os dados desapareceram
quando parte deles ficou.

**Consequência aceite:** é irreversível e leva o histórico de crise com ele. Não há
"desfazer" nem período de graça. Um período de graça seria dados retidos depois de a
pessoa pedir que não fossem — exatamente o que o direito proíbe.

## Decisão 5 — Todos os `delete` são JPQL em massa, explícitos

Detalhe de implementação que é, na prática, uma decisão de correção.

Um `void deleteByUserId(UUID)` derivado do Spring Data carrega as linhas e enfileira a
remoção no contexto de persistência, que o Hibernate descarrega pela **sua** ordem. Um
`@Modifying @Query("DELETE ...")` executa no momento em que é chamado. Misturar os dois
estilos faz com que a ordem de chamadas no serviço não corresponda à ordem de execução no
Postgres, e o apagamento rebenta com violação de chave estrangeira.

Foi assim que rebentou: `mood_check_ins` era apagado antes das `recommendations` que lhe
apontavam, apesar de o código chamar as `recommendations` primeiro. Apanhado a correr o
teste de integração, não a ler o código — e é a razão de o teste existir.

## Decisão 6 — O audit log sobrevive ao apagamento; as chaves estrangeiras não

A migração `V19` remove as duas foreign keys de `audit_log_entries` para `users`.

Enquanto existiam, accountability (Artigo 5(2) — conseguir demonstrar que um apagamento
aconteceu) e apagamento (Artigo 17) eram mutuamente exclusivos: o registo só sobrevivia se
a pessoa sobrevivesse.

Sem elas, as entradas ficam, e `actor_user_id`/`subject_user_id` passam a ser UUIDs opacos.
Depois de a linha em `users` e todas as outras que a referenciam desaparecerem, não existe
nada no sistema que ligue aquele UUID a uma pessoa. Dados pseudonimizados tornam-se dados
anónimos precisamente quando a chave que os reidentifica deixa de existir — e dados
anónimos estão fora do âmbito do RGPD.

**Alternativa rejeitada:** apagar também as entradas de audit da pessoa. Cumpria o Artigo
17 de forma trivial e destruía a única prova de que o apagamento foi feito — o que o
Artigo 5(2) exige conservar.

## Decisão 7 — A invariante é testada contra o schema vivo

`shouldLeaveNoRowBehindInAnyTableKeyedByUser` não compara com uma lista de tabelas escrita
à mão. Pergunta ao `information_schema` que tabelas têm uma coluna `user_id` e exige que
todas fiquem vazias para aquele utilizador.

Uma lista à mão está correta no dia em que é escrita. Uma query ao schema continua correta
quando alguém acrescentar a décima primeira tabela e se esquecer do apagamento — nesse dia
o teste falha, em vez de ficarem dados de uma pessoa para trás em produção.

As filhas de `assessments` não têm `user_id` próprio, por isso têm verificação separada.

## Consequências

- Duas rotas novas, ambas self-scoped e sem exceção para ADMIN: um administrador a exportar
  ou apagar a conta de outra pessoa seria a coisa mais danosa que esta API podia oferecer.
- Apagar limpa também os cookies de sessão. Os refresh tokens morrem com a conta, mas o
  access token continuaria válido durante o TTL restante e autenticaria como um utilizador
  que já não existe. Isso obrigou a extrair `AuthCookies` do `AuthController`: um cookie
  limpo com atributos diferentes daqueles com que foi criado é ignorado pelo browser, e ter
  dois sítios a construir esses atributos era o bug à espera de acontecer.
- Novas tabelas com dados de utilizador passam a ter uma obrigação explícita: adicionar o
  `deleteByUserId` ao `AccountErasureService`. O teste do schema vivo é quem a cobra.
