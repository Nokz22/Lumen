package dev.lumen.infrastructure.persistence.moodcheckin;

import dev.lumen.domain.moodcheckin.MoodCheckIn;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

interface SpringDataMoodCheckInJpaRepository extends JpaRepository<MoodCheckIn, UUID> {

    @Query("SELECT m FROM MoodCheckIn m WHERE m.user.id = :userId AND m.checkInDate = :checkInDate")
    Optional<MoodCheckIn> findByUserIdAndCheckInDate(
            @Param("userId") UUID userId, @Param("checkInDate") LocalDate checkInDate);

    @Query("SELECT m FROM MoodCheckIn m WHERE m.user.id = :userId ORDER BY m.checkInDate DESC")
    List<MoodCheckIn> findByUserIdOrderByCheckInDateDesc(@Param("userId") UUID userId);

    @Modifying
    @Query("DELETE FROM MoodCheckIn m WHERE m.user.id = :userId")
    void deleteByUserId(@Param("userId") UUID userId);

    @Query(value = "SELECT m FROM MoodCheckIn m WHERE m.user.id = :userId ORDER BY m.checkInDate DESC",
            countQuery = "SELECT count(m) FROM MoodCheckIn m WHERE m.user.id = :userId")
    Page<MoodCheckIn> findPageByUserIdOrderByCheckInDateDesc(@Param("userId") UUID userId, Pageable pageable);
}
