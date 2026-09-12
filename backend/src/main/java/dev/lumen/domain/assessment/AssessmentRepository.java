package dev.lumen.domain.assessment;

import dev.lumen.domain.shared.PageQuery;
import dev.lumen.domain.shared.PagedResult;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface AssessmentRepository {

    Assessment save(Assessment assessment);

    Optional<Assessment> findById(UUID id);

    Optional<Assessment> findMostRecentCompletedByUserIdAndType(
            UUID userId, AssessmentType assessmentType, Instant since);

    List<Assessment> findByUserIdOrderByCreatedAtDesc(UUID userId);

    void deleteByUserId(UUID userId);

    /**
     * Alongside the full read above, not instead of it: the data export must stay
     * complete (ADR-0011) and the correlation and LLM-context paths need whole windows.
     */
    PagedResult<Assessment> findPageByUserIdOrderByCreatedAtDesc(UUID userId, PageQuery pageQuery);
}
