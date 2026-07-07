package dev.lumen.presentation.assessment.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotEmpty;
import java.util.List;

/**
 * Item count is instrument-specific (9 for PHQ-9, 7 for GAD-7), so it is validated in
 * AssessmentService against the {assessmentType} path variable rather than here.
 */
public record SubmitAssessmentRequest(@NotEmpty List<@Min(0) @Max(3) Integer> responses) {
}
