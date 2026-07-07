package dev.lumen.application.wearable;

import dev.lumen.domain.moodcheckin.MoodCheckIn;
import dev.lumen.domain.moodcheckin.MoodCheckInRepository;
import dev.lumen.domain.user.UserNotFoundException;
import dev.lumen.domain.user.UserRepository;
import dev.lumen.domain.wearable.WearableReading;
import dev.lumen.domain.wearable.WearableReadingRepository;
import dev.lumen.domain.wearable.WearableReadingType;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Insight, never verdict (project-brief.md §7): an insight is only returned when the
 * Pearson correlation over paired daily aggregates is at least MIN_ABSOLUTE_CORRELATION
 * and there are at least MIN_SAMPLE_SIZE pairs — small samples produce spurious
 * correlations, and a single-digit sample size is not something worth surfacing to
 * a person as a pattern in their own life.
 */
@Service
public class WearableInsightService {

    private static final double MIN_ABSOLUTE_CORRELATION = 0.3;
    private static final int MIN_SAMPLE_SIZE = 5;
    private static final int LOOKBACK_DAYS = 30;

    private final WearableReadingRepository wearableReadingRepository;
    private final MoodCheckInRepository moodCheckInRepository;
    private final UserRepository userRepository;

    public WearableInsightService(
            WearableReadingRepository wearableReadingRepository,
            MoodCheckInRepository moodCheckInRepository,
            UserRepository userRepository) {
        this.wearableReadingRepository = wearableReadingRepository;
        this.moodCheckInRepository = moodCheckInRepository;
        this.userRepository = userRepository;
    }

    @Transactional(readOnly = true)
    public List<CorrelationInsight> computeInsights(UUID userId) {
        if (userRepository.findById(userId).isEmpty()) {
            throw new UserNotFoundException(userId);
        }

        Instant since = Instant.now().minus(LOOKBACK_DAYS, ChronoUnit.DAYS);
        LocalDate sinceDate = LocalDate.ofInstant(since, ZoneOffset.UTC);

        List<WearableReading> readings =
                wearableReadingRepository.findByUserIdAndRecordedAtBetween(userId, since, Instant.now());
        Map<LocalDate, Integer> energyByDay = moodCheckInRepository.findByUserIdOrderByCheckInDateDesc(userId).stream()
                .filter(checkIn -> !checkIn.getCheckInDate().isBefore(sinceDate))
                .collect(Collectors.toMap(MoodCheckIn::getCheckInDate, MoodCheckIn::getEnergyLevel, (a, b) -> a));

        List<CorrelationInsight> insights = new ArrayList<>();
        evaluate(WearableReadingType.SLEEP_DURATION, readings, energyByDay, 1).ifPresent(insights::add);
        evaluate(WearableReadingType.HRV, readings, energyByDay, 0).ifPresent(insights::add);
        evaluate(WearableReadingType.STEPS, readings, energyByDay, 0).ifPresent(insights::add);
        return insights;
    }

    private Optional<CorrelationInsight> evaluate(
            WearableReadingType metric,
            List<WearableReading> readings,
            Map<LocalDate, Integer> energyByDay,
            int dayOffset) {
        Map<LocalDate, Double> metricByDay = averageByDay(readings, metric);

        List<Double> xs = new ArrayList<>();
        List<Double> ys = new ArrayList<>();
        for (Map.Entry<LocalDate, Double> entry : metricByDay.entrySet()) {
            Integer energy = energyByDay.get(entry.getKey().plusDays(dayOffset));
            if (energy != null) {
                xs.add(entry.getValue());
                ys.add(energy.doubleValue());
            }
        }

        if (xs.size() < MIN_SAMPLE_SIZE) {
            return Optional.empty();
        }
        double r = pearson(xs, ys);
        if (Math.abs(r) < MIN_ABSOLUTE_CORRELATION) {
            return Optional.empty();
        }
        return Optional.of(new CorrelationInsight(metric, r, describe(metric, dayOffset, r)));
    }

    private Map<LocalDate, Double> averageByDay(List<WearableReading> readings, WearableReadingType type) {
        Map<LocalDate, List<Double>> valuesByDay = new LinkedHashMap<>();
        for (WearableReading reading : readings) {
            if (reading.getType() != type) {
                continue;
            }
            LocalDate day = LocalDate.ofInstant(reading.getRecordedAt(), ZoneOffset.UTC);
            valuesByDay.computeIfAbsent(day, d -> new ArrayList<>()).add(reading.getValue().doubleValue());
        }
        Map<LocalDate, Double> averages = new LinkedHashMap<>();
        valuesByDay.forEach((day, values) -> averages.put(
                day, values.stream().mapToDouble(Double::doubleValue).average().orElse(0)));
        return averages;
    }

    /** Pure function: given a known input, the correlation coefficient is deterministic and testable in isolation. */
    static double pearson(List<Double> xs, List<Double> ys) {
        int n = xs.size();
        double meanX = xs.stream().mapToDouble(Double::doubleValue).average().orElse(0);
        double meanY = ys.stream().mapToDouble(Double::doubleValue).average().orElse(0);

        double covariance = 0;
        double varianceX = 0;
        double varianceY = 0;
        for (int i = 0; i < n; i++) {
            double dx = xs.get(i) - meanX;
            double dy = ys.get(i) - meanY;
            covariance += dx * dy;
            varianceX += dx * dx;
            varianceY += dy * dy;
        }

        if (varianceX == 0 || varianceY == 0) {
            return 0;
        }
        return covariance / Math.sqrt(varianceX * varianceY);
    }

    private String describe(WearableReadingType metric, int dayOffset, double correlation) {
        String metricLabel = switch (metric) {
            case SLEEP_DURATION -> "sleep";
            case HRV -> "HRV";
            case HEART_RATE -> "heart rate";
            case STEPS -> "step count";
        };
        String timing = dayOffset == 0 ? "that same day" : "the following day";
        String pattern = correlation > 0
                ? "tended to also report higher energy " + timing
                : "tended to report lower energy " + timing;
        return "On days with higher " + metricLabel + ", you " + pattern + ".";
    }
}
