package dev.lumen.domain.moodcheckin;

import dev.lumen.domain.shared.PageQuery;
import dev.lumen.domain.shared.PagedResult;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface MoodCheckInRepository {

    Optional<MoodCheckIn> findByUserIdAndCheckInDate(UUID userId, LocalDate checkInDate);

    List<MoodCheckIn> findByUserIdOrderByCheckInDateDesc(UUID userId);

    MoodCheckIn save(MoodCheckIn moodCheckIn);

    void deleteByUserId(UUID userId);

    /**
     * Alongside the full read above, not instead of it: the data export must stay
     * complete (ADR-0011) and the correlation and LLM-context paths need whole windows.
     */
    PagedResult<MoodCheckIn> findPageByUserIdOrderByCheckInDateDesc(UUID userId, PageQuery pageQuery);
}
