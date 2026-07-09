package dev.lumen.application.companion;

import java.util.List;
import java.util.Locale;
import org.springframework.stereotype.Component;

/**
 * Catches the model failing to follow the system prompt (§8.2 layer 3: "verificação
 * na saída"), not risk in what the person said — that's ChatRiskClassifier's job,
 * evaluated before the model is ever called. A failure here means the reply text is
 * replaced wholesale with SAFE_FALLBACK_MESSAGE; it does not open a RiskEvent.
 */
@Component
class ChatOutputVerifier {

    static final String SAFE_FALLBACK_MESSAGE =
            "I want to make sure I respond thoughtfully here. I'm not able to give medical advice or "
                    + "diagnoses — if you'd like, we can keep talking about how you're feeling, or I can "
                    + "point you to some support resources.";

    private static final List<String> FORBIDDEN_PHRASES = List.of(
            "you have depression",
            "you have anxiety",
            "you are depressed",
            "you're depressed",
            "diagnos",
            "clinical depression",
            "generalized anxiety disorder",
            "major depressive disorder",
            "you should take",
            "increase your dose",
            "stop taking your medication",
            "prescribe");

    boolean isSafe(String responseText) {
        if (responseText == null || responseText.isBlank()) {
            return true;
        }
        String normalized = responseText.toLowerCase(Locale.ROOT);
        return FORBIDDEN_PHRASES.stream().noneMatch(normalized::contains);
    }
}
