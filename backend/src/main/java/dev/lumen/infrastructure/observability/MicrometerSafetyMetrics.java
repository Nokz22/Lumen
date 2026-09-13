package dev.lumen.infrastructure.observability;

import dev.lumen.domain.crisis.TriggerSource;
import dev.lumen.domain.observability.GuardrailLayer;
import dev.lumen.domain.observability.SafetyMetrics;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import java.util.Locale;
import org.springframework.stereotype.Component;

/**
 * Counters are registered eagerly in the constructor rather than looked up per call, so
 * each series exists at zero from startup. A counter that only appears once it is first
 * incremented is indistinguishable from a broken exporter on a dashboard, which for the
 * crisis-flow counter is the difference between "nobody was in trouble today" and "the
 * safety path has been silently failing since the deploy".
 */
@Component
public class MicrometerSafetyMetrics implements SafetyMetrics {

    private final MeterRegistry registry;
    private final Counter companionFallbacks;

    public MicrometerSafetyMetrics(MeterRegistry registry) {
        this.registry = registry;
        for (TriggerSource source : TriggerSource.values()) {
            riskEventCounter(source).count();
        }
        for (GuardrailLayer layer : GuardrailLayer.values()) {
            guardrailCounter(layer).count();
        }
        this.companionFallbacks = Counter.builder("lumen.companion.fallback.served")
                .description("Safe canned responses served because the language model failed or was unreachable")
                .register(registry);
    }

    @Override
    public void riskEventTriggered(TriggerSource source) {
        riskEventCounter(source).increment();
    }

    @Override
    public void guardrailBlocked(GuardrailLayer layer) {
        guardrailCounter(layer).increment();
    }

    @Override
    public void companionFallbackServed() {
        companionFallbacks.increment();
    }

    private Counter riskEventCounter(TriggerSource source) {
        return Counter.builder("lumen.risk_event.triggered")
                .description("Crisis flows started, by what detected the risk")
                .tag("source", tagValue(source.name()))
                .register(registry);
    }

    private Counter guardrailCounter(GuardrailLayer layer) {
        return Counter.builder("lumen.companion.guardrail.blocked")
                .description("Companion messages stopped by a guardrail, by which layer stopped them")
                .tag("layer", tagValue(layer.name()))
                .register(registry);
    }

    private String tagValue(String enumName) {
        return enumName.toLowerCase(Locale.ROOT);
    }
}
