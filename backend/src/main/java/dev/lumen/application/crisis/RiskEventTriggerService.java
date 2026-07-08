package dev.lumen.application.crisis;

import dev.lumen.application.audit.AuditLogService;
import dev.lumen.domain.audit.AuditAction;
import dev.lumen.domain.crisis.CrisisResourceRepository;
import dev.lumen.domain.crisis.RiskEvent;
import dev.lumen.domain.crisis.RiskEventRepository;
import dev.lumen.domain.crisis.TriggerSource;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Shared by every trigger source (PHQ-9 item 9, a risky chat message, and whatever
 * comes next) so the crisis-triggering sequence — create the RiskEvent, present
 * resources, audit, look up region resources — exists exactly once. Deliberately does
 * not depend on AssessmentService or CrisisService: CrisisService already depends on
 * AssessmentService (to release a score after acknowledgment), so this class staying
 * independent of both avoids a circular bean dependency.
 */
@Service
public class RiskEventTriggerService {

    private final RiskEventRepository riskEventRepository;
    private final CrisisResourceRepository crisisResourceRepository;
    private final AuditLogService auditLogService;

    public RiskEventTriggerService(
            RiskEventRepository riskEventRepository,
            CrisisResourceRepository crisisResourceRepository,
            AuditLogService auditLogService) {
        this.riskEventRepository = riskEventRepository;
        this.crisisResourceRepository = crisisResourceRepository;
        this.auditLogService = auditLogService;
    }

    @Transactional
    public CrisisTriggerOutcome trigger(UUID userId, UUID assessmentId, TriggerSource triggerSource, String region) {
        RiskEvent riskEvent = new RiskEvent(userId, assessmentId, triggerSource);
        riskEvent.presentResources();
        riskEvent = riskEventRepository.save(riskEvent);
        auditLogService.record(userId, userId, AuditAction.CRISIS_FLOW_TRIGGERED);

        List<CrisisResourceResponse> resources = crisisResourceRepository.findByRegion(region).stream()
                .map(resource -> new CrisisResourceResponse(
                        resource.getName(), resource.getType(), resource.getContact(), resource.getAvailability()))
                .toList();
        return new CrisisTriggerOutcome(riskEvent.getId(), resources);
    }
}
