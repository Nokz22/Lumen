package dev.lumen.domain.exercise;

import dev.lumen.domain.shared.PageQuery;
import dev.lumen.domain.shared.PagedResult;
import java.util.List;
import java.util.UUID;

public interface ExerciseCompletionRepository {

    ExerciseCompletion save(ExerciseCompletion completion);

    List<ExerciseCompletion> findByUserIdOrderByCompletedAtDesc(UUID userId);

    void deleteByUserId(UUID userId);

    /**
     * Alongside the full read above, not instead of it: the data export must stay
     * complete (ADR-0011) and the correlation and LLM-context paths need whole windows.
     */
    PagedResult<ExerciseCompletion> findPageByUserIdOrderByCompletedAtDesc(UUID userId, PageQuery pageQuery);
}
