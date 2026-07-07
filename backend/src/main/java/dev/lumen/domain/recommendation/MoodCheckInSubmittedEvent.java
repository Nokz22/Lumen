package dev.lumen.domain.recommendation;

import dev.lumen.domain.moodcheckin.MoodEmotion;
import java.io.Serializable;
import java.math.BigDecimal;
import java.util.UUID;

/**
 * Serialized onto the RabbitMQ queue as-is (Jackson, see infrastructure.messaging) —
 * carries only what the rule engine needs, not the full MoodCheckIn entity.
 */
public record MoodCheckInSubmittedEvent(
        UUID moodCheckInId,
        UUID userId,
        MoodEmotion emotion,
        int energyLevel,
        BigDecimal sleepHours,
        int sleepQuality)
        implements Serializable {
}
