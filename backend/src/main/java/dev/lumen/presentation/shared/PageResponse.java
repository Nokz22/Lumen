package dev.lumen.presentation.shared;

import dev.lumen.domain.shared.PagedResult;
import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;

/**
 * The API shape of a page. Deliberately its own record rather than Spring Data's Page,
 * whose JSON structure is an implementation detail Spring itself warns against serializing
 * — it would make the published contract change when the framework does.
 */
@Schema(description = "One page of results, plus what a client needs to ask for the next one.")
public record PageResponse<T>(
        List<T> content,
        @Schema(description = "Zero-based index of this page.") int page,
        @Schema(description = "Maximum entries per page.") int size,
        @Schema(description = "Total across every page, so a client can say \"14 of 55\".") long totalElements,
        int totalPages,
        boolean hasNext) {

    public static <T> PageResponse<T> from(PagedResult<T> result) {
        return new PageResponse<>(
                result.content(),
                result.page(),
                result.size(),
                result.totalElements(),
                result.totalPages(),
                result.hasNext());
    }
}
