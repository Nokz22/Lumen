package dev.lumen.application.privacy;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import dev.lumen.application.audit.AuditLogService;
import dev.lumen.domain.assessment.AssessmentAnswerRepository;
import dev.lumen.domain.assessment.AssessmentRepository;
import dev.lumen.domain.assessment.AssessmentScoreRepository;
import dev.lumen.domain.audit.AuditAction;
import dev.lumen.domain.companion.ConversationMessageRepository;
import dev.lumen.domain.companion.ConversationSummaryRepository;
import dev.lumen.domain.crisis.RiskEventRepository;
import dev.lumen.domain.exercise.ExerciseCompletionRepository;
import dev.lumen.domain.moodcheckin.MoodCheckInRepository;
import dev.lumen.domain.recommendation.RecommendationRepository;
import dev.lumen.domain.user.ConsentRecordRepository;
import dev.lumen.domain.user.RefreshTokenRepository;
import dev.lumen.domain.user.Role;
import dev.lumen.domain.user.User;
import dev.lumen.domain.user.UserNotFoundException;
import dev.lumen.domain.user.UserRepository;
import dev.lumen.domain.wearable.WearableReadingRepository;
import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InOrder;

class AccountErasureServiceTest {

    private final UserRepository userRepository = mock(UserRepository.class);
    private final RefreshTokenRepository refreshTokenRepository = mock(RefreshTokenRepository.class);
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

    private final AccountErasureService service = new AccountErasureService(
            userRepository,
            refreshTokenRepository,
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
    void shouldEraseEveryCategoryOfPersonalDataTheUserOwns() {
        service.erase(user.getId());

        verify(moodCheckInRepository).deleteByUserId(user.getId());
        verify(assessmentRepository).deleteByUserId(user.getId());
        verify(assessmentAnswerRepository).deleteByUserId(user.getId());
        verify(assessmentScoreRepository).deleteByUserId(user.getId());
        verify(riskEventRepository).deleteByUserId(user.getId());
        verify(recommendationRepository).deleteByUserId(user.getId());
        verify(exerciseCompletionRepository).deleteByUserId(user.getId());
        verify(wearableReadingRepository).deleteByUserId(user.getId());
        verify(conversationMessageRepository).deleteByUserId(user.getId());
        verify(conversationSummaryRepository).deleteByUserId(user.getId());
        verify(consentRecordRepository).deleteByUserId(user.getId());
        verify(refreshTokenRepository).deleteByUserId(user.getId());
        verify(userRepository).deleteById(user.getId());
    }

    /**
     * Not a stylistic preference: each of these pairs is a foreign key, and getting the
     * order wrong fails at the database with a constraint violation rather than politely.
     */
    @Test
    void shouldDeleteChildRowsBeforeTheRowsTheyReference() {
        service.erase(user.getId());

        InOrder order = inOrder(
                recommendationRepository,
                moodCheckInRepository,
                assessmentScoreRepository,
                assessmentAnswerRepository,
                riskEventRepository,
                assessmentRepository,
                conversationSummaryRepository,
                conversationMessageRepository,
                userRepository);
        order.verify(recommendationRepository).deleteByUserId(user.getId());
        order.verify(moodCheckInRepository).deleteByUserId(user.getId());
        order.verify(riskEventRepository).deleteByUserId(user.getId());
        order.verify(assessmentScoreRepository).deleteByUserId(user.getId());
        order.verify(assessmentAnswerRepository).deleteByUserId(user.getId());
        order.verify(assessmentRepository).deleteByUserId(user.getId());
        order.verify(conversationSummaryRepository).deleteByUserId(user.getId());
        order.verify(conversationMessageRepository).deleteByUserId(user.getId());
        order.verify(userRepository).deleteById(user.getId());
    }

    @Test
    void shouldRecordTheErasureInTheAuditTrailAfterTheUserIsGone() {
        service.erase(user.getId());

        InOrder order = inOrder(userRepository, auditLogService);
        order.verify(userRepository).deleteById(user.getId());
        order.verify(auditLogService).record(user.getId(), user.getId(), AuditAction.ERASE_ACCOUNT);
    }

    @Test
    void shouldDeleteNothingWhenTheAccountDoesNotExist() {
        UUID unknownUserId = UUID.randomUUID();
        when(userRepository.findById(unknownUserId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.erase(unknownUserId)).isInstanceOf(UserNotFoundException.class);

        verify(moodCheckInRepository, never()).deleteByUserId(unknownUserId);
        verify(conversationMessageRepository, never()).deleteByUserId(unknownUserId);
        verify(userRepository, never()).deleteById(unknownUserId);
    }
}
