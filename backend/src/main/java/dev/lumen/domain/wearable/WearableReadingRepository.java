package dev.lumen.domain.wearable;

import dev.lumen.domain.shared.PageQuery;
import dev.lumen.domain.shared.PagedResult;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public interface WearableReadingRepository {

    List<WearableReading> saveAll(List<WearableReading> readings);

    List<WearableReading> findByUserIdOrderByRecordedAtDesc(UUID userId);

    List<WearableReading> findByUserIdAndRecordedAtBetween(UUID userId, Instant since, Instant until);

    void deleteByUserId(UUID userId);

    /**
     * Alongside the full read above, not instead of it: the data export must stay
     * complete (ADR-0011) and the correlation and LLM-context paths need whole windows.
     */
    PagedResult<WearableReading> findPageByUserIdOrderByRecordedAtDesc(UUID userId, PageQuery pageQuery);
}
