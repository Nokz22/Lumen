package dev.lumen.presentation.exercise;

import dev.lumen.application.exercise.ExerciseCompletionResponse;
import dev.lumen.application.exercise.ExerciseCompletionService;
import dev.lumen.presentation.exercise.dto.CompleteExerciseRequest;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(
        name = "Exercise completions",
        description = "Records that an exercise was actually done, closing the self-care loop.")
@RestController
@RequestMapping("/api/v1/users/{userId}/exercise-completions")
@PreAuthorize("#userId == authentication.principal.userId()")
public class ExerciseCompletionController {

    private final ExerciseCompletionService completionService;

    public ExerciseCompletionController(ExerciseCompletionService completionService) {
        this.completionService = completionService;
    }

    @PostMapping
    public ExerciseCompletionResponse complete(
            @PathVariable UUID userId, @Valid @RequestBody CompleteExerciseRequest request) {
        return completionService.complete(userId, request.exerciseId(), request.recommendationId());
    }

    @GetMapping
    public List<ExerciseCompletionResponse> history(@PathVariable UUID userId) {
        return completionService.getHistory(userId);
    }
}
