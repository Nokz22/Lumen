package dev.lumen.infrastructure.persistence.assessment;

import dev.lumen.domain.assessment.AssessmentAnswer;
import dev.lumen.domain.assessment.AssessmentAnswerRepository;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Repository;

@Repository
class AssessmentAnswerRepositoryImpl implements AssessmentAnswerRepository {

    private final SpringDataAssessmentAnswerJpaRepository jpaRepository;

    AssessmentAnswerRepositoryImpl(SpringDataAssessmentAnswerJpaRepository jpaRepository) {
        this.jpaRepository = jpaRepository;
    }

    @Override
    public List<AssessmentAnswer> saveAll(List<AssessmentAnswer> responses) {
        return jpaRepository.saveAll(responses);
    }

    @Override
    public List<AssessmentAnswer> findByAssessmentId(UUID assessmentId) {
        return jpaRepository.findByAssessmentId(assessmentId);
    }
}
