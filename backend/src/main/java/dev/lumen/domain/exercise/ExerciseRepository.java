package dev.lumen.domain.exercise;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ExerciseRepository {

    List<Exercise> findAll();

    Optional<Exercise> findById(UUID id);

    List<Exercise> findByCategory(ExerciseCategory category);
}
