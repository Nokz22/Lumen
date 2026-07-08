package dev.lumen.application.companion;

import dev.lumen.application.crisis.CrisisResourceResponse;
import java.util.List;
import java.util.UUID;

public record ConversationCrisisResult(UUID riskEventId, List<CrisisResourceResponse> resources)
        implements ConversationSubmissionResult {
}
