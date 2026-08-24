package dev.lumen.domain.user;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ConsentRecordRepository {

    Optional<ConsentRecord> findLatestByUserIdAndConsentType(UUID userId, ConsentType consentType);

    ConsentRecord save(ConsentRecord consentRecord);

    List<ConsentRecord> findAllByUserIdOrderByCreatedAtAsc(UUID userId);

    void deleteByUserId(UUID userId);
}
