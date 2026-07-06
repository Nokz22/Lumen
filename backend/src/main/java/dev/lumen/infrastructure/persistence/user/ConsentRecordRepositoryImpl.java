package dev.lumen.infrastructure.persistence.user;

import dev.lumen.domain.user.ConsentRecord;
import dev.lumen.domain.user.ConsentRecordRepository;
import dev.lumen.domain.user.ConsentType;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Repository;

@Repository
class ConsentRecordRepositoryImpl implements ConsentRecordRepository {

    private final SpringDataConsentRecordJpaRepository jpaRepository;

    ConsentRecordRepositoryImpl(SpringDataConsentRecordJpaRepository jpaRepository) {
        this.jpaRepository = jpaRepository;
    }

    @Override
    public Optional<ConsentRecord> findLatestByUserIdAndConsentType(UUID userId, ConsentType consentType) {
        List<ConsentRecord> results =
                jpaRepository.findLatestByUserIdAndConsentType(userId, consentType, PageRequest.of(0, 1));
        return results.isEmpty() ? Optional.empty() : Optional.of(results.get(0));
    }

    @Override
    public ConsentRecord save(ConsentRecord consentRecord) {
        return jpaRepository.save(consentRecord);
    }
}
