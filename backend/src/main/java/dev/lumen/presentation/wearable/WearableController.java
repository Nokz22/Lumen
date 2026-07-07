package dev.lumen.presentation.wearable;

import dev.lumen.application.wearable.WearableIngestionService;
import dev.lumen.application.wearable.WearableReadingItem;
import dev.lumen.application.wearable.WearableReadingResponse;
import dev.lumen.presentation.wearable.dto.IngestWearableReadingsRequest;
import dev.lumen.presentation.wearable.dto.SimulateWearableReadingsRequest;
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

@RestController
@RequestMapping("/api/v1/users/{userId}/wearable-readings")
@PreAuthorize("#userId == authentication.principal.userId()")
public class WearableController {

    private final WearableIngestionService wearableIngestionService;

    public WearableController(WearableIngestionService wearableIngestionService) {
        this.wearableIngestionService = wearableIngestionService;
    }

    @PostMapping
    public List<WearableReadingResponse> ingest(
            @PathVariable UUID userId, @Valid @RequestBody IngestWearableReadingsRequest request) {
        List<WearableReadingItem> items = request.items().stream()
                .map(item -> new WearableReadingItem(item.type(), item.value(), item.recordedAt()))
                .toList();
        return wearableIngestionService.ingest(userId, request.source(), items);
    }

    @PostMapping("/simulate")
    public List<WearableReadingResponse> simulate(
            @PathVariable UUID userId, @Valid @RequestBody SimulateWearableReadingsRequest request) {
        return wearableIngestionService.simulate(userId, request.days());
    }

    @GetMapping
    public List<WearableReadingResponse> history(@PathVariable UUID userId) {
        return wearableIngestionService.getHistory(userId);
    }
}
