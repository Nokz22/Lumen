package dev.lumen.application.wearable;

import dev.lumen.application.consent.ConsentService;
import dev.lumen.domain.user.ConsentRequiredException;
import dev.lumen.domain.user.ConsentType;
import dev.lumen.domain.user.UserNotFoundException;
import dev.lumen.domain.user.UserRepository;
import dev.lumen.domain.wearable.WearableReading;
import dev.lumen.domain.wearable.WearableReadingRepository;
import dev.lumen.domain.wearable.WearableSource;
import dev.lumen.domain.wearable.WearableSourceType;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * ingest() is the provider-agnostic entry point a real webhook would call once a
 * Fitbit/Garmin adapter exists; simulate() exercises the exact same persistence path
 * but sources its data from the SIMULATOR WearableSource instead — proving the two
 * flows converge on one normalized model (ADR-0008).
 */
@Service
public class WearableIngestionService {

    private final WearableReadingRepository wearableReadingRepository;
    private final Map<WearableSourceType, WearableSource> sourcesByType;
    private final UserRepository userRepository;
    private final ConsentService consentService;

    public WearableIngestionService(
            WearableReadingRepository wearableReadingRepository,
            List<WearableSource> sources,
            UserRepository userRepository,
            ConsentService consentService) {
        this.wearableReadingRepository = wearableReadingRepository;
        this.sourcesByType =
                sources.stream().collect(Collectors.toMap(WearableSource::getSourceType, Function.identity()));
        this.userRepository = userRepository;
        this.consentService = consentService;
    }

    @Transactional
    public List<WearableReadingResponse> ingest(
            UUID userId, WearableSourceType source, List<WearableReadingItem> items) {
        requireUser(userId);
        requireWearableConsent(userId);

        List<WearableReading> readings = items.stream()
                .map(item -> new WearableReading(userId, item.type(), item.value(), item.recordedAt(), source))
                .toList();
        return wearableReadingRepository.saveAll(readings).stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional
    public List<WearableReadingResponse> simulate(UUID userId, int days) {
        requireUser(userId);
        requireWearableConsent(userId);

        WearableSource simulator = sourcesByType.get(WearableSourceType.SIMULATOR);
        Instant until = Instant.now();
        Instant since = until.minus(days, ChronoUnit.DAYS);

        List<WearableReading> generated = simulator.fetch(userId, since, until);
        return wearableReadingRepository.saveAll(generated).stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<WearableReadingResponse> getHistory(UUID userId) {
        requireUser(userId);
        return wearableReadingRepository.findByUserIdOrderByRecordedAtDesc(userId).stream()
                .map(this::toResponse)
                .toList();
    }

    private void requireUser(UUID userId) {
        if (userRepository.findById(userId).isEmpty()) {
            throw new UserNotFoundException(userId);
        }
    }

    private void requireWearableConsent(UUID userId) {
        if (!consentService.isActive(userId, ConsentType.WEARABLE_INGESTION)) {
            throw new ConsentRequiredException(ConsentType.WEARABLE_INGESTION);
        }
    }

    private WearableReadingResponse toResponse(WearableReading reading) {
        return new WearableReadingResponse(
                reading.getId(), reading.getType(), reading.getValue(), reading.getRecordedAt(), reading.getSource());
    }
}
