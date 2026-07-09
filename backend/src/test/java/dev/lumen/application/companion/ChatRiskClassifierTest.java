package dev.lumen.application.companion;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class ChatRiskClassifierTest {

    private final ChatRiskClassifier classifier = new ChatRiskClassifier();

    @Test
    void shouldFlagExplicitEnglishSelfHarmLanguage() {
        assertThat(classifier.isHighRisk("I just want to kill myself")).isTrue();
        assertThat(classifier.isHighRisk("Sometimes I think I'd be better off dead")).isTrue();
        assertThat(classifier.isHighRisk("I've been thinking about suicide a lot")).isTrue();
    }

    @Test
    void shouldFlagExplicitPortugueseSelfHarmLanguage() {
        assertThat(classifier.isHighRisk("Só quero acabar com a minha vida")).isTrue();
        assertThat(classifier.isHighRisk("Ás vezes penso em matar-me")).isTrue();
    }

    @Test
    void shouldFlagRegardlessOfDiacriticsOrCase() {
        assertThat(classifier.isHighRisk("NAO VALE A PENA VIVER")).isTrue();
        assertThat(classifier.isHighRisk("não vale a pena viver")).isTrue();
    }

    @Test
    void shouldNotFlagNeutralOrPositiveMessages() {
        assertThat(classifier.isHighRisk("I had a pretty good day today, just a bit tired")).isFalse();
        assertThat(classifier.isHighRisk("Hoje correu bem, dormi bem")).isFalse();
    }

    @Test
    void shouldNotFlagBlankOrNullMessages() {
        assertThat(classifier.isHighRisk("")).isFalse();
        assertThat(classifier.isHighRisk("   ")).isFalse();
        assertThat(classifier.isHighRisk(null)).isFalse();
    }
}
