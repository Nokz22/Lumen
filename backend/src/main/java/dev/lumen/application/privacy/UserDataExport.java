package dev.lumen.application.privacy;

import dev.lumen.domain.assessment.AssessmentType;
import dev.lumen.domain.assessment.WellbeingBand;
import dev.lumen.domain.crisis.RiskEventStatus;
import dev.lumen.domain.crisis.TriggerSource;
import dev.lumen.domain.moodcheckin.MoodEmotion;
import dev.lumen.domain.user.ConsentType;
import dev.lumen.domain.wearable.WearableReadingType;
import dev.lumen.domain.wearable.WearableSourceType;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/**
 * The GDPR Article 15 payload: everything the platform holds about one person, in a
 * structured, commonly used, machine-readable form (Article 20).
 *
 * <p>Deliberately its own shape rather than a reuse of the API response records. Those
 * exist to serve screens and change when a screen changes; this exists to be complete,
 * and "complete" must not quietly shrink because a dashboard stopped showing a field.
 *
 * <p>Encrypted columns (check-in notes, chat messages, the conversation summary) are
 * decrypted on the way out — an export the person cannot read would not answer the
 * request they made.
 */
public record UserDataExport(
        Instant exportedAt,
        Account account,
        List<Consent> consents,
        List<MoodCheckIn> moodCheckIns,
        List<Assessment> assessments,
        List<RiskEvent> riskEvents,
        List<Recommendation> recommendations,
        List<ExerciseCompletion> exerciseCompletions,
        List<WearableReading> wearableReadings,
        Conversation conversation) {

    /**
     * No password hash: it is a credential, not personal data the person is entitled to
     * receive, and shipping it in a file that lands in a downloads folder would be a
     * fresh security problem created by a privacy feature.
     */
    public record Account(
            UUID id,
            String email,
            String displayName,
            String locale,
            String region,
            LocalDate dateOfBirth,
            String role,
            Instant createdAt) {
    }

    public record Consent(
            ConsentType consentType,
            boolean granted,
            int consentVersion,
            Instant grantedAt,
            Instant revokedAt,
            Instant createdAt) {
    }

    public record MoodCheckIn(
            UUID id,
            MoodEmotion emotion,
            int energyLevel,
            BigDecimal sleepHours,
            int sleepQuality,
            String note,
            LocalDate checkInDate,
            Instant createdAt) {
    }

    /**
     * The band is exported as the wellbeing label the person was actually shown, never as
     * a diagnostic one — an export is not a loophole around ADR-0001.
     */
    public record Assessment(
            UUID id,
            AssessmentType assessmentType,
            String status,
            Instant startedAt,
            Instant completedAt,
            Instant createdAt,
            List<AssessmentAnswer> answers,
            Integer totalScore,
            WellbeingBand wellbeingBand) {
    }

    public record AssessmentAnswer(int itemNumber, int value) {
    }

    public record RiskEvent(
            UUID id,
            UUID assessmentId,
            TriggerSource triggerSource,
            RiskEventStatus status,
            Instant detectedAt,
            Instant resourcesPresentedAt,
            Instant acknowledgedAt) {
    }

    public record Recommendation(UUID id, UUID exerciseId, UUID moodCheckInId, String reason, Instant createdAt) {
    }

    public record ExerciseCompletion(UUID id, UUID exerciseId, UUID recommendationId, Instant completedAt) {
    }

    public record WearableReading(
            UUID id,
            WearableReadingType type,
            BigDecimal value,
            WearableSourceType source,
            Instant recordedAt,
            Instant createdAt) {
    }

    public record Conversation(List<ConversationMessage> messages, String summary, Instant summaryUpdatedAt) {
    }

    public record ConversationMessage(UUID id, String role, String content, Instant createdAt) {
    }
}
