package dev.lumen.application.privacy;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import dev.lumen.application.audit.AuditLogService;
import dev.lumen.domain.assessment.Assessment;
import dev.lumen.domain.assessment.AssessmentAnswer;
import dev.lumen.domain.assessment.AssessmentAnswerRepository;
import dev.lumen.domain.assessment.AssessmentRepository;
import dev.lumen.domain.assessment.AssessmentScore;
import dev.lumen.domain.assessment.AssessmentScoreRepository;
import dev.lumen.domain.assessment.AssessmentType;
import dev.lumen.domain.assessment.WellbeingBand;
import dev.lumen.domain.audit.AuditAction;
import dev.lumen.domain.companion.ConversationMessage;
import dev.lumen.domain.companion.ConversationMessageRepository;
import dev.lumen.domain.companion.ConversationRole;
import dev.lumen.domain.companion.ConversationSummary;
import dev.lumen.domain.companion.ConversationSummaryRepository;
import dev.lumen.domain.crisis.RiskEvent;
import dev.lumen.domain.crisis.RiskEventRepository;
import dev.lumen.domain.crisis.TriggerSource;
import dev.lumen.domain.exercise.ExerciseCompletion;
import dev.lumen.domain.exercise.ExerciseCompletionRepository;
import dev.lumen.domain.moodcheckin.MoodCheckIn;
import dev.lumen.domain.moodcheckin.MoodCheckInRepository;
import dev.lumen.domain.moodcheckin.MoodEmotion;
import dev.lumen.domain.recommendation.Recommendation;
import dev.lumen.domain.recommendation.RecommendationRepository;
import dev.lumen.domain.user.ConsentRecord;
import dev.lumen.domain.user.ConsentRecordRepository;
import dev.lumen.domain.user.ConsentType;
import dev.lumen.domain.user.Role;
import dev.lumen.domain.user.User;
import dev.lumen.domain.user.UserNotFoundException;
import dev.lumen.domain.user.UserRepository;
import dev.lumen.domain.wearable.WearableReading;
import dev.lumen.domain.wearable.WearableReadingRepository;
import dev.lumen.domain.wearable.WearableReadingType;
import dev.lumen.domain.wearable.WearableSourceType;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class DataExportServiceTest {

    private final UserRepository userRepository = mock(UserRepository.class);
    private final ConsentRecordRepository consentRecordRepository = mock(ConsentRecordRepository.class);
    private final MoodCheckInRepository moodCheckInRepository = mock(MoodCheckInRepository.class);
    private final AssessmentRepository assessmentRepository = mock(AssessmentRepository.class);
    private final AssessmentAnswerRepository assessmentAnswerRepository = mock(AssessmentAnswerRepository.class);
    private final AssessmentScoreRepository assessmentScoreRepository = mock(AssessmentScoreRepository.class);
    private final RiskEventRepository riskEventRepository = mock(RiskEventRepository.class);
    private final RecommendationRepository recommendationRepository = mock(RecommendationRepository.class);
    private final ExerciseCompletionRepository exerciseCompletionRepository =
            mock(ExerciseCompletionRepository.class);
    private final WearableReadingRepository wearableReadingRepository = mock(WearableReadingRepository.class);
    private final ConversationMessageRepository conversationMessageRepository =
            mock(ConversationMessageRepository.class);
    private final ConversationSummaryRepository conversationSummaryRepository =
            mock(ConversationSummaryRepository.class);
    private final AuditLogService auditLogService = mock(AuditLogService.class);

    private final DataExportService service = new DataExportService(
            userRepository,
            consentRecordRepository,
            moodCheckInRepository,
            assessmentRepository,
            assessmentAnswerRepository,
            assessmentScoreRepository,
            riskEventRepository,
            recommendationRepository,
            exerciseCompletionRepository,
            wearableReadingRepository,
            conversationMessageRepository,
            conversationSummaryRepository,
            auditLogService);

    private User user;

    @BeforeEach
    void setUp() {
        user = new User("jane@lumen.dev", "hashed-password", "Jane", "en", "PT", LocalDate.of(1990, 1, 1), Role.USER);
        when(userRepository.findById(user.getId())).thenReturn(Optional.of(user));
    }

    @Test
    void shouldIncludeEveryCategoryOfPersonalDataTheUserOwns() {
        givenOneRecordInEveryCategory();

        UserDataExport export = service.export(user.getId());

        assertThat(export.account().email()).isEqualTo("jane@lumen.dev");
        assertThat(export.consents()).hasSize(1);
        assertThat(export.moodCheckIns()).hasSize(1);
        assertThat(export.assessments()).hasSize(1);
        assertThat(export.assessments().get(0).answers()).hasSize(1);
        assertThat(export.assessments().get(0).totalScore()).isEqualTo(7);
        assertThat(export.riskEvents()).hasSize(1);
        assertThat(export.recommendations()).hasSize(1);
        assertThat(export.exerciseCompletions()).hasSize(1);
        assertThat(export.wearableReadings()).hasSize(1);
        assertThat(export.conversation().messages()).hasSize(1);
        assertThat(export.conversation().summary()).isEqualTo("a summary");

    }

    /** One row in every table the person owns, so a missing category shows up as a failure. */
    private void givenOneRecordInEveryCategory() {
        Assessment assessment = new Assessment(user, AssessmentType.PHQ9);
        when(consentRecordRepository.findAllByUserIdOrderByCreatedAtAsc(user.getId()))
                .thenReturn(List.of(new ConsentRecord(user, ConsentType.HEALTH_DATA_PROCESSING, true, 1)));
        when(moodCheckInRepository.findByUserIdOrderByCheckInDateDesc(user.getId()))
                .thenReturn(List.of(new MoodCheckIn(
                        user,
                        MoodEmotion.ANXIOUS,
                        2,
                        new BigDecimal("5.5"),
                        2,
                        "a private note",
                        LocalDate.of(2026, 1, 5))));
        when(assessmentRepository.findByUserIdOrderByCreatedAtDesc(user.getId())).thenReturn(List.of(assessment));
        when(assessmentAnswerRepository.findByAssessmentId(assessment.getId()))
                .thenReturn(List.of(new AssessmentAnswer(assessment, 9, 0)));
        when(assessmentScoreRepository.findByAssessmentId(assessment.getId()))
                .thenReturn(Optional.of(new AssessmentScore(assessment, 7, WellbeingBand.MILD)));
        when(riskEventRepository.findByUserIdOrderByDetectedAtDesc(user.getId()))
                .thenReturn(List.of(new RiskEvent(user.getId(), assessment.getId(), TriggerSource.PHQ9_ITEM9)));
        when(recommendationRepository.findByUserIdOrderByCreatedAtDesc(user.getId()))
                .thenReturn(List.of(new Recommendation(
                        user.getId(), UUID.randomUUID(), UUID.randomUUID(), "because you slept 5h")));
        when(exerciseCompletionRepository.findByUserIdOrderByCompletedAtDesc(user.getId()))
                .thenReturn(List.of(new ExerciseCompletion(user.getId(), UUID.randomUUID(), null)));
        when(wearableReadingRepository.findByUserIdOrderByRecordedAtDesc(user.getId()))
                .thenReturn(List.of(new WearableReading(
                        user.getId(),
                        WearableReadingType.HEART_RATE,
                        new BigDecimal("62"),
                        Instant.parse("2026-01-05T08:00:00Z"),
                        WearableSourceType.SIMULATOR)));
        when(conversationMessageRepository.findByUserIdOrderByCreatedAtAsc(user.getId()))
                .thenReturn(List.of(
                        new ConversationMessage(user.getId(), ConversationRole.USER, "something personal")));
        when(conversationSummaryRepository.findByUserId(user.getId()))
                .thenReturn(Optional.of(new ConversationSummary(user.getId(), "a summary", UUID.randomUUID())));
    }

    /**
     * The free-text fields are the ones stored encrypted. An export that handed back
     * ciphertext would technically contain the data and still fail the person asking.
     */
    @Test
    void shouldReturnEncryptedFieldsAsReadableText() {
        when(moodCheckInRepository.findByUserIdOrderByCheckInDateDesc(user.getId()))
                .thenReturn(List.of(new MoodCheckIn(
                        user, MoodEmotion.SAD, 2, new BigDecimal("6.0"), 3, "a private note", LocalDate.now())));
        when(conversationMessageRepository.findByUserIdOrderByCreatedAtAsc(user.getId()))
                .thenReturn(List.of(
                        new ConversationMessage(user.getId(), ConversationRole.USER, "something personal")));

        UserDataExport export = service.export(user.getId());

        assertThat(export.moodCheckIns().get(0).note()).isEqualTo("a private note");
        assertThat(export.conversation().messages().get(0).content()).isEqualTo("something personal");
    }

    /** A credential is not personal data the person is owed, and a downloads folder is no place for it. */
    @Test
    void shouldNeverExposeThePasswordHash() {
        UserDataExport export = service.export(user.getId());

        assertThat(export.account().toString()).doesNotContain("hashed-password");
        assertThat(UserDataExport.Account.class.getRecordComponents())
                .noneMatch(component -> component.getName().toLowerCase().contains("password"));
    }

    @Test
    void shouldRecordTheExportInTheAuditTrail() {
        service.export(user.getId());

        verify(auditLogService).record(user.getId(), user.getId(), AuditAction.EXPORT_PERSONAL_DATA);
    }

    @Test
    void shouldRejectExportForAnAccountThatDoesNotExist() {
        UUID unknownUserId = UUID.randomUUID();
        when(userRepository.findById(unknownUserId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.export(unknownUserId)).isInstanceOf(UserNotFoundException.class);

        verify(auditLogService, never()).record(any(), any(), any());
    }
}
