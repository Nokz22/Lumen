package dev.lumen.domain.shared;

import java.util.List;
import java.util.function.Function;

/**
 * One page of results plus what a caller needs to ask for the next one.
 *
 * <p>Offset-based rather than keyset. Keyset paging is the right answer for a feed shared by
 * everyone; these lists are one person's own history, bounded by how long they have used the
 * app, and offset keeps the API something a client can drive with a page number. If a single
 * account's history ever grows large enough for deep offsets to hurt, this is the type that
 * changes shape and the ports change with it.
 *
 * @param totalElements total across all pages, so a caller can say "14 of 55" without
 *                      fetching the rest
 */
public record PagedResult<T>(List<T> content, int page, int size, long totalElements) {

    public PagedResult {
        content = List.copyOf(content);
    }

    public int totalPages() {
        return size == 0 ? 0 : (int) Math.ceil((double) totalElements / size);
    }

    public boolean hasNext() {
        return (long) (page + 1) * size < totalElements;
    }

    /** Maps the content while carrying the paging metadata across unchanged. */
    public <R> PagedResult<R> map(Function<T, R> mapper) {
        return new PagedResult<>(content.stream().map(mapper).toList(), page, size, totalElements);
    }
}
