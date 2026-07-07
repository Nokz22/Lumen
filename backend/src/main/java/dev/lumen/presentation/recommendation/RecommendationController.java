package dev.lumen.presentation.recommendation;

import dev.lumen.application.recommendation.RecommendationService;
import dev.lumen.application.recommendation.RecommendationSummaryResponse;
import java.util.List;
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
@RestController
@RequestMapping("/api/v1/users/{userId}/recommendations")
@PreAuthorize("#userId == authentication.principal.userId()")
public class RecommendationController {

    private final RecommendationService recommendationService;

    public RecommendationController(RecommendationService recommendationService) {
        this.recommendationService = recommendationService;
    }

    @GetMapping
    public List<RecommendationSummaryResponse> history(@PathVariable UUID userId) {
        return recommendationService.getHistory(userId);
    }
}
