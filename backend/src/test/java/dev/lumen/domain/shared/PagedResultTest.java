package dev.lumen.domain.shared;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

class PagedResultTest {

    @Test
    void shouldRoundTotalPagesUpForAPartialLastPage() {
        assertThat(new PagedResult<>(List.of("a"), 0, 20, 55).totalPages()).isEqualTo(3);
        assertThat(new PagedResult<>(List.of("a"), 0, 20, 40).totalPages()).isEqualTo(2);
        assertThat(new PagedResult<>(List.of(), 0, 20, 0).totalPages()).isZero();
    }

    @Test
    void shouldKnowWhetherAnotherPageExists() {
        assertThat(new PagedResult<>(List.of("a"), 0, 20, 55).hasNext()).isTrue();
        assertThat(new PagedResult<>(List.of("a"), 2, 20, 55).hasNext()).isFalse();
        assertThat(new PagedResult<>(List.of("a"), 1, 20, 40).hasNext()).isFalse();
    }

    /** Mapping the rows must not silently lose the count a client needs for "14 of 55". */
    @Test
    void shouldCarryThePagingMetadataThroughAMap() {
        PagedResult<Integer> mapped = new PagedResult<>(List.of("a", "bb"), 1, 20, 55).map(String::length);

        assertThat(mapped.content()).containsExactly(1, 2);
        assertThat(mapped.page()).isEqualTo(1);
        assertThat(mapped.size()).isEqualTo(20);
        assertThat(mapped.totalElements()).isEqualTo(55);
    }

    @Test
    void shouldNotLetCallersMutateTheContentAfterwards() {
        List<String> source = new ArrayList<>(List.of("a"));
        PagedResult<String> result = new PagedResult<>(source, 0, 20, 1);

        source.add("b");

        assertThat(result.content()).containsExactly("a");
        assertThatThrownBy(() -> result.content().add("c")).isInstanceOf(UnsupportedOperationException.class);
    }
}
