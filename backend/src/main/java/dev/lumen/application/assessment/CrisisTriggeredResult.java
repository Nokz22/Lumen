package dev.lumen.application.assessment;

import java.util.List;
import java.util.UUID;

public record CrisisTriggeredResult(UUID riskEventId, List<CrisisResourceResponse> resources)
        implements AssessmentSubmissionResult {
}
