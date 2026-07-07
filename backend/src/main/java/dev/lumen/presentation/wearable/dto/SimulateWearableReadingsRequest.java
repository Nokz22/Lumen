package dev.lumen.presentation.wearable.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;

public record SimulateWearableReadingsRequest(@Min(1) @Max(90) int days) {
}
