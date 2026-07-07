package dev.lumen.domain.crisis;

/**
 * Extensible on purpose — a future chat risk classifier (Fase 6) will add its own
 * value here without touching the RiskEvent state machine.
 */
public enum TriggerSource {
    PHQ9_ITEM9
}
