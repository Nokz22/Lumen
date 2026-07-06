package dev.lumen.domain.crisis;

import java.util.List;

public interface CrisisResourceRepository {

    List<CrisisResource> findByRegion(String region);
}
