package dev.lumen.domain.wearable;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/**
 * Generic (type, value, recordedAt) row rather than a wide table with one column per
 * metric — the standard time-series shape, and what makes ingestion provider-agnostic:
 * a Fitbit adapter and a Garmin adapter would both produce this exact shape, only
 * `source` differs. userId is a plain UUID, not a JPA relation — same pattern as
 * RiskEvent/AuditLogEntry/ExerciseCompletion, appropriate for a high-volume
 * time-series table.
 */
@Entity
@Table(name = "wearable_readings")
public class WearableReading {

    @Id
    private UUID id;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private WearableReadingType type;

    @Column(nullable = false)
    private BigDecimal value;

    @Column(name = "recorded_at", nullable = false)
    private Instant recordedAt;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private WearableSourceType source;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    protected WearableReading() {
    }

    public WearableReading(
            UUID userId, WearableReadingType type, BigDecimal value, Instant recordedAt, WearableSourceType source) {
        this.id = UUID.randomUUID();
        this.userId = userId;
        this.type = type;
        this.value = value;
        this.recordedAt = recordedAt;
        this.source = source;
        this.createdAt = Instant.now();
    }

    public UUID getId() {
        return id;
    }

    public UUID getUserId() {
        return userId;
    }

    public WearableReadingType getType() {
        return type;
    }

    public BigDecimal getValue() {
        return value;
    }

    public Instant getRecordedAt() {
        return recordedAt;
    }

    public WearableSourceType getSource() {
        return source;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
