package dev.lumen.application.assessment;

import dev.lumen.domain.assessment.AssessmentStatus;
import dev.lumen.domain.assessment.AssessmentType;
import dev.lumen.domain.assessment.WellbeingBand;
import java.time.Instant;
import java.util.UUID;

/**
 * totalScore/wellbeingBand are null while status is COMPLETED-but-awaiting-crisis-
 * acknowledgment (see Assessment) — the score genuinely does not exist yet.
 */
public record AssessmentSummaryResponse(
        UUID id,
        AssessmentType assessmentType,
        AssessmentStatus status,
        Integer totalScore,
        WellbeingBand wellbeingBand,
        Instant createdAt) {
}
