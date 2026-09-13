package dev.lumen.domain.observability;

/**
 * The two guardrail layers that can actually stop something (ADR-0010). The middle layer —
 * the clinical system prompt — is an instruction to the model, not a check with an outcome,
 * so there is nothing for it to block and nothing to count.
 */
public enum GuardrailLayer {
    /** Ran before the model, so the model was never called at all. */
    INPUT_CLASSIFIER,
    /** Ran on the model's answer, which was replaced before anyone saw it. */
    OUTPUT_VERIFIER
}
