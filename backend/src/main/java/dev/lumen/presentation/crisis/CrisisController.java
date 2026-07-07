package dev.lumen.presentation.crisis;

import dev.lumen.application.assessment.ScoredAssessmentResult;
import dev.lumen.application.crisis.CrisisService;
import dev.lumen.presentation.crisis.dto.AcknowledgeResponse;
import java.util.Optional;
import java.util.UUID;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/users/{userId}/risk-events/{riskEventId}")
@PreAuthorize("#userId == authentication.principal.userId()")
public class CrisisController {

    private final CrisisService crisisService;

    public CrisisController(CrisisService crisisService) {
        this.crisisService = crisisService;
    }

    @PostMapping("/acknowledge")
    public AcknowledgeResponse acknowledge(@PathVariable UUID userId, @PathVariable UUID riskEventId) {
        Optional<ScoredAssessmentResult> result = crisisService.acknowledge(userId, riskEventId);
        return new AcknowledgeResponse(
                result.map(ScoredAssessmentResult::totalScore).orElse(null),
                result.map(ScoredAssessmentResult::wellbeingBand).orElse(null));
    }
}
