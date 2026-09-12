package dev.lumen.presentation.shared;

import dev.lumen.domain.shared.PageQuery;
import io.swagger.v3.oas.annotations.media.Schema;

/**
 * The {@code page} and {@code size} query parameters, bound once instead of being repeated
 * as a pair of {@code @RequestParam} arguments on every listing endpoint.
 *
 * <p>No bean validation annotations: the bounds belong to {@link PageQuery}, which enforces
 * them for every caller rather than only for callers that arrive over HTTP.
 */
public class PageParameters {

    @Schema(description = "Zero-based page index.", defaultValue = "0")
    private int page;

    @Schema(description = "Entries per page (maximum 100).", defaultValue = "20")
    private int size = PageQuery.DEFAULT_SIZE;

    public void setPage(int page) {
        this.page = page;
    }

    public void setSize(int size) {
        this.size = size;
    }

    public PageQuery toPageQuery() {
        return new PageQuery(page, size);
    }
}
