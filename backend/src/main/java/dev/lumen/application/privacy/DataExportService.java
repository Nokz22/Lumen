package dev.lumen.application.privacy;

import dev.lumen.application.audit.AuditLogService;
import dev.lumen.domain.assessment.Assessment;
import dev.lumen.domain.assessment.AssessmentAnswerRepository;
import dev.lumen.domain.assessment.AssessmentRepository;
import dev.lumen.domain.assessment.AssessmentScore;
import dev.lumen.domain.assessment.AssessmentScoreRepository;
import dev.lumen.domain.audit.AuditAction;
import dev.lumen.domain.companion.ConversationMessageRepository;
import dev.lumen.domain.companion.ConversationSummary;
import dev.lumen.domain.companion.ConversationSummaryRepository;
import dev.lumen.domain.crisis.RiskEventRepository;
import dev.lumen.domain.exercise.ExerciseCompletionRepository;
import dev.lumen.domain.moodcheckin.MoodCheckInRepository;
import dev.lumen.domain.recommendation.RecommendationRepository;
import dev.lumen.domain.user.ConsentRecordRepository;
import dev.lumen.domain.user.User;
import dev.lumen.domain.user.UserNotFoundException;
import dev.lumen.domain.user.UserRepository;
import dev.lumen.domain.wearable.WearableReadingRepository;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * The right of access (GDPR Article 15) and to portability (Article 20), served as one
 * JSON document.
 *
 * <p>Not gated on HEALTH_DATA_PROCESSING consent, unlike every other read of this data.
 * That is deliberate: a person who has just withdrawn consent is precisely the person most
 * likely to want a copy of what was held, and a right of access that evaporates when
 * consent does is not a right. Withdrawing consent stops new processing; it does not
 * delete history, and it must not lock the person out of their own record.
 *
 * <p>Reads across every aggregate rather than through the feature services, so an export
 * cannot silently lose a category the day a feature service adds a filter of its own.
 */
@Service
public class DataExportService {

    private final UserRepository userRepository;
    private final ConsentRecordRepository consentRecordRepository;
    private final MoodCheckInRepository moodCheckInRepository;
    private final AssessmentRepository assessmentRepository;
    private final AssessmentAnswerRepository assessmentAnswerRepository;
    private final AssessmentScoreRepository assessmentScoreRepository;
    private final RiskEventRepository riskEventRepository;
    private final RecommendationRepository recommendationRepository;
    private final ExerciseCompletionRepository exerciseCompletionRepository;
    private final WearableReadingRepository wearableReadingRepository;
    private final ConversationMessageRepository conversationMessageRepository;
    private final ConversationSummaryRepository conversationSummaryRepository;
    private final AuditLogService auditLogService;

    public DataExportService(
            UserRepository userRepository,
            ConsentRecordRepository consentRecordRepository,
            MoodCheckInRepository moodCheckInRepository,
            AssessmentRepository assessmentRepository,
            AssessmentAnswerRepository assessmentAnswerRepository,
            AssessmentScoreRepository assessmentScoreRepository,
            RiskEventRepository riskEventRepository,
            RecommendationRepository recommendationRepository,
            ExerciseCompletionRepository exerciseCompletionRepository,
            WearableReadingRepository wearableReadingRepository,
            ConversationMessageRepository conversationMessageRepository,
            ConversationSummaryRepository conversationSummaryRepository,
            AuditLogService auditLogService) {
        this.userRepository = userRepository;
        this.consentRecordRepository = consentRecordRepository;
        this.moodCheckInRepository = moodCheckInRepository;
        this.assessmentRepository = assessmentRepository;
        this.assessmentAnswerRepository = assessmentAnswerRepository;
        this.assessmentScoreRepository = assessmentScoreRepository;
        this.riskEventRepository = riskEventRepository;
        this.recommendationRepository = recommendationRepository;
        this.exerciseCompletionRepository = exerciseCompletionRepository;
        this.wearableReadingRepository = wearableReadingRepository;
        this.conversationMessageRepository = conversationMessageRepository;
        this.conversationSummaryRepository = conversationSummaryRepository;
        this.auditLogService = auditLogService;
    }

    @Transactional(readOnly = true)
    public UserDataExport export(UUID userId) {
        User user = userRepository.findById(userId).orElseThrow(() -> new UserNotFoundException(userId));
        auditLogService.record(userId, userId, AuditAction.EXPORT_PERSONAL_DATA);

        return new UserDataExport(
                Instant.now(),
                account(user),
                consents(userId),
                moodCheckIns(userId),
                assessments(userId),
                riskEvents(userId),
                recommendations(userId),
                exerciseCompletions(userId),
                wearableReadings(userId),
                conversation(userId));
    }

