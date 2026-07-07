package dev.lumen.application.wearable;

import dev.lumen.domain.wearable.WearableReadingType;
import dev.lumen.domain.wearable.WearableSourceType;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record WearableReadingResponse(
        UUID id, WearableReadingType type, BigDecimal value, Instant recordedAt, WearableSourceType source) {
}
