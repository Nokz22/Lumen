package dev.lumen.application.moodcheckin;

import dev.lumen.domain.moodcheckin.MoodEmotion;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

public record MoodCheckInResponse(
        UUID id,
        MoodEmotion emotion,
        int energyLevel,
        BigDecimal sleepHours,
        int sleepQuality,
        String note,
        LocalDate checkInDate,
        Instant createdAt) {
}
