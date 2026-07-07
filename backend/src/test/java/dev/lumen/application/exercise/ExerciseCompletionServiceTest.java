package dev.lumen.application.exercise;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import dev.lumen.domain.exercise.Exercise;
import dev.lumen.domain.exercise.ExerciseCategory;
import dev.lumen.domain.exercise.ExerciseCompletion;
import dev.lumen.domain.exercise.ExerciseCompletionRepository;
import dev.lumen.domain.exercise.ExerciseIntensity;
import dev.lumen.domain.exercise.ExerciseNotFoundException;
import dev.lumen.domain.exercise.ExerciseRepository;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class ExerciseCompletionServiceTest {

    private final ExerciseCompletionRepository completionRepository = mock(ExerciseCompletionRepository.class);
    private final ExerciseRepository exerciseRepository = mock(ExerciseRepository.class);
    private final ExerciseCompletionService service =
            new ExerciseCompletionService(completionRepository, exerciseRepository);

    private final UUID userId = UUID.randomUUID();
    private Exercise exercise;

    @BeforeEach
    void setUp() {
        exercise = new Exercise(ExerciseCategory.BREATHING, "Box Breathing", 5, ExerciseIntensity.LOW, "rationale");
        when(exerciseRepository.findById(exercise.getId())).thenReturn(Optional.of(exercise));
        when(completionRepository.save(any(ExerciseCompletion.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
    }

    @Test
    void shouldCompleteExerciseLinkedToARecommendation() {
        UUID recommendationId = UUID.randomUUID();

        ExerciseCompletionResponse response = service.complete(userId, exercise.getId(), recommendationId);

        assertThat(response.exerciseId()).isEqualTo(exercise.getId());
        assertThat(response.recommendationId()).isEqualTo(recommendationId);
    }

    @Test
    void shouldCompleteExerciseWithoutARecommendation() {
        ExerciseCompletionResponse response = service.complete(userId, exercise.getId(), null);

        assertThat(response.recommendationId()).isNull();
    }

    @Test
    void shouldThrowWhenExerciseDoesNotExist() {
        UUID unknownExerciseId = UUID.randomUUID();
        when(exerciseRepository.findById(unknownExerciseId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.complete(userId, unknownExerciseId, null))
                .isInstanceOf(ExerciseNotFoundException.class);
    }
}
