package dev.lumen.presentation.assessment;

import dev.lumen.application.assessment.AssessmentService;
import dev.lumen.application.assessment.AssessmentSubmissionResult;
import dev.lumen.application.assessment.AssessmentSummaryResponse;
import dev.lumen.domain.assessment.AssessmentType;
import dev.lumen.presentation.assessment.dto.SubmitAssessmentRequest;
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

/**
 * submit() returns either a ScoredAssessmentResult or a CrisisTriggeredResult
 * (AssessmentSubmissionResult) — the two shapes are deliberately different JSON
 * payloads so a crisis response can never be mistaken for a normal score.
 */
@RestController
@RequestMapping("/api/v1/users/{userId}/assessments")
@PreAuthorize("#userId == authentication.principal.userId()")
public class AssessmentController {

    private final AssessmentService assessmentService;

    public AssessmentController(AssessmentService assessmentService) {
        this.assessmentService = assessmentService;
    }

    @PostMapping("/{assessmentType}")
    public AssessmentSubmissionResult submit(
            @PathVariable UUID userId,
            @PathVariable AssessmentType assessmentType,
            @Valid @RequestBody SubmitAssessmentRequest request) {
        return assessmentService.submit(userId, assessmentType, request.responses());
    }

    @GetMapping
    public List<AssessmentSummaryResponse> history(@PathVariable UUID userId) {
        return assessmentService.getHistory(userId);
    }
}
