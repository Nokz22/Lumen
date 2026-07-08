package dev.lumen.application.assessment;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import dev.lumen.application.consent.ConsentService;
import dev.lumen.application.crisis.CrisisTriggerOutcome;
import dev.lumen.application.crisis.RiskEventTriggerService;
import dev.lumen.domain.assessment.Assessment;
import dev.lumen.domain.assessment.AssessmentAnswer;
import dev.lumen.domain.assessment.AssessmentAnswerRepository;
import dev.lumen.domain.assessment.AssessmentRepository;
import dev.lumen.domain.assessment.AssessmentScoreRepository;
import dev.lumen.domain.assessment.AssessmentTooSoonException;
import dev.lumen.domain.assessment.AssessmentType;
import dev.lumen.domain.assessment.InvalidAssessmentSubmissionException;
import dev.lumen.domain.assessment.WellbeingBand;
import dev.lumen.domain.user.ConsentRequiredException;
import dev.lumen.domain.user.ConsentType;
import dev.lumen.domain.user.Role;
import dev.lumen.domain.user.User;
import dev.lumen.domain.user.UserRepository;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class AssessmentServiceTest {

    private final AssessmentRepository assessmentRepository = mock(AssessmentRepository.class);
    private final AssessmentAnswerRepository assessmentAnswerRepository = mock(AssessmentAnswerRepository.class);
    private final AssessmentScoreRepository assessmentScoreRepository = mock(AssessmentScoreRepository.class);
    private final RiskEventTriggerService riskEventTriggerService = mock(RiskEventTriggerService.class);
    private final UserRepository userRepository = mock(UserRepository.class);
    private final ConsentService consentService = mock(ConsentService.class);

    private final AssessmentService service = new AssessmentService(
            assessmentRepository,
            assessmentAnswerRepository,
            assessmentScoreRepository,
            riskEventTriggerService,
            userRepository,
            consentService);

    private User user;

    @BeforeEach
    void setUp() {
        user = new User(
                "jane@lumen.dev", "hashed-password", "Jane", "en", "PT", LocalDate.of(1990, 1, 1), Role.USER);
        when(userRepository.findById(user.getId())).thenReturn(Optional.of(user));
        when(consentService.isActive(user.getId(), ConsentType.HEALTH_DATA_PROCESSING)).thenReturn(true);
        when(assessmentRepository.findMostRecentCompletedByUserIdAndType(any(), any(), any()))
                .thenReturn(Optional.empty());
        when(assessmentRepository.save(any(Assessment.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(assessmentAnswerRepository.saveAll(any())).thenAnswer(invocation -> invocation.getArgument(0));
        when(assessmentScoreRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
        when(riskEventTriggerService.trigger(any(), any(), any(), any()))
                .thenReturn(new CrisisTriggerOutcome(UUID.randomUUID(), List.of()));
    }

    @Test
    void shouldScoreNormallyWhenPhq9Item9IsZero() {
        List<Integer> responses = List.of(0, 0, 0, 0, 0, 0, 0, 0, 0);

        AssessmentSubmissionResult result = service.submit(user.getId(), AssessmentType.PHQ9, responses);

        assertThat(result).isInstanceOf(ScoredAssessmentResult.class);
        ScoredAssessmentResult scored = (ScoredAssessmentResult) result;
        assertThat(scored.totalScore()).isZero();
        assertThat(scored.wellbeingBand()).isEqualTo(WellbeingBand.MINIMAL);
        verify(riskEventTriggerService, never()).trigger(any(), any(), any(), any());
    }

    @Test
    void shouldTriggerCrisisBeforeAnyScoreExistsWhenPhq9Item9IsPositive() {
        List<Integer> responses = List.of(0, 0, 0, 0, 0, 0, 0, 0, 1);

        AssessmentSubmissionResult result = service.submit(user.getId(), AssessmentType.PHQ9, responses);

        assertThat(result).isInstanceOf(CrisisTriggeredResult.class);
        verify(assessmentScoreRepository, never()).save(any());
        verify(riskEventTriggerService).trigger(any(), any(), any(), any());
    }

    @Test
    void shouldNeverTriggerCrisisForGad7EvenAtMaximumScore() {
        List<Integer> responses = List.of(3, 3, 3, 3, 3, 3, 3);

        AssessmentSubmissionResult result = service.submit(user.getId(), AssessmentType.GAD7, responses);

        assertThat(result).isInstanceOf(ScoredAssessmentResult.class);
        ScoredAssessmentResult scored = (ScoredAssessmentResult) result;
        assertThat(scored.totalScore()).isEqualTo(21);
        assertThat(scored.wellbeingBand()).isEqualTo(WellbeingBand.ELEVATED);
        verify(riskEventTriggerService, never()).trigger(any(), any(), any(), any());
    }

    @Test
    void shouldRejectSubmissionWithWrongItemCountForInstrument() {
        List<Integer> tooFewResponses = List.of(0, 0, 0);

        assertThatThrownBy(() -> service.submit(user.getId(), AssessmentType.PHQ9, tooFewResponses))
                .isInstanceOf(InvalidAssessmentSubmissionException.class);
    }

    @Test
    void shouldRejectSubmissionWhenHealthDataConsentIsNotActive() {
        when(consentService.isActive(user.getId(), ConsentType.HEALTH_DATA_PROCESSING)).thenReturn(false);
        List<Integer> responses = List.of(0, 0, 0, 0, 0, 0, 0, 0, 0);

        assertThatThrownBy(() -> service.submit(user.getId(), AssessmentType.PHQ9, responses))
                .isInstanceOf(ConsentRequiredException.class);
    }

    @Test
    void shouldRejectSecondSubmissionOfSameInstrumentWithinThirtyDays() {
        Assessment previous = new Assessment(user, AssessmentType.PHQ9);
        when(assessmentRepository.findMostRecentCompletedByUserIdAndType(any(), any(), any()))
                .thenReturn(Optional.of(previous));
        List<Integer> responses = List.of(0, 0, 0, 0, 0, 0, 0, 0, 0);

        assertThatThrownBy(() -> service.submit(user.getId(), AssessmentType.PHQ9, responses))
                .isInstanceOf(AssessmentTooSoonException.class);
    }

    @Test
    void shouldComputeAndReleaseScoreAfterCrisisAcknowledgment() {
        Assessment assessment = new Assessment(user, AssessmentType.PHQ9);
        assessment.start();
        assessment.complete();
        when(assessmentRepository.findById(assessment.getId())).thenReturn(Optional.of(assessment));
        when(assessmentAnswerRepository.findByAssessmentId(assessment.getId()))
                .thenReturn(List.of(new AssessmentAnswer(assessment, 9, 1)));

        ScoredAssessmentResult result = service.scoreAfterCrisisAcknowledgment(assessment.getId());

        assertThat(result.totalScore()).isEqualTo(1);
        assertThat(result.wellbeingBand()).isEqualTo(WellbeingBand.MINIMAL);
    }
}
