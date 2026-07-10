# Constituição do projeto — Lumen

Constituição do projeto. Lida em cada sessão. Detalhe completo em `docs/`.
Mantém este ficheiro curto (< 200 linhas) e verificável.

## O que é
Lumen é uma plataforma de **bem-estar corpo-mente**. Backend Java 17 + Spring Boot 3;
frontend React 18 + TypeScript. Projeto de portfólio: código ao nível de produção, e
eu tenho de saber defender cada decisão numa entrevista técnica. A minha formação em
Psicologia é o diferencial — a segurança clínica é a prioridade número um.

## FRONTEIRA ÉTICA (a regra que domina todas as outras)
Lumen é bem-estar/auto-cuidado. NUNCA é dispositivo médico, NUNCA diagnostica, NUNCA
substitui cuidado profissional. Apenas adultos (18+).
- Linguagem: "pontuação de bem-estar", nunca "de depressão". "Registou sentir-se",
  nunca "está". A app descreve o que a pessoa reporta; nunca interpreta clinicamente.
- Esta regra vive nos textos de UI, nos nomes de campos da API e nos prompts do LLM.
- Se eu pedir algo que aproxime a app de diagnóstico ou que enfraqueça a segurança,
  **para e avisa antes de implementar.** "O utilizador pediu" não é justificação.

## INVARIANTES CLÍNICOS (sempre verdadeiros, cada um com teste próprio)
- Uma resposta > 0 ao **item 9 do PHQ-9** cria SEMPRE um RiskEvent e dispara o fluxo
  de crise ANTES de a pontuação ser mostrada.
- Um RiskEvent NUNCA atinge ACKNOWLEDGED sem passar por RESOURCES_PRESENTED.
- O LLM NUNCA é invocado quando o classificador de risco de entrada dispara.
- A deteção de crise é INDEPENDENTE da disponibilidade do LLM (se o LLM cai, a
  segurança continua a funcionar).
- Conteúdo emocional, respostas de instrumentos e mensagens de chat NUNCA aparecem
  em logs. ADMIN nunca lê conteúdo de um USER.
- Retirar um consentimento desativa a funcionalidade dependente no mesmo ciclo de
  pedido — nunca "eventualmente".

## Como trabalhamos (regras de comportamento)
- **Uma fase de cada vez.** Segue o plano em `docs/project-brief.md`. Nunca saltes fases.
- **Plano antes de código.** No início de cada fase, apresenta plano (ficheiros,
  abordagem, trade-offs) e espera a minha validação antes de escrever nada.
- **Explica sempre**: porquê assim, que alternativa rejeitaste, qual o trade-off, e
  como eu defenderia isto numa entrevista. Sou júnior e quero aprender ao construir.
- **Eu faço os commits.** Sugere a mensagem; o commit é meu. Pequenos e incrementais.
- **Alterações mínimas.** Não refactorizes o que não pedi. Duas abordagens válidas ->
  mostra ambas e deixa-me escolher.
- No fim de cada fase, assume **Staff Engineer em Devil's Advocate** e lista a dívida
  técnica conscientemente aceite.

## Stack (fixa — não trocar sem me perguntar)
Java 17, Spring Boot 3 (Gradle), PostgreSQL, Flyway, Spring Security + JWT, RabbitMQ,
WebSocket/STOMP, MapStruct, Testcontainers, JUnit 5, Mockito.
React 18 + TS strict + Vite, Tailwind, TanStack Query, react-i18next (EN principal, PT-PT opção).
Docker, GitHub Actions. LLM (Fase 6) atrás da interface `LlmClient`.

## Arquitetura (não-negociável, verificada por ArchUnit no CI)
- Camadas: Presentation -> Application -> Domain; Infrastructure implementa portas do
  Domain. Nunca o inverso.
- O domínio não depende do Spring. Entidades JPA nunca cruzam a fronteira da API —
  sempre DTOs. Sem lógica de negócio em controllers.
- SOLID, baixo acoplamento, alta coesão. Métodos curtos (avaliar divisão > ~30 linhas).

## Quality attributes (ordem de desempate)
Segurança clínica e Privacidade > Clareza > Testabilidade > Evolutibilidade >
Observabilidade > Performance > Escalabilidade. Simplicidade é o desempate por defeito:
entre duas opções equivalentes, a mais simples ganha. Complexidade tem de ser ganha.

## Testes
- Código novo tem testes que provam comportamento, não cobertura vazia.
- Unit (JUnit + Mockito) + Integração (Testcontainers com Postgres/RabbitMQ reais,
  MockMvc). Guardrails do LLM testados por comportamento, nunca por strings exatas.
- Nomes descritivos: shouldTriggerCrisisFlowWhenPhq9Item9IsPositive().
- Gate JaCoCo no CI: falha abaixo de 80% em código novo.

## Git e commits
- GitHub Flow. `main` protegido: merge só com CI verde.
- Branch por funcionalidade: feature/…, fix/…, refactor/…, chore/…, ci:.
- Conventional Commits em inglês: feat:, fix:, test:, docs:, refactor:, chore:, ci:.
  Scope quando ajuda: feat(crisis): ….
- 1 commit = 1 unidade lógica. 8–15 commits por fase, no mínimo. Nada de push gigante.

## Segurança, privacidade e configuração
- Dados de saúde = categoria especial RGPD. Consentimento granular e revogável;
  minimização; pseudonimização; audit log; cifra em trânsito e em repouso;
  exportação e direito ao esquecimento.
- Zero segredos no repo; `.env.example`. Perfis: dev, test, demo, prod.
- Erros da API em RFC 7807. Input validation em todos os endpoints.
- Tempo sempre em UTC na persistência; hora local só na UI.
- Nunca System.out.println() — SLF4J. Nunca logar dados sensíveis.
- Dados 100% sintéticos em dev/demo.

## Documentos de referência
- `docs/project-brief.md` — o quê e porquê, fases, entidades, fluxo de crise.
- `docs/standards.md` — standards de engenharia completos (o "como"), com planos de
  commits e Definition of Done por fase.
- `docs/adr/` — Architecture Decision Records (escritos quando a decisão é tomada).

## Definition of Done
Uma fase só fecha quando a checklist de DoD dessa fase (em `docs/standards.md`) fecha.
Fechada, não reabre — melhorias vão para o roadmap.
