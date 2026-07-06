package dev.lumen.domain.crisis;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.UUID;
import org.junit.jupiter.api.Test;

class RiskEventTest {

    private final UUID userId = UUID.randomUUID();
    private final UUID assessmentId = UUID.randomUUID();

    @Test
    void shouldStartInDetectedStatus() {
        RiskEvent riskEvent = new RiskEvent(userId, assessmentId, TriggerSource.PHQ9_ITEM9);

        assertThat(riskEvent.getStatus()).isEqualTo(RiskEventStatus.DETECTED);
        assertThat(riskEvent.getDetectedAt()).isNotNull();
        assertThat(riskEvent.getResourcesPresentedAt()).isNull();
        assertThat(riskEvent.getAcknowledgedAt()).isNull();
    }

    @Test
    void shouldTransitionFromDetectedToResourcesPresented() {
        RiskEvent riskEvent = new RiskEvent(userId, assessmentId, TriggerSource.PHQ9_ITEM9);

        riskEvent.presentResources();

        assertThat(riskEvent.getStatus()).isEqualTo(RiskEventStatus.RESOURCES_PRESENTED);
        assertThat(riskEvent.getResourcesPresentedAt()).isNotNull();
    }

    @Test
    void shouldTransitionFromResourcesPresentedToAcknowledged() {
        RiskEvent riskEvent = new RiskEvent(userId, assessmentId, TriggerSource.PHQ9_ITEM9);
        riskEvent.presentResources();

        riskEvent.acknowledge();

        assertThat(riskEvent.getStatus()).isEqualTo(RiskEventStatus.ACKNOWLEDGED);
        assertThat(riskEvent.getAcknowledgedAt()).isNotNull();
    }

    @Test
    void shouldRejectAcknowledgeDirectlyFromDetected() {
        RiskEvent riskEvent = new RiskEvent(userId, assessmentId, TriggerSource.PHQ9_ITEM9);

        assertThatThrownBy(riskEvent::acknowledge).isInstanceOf(InvalidRiskEventTransitionException.class);
        assertThat(riskEvent.getStatus()).isEqualTo(RiskEventStatus.DETECTED);
    }

    @Test
    void shouldRejectPresentingResourcesTwice() {
        RiskEvent riskEvent = new RiskEvent(userId, assessmentId, TriggerSource.PHQ9_ITEM9);
        riskEvent.presentResources();

        assertThatThrownBy(riskEvent::presentResources).isInstanceOf(InvalidRiskEventTransitionException.class);
    }

    @Test
    void shouldRejectAcknowledgingTwice() {
        RiskEvent riskEvent = new RiskEvent(userId, assessmentId, TriggerSource.PHQ9_ITEM9);
        riskEvent.presentResources();
        riskEvent.acknowledge();

        assertThatThrownBy(riskEvent::acknowledge).isInstanceOf(InvalidRiskEventTransitionException.class);
    }

    @Test
    void shouldAllowNullAssessmentIdForNonAssessmentTriggers() {
        RiskEvent riskEvent = new RiskEvent(userId, null, TriggerSource.PHQ9_ITEM9);

        assertThat(riskEvent.getAssessmentId()).isNull();
    }
}
