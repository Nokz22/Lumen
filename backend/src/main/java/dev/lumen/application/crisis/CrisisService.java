package dev.lumen.application.crisis;

import dev.lumen.application.assessment.AssessmentService;
import dev.lumen.application.assessment.ScoredAssessmentResult;
import dev.lumen.application.audit.AuditLogService;
import dev.lumen.domain.audit.AuditAction;
import dev.lumen.domain.crisis.RiskEvent;
import dev.lumen.domain.crisis.RiskEventNotFoundException;
import dev.lumen.domain.crisis.RiskEventRepository;
import java.util.Optional;
import java.util.UUID;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Ownership is re-checked here, not just at the controller's @PreAuthorize on the path
 * userId — that annotation only proves the caller is who they claim to be, not that the
 * riskEventId in the path actually belongs to them.
 */
@Service
public class CrisisService {

    private final RiskEventRepository riskEventRepository;
    private final AssessmentService assessmentService;
    private final AuditLogService auditLogService;

    public CrisisService(
            RiskEventRepository riskEventRepository,
            AssessmentService assessmentService,
            AuditLogService auditLogService) {
        this.riskEventRepository = riskEventRepository;
        this.assessmentService = assessmentService;
        this.auditLogService = auditLogService;
    }

    @Transactional
    public Optional<ScoredAssessmentResult> acknowledge(UUID userId, UUID riskEventId) {
        RiskEvent riskEvent = riskEventRepository
                .findById(riskEventId)
                .orElseThrow(() -> new RiskEventNotFoundException(riskEventId));
        if (!riskEvent.getUserId().equals(userId)) {
            throw new AccessDeniedException("Access denied");
        }

        riskEvent.acknowledge();
        riskEventRepository.save(riskEvent);
        auditLogService.record(userId, userId, AuditAction.CRISIS_FLOW_ACKNOWLEDGED);

        if (riskEvent.getAssessmentId() == null) {
            return Optional.empty();
        }
        return Optional.of(assessmentService.scoreAfterCrisisAcknowledgment(riskEvent.getAssessmentId()));
    }
}
