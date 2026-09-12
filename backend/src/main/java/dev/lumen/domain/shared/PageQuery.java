package dev.lumen.domain.shared;

/**
 * A request for one page of a list, expressed without Spring Data — the domain layer may
 * not depend on the framework (enforced by LayeredArchitectureTest), so the ports speak
 * this and the persistence adapters translate it.
 *
 * <p>The maximum size is enforced here rather than in a controller. A cap that lives in the
 * presentation layer is a cap on one endpoint; a cap in the constructor is a cap on every
 * caller that will ever exist, including the ones added after everyone has forgotten the
 * reason for it.
 *
 * @param page zero-based
 */
public record PageQuery(int page, int size) {

    public static final int DEFAULT_SIZE = 20;
    public static final int MAX_SIZE = 100;

    public PageQuery {
        if (page < 0) {
            throw new InvalidPageRequestException("page must not be negative, was " + page);
        }
        if (size < 1) {
            throw new InvalidPageRequestException("size must be at least 1, was " + size);
        }
        if (size > MAX_SIZE) {
            throw new InvalidPageRequestException("size must not exceed " + MAX_SIZE + ", was " + size);
        }
    }

    public static PageQuery firstPage() {
        return new PageQuery(0, DEFAULT_SIZE);
    }
}
