package dev.lumen.application.moodcheckin;

import dev.lumen.application.audit.AuditLogService;
import dev.lumen.application.consent.ConsentService;
import dev.lumen.domain.audit.AuditAction;
import dev.lumen.domain.moodcheckin.MoodCheckIn;
import dev.lumen.domain.moodcheckin.MoodCheckInRepository;
import dev.lumen.domain.moodcheckin.MoodEmotion;
import dev.lumen.domain.user.ConsentRequiredException;
import dev.lumen.domain.user.ConsentType;
import dev.lumen.domain.user.User;
import dev.lumen.domain.user.UserNotFoundException;
import dev.lumen.domain.user.UserRepository;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Today is always computed in UTC — the canonical zone the platform persists in — rather
 * than the server's local zone, keeping the daily check-in boundary unambiguous.
 */
@Service
public class MoodCheckInService {

    private final MoodCheckInRepository moodCheckInRepository;
    private final UserRepository userRepository;
    private final MoodCheckInMapper mapper;
    private final ConsentService consentService;
    private final AuditLogService auditLogService;

    public MoodCheckInService(
            MoodCheckInRepository moodCheckInRepository,
            UserRepository userRepository,
            MoodCheckInMapper mapper,
            ConsentService consentService,
            AuditLogService auditLogService) {
        this.moodCheckInRepository = moodCheckInRepository;
        this.userRepository = userRepository;
        this.mapper = mapper;
        this.consentService = consentService;
        this.auditLogService = auditLogService;
    }

    @Transactional
    public MoodCheckInResponse checkIn(
            UUID userId, MoodEmotion emotion, int energyLevel, BigDecimal sleepHours, int sleepQuality, String note) {
        User user = userRepository.findById(userId).orElseThrow(() -> new UserNotFoundException(userId));
        requireHealthDataConsent(userId);
        LocalDate today = LocalDate.now(ZoneOffset.UTC);

        Optional<MoodCheckIn> existing = moodCheckInRepository.findByUserIdAndCheckInDate(userId, today);
        MoodCheckIn checkIn;
        if (existing.isPresent()) {
            checkIn = existing.get();
            checkIn.updateDetails(emotion, energyLevel, sleepHours, sleepQuality, note);
        } else {
            checkIn = new MoodCheckIn(user, emotion, energyLevel, sleepHours, sleepQuality, note, today);
        }
        return mapper.toResponse(moodCheckInRepository.save(checkIn));
    }

    @Transactional(readOnly = true)
    public List<MoodCheckInResponse> getHistory(UUID userId) {
        if (userRepository.findById(userId).isEmpty()) {
            throw new UserNotFoundException(userId);
        }
        requireHealthDataConsent(userId);
        auditLogService.record(userId, userId, AuditAction.VIEW_MOOD_HISTORY);
        return moodCheckInRepository.findByUserIdOrderByCheckInDateDesc(userId).stream()
                .map(mapper::toResponse)
                .toList();
    }

    private void requireHealthDataConsent(UUID userId) {
        if (!consentService.isActive(userId, ConsentType.HEALTH_DATA_PROCESSING)) {
            throw new ConsentRequiredException(ConsentType.HEALTH_DATA_PROCESSING);
        }
    }
}
