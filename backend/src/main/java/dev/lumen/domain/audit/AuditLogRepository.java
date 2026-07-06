package dev.lumen.domain.audit;

public interface AuditLogRepository {

    AuditLogEntry save(AuditLogEntry entry);
}
