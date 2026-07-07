package dev.lumen.application.recommendation;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import dev.lumen.domain.exercise.Exercise;
import dev.lumen.domain.exercise.ExerciseCategory;
import dev.lumen.domain.exercise.ExerciseIntensity;
import dev.lumen.domain.exercise.ExerciseRepository;
import dev.lumen.domain.moodcheckin.MoodEmotion;
import dev.lumen.domain.recommendation.MoodCheckInSubmittedEvent;
import dev.lumen.domain.recommendation.Recommendation;
import dev.lumen.domain.recommendation.RecommendationNotification;
import dev.lumen.domain.recommendation.RecommendationNotifier;
import dev.lumen.domain.recommendation.RecommendationRepository;
import java.math.BigDecimal;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

class RecommendationServiceTest {

    private static final List<String> DIAGNOSTIC_TERMS =
            List.of("depress", "disorder", "anxiety disorder", "diagnos", "clinical");

    private final ExerciseRepository exerciseRepository = mock(ExerciseRepository.class);
    private final RecommendationRepository recommendationRepository = mock(RecommendationRepository.class);
    private final RecommendationNotifier notifier = mock(RecommendationNotifier.class);
    private final RecommendationService service =
            new RecommendationService(exerciseRepository, recommendationRepository, notifier);

    private final UUID userId = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        when(recommendationRepository.existsByMoodCheckInId(any())).thenReturn(false);
        when(recommendationRepository.save(any(Recommendation.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        for (ExerciseCategory category : ExerciseCategory.values()) {
            when(exerciseRepository.findByCategory(category)).thenReturn(List.of(exerciseFor(category)));
        }
    }

    private Exercise exerciseFor(ExerciseCategory category) {
        return new Exercise(category, category.name() + " exercise", 5, ExerciseIntensity.LOW, "rationale");
    }

    private MoodCheckInSubmittedEvent event(
            UUID moodCheckInId, MoodEmotion emotion, int energyLevel, BigDecimal sleepHours) {
        return new MoodCheckInSubmittedEvent(moodCheckInId, userId, emotion, energyLevel, sleepHours, 3);
    }

    @Test
    void shouldRecommendBehavioralActivationAndSleepHygieneForLowEnergy() {
        service.handleMoodCheckInSubmitted(
                event(UUID.randomUUID(), MoodEmotion.NEUTRAL, 1, new BigDecimal("8.0")));

        verify(recommendationRepository, times(2)).save(any(Recommendation.class));
        verify(notifier).notify(any(), argThatCategory(ExerciseCategory.BEHAVIORAL_ACTIVATION));
        verify(notifier).notify(any(), argThatCategory(ExerciseCategory.SLEEP_HYGIENE));
    }

    @Test
    void shouldRecommendBehavioralActivationAndSleepHygieneForShortSleep() {
        service.handleMoodCheckInSubmitted(
                event(UUID.randomUUID(), MoodEmotion.NEUTRAL, 4, new BigDecimal("4.0")));

        verify(recommendationRepository, times(2)).save(any(Recommendation.class));
    }

    @Test
    void shouldRecommendBreathingAndWalkingForAnxious() {
        service.handleMoodCheckInSubmitted(
                event(UUID.randomUUID(), MoodEmotion.ANXIOUS, 4, new BigDecimal("8.0")));

        verify(notifier).notify(any(), argThatCategory(ExerciseCategory.BREATHING));
        verify(notifier).notify(any(), argThatCategory(ExerciseCategory.WALKING));
    }

    @Test
    void shouldRecommendGroundingAndWalkingForFrustratedOrAngry() {
        service.handleMoodCheckInSubmitted(
                event(UUID.randomUUID(), MoodEmotion.FRUSTRATED, 4, new BigDecimal("8.0")));

        verify(notifier).notify(any(), argThatCategory(ExerciseCategory.GROUNDING));
        verify(notifier).notify(any(), argThatCategory(ExerciseCategory.WALKING));
    }

    @Test
    void shouldRecommendBehavioralActivationForSad() {
        service.handleMoodCheckInSubmitted(event(UUID.randomUUID(), MoodEmotion.SAD, 4, new BigDecimal("8.0")));

        verify(notifier).notify(any(), argThatCategory(ExerciseCategory.BEHAVIORAL_ACTIVATION));
    }

    @Test
    void shouldRecommendNothingForHappyWithNormalEnergyAndSleep() {
        service.handleMoodCheckInSubmitted(event(UUID.randomUUID(), MoodEmotion.HAPPY, 4, new BigDecimal("8.0")));

        verify(recommendationRepository, never()).save(any());
        verify(notifier, never()).notify(any(), any());
    }

    @Test
    void shouldBeIdempotentWhenSameMoodCheckInIsProcessedTwice() {
        UUID moodCheckInId = UUID.randomUUID();
        when(recommendationRepository.existsByMoodCheckInId(moodCheckInId)).thenReturn(true);

        service.handleMoodCheckInSubmitted(event(moodCheckInId, MoodEmotion.ANXIOUS, 4, new BigDecimal("8.0")));

        verify(recommendationRepository, never()).save(any());
        verify(notifier, never()).notify(any(), any());
    }

    @Test
    void shouldNeverUseDiagnosticVocabularyInGeneratedReasons() {
        for (MoodEmotion emotion : MoodEmotion.values()) {
            service.handleMoodCheckInSubmitted(event(UUID.randomUUID(), emotion, 1, new BigDecimal("3.0")));
        }

        List<String> capturedReasons = capturedReasons();
        assertThat(capturedReasons).isNotEmpty();
        for (String reason : capturedReasons) {
            String normalized = reason.toLowerCase(Locale.ROOT);
            for (String term : DIAGNOSTIC_TERMS) {
                assertThat(normalized).doesNotContain(term);
            }
        }
    }

    private List<String> capturedReasons() {
        ArgumentCaptor<Recommendation> captor = ArgumentCaptor.forClass(Recommendation.class);
        verify(recommendationRepository, atLeastOnce()).save(captor.capture());
        return captor.getAllValues().stream().map(Recommendation::getReason).toList();
    }

    private RecommendationNotification argThatCategory(ExerciseCategory category) {
        return argThat(notification -> notification != null && notification.category() == category);
    }
}
