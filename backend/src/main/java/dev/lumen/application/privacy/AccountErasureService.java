package dev.lumen.application.privacy;

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
import dev.lumen.domain.user.UserNotFoundException;
import dev.lumen.domain.user.UserRepository;
import dev.lumen.domain.wearable.WearableReadingRepository;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * The right to erasure (GDPR Article 17). Actual deletion, not a soft-delete flag: a row
 * that is still there with deleted = true has not been erased, it has been hidden, and the
 * distinction is the whole point of the right.
 *
 * <p>One transaction. A partial erasure is worse than none — it leaves a person believing
 * their data is gone while some of it is not — so either every table is cleared or the
 * whole thing rolls back and the caller gets an error.
 *
 * <p>Deletion order is dictated by the foreign keys, not by preference: children before
 * parents, all the way down to the user row. Every repository delete behind this is an
 * explicit bulk statement rather than a derived Spring Data method, and that is load-bearing
 * — a derived delete loads the rows and queues their removal in the persistence context,
 * which Hibernate then flushes in its own order, ignoring the order they were called in.
 * Mixing the two styles fails against the database with a constraint violation.
 *
 * <p>What survives is the audit trail, which after this runs holds nothing but opaque UUIDs
 * that no longer resolve to a person — see ADR-0011 for why that is erasure and not a
 * leftover.
 */
@Service
public class AccountErasureService {

    private static final Logger LOG = LoggerFactory.getLogger(AccountErasureService.class);

    private final UserRepository userRepository;
    private final RefreshTokenRepository refreshTokenRepository;
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

    public AccountErasureService(
            UserRepository userRepository,
            RefreshTokenRepository refreshTokenRepository,
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
        this.refreshTokenRepository = refreshTokenRepository;
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

    @Transactional
    public void erase(UUID userId) {
        if (userRepository.findById(userId).isEmpty()) {
            throw new UserNotFoundException(userId);
        }

        eraseSelfCareData(userId);
        eraseAssessmentData(userId);
        eraseCompanionData(userId);
        eraseIdentityData(userId);
        userRepository.deleteById(userId);

        // Recorded after the user row is gone, and holds only the (now unresolvable) id:
        // accountability under Article 5(2) without keeping the person behind it.
        auditLogService.record(userId, userId, AuditAction.ERASE_ACCOUNT);
        LOG.info("Erased all personal data for a user account");
    }

    /** Recommendations reference check-ins, so they go first. */
    private void eraseSelfCareData(UUID userId) {
        exerciseCompletionRepository.deleteByUserId(userId);
        recommendationRepository.deleteByUserId(userId);
        moodCheckInRepository.deleteByUserId(userId);
        wearableReadingRepository.deleteByUserId(userId);
    }

    /** Risk events, answers and scores all reference assessments, so the parent goes last. */
    private void eraseAssessmentData(UUID userId) {
        riskEventRepository.deleteByUserId(userId);
        assessmentScoreRepository.deleteByUserId(userId);
        assessmentAnswerRepository.deleteByUserId(userId);
        assessmentRepository.deleteByUserId(userId);
    }

    /** The summary references the message it summarised through. */
    private void eraseCompanionData(UUID userId) {
        conversationSummaryRepository.deleteByUserId(userId);
        conversationMessageRepository.deleteByUserId(userId);
    }

    private void eraseIdentityData(UUID userId) {
        refreshTokenRepository.deleteByUserId(userId);
        consentRecordRepository.deleteByUserId(userId);
    }
}
