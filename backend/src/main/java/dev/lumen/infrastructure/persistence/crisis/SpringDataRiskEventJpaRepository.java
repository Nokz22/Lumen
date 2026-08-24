package dev.lumen.infrastructure.persistence.crisis;

import dev.lumen.domain.crisis.RiskEvent;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

interface SpringDataRiskEventJpaRepository extends JpaRepository<RiskEvent, UUID> {

    List<RiskEvent> findByUserIdOrderByDetectedAtDesc(UUID userId);

    void deleteByUserId(UUID userId);
}
