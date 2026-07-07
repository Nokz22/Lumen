package dev.lumen.presentation.exercise;

import dev.lumen.application.exercise.ExerciseResponse;
import dev.lumen.application.exercise.ExerciseService;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Not self-scoped — the library is shared reference content, not per-user data (any
 * authenticated user may browse it, matching how CrisisResource content is embedded
 * directly in the assessment response rather than access-controlled per user).
 */
@RestController
@RequestMapping("/api/v1/exercises")
public class ExerciseController {

    private final ExerciseService exerciseService;

    public ExerciseController(ExerciseService exerciseService) {
        this.exerciseService = exerciseService;
    }

    @GetMapping
    public List<ExerciseResponse> list() {
        return exerciseService.listAll();
    }
}
