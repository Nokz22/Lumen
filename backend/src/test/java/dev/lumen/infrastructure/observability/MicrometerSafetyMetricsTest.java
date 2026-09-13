package dev.lumen.infrastructure.observability;

import static org.assertj.core.api.Assertions.assertThat;

import dev.lumen.domain.crisis.TriggerSource;
import dev.lumen.domain.observability.GuardrailLayer;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class MicrometerSafetyMetricsTest {

    private MeterRegistry registry;
    private MicrometerSafetyMetrics metrics;

    @BeforeEach
    void setUp() {
        registry = new SimpleMeterRegistry();
        metrics = new MicrometerSafetyMetrics(registry);
    }

    private double count(String name, String tagKey, String tagValue) {
        return registry.get(name).tag(tagKey, tagValue).counter().count();
    }

    /**
     * A series that only appears on first increment is indistinguishable from a broken
     * exporter, and for the crisis counter that is the difference between "a quiet day"
     * and "the safety path has been failing since the deploy".
     */
    @Test
    void shouldRegisterEverySeriesAtZeroBeforeAnythingHappens() {
        assertThat(count("lumen.risk_event.triggered", "source", "phq9_item9")).isZero();
        assertThat(count("lumen.risk_event.triggered", "source", "chat_message")).isZero();
        assertThat(count("lumen.companion.guardrail.blocked", "layer", "input_classifier"))
                .isZero();
        assertThat(count("lumen.companion.guardrail.blocked", "layer", "output_verifier"))
                .isZero();
    }

    @Test
    void shouldCountCrisisFlowsByWhatDetectedTheRisk() {
        metrics.riskEventTriggered(TriggerSource.PHQ9_ITEM9);
        metrics.riskEventTriggered(TriggerSource.CHAT_MESSAGE);
        metrics.riskEventTriggered(TriggerSource.CHAT_MESSAGE);

        assertThat(count("lumen.risk_event.triggered", "source", "phq9_item9")).isEqualTo(1);
        assertThat(count("lumen.risk_event.triggered", "source", "chat_message")).isEqualTo(2);
    }

    @Test
    void shouldCountBlockedMessagesByTheLayerThatStoppedThem() {
        metrics.guardrailBlocked(GuardrailLayer.INPUT_CLASSIFIER);
        metrics.guardrailBlocked(GuardrailLayer.OUTPUT_VERIFIER);

        assertThat(count("lumen.companion.guardrail.blocked", "layer", "input_classifier"))
                .isEqualTo(1);
        assertThat(count("lumen.companion.guardrail.blocked", "layer", "output_verifier"))
                .isEqualTo(1);
    }

    /**
     * A metric label is stored indefinitely and readable by anyone with the endpoint. What
     * is measured is that a safety path fired, never whose.
     */
    @Test
    void shouldNeverLabelAMetricWithSomethingIdentifying() {
        metrics.riskEventTriggered(TriggerSource.CHAT_MESSAGE);
        metrics.guardrailBlocked(GuardrailLayer.INPUT_CLASSIFIER);
        metrics.companionFallbackServed();

        assertThat(registry.getMeters())
                .flatExtracting(meter -> meter.getId().getTags())
                .extracting(tag -> tag.getKey().toLowerCase())
                .doesNotContain("user", "userid", "user_id", "email", "content", "message");
    }
}
