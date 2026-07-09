package dev.lumen.application.companion;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class ChatOutputVerifierTest {

    private final ChatOutputVerifier verifier = new ChatOutputVerifier();

    @Test
    void shouldRejectDiagnosticLanguage() {
        assertThat(verifier.isSafe("It sounds like you have depression.")).isFalse();
        assertThat(verifier.isSafe("This is a classic sign of generalized anxiety disorder.")).isFalse();
    }

    @Test
    void shouldRejectMedicationAdvice() {
        assertThat(verifier.isSafe("You should take a higher dose of your antidepressant.")).isFalse();
        assertThat(verifier.isSafe("I'd prescribe something for that.")).isFalse();
    }

    @Test
    void shouldAcceptSupportiveNonDiagnosticText() {
        assertThat(verifier.isSafe("It sounds like today was really hard. Thank you for sharing that with me."))
                .isTrue();
    }

    @Test
    void shouldAcceptBlankOrNullText() {
        assertThat(verifier.isSafe(null)).isTrue();
        assertThat(verifier.isSafe("")).isTrue();
    }
}
