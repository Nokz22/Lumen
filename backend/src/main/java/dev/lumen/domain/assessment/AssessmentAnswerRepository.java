package dev.lumen.domain.assessment;

import java.util.List;
import java.util.UUID;

public interface AssessmentAnswerRepository {

    List<AssessmentAnswer> saveAll(List<AssessmentAnswer> responses);

    List<AssessmentAnswer> findByAssessmentId(UUID assessmentId);
}
