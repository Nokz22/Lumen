package dev.lumen.domain.shared;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

class PageQueryTest {

    @Test
    void shouldRejectANegativePage() {
        assertThatThrownBy(() -> new PageQuery(-1, 20)).isInstanceOf(InvalidPageRequestException.class);
    }

    @Test
    void shouldRejectAPageSizeOfZeroOrLess() {
        assertThatThrownBy(() -> new PageQuery(0, 0)).isInstanceOf(InvalidPageRequestException.class);
        assertThatThrownBy(() -> new PageQuery(0, -5)).isInstanceOf(InvalidPageRequestException.class);
    }

    /**
     * The cap is the whole point: without it, size=1000000 turns a paginated endpoint back
     * into the unbounded one it replaced. Enforced in the constructor rather than in a
     * controller so it holds for every caller, not only the ones arriving over HTTP.
     */
    @Test
    void shouldRefuseToHandOutMoreThanTheMaximumPageSize() {
        assertThatCode(() -> new PageQuery(0, PageQuery.MAX_SIZE)).doesNotThrowAnyException();
        assertThatThrownBy(() -> new PageQuery(0, PageQuery.MAX_SIZE + 1))
                .isInstanceOf(InvalidPageRequestException.class)
                .hasMessageContaining(String.valueOf(PageQuery.MAX_SIZE));
    }

    @Test
    void shouldDefaultToTheFirstPage() {
        assertThatCode(() -> PageQuery.firstPage()).doesNotThrowAnyException();
    }
}
