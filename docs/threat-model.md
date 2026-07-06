# Threat model — Lumen

Calibrado, não STRIDE exaustivo (`docs/standards.md` §7, §15) — uma tabela
ativo → ameaça → mitigação, revista à medida que features novas introduzem ativos novos.

| Ativo | Ameaça | Mitigação |
|---|---|---|
| Access token (JWT) | Roubo via XSS | Cookie `httpOnly` — nunca legível por JavaScript (ADR-0004) |
| Refresh token | Roubo, reutilização (replay) | Opaco, guardado com hash SHA-256; rotação a cada uso; reutilização de um token já rodado revoga a família inteira (ADR-0004) |
| Sessão (cookies) | CSRF | `CookieCsrfTokenRepository` (double-submit); `SameSite=Strict`; login/registo/refresh isentos (sem sessão prévia a proteger) |
| Password | Ataque de força bruta offline após fuga da BD | BCrypt (custo adaptativo); nunca guardada nem logada em claro |
| Conteúdo de chat/notas (texto livre) | Fuga / acesso indevido à BD | `MoodCheckIn.note` cifrado (AES-GCM) em repouso; nunca em logs |
| Consentimento | Manipulação, acesso sem autorização | Append-only, versionado, verificado em cada pedido de check-in (`ConsentService.isActive`) |
| Dados de humor de um USER | ADMIN ou outro USER acede a dados que não são seus | `@PreAuthorize` self-scoped em `MoodCheckInController`/`ConsentController` — identidade, não papel; testado (`shouldPreventAdminFromReadingUserMoodCheckIns`) |
| Rotas admin (`/api/v1/admin/**`) | Escalada de privilégio | `hasRole("ADMIN")` ao nível do filter chain, não por endpoint individual |
| Registo de conta | Menores de idade a registarem-se | Verificação de idade 18+ no `AuthService.register`, servidor é a fonte de verdade (cliente também valida, por UX) |
| Logs da aplicação | PII/conteúdo sensível acidental | Regra dura (Nível 0, `CLAUDE.md`): nunca logar conteúdo emocional, respostas de instrumentos, tokens; SLF4J sempre |

**Não fazemos** (decisão consciente, `docs/standards.md` §15): STRIDE exaustivo, KMS
gerido, pentest — práticas de equipa com produto em produção real, fora de escopo de
um portfólio nesta fase.
