package dev.lumen.infrastructure.persistence.assessment;

import dev.lumen.domain.assessment.Assessment;
import dev.lumen.domain.assessment.AssessmentRepository;
import dev.lumen.domain.assessment.AssessmentStatus;
import dev.lumen.domain.assessment.AssessmentType;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Repository;

@Repository
class AssessmentRepositoryImpl implements AssessmentRepository {

    private static final List<AssessmentStatus> SUBMITTED_STATUSES =
            List.of(AssessmentStatus.COMPLETED, AssessmentStatus.SCORED);

    private final SpringDataAssessmentJpaRepository jpaRepository;

    AssessmentRepositoryImpl(SpringDataAssessmentJpaRepository jpaRepository) {
        this.jpaRepository = jpaRepository;
    }

    @Override
    public Assessment save(Assessment assessment) {
        return jpaRepository.save(assessment);
    }

    @Override
    public Optional<Assessment> findById(UUID id) {
        return jpaRepository.findById(id);
    }

    @Override
    public Optional<Assessment> findMostRecentCompletedByUserIdAndType(
            UUID userId, AssessmentType assessmentType, Instant since) {
        return jpaRepository.findRecent(userId, assessmentType, SUBMITTED_STATUSES, since).stream()
                .findFirst();
    }

    @Override
    public List<Assessment> findByUserIdOrderByCreatedAtDesc(UUID userId) {
        return jpaRepository.findByUserIdOrderByCreatedAtDesc(userId);
    }
}
