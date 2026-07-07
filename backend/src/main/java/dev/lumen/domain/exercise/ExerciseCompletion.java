package dev.lumen.domain.exercise;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

/**
 * userId/exerciseId/recommendationId are plain UUIDs, not JPA relations — same
 * loosely-coupled pattern as AuditLogEntry and RiskEvent. recommendationId is nullable
 * because a user can complete an exercise directly from the library, without it coming
 * from a Recommendation.
 */
@Entity
@Table(name = "exercise_completions")
public class ExerciseCompletion {

    @Id
    private UUID id;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Column(name = "exercise_id", nullable = false)
    private UUID exerciseId;

    @Column(name = "recommendation_id")
    private UUID recommendationId;

    @Column(name = "completed_at", nullable = false)
    private Instant completedAt;

    protected ExerciseCompletion() {
    }

    public ExerciseCompletion(UUID userId, UUID exerciseId, UUID recommendationId) {
        this.id = UUID.randomUUID();
        this.userId = userId;
        this.exerciseId = exerciseId;
        this.recommendationId = recommendationId;
        this.completedAt = Instant.now();
    }

    public UUID getId() {
        return id;
    }

    public UUID getUserId() {
        return userId;
    }

    public UUID getExerciseId() {
        return exerciseId;
    }

    public UUID getRecommendationId() {
        return recommendationId;
    }

    public Instant getCompletedAt() {
        return completedAt;
    }
}
