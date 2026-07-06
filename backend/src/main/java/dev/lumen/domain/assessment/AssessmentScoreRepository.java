package dev.lumen.domain.assessment;

import java.util.Optional;
import java.util.UUID;

public interface AssessmentScoreRepository {

    AssessmentScore save(AssessmentScore score);

    Optional<AssessmentScore> findByAssessmentId(UUID assessmentId);
}
