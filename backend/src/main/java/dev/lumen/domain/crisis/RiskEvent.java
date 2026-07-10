package dev.lumen.domain.crisis;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import java.time.Instant;
import java.util.UUID;

/**
 * userId/assessmentId are plain UUIDs rather than JPA relations, mirroring
 * AuditLogEntry — a safety record should not depend on lazy-loading its subject to be
 * read back, and assessmentId must stay nullable for future non-assessment triggers
 * (e.g. a chat risk classifier in Fase 6).
 *
 * <p>Invariant enforced here (docs/constitution.md): ACKNOWLEDGED is only reachable from
 * RESOURCES_PRESENTED, never directly from DETECTED.
 */
@Entity
@Table(name = "risk_events")
public class RiskEvent {

    @Id
    private UUID id;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Column(name = "assessment_id")
    private UUID assessmentId;

    @Enumerated(EnumType.STRING)
    @Column(name = "trigger_source", nullable = false)
    private TriggerSource triggerSource;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private RiskEventStatus status;

    @Column(name = "detected_at", nullable = false)
    private Instant detectedAt;

    @Column(name = "resources_presented_at")
    private Instant resourcesPresentedAt;

    @Column(name = "acknowledged_at")
    private Instant acknowledgedAt;

    @Version
    private long version;

    protected RiskEvent() {
    }

    public RiskEvent(UUID userId, UUID assessmentId, TriggerSource triggerSource) {
        this.id = UUID.randomUUID();
        this.userId = userId;
        this.assessmentId = assessmentId;
        this.triggerSource = triggerSource;
        this.status = RiskEventStatus.DETECTED;
        this.detectedAt = Instant.now();
    }

    public void presentResources() {
        requireStatus(RiskEventStatus.DETECTED);
        this.status = RiskEventStatus.RESOURCES_PRESENTED;
        this.resourcesPresentedAt = Instant.now();
    }

    public void acknowledge() {
        requireStatus(RiskEventStatus.RESOURCES_PRESENTED);
        this.status = RiskEventStatus.ACKNOWLEDGED;
        this.acknowledgedAt = Instant.now();
    }

    private void requireStatus(RiskEventStatus required) {
        if (status != required) {
            throw new InvalidRiskEventTransitionException(status, required);
        }
    }

    public UUID getId() {
        return id;
    }

    public UUID getUserId() {
        return userId;
    }

    public UUID getAssessmentId() {
        return assessmentId;
    }

    public TriggerSource getTriggerSource() {
        return triggerSource;
    }

    public RiskEventStatus getStatus() {
        return status;
    }

    public Instant getDetectedAt() {
        return detectedAt;
    }

    public Instant getResourcesPresentedAt() {
        return resourcesPresentedAt;
    }

    public Instant getAcknowledgedAt() {
        return acknowledgedAt;
    }

    public long getVersion() {
        return version;
    }
}
