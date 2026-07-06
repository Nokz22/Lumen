package dev.lumen.infrastructure.persistence.crisis;

import dev.lumen.domain.crisis.RiskEvent;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

interface SpringDataRiskEventJpaRepository extends JpaRepository<RiskEvent, UUID> {
}
