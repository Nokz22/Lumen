package dev.lumen.infrastructure.persistence.exercise;

import dev.lumen.domain.exercise.Exercise;
import dev.lumen.domain.exercise.ExerciseCategory;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

interface SpringDataExerciseJpaRepository extends JpaRepository<Exercise, UUID> {

    List<Exercise> findByCategory(ExerciseCategory category);
}
