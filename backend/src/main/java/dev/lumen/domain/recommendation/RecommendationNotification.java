package dev.lumen.domain.recommendation;

import dev.lumen.domain.exercise.ExerciseCategory;
import java.util.UUID;

/**
 * What gets pushed over WebSocket — deliberately not the full Recommendation/Exercise
 * entities, so the real-time transport stays decoupled from persistence shape changes.
 */
public record RecommendationNotification(
        UUID recommendationId,
        UUID exerciseId,
        String exerciseName,
        ExerciseCategory category,
        int durationMinutes,
        String reason) {
}
