package dev.lumen.infrastructure.persistence.recommendation;

import dev.lumen.domain.recommendation.Recommendation;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

interface SpringDataRecommendationJpaRepository extends JpaRepository<Recommendation, UUID> {

    boolean existsByMoodCheckInId(UUID moodCheckInId);

    @Query("SELECT r FROM Recommendation r WHERE r.userId = :userId ORDER BY r.createdAt DESC")
    List<Recommendation> findByUserIdOrderByCreatedAtDesc(@Param("userId") UUID userId);
}
