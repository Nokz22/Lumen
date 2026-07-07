package dev.lumen.domain.assessment;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

/**
 * totalScore is the raw clinical score (available to whoever wants the detail);
 * wellbeingBand is the only value ever surfaced in UI copy or API response text
 * describing the result (ADR-0001).
 */
@Entity
@Table(name = "assessment_scores")
public class AssessmentScore {

    @Id
    private UUID id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "assessment_id", nullable = false, unique = true)
    private Assessment assessment;

    @Column(name = "total_score", nullable = false)
    private int totalScore;

    @Enumerated(EnumType.STRING)
    @Column(name = "wellbeing_band", nullable = false)
    private WellbeingBand wellbeingBand;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    protected AssessmentScore() {
    }

    public AssessmentScore(Assessment assessment, int totalScore, WellbeingBand wellbeingBand) {
        this.id = UUID.randomUUID();
        this.assessment = assessment;
        this.totalScore = totalScore;
        this.wellbeingBand = wellbeingBand;
        this.createdAt = Instant.now();
    }

    public UUID getId() {
        return id;
    }

    public Assessment getAssessment() {
        return assessment;
    }

    public int getTotalScore() {
        return totalScore;
    }

    public WellbeingBand getWellbeingBand() {
        return wellbeingBand;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
