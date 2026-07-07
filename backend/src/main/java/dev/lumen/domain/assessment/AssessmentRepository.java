package dev.lumen.domain.assessment;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface AssessmentRepository {

    Assessment save(Assessment assessment);

    Optional<Assessment> findById(UUID id);

    Optional<Assessment> findMostRecentCompletedByUserIdAndType(
            UUID userId, AssessmentType assessmentType, Instant since);

    List<Assessment> findByUserIdOrderByCreatedAtDesc(UUID userId);
}
