package dev.lumen.infrastructure.persistence.audit;

import dev.lumen.domain.audit.AuditLogEntry;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

interface SpringDataAuditLogJpaRepository extends JpaRepository<AuditLogEntry, UUID> {
}
