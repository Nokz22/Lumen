package dev.lumen.domain.wearable;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public interface WearableReadingRepository {

    List<WearableReading> saveAll(List<WearableReading> readings);

    List<WearableReading> findByUserIdOrderByRecordedAtDesc(UUID userId);

    List<WearableReading> findByUserIdAndRecordedAtBetween(UUID userId, Instant since, Instant until);
}
