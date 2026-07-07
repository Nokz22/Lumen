package dev.lumen.infrastructure.persistence.assessment;

import dev.lumen.domain.assessment.AssessmentAnswer;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

interface SpringDataAssessmentAnswerJpaRepository extends JpaRepository<AssessmentAnswer, UUID> {

    @Query("SELECT r FROM AssessmentAnswer r WHERE r.assessment.id = :assessmentId ORDER BY r.itemNumber")
    List<AssessmentAnswer> findByAssessmentId(@Param("assessmentId") UUID assessmentId);
}
