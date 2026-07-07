package dev.lumen.domain.recommendation;

import java.util.List;
import java.util.UUID;

public interface RecommendationRepository {

    Recommendation save(Recommendation recommendation);

    boolean existsByMoodCheckInId(UUID moodCheckInId);

    List<Recommendation> findByUserIdOrderByCreatedAtDesc(UUID userId);
}
