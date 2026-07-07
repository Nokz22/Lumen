package dev.lumen.infrastructure.persistence.exercise;

import dev.lumen.domain.exercise.ExerciseCompletion;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

interface SpringDataExerciseCompletionJpaRepository extends JpaRepository<ExerciseCompletion, UUID> {

    @Query("SELECT c FROM ExerciseCompletion c WHERE c.userId = :userId ORDER BY c.completedAt DESC")
    List<ExerciseCompletion> findByUserIdOrderByCompletedAtDesc(@Param("userId") UUID userId);
}
