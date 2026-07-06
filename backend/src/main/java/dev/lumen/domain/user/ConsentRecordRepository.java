package dev.lumen.domain.user;

import java.util.Optional;
import java.util.UUID;

public interface ConsentRecordRepository {

    Optional<ConsentRecord> findLatestByUserIdAndConsentType(UUID userId, ConsentType consentType);

    ConsentRecord save(ConsentRecord consentRecord);
}
