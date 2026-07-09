package dev.lumen.application.assessment;

import dev.lumen.application.crisis.CrisisResourceResponse;
import java.util.List;
import java.util.UUID;

public record CrisisTriggeredResult(UUID riskEventId, List<CrisisResourceResponse> resources)
        implements AssessmentSubmissionResult {
}
