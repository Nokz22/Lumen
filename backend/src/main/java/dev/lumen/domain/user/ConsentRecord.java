package dev.lumen.domain.user;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

/**
 * Append-only: a new consent decision is a new row, never an update of a previous one.
 */
@Entity
@Table(name = "consent_records")
public class ConsentRecord {

    @Id
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Enumerated(EnumType.STRING)
    @Column(name = "consent_type", nullable = false)
    private ConsentType consentType;

    @Column(nullable = false)
    private boolean granted;

    @Column(name = "consent_version", nullable = false)
    private int consentVersion;

    @Column(name = "granted_at")
    private Instant grantedAt;

    @Column(name = "revoked_at")
    private Instant revokedAt;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    protected ConsentRecord() {
    }

    public ConsentRecord(User user, ConsentType consentType, boolean granted, int consentVersion) {
        this.id = UUID.randomUUID();
        this.user = user;
        this.consentType = consentType;
        this.granted = granted;
        this.consentVersion = consentVersion;
        this.createdAt = Instant.now();
        this.grantedAt = granted ? this.createdAt : null;
        this.revokedAt = granted ? null : this.createdAt;
    }

    public UUID getId() {
        return id;
    }

    public User getUser() {
        return user;
    }

    public ConsentType getConsentType() {
        return consentType;
    }

    public boolean isGranted() {
        return granted;
    }

    public int getConsentVersion() {
        return consentVersion;
    }

    public Instant getGrantedAt() {
        return grantedAt;
    }

    public Instant getRevokedAt() {
        return revokedAt;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
