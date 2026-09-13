package dev.lumen.application.demo;

import static org.assertj.core.api.Assertions.assertThat;

import dev.lumen.domain.moodcheckin.MoodEmotion;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.Profile;

class DemoDataSeederTest {

    private static final int PHQ9_SELF_HARM_ITEM_INDEX = 8;

    /**
     * The rule from project-brief section 11, as a test rather than as a good intention:
     * demo data never contains a crisis scenario. A public instance seeded with a positive
     * item 9 would carry a RiskEvent that reads exactly like a real person having been in
     * danger. The crisis flow is demonstrated deliberately and in a controlled setting.
     */
    @Test
    void shouldNeverSeedAPositiveAnswerToPhq9ItemNine() {
        assertThat(DemoDataSeeder.PHQ9_DEMO_RESPONSES.get(PHQ9_SELF_HARM_ITEM_INDEX))
                .isZero();
    }

    @Test
    void shouldSeedInstrumentAnswersThatAreValidForTheirInstrument() {
        assertThat(DemoDataSeeder.PHQ9_DEMO_RESPONSES).hasSize(9).allSatisfy(value -> assertThat(value)
                .isBetween(0, 3));
        assertThat(DemoDataSeeder.GAD7_DEMO_RESPONSES).hasSize(7).allSatisfy(value -> assertThat(value)
                .isBetween(0, 3));
    }

    /**
     * Fabricated health history in a production database would be indistinguishable from a
     * real person's. The profile annotation is the only thing preventing that.
     */
    @Test
    void shouldOnlyEverRunUnderTheDemoProfile() {
        Profile profile = DemoDataSeeder.class.getAnnotation(Profile.class);

        assertThat(profile).isNotNull();
        assertThat(profile.value()).containsExactly("demo");
    }

    @Test
    void shouldProduceValuesInsideEveryDomainRange() {
        for (DemoDataSeeder.DemoStory.Day day : generateHistory()) {
            assertThat(day.energyLevel()).isBetween(1, 5);
            assertThat(day.sleepQuality()).isBetween(1, 5);
            assertThat(day.sleepHours()).isBetween(new BigDecimal("4.0"), new BigDecimal("10.0"));
            assertThat(day.emotion()).isIn((Object[]) MoodEmotion.values());
            assertThat(day.steps()).isPositive();
        }
    }

    /**
     * The dashboard's insight only appears when sleep and next-day energy actually
     * correlate. If the generated story were noise, the demo would show an empty panel —
     * the one thing it exists to demonstrate.
     */
    @Test
    void shouldGenerateSleepAndEnergyThatActuallyCorrelate() {
        List<DemoDataSeeder.DemoStory.Day> history = generateHistory();
        List<Double> sleep = new ArrayList<>();
        List<Double> nextDayEnergy = new ArrayList<>();
        for (int i = 0; i < history.size() - 1; i++) {
            sleep.add(history.get(i).sleepHours().doubleValue());
            nextDayEnergy.add((double) history.get(i + 1).energyLevel());
        }

        assertThat(pearson(sleep, nextDayEnergy)).isGreaterThan(0.3);
    }

    /** A gapless streak of check-ins is the tell of generated data; some days are missed. */
    @Test
    void shouldMissSomeDaysTheWayARealPersonDoes() {
        DemoDataSeeder.DemoStory story = new DemoDataSeeder.DemoStory(new Random(1L));
        long skipped = 0;
        for (int i = 0; i < 56; i++) {
            if (story.dayAt(i, 56).skipped()) {
                skipped++;
            }
        }

        assertThat(skipped).isBetween(1L, 12L);
    }

    private List<DemoDataSeeder.DemoStory.Day> generateHistory() {
        DemoDataSeeder.DemoStory story = new DemoDataSeeder.DemoStory(new Random(20260907L));
        List<DemoDataSeeder.DemoStory.Day> days = new ArrayList<>();
        for (int i = 0; i < 56; i++) {
            days.add(story.dayAt(i, 56));
        }
        return days;
    }

    private double pearson(List<Double> xs, List<Double> ys) {
        int n = xs.size();
        double meanX = xs.stream().mapToDouble(Double::doubleValue).average().orElseThrow();
        double meanY = ys.stream().mapToDouble(Double::doubleValue).average().orElseThrow();
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
        return covariance / Math.sqrt(varianceX * varianceY);
    }
}
