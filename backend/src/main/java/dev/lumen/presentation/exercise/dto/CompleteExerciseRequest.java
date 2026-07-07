package dev.lumen.presentation.exercise.dto;

import jakarta.validation.constraints.NotNull;
import java.util.UUID;

public record CompleteExerciseRequest(@NotNull UUID exerciseId, UUID recommendationId) {
}
