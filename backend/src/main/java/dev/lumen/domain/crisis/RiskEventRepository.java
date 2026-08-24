package dev.lumen.domain.crisis;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface RiskEventRepository {

    RiskEvent save(RiskEvent riskEvent);

    Optional<RiskEvent> findById(UUID id);

    List<RiskEvent> findByUserIdOrderByDetectedAtDesc(UUID userId);

    void deleteByUserId(UUID userId);
}
