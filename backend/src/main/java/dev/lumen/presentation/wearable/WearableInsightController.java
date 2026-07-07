package dev.lumen.presentation.wearable;

import dev.lumen.application.wearable.CorrelationInsight;
import dev.lumen.application.wearable.WearableInsightService;
import java.util.List;
import java.util.UUID;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/users/{userId}/wearable-insights")
@PreAuthorize("#userId == authentication.principal.userId()")
public class WearableInsightController {

    private final WearableInsightService wearableInsightService;

    public WearableInsightController(WearableInsightService wearableInsightService) {
        this.wearableInsightService = wearableInsightService;
    }

    @GetMapping
    public List<CorrelationInsight> insights(@PathVariable UUID userId) {
        return wearableInsightService.computeInsights(userId);
    }
}
