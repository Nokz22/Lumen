package dev.lumen.application.crisis;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import dev.lumen.application.assessment.AssessmentService;
import dev.lumen.application.assessment.ScoredAssessmentResult;
import dev.lumen.application.audit.AuditLogService;
import dev.lumen.domain.assessment.WellbeingBand;
import dev.lumen.domain.crisis.InvalidRiskEventTransitionException;
import dev.lumen.domain.crisis.RiskEvent;
import dev.lumen.domain.crisis.RiskEventNotFoundException;
import dev.lumen.domain.crisis.RiskEventRepository;
import dev.lumen.domain.crisis.TriggerSource;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.access.AccessDeniedException;

class CrisisServiceTest {

    private final RiskEventRepository riskEventRepository = mock(RiskEventRepository.class);
    private final AssessmentService assessmentService = mock(AssessmentService.class);
    private final AuditLogService auditLogService = mock(AuditLogService.class);
    private final CrisisService service = new CrisisService(riskEventRepository, assessmentService, auditLogService);

    private final UUID userId = UUID.randomUUID();
    private final UUID assessmentId = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        when(riskEventRepository.save(any(RiskEvent.class))).thenAnswer(invocation -> invocation.getArgument(0));
    }

    @Test
    void shouldAcknowledgeAndReleaseScoreWhenLinkedToAnAssessment() {
        RiskEvent riskEvent = new RiskEvent(userId, assessmentId, TriggerSource.PHQ9_ITEM9);
        riskEvent.presentResources();
        when(riskEventRepository.findById(riskEvent.getId())).thenReturn(Optional.of(riskEvent));
        when(assessmentService.scoreAfterCrisisAcknowledgment(assessmentId))
                .thenReturn(new ScoredAssessmentResult(assessmentId, 3, WellbeingBand.MINIMAL));

        Optional<ScoredAssessmentResult> result = service.acknowledge(userId, riskEvent.getId());

        assertThat(result).isPresent();
        assertThat(result.get().totalScore()).isEqualTo(3);
        assertThat(riskEvent.getStatus().name()).isEqualTo("ACKNOWLEDGED");
    }

    @Test
    void shouldAcknowledgeWithoutScoringWhenNotLinkedToAnAssessment() {
        RiskEvent riskEvent = new RiskEvent(userId, null, TriggerSource.PHQ9_ITEM9);
        riskEvent.presentResources();
        when(riskEventRepository.findById(riskEvent.getId())).thenReturn(Optional.of(riskEvent));

        Optional<ScoredAssessmentResult> result = service.acknowledge(userId, riskEvent.getId());

        assertThat(result).isEmpty();
        verify(assessmentService, never()).scoreAfterCrisisAcknowledgment(any());
    }

    @Test
    void shouldRejectAcknowledgeWhenResourcesWereNeverPresented() {
        RiskEvent riskEvent = new RiskEvent(userId, assessmentId, TriggerSource.PHQ9_ITEM9);
        when(riskEventRepository.findById(riskEvent.getId())).thenReturn(Optional.of(riskEvent));

        assertThatThrownBy(() -> service.acknowledge(userId, riskEvent.getId()))
                .isInstanceOf(InvalidRiskEventTransitionException.class);
    }

    @Test
    void shouldRejectAcknowledgeWhenRiskEventBelongsToAnotherUser() {
        RiskEvent riskEvent = new RiskEvent(UUID.randomUUID(), assessmentId, TriggerSource.PHQ9_ITEM9);
        riskEvent.presentResources();
        when(riskEventRepository.findById(riskEvent.getId())).thenReturn(Optional.of(riskEvent));

        assertThatThrownBy(() -> service.acknowledge(userId, riskEvent.getId()))
                .isInstanceOf(AccessDeniedException.class);
    }

    @Test
    void shouldThrowWhenRiskEventDoesNotExist() {
        UUID unknownId = UUID.randomUUID();
        when(riskEventRepository.findById(unknownId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.acknowledge(userId, unknownId)).isInstanceOf(RiskEventNotFoundException.class);
    }
}
