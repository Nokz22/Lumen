package dev.lumen.domain.recommendation;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

/**
 * A record of "this was suggested" — no mutable state, no @Version. Whether the
 * suggestion was acted on is tracked separately by ExerciseCompletion, linked via its
 * own optional recommendationId rather than a status field here, so a Recommendation
 * never needs to be written to twice.
 */
@Entity
@Table(name = "recommendations")
public class Recommendation {

    @Id
    private UUID id;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Column(name = "mood_check_in_id", nullable = false)
    private UUID moodCheckInId;

    @Column(name = "exercise_id", nullable = false)
    private UUID exerciseId;

    @Column(nullable = false, length = 500)
    private String reason;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    protected Recommendation() {
    }

    public Recommendation(UUID userId, UUID moodCheckInId, UUID exerciseId, String reason) {
        this.id = UUID.randomUUID();
        this.userId = userId;
        this.moodCheckInId = moodCheckInId;
        this.exerciseId = exerciseId;
        this.reason = reason;
        this.createdAt = Instant.now();
    }

    public UUID getId() {
        return id;
    }

    public UUID getUserId() {
        return userId;
    }

    public UUID getMoodCheckInId() {
        return moodCheckInId;
    }

    public UUID getExerciseId() {
        return exerciseId;
    }

    public String getReason() {
        return reason;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
