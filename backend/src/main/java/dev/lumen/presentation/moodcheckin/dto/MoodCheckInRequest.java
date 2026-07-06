package dev.lumen.presentation.moodcheckin.dto;

import dev.lumen.domain.moodcheckin.MoodEmotion;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;

public record MoodCheckInRequest(
        @NotNull MoodEmotion emotion,
        @Min(1) @Max(5) int energyLevel,
        @NotNull @DecimalMin("0.0") @DecimalMax("24.0") BigDecimal sleepHours,
        @Min(1) @Max(5) int sleepQuality,
        @Size(max = 1000) String note) {
}
