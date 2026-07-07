package dev.lumen.infrastructure.persistence.crisis;

import dev.lumen.domain.crisis.CrisisResource;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

interface SpringDataCrisisResourceJpaRepository extends JpaRepository<CrisisResource, UUID> {

    List<CrisisResource> findByRegion(String region);
}
