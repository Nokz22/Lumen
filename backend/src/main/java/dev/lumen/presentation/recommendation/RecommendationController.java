package dev.lumen.presentation.recommendation;

import dev.lumen.application.recommendation.RecommendationService;
import dev.lumen.application.recommendation.RecommendationSummaryResponse;
import dev.lumen.presentation.shared.PageParameters;
import dev.lumen.presentation.shared.PageResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.UUID;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Used for the initial page load; live updates after that arrive over WebSocket
 * (see infrastructure.websocket.WebSocketRecommendationNotifier) rather than polling.
 */
@Tag(
        name = "Recommendations",
        description = "Deterministic, rule-based suggestions, each carrying the explanation of why it was suggested"
                + " (ADR-0007). Also pushed live over WebSocket.")
@RestController
@RequestMapping("/api/v1/users/{userId}/recommendations")
@PreAuthorize("#userId == authentication.principal.userId()")
public class RecommendationController {

    private final RecommendationService recommendationService;

    public RecommendationController(RecommendationService recommendationService) {
        this.recommendationService = recommendationService;
    }

    @GetMapping
    public PageResponse<RecommendationSummaryResponse> history(
            @PathVariable UUID userId, PageParameters pageParameters) {
        return PageResponse.from(recommendationService.getHistory(userId, pageParameters.toPageQuery()));
    }
}
