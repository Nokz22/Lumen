package dev.lumen.infrastructure.persistence.recommendation;

import dev.lumen.domain.recommendation.Recommendation;
import dev.lumen.domain.recommendation.RecommendationRepository;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Repository;

@Repository
class RecommendationRepositoryImpl implements RecommendationRepository {

    private final SpringDataRecommendationJpaRepository jpaRepository;

    RecommendationRepositoryImpl(SpringDataRecommendationJpaRepository jpaRepository) {
        this.jpaRepository = jpaRepository;
    }

    @Override
    public Recommendation save(Recommendation recommendation) {
        return jpaRepository.save(recommendation);
    }

    @Override
    public boolean existsByMoodCheckInId(UUID moodCheckInId) {
        return jpaRepository.existsByMoodCheckInId(moodCheckInId);
    }

    @Override
    public List<Recommendation> findByUserIdOrderByCreatedAtDesc(UUID userId) {
        return jpaRepository.findByUserIdOrderByCreatedAtDesc(userId);
    }
}
