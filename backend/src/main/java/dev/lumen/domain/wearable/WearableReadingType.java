package dev.lumen.domain.wearable;

/**
 * Extensible on purpose — new physiological metrics can be added without changing the
 * WearableReading shape (generic type/value/recordedAt row, see WearableReading).
 */
public enum WearableReadingType {
    HEART_RATE,
    HRV,
    SLEEP_DURATION,
    STEPS
}
