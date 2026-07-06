# ADR-0004 — JWT em cookie httpOnly, com refresh token opaco e rotação

**Status:** Aceite
**Data:** 2026-07-06
**Decisores:** Nuno (autor/mantenedor)

## Contexto

A Fase 2 precisa de autenticação real para o Lumen: um SPA React a falar com uma API
Spring Boot. O `MoodCheckInController` da Fase 1 já identificava o utilizador por
`{userId}` explícito no path — essa forma fica, mas passa a ser validada contra quem
está autenticado, e não confiada às cegas.

## Problema

Onde guardar o token de sessão no browser, e como lidar com a sua expiração, sem abrir
uma porta a roubo de sessão (XSS) nem a pedidos forjados (CSRF)?

## Opções consideradas (armazenamento do token)

1. **`localStorage` / `sessionStorage`.** Simples, sem necessidade de CSRF. Mas
   qualquer XSS bem-sucedido lê o token diretamente via JavaScript — para uma app que
   lida com dados de saúde, este risco é inaceitável mesmo sendo o padrão mais comum em
   tutoriais de SPA.
2. **Cookie `httpOnly` + `Secure` + `SameSite=Strict`.** JavaScript nunca lê o token —
   imune a exfiltração via XSS. Custo: precisa de proteção CSRF explícita, porque o
   browser envia cookies automaticamente em qualquer pedido para o domínio.

**Decisão:** opção 2. Mitigo CSRF com o padrão *double-submit cookie* do Spring
Security (`CookieCsrfTokenRepository`, cookie `XSRF-TOKEN` legível por JS, devolvido
como header `X-XSRF-TOKEN`) — o atacante consegue fazer o browser enviar o cookie de
sessão, mas não consegue ler o cookie CSRF de outra origem para o reproduzir no header.

## Opções consideradas (forma do token)

1. **Access + refresh, ambos JWT.** Um JWT de refresh não se revoga sem infraestrutura
   extra (blacklist com TTL) — contradiz "revogar tem de funcionar já".
2. **Access token JWT (stateless, curto — 15 min) + refresh token opaco com estado na
   BD (`RefreshToken`, 7 dias).** O access token é barato de validar em cada pedido
   (só verificação de assinatura, sem ir à BD). O refresh token, por ser opaco e
   guardado com hash, revoga-se trivialmente — é exatamente o que permite rotação e
   deteção de reutilização.

**Decisão:** opção 2.

## Rotação com deteção de reutilização

Cada `POST /auth/refresh` bem-sucedido: revoga o refresh token usado, emite um novo
com o mesmo `familyId`. Se um token **já revogado** for apresentado outra vez, trato
como sinal de roubo — revogo a família inteira (todos os tokens dessa cadeia de login),
forçando novo login. Isto significa que um atacante que copie o refresh token de
alguém só consegue usá-lo uma vez antes de o dono legítimo (ao rodar o seu) invalidar o
roubo — ou vice-versa, dependendo de quem chega primeiro; em ambos os casos, a conta
não fica silenciosamente comprometida.

## Consequências

**Positivas:**
- Nenhum JavaScript no browser consegue ler o token de acesso — a superfície de
  ataque de XSS para roubo de sessão fecha-se.
- `RefreshToken` guardado com **hash** (nunca o valor em claro) — um dump da BD não dá
  sessões válidas de graça.
- CORS + CSRF + cookies exigiram mexer na configuração de CORS da Fase 1
  (`WebConfig`/`WebMvcConfigurer` foi removido — Spring Security precisa de ver e
  autorizar os pedidos de preflight ele próprio, não só o MVC).

**Negativas / custos aceites:**
- `Secure` em cookies só funciona nativamente em produção com HTTPS real — em
  `localhost` os browsers modernos tratam-no como contexto seguro por exceção; isto só
  se confirma a sério na Fase 7 (deploy).
- Mais complexidade do que "só um JWT" — aceite conscientemente, o trade-off de
  segurança vale a pena para dados de saúde.

## Trade-offs (como eu defenderia isto numa entrevista)

`localStorage` é o que a maioria dos tutoriais ensina, e um Tech Lead vai perguntar
exatamente "porque não usaste `localStorage`, é mais simples?" — a resposta é: para
uma app que processa dados de saúde (RGPD categoria especial), o custo assimétrico de
um roubo de sessão via XSS não se compara ao custo de implementar CSRF corretamente.
A rotação com deteção de reutilização é o mesmo padrão usado por OAuth2 refresh tokens
em produção (ex.: Auth0, Okta) — não é invenção minha, é como a indústria já resolveu
este problema.
