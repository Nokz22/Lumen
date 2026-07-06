package dev.lumen.domain.crisis;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

/**
 * Public reference data (helplines/emergency services), not user data — seeded via a
 * regular Flyway migration so it exists in every environment, not just dev/demo.
 */
@Entity
@Table(name = "crisis_resources")
public class CrisisResource {

    @Id
    private UUID id;

    @Column(nullable = false)
    private String region;

    @Column(nullable = false)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private CrisisResourceType type;

    @Column(nullable = false)
    private String contact;

    @Column(nullable = false)
    private String availability;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    protected CrisisResource() {
    }

    public CrisisResource(String region, String name, CrisisResourceType type, String contact, String availability) {
        this.id = UUID.randomUUID();
        this.region = region;
        this.name = name;
        this.type = type;
        this.contact = contact;
        this.availability = availability;
        this.createdAt = Instant.now();
    }

    public UUID getId() {
        return id;
    }

    public String getRegion() {
        return region;
    }

    public String getName() {
        return name;
    }

    public CrisisResourceType getType() {
        return type;
    }

    public String getContact() {
        return contact;
    }

    public String getAvailability() {
        return availability;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
