package dev.lumen.infrastructure.wearable;

import dev.lumen.domain.wearable.WearableReading;
import dev.lumen.domain.wearable.WearableReadingType;
import dev.lumen.domain.wearable.WearableSource;
import dev.lumen.domain.wearable.WearableSourceType;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.UUID;
import org.springframework.stereotype.Component;

/**
 * Generates one day's worth of readings at a time. Each day's HRV/heart rate/step
 * count is nudged by the previous night's sleep deficit (less sleep than the 7h
 * baseline pushes HRV and steps down, heart rate up) plus Gaussian noise — a mild,
 * noisy correlation, not a deterministic one. A simulator with a perfect correlation
 * would misrepresent what "correlation" means in the insight it is meant to
 * demonstrate; one with no pattern at all would never have anything to show
 * (ADR-0008).
 */
@Component
class SimulatorWearableSource implements WearableSource {

    private static final double BASELINE_SLEEP_HOURS = 7.0;
    private static final double SLEEP_NOISE_STD_DEV = 1.2;
    private static final double MIN_SLEEP_HOURS = 3.5;
    private static final double MAX_SLEEP_HOURS = 9.5;

    private static final double BASELINE_HRV = 45.0;
    private static final double HRV_SLEEP_SENSITIVITY = 3.0;
    private static final double HRV_NOISE_STD_DEV = 6.0;
    private static final double MIN_HRV = 15.0;
    private static final double MAX_HRV = 80.0;

    private static final double BASELINE_HEART_RATE = 65.0;
    private static final double HEART_RATE_SLEEP_SENSITIVITY = 1.5;
    private static final double HEART_RATE_NOISE_STD_DEV = 5.0;
    private static final double MIN_HEART_RATE = 45.0;
    private static final double MAX_HEART_RATE = 100.0;

    private static final double BASELINE_STEPS = 6000.0;
    private static final double STEPS_SLEEP_SENSITIVITY = 400.0;
    private static final double STEPS_NOISE_STD_DEV = 1500.0;
    private static final double MIN_STEPS = 500.0;
    private static final double MAX_STEPS = 15_000.0;

    private static final int READING_HOUR_UTC = 7;

    @Override
    public WearableSourceType getSourceType() {
        return WearableSourceType.SIMULATOR;
    }

    @Override
    public List<WearableReading> fetch(UUID userId, Instant since, Instant until) {
        Random random = new Random();
        List<WearableReading> readings = new ArrayList<>();

        LocalDate date = LocalDate.ofInstant(since, ZoneOffset.UTC);
        LocalDate endDate = LocalDate.ofInstant(until, ZoneOffset.UTC);
        double previousSleepHours = BASELINE_SLEEP_HOURS;

        while (!date.isAfter(endDate)) {
            double sleepHours = clamp(
                    BASELINE_SLEEP_HOURS + gaussian(random) * SLEEP_NOISE_STD_DEV, MIN_SLEEP_HOURS, MAX_SLEEP_HOURS);
            double sleepDeficit = BASELINE_SLEEP_HOURS - previousSleepHours;

            double hrv = clamp(
                    BASELINE_HRV - sleepDeficit * HRV_SLEEP_SENSITIVITY + gaussian(random) * HRV_NOISE_STD_DEV,
                    MIN_HRV,
                    MAX_HRV);
            double heartRate = clamp(
                    BASELINE_HEART_RATE
                            + sleepDeficit * HEART_RATE_SLEEP_SENSITIVITY
                            + gaussian(random) * HEART_RATE_NOISE_STD_DEV,
                    MIN_HEART_RATE,
                    MAX_HEART_RATE);
            double steps = clamp(
                    BASELINE_STEPS - sleepDeficit * STEPS_SLEEP_SENSITIVITY + gaussian(random) * STEPS_NOISE_STD_DEV,
                    MIN_STEPS,
                    MAX_STEPS);

            Instant recordedAt = date.atTime(READING_HOUR_UTC, 0).toInstant(ZoneOffset.UTC);
            readings.add(reading(userId, WearableReadingType.SLEEP_DURATION, sleepHours, recordedAt));
            readings.add(reading(userId, WearableReadingType.HRV, hrv, recordedAt));
            readings.add(reading(userId, WearableReadingType.HEART_RATE, heartRate, recordedAt));
            readings.add(reading(userId, WearableReadingType.STEPS, steps, recordedAt));

            previousSleepHours = sleepHours;
            date = date.plusDays(1);
        }

        return readings;
    }

    private WearableReading reading(UUID userId, WearableReadingType type, double value, Instant recordedAt) {
        return new WearableReading(
                userId,
                type,
                BigDecimal.valueOf(value).setScale(2, RoundingMode.HALF_UP),
                recordedAt,
                WearableSourceType.SIMULATOR);
    }

    private double gaussian(Random random) {
        return random.nextGaussian();
    }

    private double clamp(double value, double min, double max) {
        return Math.max(min, Math.min(max, value));
    }
}
