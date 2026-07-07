package dev.lumen.application.exercise;

import dev.lumen.domain.exercise.ExerciseCompletion;
import dev.lumen.domain.exercise.ExerciseCompletionRepository;
import dev.lumen.domain.exercise.ExerciseNotFoundException;
import dev.lumen.domain.exercise.ExerciseRepository;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * recommendationId is nullable — a user can mark an exercise as done straight from
 * the library, not only from a pushed recommendation (see ExerciseCompletion).
 */
@Service
public class ExerciseCompletionService {

    private final ExerciseCompletionRepository completionRepository;
    private final ExerciseRepository exerciseRepository;

    public ExerciseCompletionService(
            ExerciseCompletionRepository completionRepository, ExerciseRepository exerciseRepository) {
        this.completionRepository = completionRepository;
        this.exerciseRepository = exerciseRepository;
    }

    @Transactional
    public ExerciseCompletionResponse complete(UUID userId, UUID exerciseId, UUID recommendationId) {
        if (exerciseRepository.findById(exerciseId).isEmpty()) {
            throw new ExerciseNotFoundException(exerciseId);
        }
        ExerciseCompletion completion =
                completionRepository.save(new ExerciseCompletion(userId, exerciseId, recommendationId));
        return toResponse(completion);
    }

    @Transactional(readOnly = true)
    public List<ExerciseCompletionResponse> getHistory(UUID userId) {
        return completionRepository.findByUserIdOrderByCompletedAtDesc(userId).stream()
                .map(this::toResponse)
                .toList();
    }

    private ExerciseCompletionResponse toResponse(ExerciseCompletion completion) {
        return new ExerciseCompletionResponse(
                completion.getId(),
                completion.getExerciseId(),
                completion.getRecommendationId(),
                completion.getCompletedAt());
    }
}
