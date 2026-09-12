package dev.lumen.infrastructure.persistence.shared;

import dev.lumen.domain.shared.PageQuery;
import dev.lumen.domain.shared.PagedResult;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

/**
 * The single translation point between the domain's paging vocabulary and Spring Data's.
 * Keeping it here is what lets the ports stay framework-free while the adapters keep using
 * the repository support they already have.
 */
public final class SpringDataPaging {

    private SpringDataPaging() {
    }

    public static Pageable toPageable(PageQuery pageQuery) {
        return PageRequest.of(pageQuery.page(), pageQuery.size());
    }

    public static <T> PagedResult<T> toPagedResult(Page<T> page) {
        return new PagedResult<>(page.getContent(), page.getNumber(), page.getSize(), page.getTotalElements());
    }
}
