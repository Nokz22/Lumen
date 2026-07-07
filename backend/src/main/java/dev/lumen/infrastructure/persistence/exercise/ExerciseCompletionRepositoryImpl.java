package dev.lumen.infrastructure.persistence.exercise;

import dev.lumen.domain.exercise.ExerciseCompletion;
import dev.lumen.domain.exercise.ExerciseCompletionRepository;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Repository;

@Repository
class ExerciseCompletionRepositoryImpl implements ExerciseCompletionRepository {

    private final SpringDataExerciseCompletionJpaRepository jpaRepository;

    ExerciseCompletionRepositoryImpl(SpringDataExerciseCompletionJpaRepository jpaRepository) {
        this.jpaRepository = jpaRepository;
    }

    @Override
    public ExerciseCompletion save(ExerciseCompletion completion) {
        return jpaRepository.save(completion);
    }

    @Override
    public List<ExerciseCompletion> findByUserIdOrderByCompletedAtDesc(UUID userId) {
        return jpaRepository.findByUserIdOrderByCompletedAtDesc(userId);
    }
}
