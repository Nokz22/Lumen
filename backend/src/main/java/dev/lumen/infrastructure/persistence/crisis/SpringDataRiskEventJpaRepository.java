package dev.lumen.infrastructure.persistence.crisis;

import dev.lumen.domain.crisis.RiskEvent;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

interface SpringDataRiskEventJpaRepository extends JpaRepository<RiskEvent, UUID> {

    List<RiskEvent> findByUserIdOrderByDetectedAtDesc(UUID userId);

    @Modifying
    @Query("DELETE FROM RiskEvent e WHERE e.userId = :userId")
    void deleteByUserId(@Param("userId") UUID userId);
}
