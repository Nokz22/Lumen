package dev.lumen.infrastructure.persistence.assessment;

import dev.lumen.domain.assessment.Assessment;
import dev.lumen.domain.assessment.AssessmentStatus;
import dev.lumen.domain.assessment.AssessmentType;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

interface SpringDataAssessmentJpaRepository extends JpaRepository<Assessment, UUID> {

    @Query(
            "SELECT a FROM Assessment a WHERE a.user.id = :userId AND a.assessmentType = :type "
                    + "AND a.status IN :statuses AND a.createdAt >= :since ORDER BY a.createdAt DESC")
    List<Assessment> findRecent(
            @Param("userId") UUID userId,
            @Param("type") AssessmentType type,
            @Param("statuses") List<AssessmentStatus> statuses,
            @Param("since") Instant since);

    @Query("SELECT a FROM Assessment a WHERE a.user.id = :userId ORDER BY a.createdAt DESC")
    List<Assessment> findByUserIdOrderByCreatedAtDesc(@Param("userId") UUID userId);
}
