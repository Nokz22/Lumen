package dev.lumen.infrastructure.persistence.audit;

import dev.lumen.domain.audit.AuditLogEntry;
import dev.lumen.domain.audit.AuditLogRepository;
import org.springframework.stereotype.Repository;

@Repository
class AuditLogRepositoryImpl implements AuditLogRepository {

    private final SpringDataAuditLogJpaRepository jpaRepository;

    AuditLogRepositoryImpl(SpringDataAuditLogJpaRepository jpaRepository) {
        this.jpaRepository = jpaRepository;
    }

    @Override
    public AuditLogEntry save(AuditLogEntry entry) {
        return jpaRepository.save(entry);
    }
}
