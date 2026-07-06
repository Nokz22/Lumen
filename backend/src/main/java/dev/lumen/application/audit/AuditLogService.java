package dev.lumen.application.audit;

import dev.lumen.domain.audit.AuditAction;
import dev.lumen.domain.audit.AuditLogEntry;
import dev.lumen.domain.audit.AuditLogRepository;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuditLogService {

    private final AuditLogRepository auditLogRepository;

    public AuditLogService(AuditLogRepository auditLogRepository) {
        this.auditLogRepository = auditLogRepository;
    }

    @Transactional
    public void record(UUID actorUserId, UUID subjectUserId, AuditAction action) {
        auditLogRepository.save(new AuditLogEntry(actorUserId, subjectUserId, action));
    }
}
