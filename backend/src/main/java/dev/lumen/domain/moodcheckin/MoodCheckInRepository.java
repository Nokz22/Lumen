package dev.lumen.domain.moodcheckin;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface MoodCheckInRepository {

    Optional<MoodCheckIn> findByUserIdAndCheckInDate(UUID userId, LocalDate checkInDate);

    List<MoodCheckIn> findByUserIdOrderByCheckInDateDesc(UUID userId);

    MoodCheckIn save(MoodCheckIn moodCheckIn);
}
