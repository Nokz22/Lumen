package dev.lumen.infrastructure.persistence.crisis;

import dev.lumen.domain.crisis.RiskEvent;
import dev.lumen.domain.crisis.RiskEventRepository;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Repository;

@Repository
class RiskEventRepositoryImpl implements RiskEventRepository {

    private final SpringDataRiskEventJpaRepository jpaRepository;

    RiskEventRepositoryImpl(SpringDataRiskEventJpaRepository jpaRepository) {
        this.jpaRepository = jpaRepository;
    }

    @Override
    public RiskEvent save(RiskEvent riskEvent) {
        return jpaRepository.save(riskEvent);
    }

    @Override
    public Optional<RiskEvent> findById(UUID id) {
        return jpaRepository.findById(id);
    }
}
