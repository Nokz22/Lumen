package dev.lumen.application.exercise;

import dev.lumen.domain.exercise.Exercise;
import dev.lumen.domain.exercise.ExerciseRepository;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ExerciseService {

    private final ExerciseRepository exerciseRepository;

    public ExerciseService(ExerciseRepository exerciseRepository) {
        this.exerciseRepository = exerciseRepository;
    }

    @Transactional(readOnly = true)
    public List<ExerciseResponse> listAll() {
        return exerciseRepository.findAll().stream().map(this::toResponse).toList();
    }

    private ExerciseResponse toResponse(Exercise exercise) {
        return new ExerciseResponse(
                exercise.getId(),
                exercise.getCategory(),
                exercise.getName(),
                exercise.getDurationMinutes(),
                exercise.getIntensity(),
                exercise.getRationale(),
                exercise.getInhaleSeconds(),
                exercise.getHoldAfterInhaleSeconds(),
                exercise.getExhaleSeconds(),
                exercise.getHoldAfterExhaleSeconds(),
                // steps is lazily fetched; copy it while the session (and this transaction) is
                // still open, otherwise Jackson fails to serialize it later
                List.copyOf(exercise.getSteps()));
    }
}
