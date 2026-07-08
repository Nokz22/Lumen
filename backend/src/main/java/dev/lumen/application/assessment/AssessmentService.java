package dev.lumen.application.assessment;

import dev.lumen.application.consent.ConsentService;
import dev.lumen.application.crisis.CrisisTriggerOutcome;
import dev.lumen.application.crisis.RiskEventTriggerService;
import dev.lumen.domain.assessment.Assessment;
import dev.lumen.domain.assessment.AssessmentNotFoundException;
import dev.lumen.domain.assessment.AssessmentRepository;
import dev.lumen.domain.assessment.AssessmentAnswer;
import dev.lumen.domain.assessment.AssessmentAnswerRepository;
import dev.lumen.domain.assessment.AssessmentScore;
import dev.lumen.domain.assessment.AssessmentScoreRepository;
import dev.lumen.domain.assessment.AssessmentTooSoonException;
import dev.lumen.domain.assessment.AssessmentType;
import dev.lumen.domain.assessment.InvalidAssessmentSubmissionException;
import dev.lumen.domain.assessment.WellbeingBand;
import dev.lumen.domain.crisis.TriggerSource;
import dev.lumen.domain.user.ConsentRequiredException;
import dev.lumen.domain.user.ConsentType;
import dev.lumen.domain.user.User;
import dev.lumen.domain.user.UserNotFoundException;
import dev.lumen.domain.user.UserRepository;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * The PHQ-9 item 9 check (thoughts of self-harm) is the one clinical invariant this
 * class must never regress: it is evaluated before any score is computed, and when
 * positive the crisis flow returns instead of a score — see submit().
 */
@Service
public class AssessmentService {

    private static final int CADENCE_DAYS = 30;
    private static final int PHQ9_SELF_HARM_ITEM_NUMBER = 9;

    private final AssessmentRepository assessmentRepository;
    private final AssessmentAnswerRepository assessmentAnswerRepository;
    private final AssessmentScoreRepository assessmentScoreRepository;
    private final RiskEventTriggerService riskEventTriggerService;
    private final UserRepository userRepository;
    private final ConsentService consentService;

    public AssessmentService(
            AssessmentRepository assessmentRepository,
            AssessmentAnswerRepository assessmentAnswerRepository,
            AssessmentScoreRepository assessmentScoreRepository,
            RiskEventTriggerService riskEventTriggerService,
            UserRepository userRepository,
            ConsentService consentService) {
        this.assessmentRepository = assessmentRepository;
        this.assessmentAnswerRepository = assessmentAnswerRepository;
        this.assessmentScoreRepository = assessmentScoreRepository;
        this.riskEventTriggerService = riskEventTriggerService;
        this.userRepository = userRepository;
        this.consentService = consentService;
    }

    @Transactional
    public AssessmentSubmissionResult submit(UUID userId, AssessmentType assessmentType, List<Integer> responses) {
        User user = userRepository.findById(userId).orElseThrow(() -> new UserNotFoundException(userId));
        requireHealthDataConsent(userId);
        requireItemCount(assessmentType, responses);
        requireCadence(userId, assessmentType);

        Assessment assessment = new Assessment(user, assessmentType);
        assessment.start();
        assessment = assessmentRepository.save(assessment);

        List<AssessmentAnswer> savedResponses = persistResponses(assessment, responses);

        assessment.complete();
        assessmentRepository.save(assessment);

        if (triggersCrisis(assessmentType, responses)) {
            return triggerCrisisFlow(user, assessment);
        }
        AssessmentScore score = computeAndPersistScore(assessment, savedResponses);
        return new ScoredAssessmentResult(assessment.getId(), score.getTotalScore(), score.getWellbeingBand());
    }

    @Transactional
    public ScoredAssessmentResult scoreAfterCrisisAcknowledgment(UUID assessmentId) {
        Assessment assessment = assessmentRepository
                .findById(assessmentId)
                .orElseThrow(() -> new AssessmentNotFoundException(assessmentId));
        List<AssessmentAnswer> responses = assessmentAnswerRepository.findByAssessmentId(assessmentId);
        AssessmentScore score = computeAndPersistScore(assessment, responses);
        return new ScoredAssessmentResult(assessment.getId(), score.getTotalScore(), score.getWellbeingBand());
    }

