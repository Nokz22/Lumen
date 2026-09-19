# ADR-0013 — Spring Boot 4 e Gradle 9

**Status:** Aceite
**Data:** 2026-09-19
**Decisores:** Nuno (autor/mantenedor)

## Contexto

A constituição fixa a stack e exige um ADR para qualquer troca. Uma subida de major do
framework é uma troca: muda o modelo de serialização, a organização dos módulos de teste e
a semântica de uma política de retry. Este ADR existe por causa dessa regra.

O projeto estava em Spring Boot 3.5.16 e Gradle 8.14.5. O Dependabot abriu PRs para 4.1.0 e
9.6.1. Ficar para trás num major não é gratuito: as correções de segurança param de chegar à
linha antiga, e a distância só cresce.

## Decisão — Subir, e absorver o custo agora

Spring Boot 4.1.1 (a Dependabot propunha 4.1.0; 4.1.1 é o patch mais recente da mesma linha)
e Gradle 9.6.1. O baseline de Java do Boot 4 continua a ser 17, por isso a toolchain não mexe.

O Gradle não custou nada: o build script compila, passa Checkstyle e corre os testes sem uma
única alteração, e com `--warning-mode all` não reporta nada depreciado.

O Boot 4 custou seis coisas.

## Consequência 1 — Jackson 3 é o novo predefinido, e não há Jackson 2 injetável

O Boot 4 auto-configura `tools.jackson.databind.json.JsonMapper`. Deixou de registar um
`ObjectMapper` do `com.fasterxml`. Duas classes de produção injetavam o antigo e teriam
falhado no arranque — não na compilação.

A migração foi barata porque nenhum DTO tem anotações Jackson: são records, serializados por
nome de propriedade, logo o JSON na rede é idêntico nas duas gerações. Se houvesse um
`@JsonProperty` sequer, esta subida teria sido uma mudança de contrato silenciosa, porque as
anotações do Jackson 2 são invisíveis para o Jackson 3.

O Jackson 2 continua no classpath, arrastado pelo `anthropic-java` e pelo `springdoc`. Isso é
aceitável — mas quer dizer que as duas gerações coexistem, e quem escrever um `import` novo
tem de escolher a certa. O código deste projeto usa `tools.jackson`, sem exceção.

## Consequência 2 — A política de retry do RabbitMQ conta retries, não tentativas

O `spring-retry` saiu do classpath; o Spring Framework 7 traz o seu próprio
`org.springframework.core.retry`. O `RetryInterceptorBuilder` perdeu `maxAttempts(int)` e
ganhou `maxRetries(int)`.

Não é um rename. `maxAttempts(3)` eram três entregas no total; `maxRetries(3)` seriam quatro.
Trocar um pelo outro sem reparar teria aumentado em 33% a carga sobre uma fila já em apuros,
exatamente quando ela está a falhar. Está agora escrito como `maxRetries(2)`, com a aritmética
explicada na constante, porque este é o género de engano que se volta a fazer.

## Consequência 3 — O `EnvironmentPostProcessor` mudou de sítio

`org.springframework.boot.env.EnvironmentPostProcessor` está depreciado e marcado para
remoção; o substituto é `org.springframework.boot.EnvironmentPostProcessor`. A chave no
`META-INF/spring.factories` muda com ele — e o `spring.factories` continua a ser o mecanismo,
não foi substituído por um ficheiro `.imports`.

Se só a interface mudasse e a chave ficasse, o `ProductionConfigurationValidator` deixava de
correr **em silêncio**: nenhum erro, apenas uma aplicação que volta a aceitar arrancar em
produção com as credenciais de desenvolvimento. Foi verificado a arrancar o jar com o perfil
`prod` e sem variáveis, e a recusa continua a nomear cada uma.

## Consequência 4 — As auto-configurações foram separadas por tecnologia, e o Flyway deixou de correr

Esta é a pior das cinco, e a única que a verificação local não apanhou.

No Boot 3, o `spring-boot-autoconfigure` trazia a auto-configuração de tudo. No Boot 4 cada
tecnologia tem o seu módulo. Depender de `org.flywaydb:flyway-core` passa a dar a biblioteca
e **não** a auto-configuração: o Flyway continua no classpath, não é chamado por ninguém,
nenhuma migração corre, e o `ddl-auto: validate` do Hibernate rebenta na primeira entidade
que verifica — `missing table [assessment_responses]`, que não diz nada sobre a causa.

O Flyway era a única dependência declarada como biblioteca crua em vez de starter, e por isso
foi a única auto-configuração perdida. Passou a `org.springframework.boot:spring-boot-starter-flyway`.

**Por que não se viu localmente.** A aplicação arrancou, as migrações "não faltavam", tudo
respondia. A base de dados local já tinha o esquema de sessões anteriores: nada precisava de
migrar, por isso nada denunciava que o migrador estava morto. Só uma base de dados vazia faz
a pergunta — e é a única coisa que o CI tem e uma máquina de desenvolvimento não tem.

A lição não é sobre o Flyway. Um migrador que deixa de correr não falha no dia em que deixa de
correr: falha no dia do primeiro deploy para um esquema novo. Verificar contra uma base de
dados que já está migrada é verificar o caminho que não interessa. Ficou verificado contra
uma base vazia: 19 migrações aplicadas, 17 tabelas, e a aplicação de pé.

## Consequência 5 — Os módulos de teste foram separados

`@AutoConfigureMockMvc` saiu do `spring-boot-starter-test` para
`org.springframework.boot:spring-boot-starter-webmvc-test`, com pacote novo. Quem usa MockMvc
declara agora essa dependência.

## Consequência 6 — O Testcontainers 2.0 renomeou os artefactos

`org.testcontainers:postgresql` passou a `testcontainers-postgresql`, e o mesmo para
`junit-jupiter` e `rabbitmq`. As versões continuam geridas pelo BOM do Boot.

## Alternativas consideradas

**Ficar no 3.5.x.** É LTS e não estava a arder. Mas a distância até ao major seguinte só
cresce, e a parte cara desta subida — Jackson — só fica mais cara quanto mais código houver a
depender da geração antiga.

**Forçar o Jackson 2 como predefinido no Boot 4.** Manteria os `import` como estavam. Prende o
projeto ao caminho depreciado e adia o mesmo trabalho para um momento pior.

## Verificação

Compilar não prova nada aqui, e arrancar contra uma base de dados já povoada quase também
não: as duas falhas mais graves — o `ObjectMapper` que não existe e o
validador que não corre — só aparecem em runtime. A aplicação foi posta de pé e exercitada:
registo e login, ciclo de consentimento, um check-in a atravessar o RabbitMQ e a produzir
recomendações reais pelo motor, a nota a voltar desencriptada, o histórico paginado, o
OpenAPI a servir 22 paths com schemas, as cinco séries `lumen_*` a zero, o rate limit a
devolver 429 na décima autenticação, e o perfil `prod` a recusar arrancar sem variáveis. E, depois de a falha do
Flyway o ter ensinado, o arranque completo contra uma base de dados vazia.
