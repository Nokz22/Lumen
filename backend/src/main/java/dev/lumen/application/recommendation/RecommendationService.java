package dev.lumen.application.recommendation;

import dev.lumen.domain.exercise.Exercise;
import dev.lumen.domain.exercise.ExerciseCategory;
import dev.lumen.domain.exercise.ExerciseRepository;
import dev.lumen.domain.moodcheckin.MoodEmotion;
import dev.lumen.domain.recommendation.MoodCheckInSubmittedEvent;
import dev.lumen.domain.recommendation.Recommendation;
import dev.lumen.domain.recommendation.RecommendationNotification;
import dev.lumen.domain.recommendation.RecommendationNotifier;
import dev.lumen.domain.recommendation.RecommendationRepository;
import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Rules are simple, deterministic and explainable on purpose (see the ADR in this
 * phase) — a domain this sensitive favors a mapping a person can read and defend over
 * a model that can't explain itself. Idempotency is enforced at the top of
 * handleMoodCheckInSubmitted so redelivering the same event (broker retry, manual
 * requeue) never produces duplicate recommendations.
 */
@Service
public class RecommendationService {

    private static final int LOW_ENERGY_THRESHOLD = 2;
    private static final BigDecimal SHORT_SLEEP_HOURS = new BigDecimal("6.0");

    private final ExerciseRepository exerciseRepository;
    private final RecommendationRepository recommendationRepository;
    private final RecommendationNotifier notifier;

    public RecommendationService(
            ExerciseRepository exerciseRepository,
            RecommendationRepository recommendationRepository,
            RecommendationNotifier notifier) {
        this.exerciseRepository = exerciseRepository;
        this.recommendationRepository = recommendationRepository;
        this.notifier = notifier;
    }

    @Transactional
    public void handleMoodCheckInSubmitted(MoodCheckInSubmittedEvent event) {
        if (recommendationRepository.existsByMoodCheckInId(event.moodCheckInId())) {
            return;
        }
        determineRecommendations(event).forEach((category, reason) -> recommend(event, category, reason));
    }

    private void recommend(MoodCheckInSubmittedEvent event, ExerciseCategory category, String reason) {
        List<Exercise> exercises = exerciseRepository.findByCategory(category);
        if (exercises.isEmpty()) {
            return;
        }
        Exercise exercise = exercises.get(Math.floorMod(event.moodCheckInId().hashCode(), exercises.size()));

        Recommendation recommendation = recommendationRepository.save(
                new Recommendation(event.userId(), event.moodCheckInId(), exercise.getId(), reason));

        notifier.notify(
                event.userId(),
                new RecommendationNotification(
                        recommendation.getId(),
                        exercise.getId(),
                        exercise.getName(),
                        exercise.getCategory(),
                        exercise.getDurationMinutes(),
                        reason));
    }

    /**
     * Order matters for which reason wins when two rules point at the same category
     * (e.g. low energy and sadness both suggest BEHAVIORAL_ACTIVATION) — the first
     * rule to touch a category keeps its reason, later rules only fill in gaps.
     */
    private Map<ExerciseCategory, String> determineRecommendations(MoodCheckInSubmittedEvent event) {
        Map<ExerciseCategory, String> recommendations = new LinkedHashMap<>();

        if (event.energyLevel() <= LOW_ENERGY_THRESHOLD || event.sleepHours().compareTo(SHORT_SLEEP_HOURS) < 0) {
            recommendations.putIfAbsent(
                    ExerciseCategory.BEHAVIORAL_ACTIVATION,
                    "You logged low energy or a short night's sleep — a small, achievable activity can help build"
                            + " momentum.");
            recommendations.putIfAbsent(
                    ExerciseCategory.SLEEP_HYGIENE,
                    "You logged low energy or a short night's sleep — a wind-down routine tonight may help.");
        }

        if (event.emotion() == MoodEmotion.ANXIOUS) {
            recommendations.putIfAbsent(
                    ExerciseCategory.BREATHING,
                    "You logged feeling anxious — a breathing exercise can help calm your nervous system right now.");
            recommendations.putIfAbsent(
                    ExerciseCategory.WALKING,
                    "You logged feeling anxious — a short walk can help release built-up tension.");
        } else if (event.emotion() == MoodEmotion.FRUSTRATED || event.emotion() == MoodEmotion.ANGRY) {
            recommendations.putIfAbsent(
                    ExerciseCategory.GROUNDING,
                    "You logged feeling frustrated — a grounding exercise can help bring your attention back to the"
                            + " present.");
            recommendations.putIfAbsent(
                    ExerciseCategory.WALKING,
                    "You logged feeling frustrated — physical movement can help release that energy.");
        } else if (event.emotion() == MoodEmotion.SAD) {
            recommendations.putIfAbsent(
                    ExerciseCategory.BEHAVIORAL_ACTIVATION,
                    "You logged feeling sad — one small action can sometimes shift how the rest of the day feels.");
        }

        return recommendations;
    }
}
