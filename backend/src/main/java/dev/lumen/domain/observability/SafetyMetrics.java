package dev.lumen.domain.observability;

import dev.lumen.domain.crisis.TriggerSource;

/**
 * A port, like LlmClient and RecommendationNotifier, so the application layer can record
 * what happened without depending on a metrics library — and so the counters can be
 * asserted in a test with a fake instead of a live registry.
 *
 * <p>Every method here takes an enum, never free text, and nothing takes a user id. A
 * metric label is stored forever, exported to whoever can read the endpoint, and would put
 * emotional context into a system that has none of this project's privacy guarantees. What
 * is measured is that a safety path fired, never who it fired for.
 */
public interface SafetyMetrics {

    /** A crisis flow started — the single most important thing this system does. */
    void riskEventTriggered(TriggerSource source);

    /** A guardrail stopped something, at the layer named (ADR-0010). */
    void guardrailBlocked(GuardrailLayer layer);

    /** The LLM was unreachable or failed, and a safe canned response was served instead. */
    void companionFallbackServed();
}
