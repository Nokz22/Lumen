# Glossário — Lumen

Termos do domínio com significado único, partilhado entre código, API, UI e (mais
adiante) prompts do LLM. Fixa a linguagem não-diagnóstica do [ADR-0001](adr/0001-ethical-boundary-non-diagnostic-wellbeing-tool.md).
Início na Fase 1 — cresce fase a fase.

| Termo | Significado | Nunca dizer |
|---|---|---|
| **Check-in diário** | Registo diário e voluntário de humor, energia e sono. Uma por utilizador por dia (a segunda submissão do mesmo dia atualiza, não duplica). | "Diagnóstico diário" |
| **Pontuação de bem-estar** | Resultado de um instrumento (PHQ-9/GAD-7, a partir da Fase 3), expresso em linguagem de auto-monitorização. | "Pontuação de depressão/ansiedade" |
| **Registaste sentir-te X** | Forma como o produto descreve um estado emocional reportado pela pessoa — descreve o que foi reportado, nunca interpreta. | "Estás X" / "Tens X" |
| **User** | Conta de uma pessoa adulta (18+) que usa o Lumen para se auto-observar. Ainda sem autenticação (chega na Fase 2). | — |
| **ConsentRecord** | Registo *append-only* de uma decisão de consentimento (dado concedido ou revogado), versionado e datado — nunca atualizado em memória, sempre uma nova linha. | — |
| **MoodCheckIn** | Entidade que representa o check-in diário: emoção (roda de 6 emoções), energia (1–5), sono (horas + qualidade 1–5), nota livre opcional. | — |

## Roda de emoções (MoodEmotion)

`HAPPY`, `SAD`, `ANGRY`, `ANXIOUS`, `FRUSTRATED`, `NEUTRAL` — enum fechado por agora;
qualquer adição futura é uma decisão de produto, não um detalhe técnico.
