package dev.lumen.domain.recommendation;

import dev.lumen.domain.shared.PageQuery;
import dev.lumen.domain.shared.PagedResult;
import java.util.List;
import java.util.UUID;

public interface RecommendationRepository {

    Recommendation save(Recommendation recommendation);

    boolean existsByMoodCheckInId(UUID moodCheckInId);

    List<Recommendation> findByUserIdOrderByCreatedAtDesc(UUID userId);

    void deleteByUserId(UUID userId);

    /**
     * Alongside the full read above, not instead of it: the data export must stay
     * complete (ADR-0011) and the correlation and LLM-context paths need whole windows.
     */
    PagedResult<Recommendation> findPageByUserIdOrderByCreatedAtDesc(UUID userId, PageQuery pageQuery);
}
