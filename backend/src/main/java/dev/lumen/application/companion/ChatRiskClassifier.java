package dev.lumen.application.companion;

import java.text.Normalizer;
import java.util.List;
import java.util.Locale;
import java.util.regex.Pattern;
import org.springframework.stereotype.Component;

/**
 * Deliberately a transparent, auditable denylist — not a second model — so it keeps
 * working even when the LLM is completely unavailable (docs/constitution.md invariant) and its
 * behavior can be read and defended line by line, the same design choice as the
 * PHQ-9 item 9 bright-line rule (project-brief.md §6.1: any value above zero
 * triggers). Bias is deliberately toward over-triggering: a false positive shows a
 * calm crisis screen unnecessarily; a false negative could miss someone who needs it.
 */
@Component
class ChatRiskClassifier {

    private static final Pattern DIACRITICS = Pattern.compile("\\p{M}");

    private static final List<String> RISK_PHRASES = List.of(
            "kill myself",
            "want to die",
            "wish i was dead",
            "wish i were dead",
            "end my life",
            "ending my life",
            "better off dead",
            "hurt myself",
            "harm myself",
            "self harm",
            "self-harm",
            "no reason to live",
            "not worth living",
            "cant go on",
            "can't go on",
            "suicidal",
            "suicide",
            "matar-me",
            "quero morrer",
            "gostava de estar morto",
            "gostava de estar morta",
            "acabar com a minha vida",
            "acabar com tudo",
            "fazer-me mal",
            "magoar-me",
            "nao vale a pena viver",
            "sem vontade de viver",
            "suicidio");

    boolean isHighRisk(String message) {
        if (message == null || message.isBlank()) {
            return false;
        }
        String normalized = normalize(message);
        return RISK_PHRASES.stream().anyMatch(normalized::contains);
    }

    private String normalize(String text) {
        String decomposed = Normalizer.normalize(text.toLowerCase(Locale.ROOT), Normalizer.Form.NFD);
        return DIACRITICS.matcher(decomposed).replaceAll("");
    }
}
