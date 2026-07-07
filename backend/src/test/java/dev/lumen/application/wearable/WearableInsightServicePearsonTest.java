package dev.lumen.application.wearable;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

import java.util.List;
import org.junit.jupiter.api.Test;

class WearableInsightServicePearsonTest {

    @Test
    void shouldReturnOneForPerfectPositiveCorrelation() {
        List<Double> xs = List.of(1.0, 2.0, 3.0, 4.0, 5.0);
        List<Double> ys = List.of(2.0, 4.0, 6.0, 8.0, 10.0);

        double r = WearableInsightService.pearson(xs, ys);

        assertThat(r).isCloseTo(1.0, within(0.0001));
    }

    @Test
    void shouldReturnMinusOneForPerfectNegativeCorrelation() {
        List<Double> xs = List.of(1.0, 2.0, 3.0, 4.0, 5.0);
        List<Double> ys = List.of(10.0, 8.0, 6.0, 4.0, 2.0);

        double r = WearableInsightService.pearson(xs, ys);

        assertThat(r).isCloseTo(-1.0, within(0.0001));
    }

    @Test
    void shouldReturnZeroWhenOneSeriesHasNoVariance() {
        List<Double> xs = List.of(1.0, 2.0, 3.0, 4.0, 5.0);
        List<Double> ys = List.of(3.0, 3.0, 3.0, 3.0, 3.0);

        double r = WearableInsightService.pearson(xs, ys);

        assertThat(r).isZero();
    }

    @Test
    void shouldReturnWeakCorrelationForUnrelatedSeries() {
        List<Double> xs = List.of(1.0, 2.0, 3.0, 4.0, 5.0);
        List<Double> ys = List.of(5.0, 1.0, 4.0, 2.0, 3.0);

        double r = WearableInsightService.pearson(xs, ys);

        assertThat(Math.abs(r)).isLessThan(0.5);
    }
}
