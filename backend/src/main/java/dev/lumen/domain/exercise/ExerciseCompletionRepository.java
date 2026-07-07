package dev.lumen.domain.exercise;

import java.util.List;
import java.util.UUID;

public interface ExerciseCompletionRepository {

    ExerciseCompletion save(ExerciseCompletion completion);

    List<ExerciseCompletion> findByUserIdOrderByCompletedAtDesc(UUID userId);
}
