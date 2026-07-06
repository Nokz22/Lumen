package dev.lumen.infrastructure.persistence.assessment;

import dev.lumen.domain.assessment.AssessmentScore;
import dev.lumen.domain.assessment.AssessmentScoreRepository;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Repository;

@Repository
class AssessmentScoreRepositoryImpl implements AssessmentScoreRepository {

    private final SpringDataAssessmentScoreJpaRepository jpaRepository;

    AssessmentScoreRepositoryImpl(SpringDataAssessmentScoreJpaRepository jpaRepository) {
        this.jpaRepository = jpaRepository;
    }

    @Override
    public AssessmentScore save(AssessmentScore score) {
        return jpaRepository.save(score);
    }

    @Override
    public Optional<AssessmentScore> findByAssessmentId(UUID assessmentId) {
        return jpaRepository.findByAssessmentId(assessmentId);
    }
}
