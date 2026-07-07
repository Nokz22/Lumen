package dev.lumen.application.exercise;

import dev.lumen.domain.exercise.ExerciseCategory;
import dev.lumen.domain.exercise.ExerciseIntensity;
import java.util.UUID;

public record ExerciseResponse(
        UUID id,
        ExerciseCategory category,
        String name,
        int durationMinutes,
        ExerciseIntensity intensity,
        String rationale) {
}
