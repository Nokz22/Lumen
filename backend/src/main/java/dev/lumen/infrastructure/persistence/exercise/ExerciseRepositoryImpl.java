package dev.lumen.infrastructure.persistence.exercise;

import dev.lumen.domain.exercise.Exercise;
import dev.lumen.domain.exercise.ExerciseCategory;
import dev.lumen.domain.exercise.ExerciseRepository;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Repository;

@Repository
class ExerciseRepositoryImpl implements ExerciseRepository {

    private final SpringDataExerciseJpaRepository jpaRepository;

    ExerciseRepositoryImpl(SpringDataExerciseJpaRepository jpaRepository) {
        this.jpaRepository = jpaRepository;
    }

    @Override
    public List<Exercise> findAll() {
        return jpaRepository.findAll();
    }

    @Override
    public Optional<Exercise> findById(UUID id) {
        return jpaRepository.findById(id);
    }

    @Override
    public List<Exercise> findByCategory(ExerciseCategory category) {
        return jpaRepository.findByCategory(category);
    }
}
