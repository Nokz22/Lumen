package dev.lumen.application.wearable;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import dev.lumen.domain.moodcheckin.MoodCheckIn;
import dev.lumen.domain.moodcheckin.MoodCheckInRepository;
import dev.lumen.domain.moodcheckin.MoodEmotion;
import dev.lumen.domain.user.Role;
import dev.lumen.domain.user.User;
import dev.lumen.domain.user.UserRepository;
import dev.lumen.domain.wearable.WearableReading;
import dev.lumen.domain.wearable.WearableReadingRepository;
import dev.lumen.domain.wearable.WearableReadingType;
import dev.lumen.domain.wearable.WearableSourceType;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class WearableInsightServiceTest {

    private static final List<String> DIAGNOSTIC_TERMS =
            List.of("depress", "disorder", "diagnos", "clinical", "causes");

    private final WearableReadingRepository wearableReadingRepository = mock(WearableReadingRepository.class);
    private final MoodCheckInRepository moodCheckInRepository = mock(MoodCheckInRepository.class);
    private final UserRepository userRepository = mock(UserRepository.class);
    private final WearableInsightService service =
            new WearableInsightService(wearableReadingRepository, moodCheckInRepository, userRepository);

    private final UUID userId = UUID.randomUUID();
    private User user;

    @BeforeEach
    void setUp() {
        user = new User("jane@lumen.dev", "hashed-password", "Jane", "en", "PT", LocalDate.of(1990, 1, 1), Role.USER);
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
    }

    @Test
    void shouldReturnSleepInsightWhenPatternIsClearAndSampleIsLargeEnough() {
        LocalDate today = LocalDate.now(ZoneOffset.UTC);
        LocalDate startDay = today.minusDays(7);

        List<WearableReading> readings = new ArrayList<>();
        List<MoodCheckIn> checkIns = new ArrayList<>();
        for (int i = 0; i < 6; i++) {
            LocalDate sleepDay = startDay.plusDays(i);
            boolean highSleepNight = i % 2 == 0;
            BigDecimal sleepHours = highSleepNight ? new BigDecimal("8.5") : new BigDecimal("5.5");
            readings.add(new WearableReading(
                    userId,
                    WearableReadingType.SLEEP_DURATION,
                    sleepHours,
                    sleepDay.atTime(7, 0).toInstant(ZoneOffset.UTC),
                    WearableSourceType.SIMULATOR));

            int energyLevel = highSleepNight ? 5 : 2;
            checkIns.add(new MoodCheckIn(
                    user, MoodEmotion.NEUTRAL, energyLevel, new BigDecimal("7.0"), 3, null, sleepDay.plusDays(1)));
        }
        // Constant HRV (zero variance) so it never produces an insight, even though data exists.
        for (int i = 0; i < 6; i++) {
            readings.add(new WearableReading(
                    userId,
                    WearableReadingType.HRV,
                    new BigDecimal("45.0"),
                    startDay.plusDays(i).atTime(7, 0).toInstant(ZoneOffset.UTC),
                    WearableSourceType.SIMULATOR));
        }

        when(wearableReadingRepository.findByUserIdAndRecordedAtBetween(any(), any(), any())).thenReturn(readings);
        when(moodCheckInRepository.findByUserIdOrderByCheckInDateDesc(userId)).thenReturn(checkIns);

        List<CorrelationInsight> insights = service.computeInsights(userId);

        assertThat(insights).hasSize(1);
        assertThat(insights.get(0).metric()).isEqualTo(WearableReadingType.SLEEP_DURATION);
        assertThat(insights.get(0).correlationCoefficient()).isPositive();
    }

    @Test
    void shouldReturnNoInsightsWhenSampleIsTooSmall() {
        LocalDate today = LocalDate.now(ZoneOffset.UTC);
        List<WearableReading> readings = List.of(
                new WearableReading(
                        userId,
                        WearableReadingType.SLEEP_DURATION,
                        new BigDecimal("8.5"),
                        today.minusDays(3).atTime(7, 0).toInstant(ZoneOffset.UTC),
                        WearableSourceType.SIMULATOR),
                new WearableReading(
                        userId,
                        WearableReadingType.SLEEP_DURATION,
                        new BigDecimal("5.5"),
                        today.minusDays(2).atTime(7, 0).toInstant(ZoneOffset.UTC),
                        WearableSourceType.SIMULATOR));
        List<MoodCheckIn> checkIns = List.of(
                new MoodCheckIn(user, MoodEmotion.NEUTRAL, 5, new BigDecimal("7.0"), 3, null, today.minusDays(2)),
                new MoodCheckIn(user, MoodEmotion.NEUTRAL, 2, new BigDecimal("7.0"), 3, null, today.minusDays(1)));

        when(wearableReadingRepository.findByUserIdAndRecordedAtBetween(any(), any(), any())).thenReturn(readings);
        when(moodCheckInRepository.findByUserIdOrderByCheckInDateDesc(userId)).thenReturn(checkIns);

        List<CorrelationInsight> insights = service.computeInsights(userId);

        assertThat(insights).isEmpty();
    }

    @Test
    void shouldNeverUseDiagnosticOrCausalVocabularyInInsightText() {
        LocalDate today = LocalDate.now(ZoneOffset.UTC);
        LocalDate startDay = today.minusDays(7);
        List<WearableReading> readings = new ArrayList<>();
        List<MoodCheckIn> checkIns = new ArrayList<>();
        for (int i = 0; i < 6; i++) {
            LocalDate sleepDay = startDay.plusDays(i);
            boolean highSleepNight = i % 2 == 0;
            readings.add(new WearableReading(
                    userId,
                    WearableReadingType.SLEEP_DURATION,
                    highSleepNight ? new BigDecimal("8.5") : new BigDecimal("5.5"),
                    sleepDay.atTime(7, 0).toInstant(ZoneOffset.UTC),
                    WearableSourceType.SIMULATOR));
            checkIns.add(new MoodCheckIn(
                    user,
                    MoodEmotion.NEUTRAL,
                    highSleepNight ? 5 : 2,
                    new BigDecimal("7.0"),
                    3,
                    null,
                    sleepDay.plusDays(1)));
        }

        when(wearableReadingRepository.findByUserIdAndRecordedAtBetween(any(), any(), any())).thenReturn(readings);
        when(moodCheckInRepository.findByUserIdOrderByCheckInDateDesc(userId)).thenReturn(checkIns);

        List<CorrelationInsight> insights = service.computeInsights(userId);

        assertThat(insights).isNotEmpty();
        for (CorrelationInsight insight : insights) {
            String normalized = insight.description().toLowerCase(Locale.ROOT);
            for (String term : DIAGNOSTIC_TERMS) {
                assertThat(normalized).doesNotContain(term);
            }
        }
    }
}