    private UserDataExport.Account account(User user) {
        return new UserDataExport.Account(
                user.getId(),
                user.getEmail(),
                user.getDisplayName(),
                user.getLocale(),
                user.getRegion(),
                user.getDateOfBirth(),
                user.getRole().name(),
                user.getCreatedAt());
    }

    private List<UserDataExport.Consent> consents(UUID userId) {
        return consentRecordRepository.findAllByUserIdOrderByCreatedAtAsc(userId).stream()
                .map(consent -> new UserDataExport.Consent(
                        consent.getConsentType(),
                        consent.isGranted(),
                        consent.getConsentVersion(),
                        consent.getGrantedAt(),
                        consent.getRevokedAt(),
                        consent.getCreatedAt()))
                .toList();
    }

    private List<UserDataExport.MoodCheckIn> moodCheckIns(UUID userId) {
        return moodCheckInRepository.findByUserIdOrderByCheckInDateDesc(userId).stream()
                .map(checkIn -> new UserDataExport.MoodCheckIn(
                        checkIn.getId(),
                        checkIn.getEmotion(),
                        checkIn.getEnergyLevel(),
                        checkIn.getSleepHours(),
                        checkIn.getSleepQuality(),
                        checkIn.getNote(),
                        checkIn.getCheckInDate(),
                        checkIn.getCreatedAt()))
                .toList();
    }

    private List<UserDataExport.Assessment> assessments(UUID userId) {
        return assessmentRepository.findByUserIdOrderByCreatedAtDesc(userId).stream()
                .map(this::toExport)
                .toList();
    }

    private UserDataExport.Assessment toExport(Assessment assessment) {
        List<UserDataExport.AssessmentAnswer> answers =
                assessmentAnswerRepository.findByAssessmentId(assessment.getId()).stream()
                        .map(answer -> new UserDataExport.AssessmentAnswer(answer.getItemNumber(), answer.getValue()))
                        .toList();
        Optional<AssessmentScore> score = assessmentScoreRepository.findByAssessmentId(assessment.getId());
        return new UserDataExport.Assessment(
                assessment.getId(),
                assessment.getAssessmentType(),
                assessment.getStatus().name(),
                assessment.getStartedAt(),
                assessment.getCompletedAt(),
                assessment.getCreatedAt(),
                answers,
                score.map(AssessmentScore::getTotalScore).orElse(null),
                score.map(AssessmentScore::getWellbeingBand).orElse(null));
    }

    private List<UserDataExport.RiskEvent> riskEvents(UUID userId) {
        return riskEventRepository.findByUserIdOrderByDetectedAtDesc(userId).stream()
                .map(event -> new UserDataExport.RiskEvent(
                        event.getId(),
                        event.getAssessmentId(),
                        event.getTriggerSource(),
                        event.getStatus(),
                        event.getDetectedAt(),
                        event.getResourcesPresentedAt(),
                        event.getAcknowledgedAt()))
                .toList();
    }

    private List<UserDataExport.Recommendation> recommendations(UUID userId) {
        return recommendationRepository.findByUserIdOrderByCreatedAtDesc(userId).stream()
                .map(recommendation -> new UserDataExport.Recommendation(
                        recommendation.getId(),
                        recommendation.getExerciseId(),
                        recommendation.getMoodCheckInId(),
                        recommendation.getReason(),
                        recommendation.getCreatedAt()))
                .toList();
    }

    private List<UserDataExport.ExerciseCompletion> exerciseCompletions(UUID userId) {
        return exerciseCompletionRepository.findByUserIdOrderByCompletedAtDesc(userId).stream()
                .map(completion -> new UserDataExport.ExerciseCompletion(
                        completion.getId(),
                        completion.getExerciseId(),
                        completion.getRecommendationId(),
                        completion.getCompletedAt()))
                .toList();
    }

    private List<UserDataExport.WearableReading> wearableReadings(UUID userId) {
        return wearableReadingRepository.findByUserIdOrderByRecordedAtDesc(userId).stream()
                .map(reading -> new UserDataExport.WearableReading(
                        reading.getId(),
                        reading.getType(),
                        reading.getValue(),
                        reading.getSource(),
                        reading.getRecordedAt(),
                        reading.getCreatedAt()))
                .toList();
    }

    private UserDataExport.Conversation conversation(UUID userId) {
        List<UserDataExport.ConversationMessage> messages =
                conversationMessageRepository.findByUserIdOrderByCreatedAtAsc(userId).stream()
                        .map(message -> new UserDataExport.ConversationMessage(
                                message.getId(),
                                message.getRole().name(),
                                message.getContent(),
                                message.getCreatedAt()))
                        .toList();
        Optional<ConversationSummary> summary = conversationSummaryRepository.findByUserId(userId);
        return new UserDataExport.Conversation(
                messages,
                summary.map(ConversationSummary::getSummaryText).orElse(null),
                summary.map(ConversationSummary::getUpdatedAt).orElse(null));
    }
}
