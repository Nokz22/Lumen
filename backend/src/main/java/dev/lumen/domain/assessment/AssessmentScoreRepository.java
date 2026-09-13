package dev.lumen.domain.assessment;

import java.util.Optional;
import java.util.UUID;

public interface AssessmentScoreRepository {

    AssessmentScore save(AssessmentScore score);

    Optional<AssessmentScore> findByAssessmentId(UUID assessmentId);

    /** Keyed by assessment, so erasure resolves the owning user through the assessment. */
    void deleteByUserId(UUID userId);
}
