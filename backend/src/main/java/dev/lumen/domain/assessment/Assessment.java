package dev.lumen.domain.assessment;

import dev.lumen.domain.user.User;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import java.time.Instant;
import java.util.UUID;

/**
 * Lifecycle is SCHEDULED -> IN_PROGRESS -> COMPLETED -> SCORED. In the current
 * synchronous submission flow (AssessmentService.submit) every transition up to
 * COMPLETED happens within the same request. SCORED is reached immediately unless a
 * crisis is detected, in which case it is deferred until the linked RiskEvent is
 * acknowledged (AssessmentService.scoreAfterCrisisAcknowledgment) — an Assessment can
 * sit in COMPLETED indefinitely while awaiting that acknowledgment.
 */
@Entity
@Table(name = "assessments")
public class Assessment {

    @Id
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Enumerated(EnumType.STRING)
    @Column(name = "assessment_type", nullable = false)
    private AssessmentType assessmentType;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private AssessmentStatus status;

    @Column(name = "started_at")
    private Instant startedAt;

    @Column(name = "completed_at")
    private Instant completedAt;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Version
    private long version;

    protected Assessment() {
    }

    public Assessment(User user, AssessmentType assessmentType) {
        this.id = UUID.randomUUID();
        this.user = user;
        this.assessmentType = assessmentType;
        this.status = AssessmentStatus.SCHEDULED;
        this.createdAt = Instant.now();
    }

    public void start() {
        requireStatus(AssessmentStatus.SCHEDULED);
        this.status = AssessmentStatus.IN_PROGRESS;
        this.startedAt = Instant.now();
    }

    public void complete() {
        requireStatus(AssessmentStatus.IN_PROGRESS);
        this.status = AssessmentStatus.COMPLETED;
        this.completedAt = Instant.now();
    }

    public void score() {
        requireStatus(AssessmentStatus.COMPLETED);
        this.status = AssessmentStatus.SCORED;
    }

    private void requireStatus(AssessmentStatus required) {
        if (status != required) {
            throw new InvalidAssessmentStateException(status, required);
        }
    }

    public UUID getId() {
        return id;
    }

    public User getUser() {
        return user;
    }

    public AssessmentType getAssessmentType() {
        return assessmentType;
    }

    public AssessmentStatus getStatus() {
        return status;
    }

    public Instant getStartedAt() {
        return startedAt;
    }

    public Instant getCompletedAt() {
        return completedAt;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public long getVersion() {
        return version;
    }
}
