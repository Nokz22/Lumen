package dev.lumen.infrastructure.persistence.wearable;

import dev.lumen.domain.shared.PageQuery;
import dev.lumen.domain.shared.PagedResult;
import dev.lumen.domain.wearable.WearableReading;
import dev.lumen.domain.wearable.WearableReadingRepository;
import dev.lumen.infrastructure.persistence.shared.SpringDataPaging;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Repository;

@Repository
class WearableReadingRepositoryImpl implements WearableReadingRepository {

    private final SpringDataWearableReadingJpaRepository jpaRepository;

    WearableReadingRepositoryImpl(SpringDataWearableReadingJpaRepository jpaRepository) {
        this.jpaRepository = jpaRepository;
    }

    @Override
    public List<WearableReading> saveAll(List<WearableReading> readings) {
        return jpaRepository.saveAll(readings);
    }

    @Override
    public List<WearableReading> findByUserIdOrderByRecordedAtDesc(UUID userId) {
        return jpaRepository.findByUserIdOrderByRecordedAtDesc(userId);
    }

    @Override
    public List<WearableReading> findByUserIdAndRecordedAtBetween(UUID userId, Instant since, Instant until) {
        return jpaRepository.findByUserIdAndRecordedAtBetween(userId, since, until);
    }

    @Override
    public void deleteByUserId(UUID userId) {
        jpaRepository.deleteByUserId(userId);
    }

    @Override
    public PagedResult<WearableReading> findPageByUserIdOrderByRecordedAtDesc(UUID userId, PageQuery pageQuery) {
        return SpringDataPaging.toPagedResult(
                jpaRepository.findPageByUserIdOrderByRecordedAtDesc(userId, SpringDataPaging.toPageable(pageQuery)));
    }
}