    @Transactional(readOnly = true)
    public List<AssessmentSummaryResponse> getHistory(UUID userId) {
        if (userRepository.findById(userId).isEmpty()) {
            throw new UserNotFoundException(userId);
        }
        return assessmentRepository.findByUserIdOrderByCreatedAtDesc(userId).stream()
                .map(this::toSummary)
                .toList();
    }

    private List<AssessmentAnswer> persistResponses(Assessment assessment, List<Integer> responses) {
        List<AssessmentAnswer> toSave = new ArrayList<>();
        for (int index = 0; index < responses.size(); index++) {
            toSave.add(new AssessmentAnswer(assessment, index + 1, responses.get(index)));
        }
        return assessmentAnswerRepository.saveAll(toSave);
    }

    private boolean triggersCrisis(AssessmentType assessmentType, List<Integer> responses) {
        return assessmentType == AssessmentType.PHQ9 && responses.get(PHQ9_SELF_HARM_ITEM_NUMBER - 1) > 0;
    }

    private CrisisTriggeredResult triggerCrisisFlow(User user, Assessment assessment) {
        CrisisTriggerOutcome outcome = riskEventTriggerService.trigger(
                user.getId(), assessment.getId(), TriggerSource.PHQ9_ITEM9, user.getRegion());
        return new CrisisTriggeredResult(outcome.riskEventId(), outcome.resources());
    }

    private AssessmentScore computeAndPersistScore(Assessment assessment, List<AssessmentAnswer> responses) {
        int totalScore = responses.stream().mapToInt(AssessmentAnswer::getValue).sum();
        WellbeingBand band = bandFor(assessment.getAssessmentType(), totalScore);
        assessment.score();
        assessmentRepository.save(assessment);
        return assessmentScoreRepository.save(new AssessmentScore(assessment, totalScore, band));
    }

    /**
     * PHQ-9 has 5 officially validated severity bands, GAD-7 only 4 — GAD-7 never
     * produces PRONOUNCED. Cutoffs are the standard published thresholds for each
     * instrument, only the labels are wellbeing language (ADR-0001).
     */
    private WellbeingBand bandFor(AssessmentType assessmentType, int totalScore) {
        if (assessmentType == AssessmentType.GAD7) {
            if (totalScore <= 4) {
                return WellbeingBand.MINIMAL;
            }
            if (totalScore <= 9) {
                return WellbeingBand.MILD;
            }
            if (totalScore <= 14) {
                return WellbeingBand.MODERATE;
            }
            return WellbeingBand.ELEVATED;
        }
        if (totalScore <= 4) {
            return WellbeingBand.MINIMAL;
        }
        if (totalScore <= 9) {
            return WellbeingBand.MILD;
        }
        if (totalScore <= 14) {
            return WellbeingBand.MODERATE;
        }
        if (totalScore <= 19) {
            return WellbeingBand.PRONOUNCED;
        }
        return WellbeingBand.ELEVATED;
    }

    private void requireItemCount(AssessmentType assessmentType, List<Integer> responses) {
        int expected = assessmentType.getItemCount();
        int actual = responses == null ? 0 : responses.size();
        if (actual != expected) {
            throw new InvalidAssessmentSubmissionException(assessmentType, expected, actual);
        }
    }

    private void requireCadence(UUID userId, AssessmentType assessmentType) {
        Instant since = Instant.now().minus(CADENCE_DAYS, ChronoUnit.DAYS);
        if (assessmentRepository
                .findMostRecentCompletedByUserIdAndType(userId, assessmentType, since)
                .isPresent()) {
            throw new AssessmentTooSoonException(assessmentType);
        }
    }

    private void requireHealthDataConsent(UUID userId) {
        if (!consentService.isActive(userId, ConsentType.HEALTH_DATA_PROCESSING)) {
            throw new ConsentRequiredException(ConsentType.HEALTH_DATA_PROCESSING);
        }
    }

    private AssessmentSummaryResponse toSummary(Assessment assessment) {
        AssessmentScore score =
                assessmentScoreRepository.findByAssessmentId(assessment.getId()).orElse(null);
        return new AssessmentSummaryResponse(
                assessment.getId(),
                assessment.getAssessmentType(),
                assessment.getStatus(),
                score != null ? score.getTotalScore() : null,
                score != null ? score.getWellbeingBand() : null,
                assessment.getCreatedAt());
    }
}
