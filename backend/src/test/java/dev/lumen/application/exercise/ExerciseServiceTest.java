package dev.lumen.application.exercise;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import dev.lumen.domain.exercise.Exercise;
import dev.lumen.domain.exercise.ExerciseCategory;
import dev.lumen.domain.exercise.ExerciseIntensity;
import dev.lumen.domain.exercise.ExerciseRepository;
import java.util.List;
import org.junit.jupiter.api.Test;

class ExerciseServiceTest {

    private final ExerciseRepository exerciseRepository = mock(ExerciseRepository.class);
    private final ExerciseService service = new ExerciseService(exerciseRepository);

    @Test
    void shouldMapBreathingPatternForBreathingExercises() {
        Exercise breathing = new Exercise(
                ExerciseCategory.BREATHING, "Box Breathing", 5, ExerciseIntensity.LOW, "rationale",
                4, 4, 4, 4);
        when(exerciseRepository.findAll()).thenReturn(List.of(breathing));

        List<ExerciseResponse> responses = service.listAll();

        ExerciseResponse response = responses.get(0);
        assertThat(response.inhaleSeconds()).isEqualTo(4);
        assertThat(response.holdAfterInhaleSeconds()).isEqualTo(4);
        assertThat(response.exhaleSeconds()).isEqualTo(4);
        assertThat(response.holdAfterExhaleSeconds()).isEqualTo(4);
    }

    @Test
    void shouldLeaveBreathingPatternNullForNonBreathingExercises() {
        Exercise walk = new Exercise(
                ExerciseCategory.WALKING, "10-Minute Outdoor Walk", 10, ExerciseIntensity.LOW, "rationale");
        when(exerciseRepository.findAll()).thenReturn(List.of(walk));

        List<ExerciseResponse> responses = service.listAll();

        ExerciseResponse response = responses.get(0);
        assertThat(response.inhaleSeconds()).isNull();
        assertThat(response.holdAfterInhaleSeconds()).isNull();
        assertThat(response.exhaleSeconds()).isNull();
        assertThat(response.holdAfterExhaleSeconds()).isNull();
    }
}
