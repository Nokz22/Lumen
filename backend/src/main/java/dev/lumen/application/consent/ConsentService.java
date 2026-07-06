package dev.lumen.application.consent;

import dev.lumen.domain.user.ConsentRecord;
import dev.lumen.domain.user.ConsentRecordRepository;
import dev.lumen.domain.user.ConsentType;
import dev.lumen.domain.user.User;
import dev.lumen.domain.user.UserNotFoundException;
import dev.lumen.domain.user.UserRepository;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Append-only (ConsentRecord never updates in place, see domain.user.ConsentRecord) — the
 * most recent decision for a (user, consentType) pair wins.
 */
@Service
public class ConsentService {

    private final ConsentRecordRepository consentRecordRepository;
    private final UserRepository userRepository;

    public ConsentService(ConsentRecordRepository consentRecordRepository, UserRepository userRepository) {
        this.consentRecordRepository = consentRecordRepository;
        this.userRepository = userRepository;
    }

    @Transactional
    public void grant(UUID userId, ConsentType consentType) {
        recordDecision(userId, consentType, true);
    }

    @Transactional
    public void revoke(UUID userId, ConsentType consentType) {
        recordDecision(userId, consentType, false);
    }

    @Transactional(readOnly = true)
    public boolean isActive(UUID userId, ConsentType consentType) {
        return consentRecordRepository
                .findLatestByUserIdAndConsentType(userId, consentType)
                .map(ConsentRecord::isGranted)
                .orElse(false);
    }

    private void recordDecision(UUID userId, ConsentType consentType, boolean granted) {
        User user = userRepository.findById(userId).orElseThrow(() -> new UserNotFoundException(userId));
        int nextVersion = consentRecordRepository
                        .findLatestByUserIdAndConsentType(userId, consentType)
                        .map(ConsentRecord::getConsentVersion)
                        .orElse(0)
                + 1;
        consentRecordRepository.save(new ConsentRecord(user, consentType, granted, nextVersion));
    }
}
