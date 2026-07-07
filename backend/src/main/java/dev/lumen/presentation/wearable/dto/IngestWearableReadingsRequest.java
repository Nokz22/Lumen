package dev.lumen.presentation.wearable.dto;

import dev.lumen.domain.wearable.WearableReadingType;
import dev.lumen.domain.wearable.WearableSourceType;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

public record IngestWearableReadingsRequest(
        @NotNull WearableSourceType source, @NotEmpty @Valid List<ReadingItem> items) {

    public record ReadingItem(
            @NotNull WearableReadingType type, @NotNull BigDecimal value, @NotNull Instant recordedAt) {
    }
}
