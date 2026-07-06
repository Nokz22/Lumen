package dev.lumen.infrastructure.persistence.crisis;

import dev.lumen.domain.crisis.CrisisResource;
import dev.lumen.domain.crisis.CrisisResourceRepository;
import java.util.List;
import org.springframework.stereotype.Repository;

@Repository
class CrisisResourceRepositoryImpl implements CrisisResourceRepository {

    private final SpringDataCrisisResourceJpaRepository jpaRepository;

    CrisisResourceRepositoryImpl(SpringDataCrisisResourceJpaRepository jpaRepository) {
        this.jpaRepository = jpaRepository;
    }

    @Override
    public List<CrisisResource> findByRegion(String region) {
        return jpaRepository.findByRegion(region);
    }
}
