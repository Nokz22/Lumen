package dev.lumen.presentation.crisis.dto;

import dev.lumen.domain.assessment.WellbeingBand;

/**
 * Both fields are null when the RiskEvent has no linked Assessment (e.g. a future
 * chat-triggered crisis) — there is simply no score to release.
 */
public record AcknowledgeResponse(Integer totalScore, WellbeingBand wellbeingBand) {
}
