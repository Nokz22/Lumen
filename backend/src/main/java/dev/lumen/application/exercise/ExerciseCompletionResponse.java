package dev.lumen.application.exercise;

import java.time.Instant;
import java.util.UUID;

public record ExerciseCompletionResponse(UUID id, UUID exerciseId, UUID recommendationId, Instant completedAt) {
}
