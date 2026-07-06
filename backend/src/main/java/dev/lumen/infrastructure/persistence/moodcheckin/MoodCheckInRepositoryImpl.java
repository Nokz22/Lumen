package dev.lumen.infrastructure.persistence.moodcheckin;

import dev.lumen.domain.moodcheckin.MoodCheckIn;
import dev.lumen.domain.moodcheckin.MoodCheckInRepository;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Repository;

@Repository
class MoodCheckInRepositoryImpl implements MoodCheckInRepository {

    private final SpringDataMoodCheckInJpaRepository jpaRepository;

    MoodCheckInRepositoryImpl(SpringDataMoodCheckInJpaRepository jpaRepository) {
        this.jpaRepository = jpaRepository;
    }

    @Override
    public Optional<MoodCheckIn> findByUserIdAndCheckInDate(UUID userId, LocalDate checkInDate) {
        return jpaRepository.findByUserIdAndCheckInDate(userId, checkInDate);
    }

    @Override
    public List<MoodCheckIn> findByUserIdOrderByCheckInDateDesc(UUID userId) {
        return jpaRepository.findByUserIdOrderByCheckInDateDesc(userId);
    }

    @Override
    public MoodCheckIn save(MoodCheckIn moodCheckIn) {
        return jpaRepository.save(moodCheckIn);
    }
}
