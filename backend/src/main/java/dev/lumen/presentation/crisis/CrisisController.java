package dev.lumen.presentation.crisis;

import dev.lumen.application.assessment.ScoredAssessmentResult;
import dev.lumen.application.crisis.CrisisService;
import dev.lumen.presentation.crisis.dto.AcknowledgeResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.Operation;
import java.util.Optional;
import java.util.UUID;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(
        name = "Crisis flow",
        description = "The safety-critical path: RiskEvent acknowledgment. A RiskEvent can never reach ACKNOWLEDGED"
                + " without passing through RESOURCES_PRESENTED (ADR-0006).")
@RestController
@RequestMapping("/api/v1/users/{userId}/risk-events/{riskEventId}")
@PreAuthorize("#userId == authentication.principal.userId()")
public class CrisisController {

    private final CrisisService crisisService;

    public CrisisController(CrisisService crisisService) {
        this.crisisService = crisisService;
    }

    @PostMapping("/acknowledge")
    @Operation(
            summary = "Acknowledge that crisis resources were seen",
            description = "Moves the RiskEvent from RESOURCES_PRESENTED to ACKNOWLEDGED; any other source state is"
                    + " rejected. When the event interrupted an instrument, the score withheld at submission time is"
                    + " returned now — after, and only after, the person has seen the resources.")
    public AcknowledgeResponse acknowledge(@PathVariable UUID userId, @PathVariable UUID riskEventId) {
        Optional<ScoredAssessmentResult> result = crisisService.acknowledge(userId, riskEventId);
        return new AcknowledgeResponse(
                result.map(ScoredAssessmentResult::totalScore).orElse(null),
                result.map(ScoredAssessmentResult::wellbeingBand).orElse(null));
    }
}
