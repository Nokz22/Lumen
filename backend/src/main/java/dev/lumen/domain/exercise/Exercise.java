package dev.lumen.domain.exercise;

import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OrderColumn;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.List;
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

    /**
     * Guided breathing pattern, in seconds per phase. Populated only for BREATHING
     * category exercises; null for every other category (no guided session yet).
     * A zero or null hold-after-exhale means the pattern has no fourth phase (e.g. 4-7-8
     * breathing), as opposed to box breathing's four equal phases.
     */
    @Column(name = "inhale_seconds")
    private Integer inhaleSeconds;

    @Column(name = "hold_after_inhale_seconds")
    private Integer holdAfterInhaleSeconds;

    @Column(name = "exhale_seconds")
    private Integer exhaleSeconds;

    @Column(name = "hold_after_exhale_seconds")
    private Integer holdAfterExhaleSeconds;

    /**
     * Ordered guided-session instructions, one exercise-specific step per row. Populated
     * for every category except BREATHING, which is guided by the phase timings above
     * instead. Empty for exercises without a guided session yet.
     */
    @ElementCollection
    @CollectionTable(name = "exercise_steps", joinColumns = @JoinColumn(name = "exercise_id"))
    @OrderColumn(name = "step_order")
    @Column(name = "instruction", length = 500)
    private List<String> steps = List.of();

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
        this(category, name, durationMinutes, intensity, rationale, null, null, null, null);
    }

    public Exercise(
            ExerciseCategory category,
            String name,
            int durationMinutes,
            ExerciseIntensity intensity,
            String rationale,
            Integer inhaleSeconds,
            Integer holdAfterInhaleSeconds,
            Integer exhaleSeconds,
            Integer holdAfterExhaleSeconds) {
        this.id = UUID.randomUUID();
        this.category = category;
        this.name = name;
        this.durationMinutes = durationMinutes;
        this.intensity = intensity;
        this.rationale = rationale;
        this.inhaleSeconds = inhaleSeconds;
        this.holdAfterInhaleSeconds = holdAfterInhaleSeconds;
        this.exhaleSeconds = exhaleSeconds;
        this.holdAfterExhaleSeconds = holdAfterExhaleSeconds;
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

    public Integer getInhaleSeconds() {
        return inhaleSeconds;
    }

    public Integer getHoldAfterInhaleSeconds() {
        return holdAfterInhaleSeconds;
    }

    public Integer getExhaleSeconds() {
        return exhaleSeconds;
    }

    public Integer getHoldAfterExhaleSeconds() {
        return holdAfterExhaleSeconds;
    }

    public List<String> getSteps() {
        return steps;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
