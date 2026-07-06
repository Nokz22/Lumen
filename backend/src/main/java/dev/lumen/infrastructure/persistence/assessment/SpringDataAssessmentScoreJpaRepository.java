package dev.lumen.infrastructure.persistence.assessment;

import dev.lumen.domain.assessment.AssessmentScore;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

interface SpringDataAssessmentScoreJpaRepository extends JpaRepository<AssessmentScore, UUID> {

    @Query("SELECT s FROM AssessmentScore s WHERE s.assessment.id = :assessmentId")
    Optional<AssessmentScore> findByAssessmentId(@Param("assessmentId") UUID assessmentId);
}
