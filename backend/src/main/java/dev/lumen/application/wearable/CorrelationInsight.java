package dev.lumen.application.wearable;

import dev.lumen.domain.wearable.WearableReadingType;

/**
 * description is always phrased as an observed pattern, never a cause — "on days
 * with X, you tended to report Y", never "X causes Y" (ADR-0008, same ethical
 * boundary as ADR-0001).
 */
public record CorrelationInsight(WearableReadingType metric, double correlationCoefficient, String description) {
}
