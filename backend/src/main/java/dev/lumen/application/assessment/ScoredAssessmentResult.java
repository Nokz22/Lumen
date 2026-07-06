package dev.lumen.application.assessment;

import dev.lumen.domain.assessment.WellbeingBand;
import java.util.UUID;

public record ScoredAssessmentResult(UUID assessmentId, int totalScore, WellbeingBand wellbeingBand)
        implements AssessmentSubmissionResult {
}
