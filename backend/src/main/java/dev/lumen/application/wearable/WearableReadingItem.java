package dev.lumen.application.wearable;

import dev.lumen.domain.wearable.WearableReadingType;
import java.math.BigDecimal;
import java.time.Instant;

public record WearableReadingItem(WearableReadingType type, BigDecimal value, Instant recordedAt) {
}
