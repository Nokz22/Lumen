package dev.lumen.infrastructure.persistence.wearable;

import dev.lumen.domain.wearable.WearableReading;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

interface SpringDataWearableReadingJpaRepository extends JpaRepository<WearableReading, UUID> {

    @Query("SELECT r FROM WearableReading r WHERE r.userId = :userId ORDER BY r.recordedAt DESC")
    List<WearableReading> findByUserIdOrderByRecordedAtDesc(@Param("userId") UUID userId);

    @Query(
            "SELECT r FROM WearableReading r WHERE r.userId = :userId AND r.recordedAt BETWEEN :since AND :until "
                    + "ORDER BY r.recordedAt")
    List<WearableReading> findByUserIdAndRecordedAtBetween(
            @Param("userId") UUID userId, @Param("since") Instant since, @Param("until") Instant until);
}
