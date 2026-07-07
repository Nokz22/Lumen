package dev.lumen.domain.exercise;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

/**
 * Library content, not user data — seeded via a regular migration (see
 * db/migration/V12) rather than dev-seed, same reasoning as CrisisResource in Fase 3.
 */
@Entity
@Table(name = "exercises")
public class Exercise {

    @Id
    private UUID id;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ExerciseCategory category;

    @Column(nullable = false)
    private String name;

    @Column(name = "duration_minutes", nullable = false)
    private int durationMinutes;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ExerciseIntensity intensity;

    @Column(nullable = false, length = 1000)
    private String rationale;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    protected Exercise() {
    }

    public Exercise(
            ExerciseCategory category,
            String name,
            int durationMinutes,
            ExerciseIntensity intensity,
            String rationale) {
        this.id = UUID.randomUUID();
        this.category = category;
        this.name = name;
        this.durationMinutes = durationMinutes;
        this.intensity = intensity;
        this.rationale = rationale;
        this.createdAt = Instant.now();
    }

    public UUID getId() {
        return id;
    }

    public ExerciseCategory getCategory() {
        return category;
    }

    public String getName() {
        return name;
    }

    public int getDurationMinutes() {
        return durationMinutes;
    }

    public ExerciseIntensity getIntensity() {
        return intensity;
    }

    public String getRationale() {
        return rationale;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
