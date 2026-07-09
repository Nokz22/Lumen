package dev.lumen.application.crisis;

import java.util.List;
import java.util.UUID;

/**
 * Generic outcome of triggering a crisis flow, independent of what triggered it
 * (a positive PHQ-9 item 9, a risky chat message, or a future trigger source) — see
 * RiskEventTriggerService.
 */
public record CrisisTriggerOutcome(UUID riskEventId, List<CrisisResourceResponse> resources) {
}
