package dev.lumen.application.recommendation;

import java.time.Instant;
import java.util.UUID;

public record RecommendationSummaryResponse(UUID id, UUID exerciseId, String reason, Instant createdAt) {
}
