package dev.lumen.domain.audit;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

/**
 * Append-only, deliberately not modeled as JPA relations to User — an audit trail should
 * not depend on lazy-loading the actor/subject to be read back.
 */
@Entity
@Table(name = "audit_log_entries")
public class AuditLogEntry {

    @Id
    private UUID id;

    @Column(name = "actor_user_id", nullable = false)
    private UUID actorUserId;

    @Column(name = "subject_user_id", nullable = false)
    private UUID subjectUserId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private AuditAction action;

    @Column(name = "occurred_at", nullable = false)
    private Instant occurredAt;

    protected AuditLogEntry() {
    }

    public AuditLogEntry(UUID actorUserId, UUID subjectUserId, AuditAction action) {
        this.id = UUID.randomUUID();
        this.actorUserId = actorUserId;
        this.subjectUserId = subjectUserId;
        this.action = action;
        this.occurredAt = Instant.now();
    }

    public UUID getId() {
        return id;
    }

    public UUID getActorUserId() {
        return actorUserId;
    }

    public UUID getSubjectUserId() {
        return subjectUserId;
    }

    public AuditAction getAction() {
        return action;
    }

    public Instant getOccurredAt() {
        return occurredAt;
    }
}
