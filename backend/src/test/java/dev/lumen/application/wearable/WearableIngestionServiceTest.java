package dev.lumen.application.wearable;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import dev.lumen.application.consent.ConsentService;
import dev.lumen.domain.user.ConsentRequiredException;
import dev.lumen.domain.user.ConsentType;
import dev.lumen.domain.user.Role;
import dev.lumen.domain.user.User;
import dev.lumen.domain.user.UserNotFoundException;
import dev.lumen.domain.user.UserRepository;
import dev.lumen.domain.wearable.WearableReading;
import dev.lumen.domain.wearable.WearableReadingRepository;
import dev.lumen.domain.wearable.WearableReadingType;
import dev.lumen.domain.wearable.WearableSource;
import dev.lumen.domain.wearable.WearableSourceType;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class WearableIngestionServiceTest {

    private final WearableReadingRepository wearableReadingRepository = mock(WearableReadingRepository.class);
    private final WearableSource simulator = mock(WearableSource.class);
    private final UserRepository userRepository = mock(UserRepository.class);
    private final ConsentService consentService = mock(ConsentService.class);

    private final UUID userId = UUID.randomUUID();
    private WearableIngestionService service;

    @BeforeEach
    void setUp() {
        User user = new User(
                "jane@lumen.dev", "hashed-password", "Jane", "en", "PT", LocalDate.of(1990, 1, 1), Role.USER);
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(consentService.isActive(userId, ConsentType.WEARABLE_INGESTION)).thenReturn(true);
        when(simulator.getSourceType()).thenReturn(WearableSourceType.SIMULATOR);
        when(wearableReadingRepository.saveAll(any())).thenAnswer(invocation -> invocation.getArgument(0));

        service = new WearableIngestionService(
                wearableReadingRepository, List.of(simulator), userRepository, consentService);
    }

    @Test
    void shouldPersistIngestedReadingsWhenConsentIsActive() {
        WearableReadingItem item = heartRateItem();

        List<WearableReadingResponse> result =
                service.ingest(userId, WearableSourceType.SIMULATOR, List.of(item));

        assertThat(result).hasSize(1);
        assertThat(result.get(0).type()).isEqualTo(WearableReadingType.HEART_RATE);
        verify(wearableReadingRepository).saveAll(any());
    }

    @Test
    void shouldRejectIngestWhenConsentIsNotActive() {
        when(consentService.isActive(userId, ConsentType.WEARABLE_INGESTION)).thenReturn(false);
        WearableReadingItem item = heartRateItem();

        assertThatThrownBy(() -> service.ingest(userId, WearableSourceType.SIMULATOR, List.of(item)))
                .isInstanceOf(ConsentRequiredException.class);
    }

    @Test
    void shouldThrowWhenUserDoesNotExist() {
        UUID unknownUserId = UUID.randomUUID();
        when(userRepository.findById(unknownUserId)).thenReturn(Optional.empty());
        WearableReadingItem item = heartRateItem();

        assertThatThrownBy(() -> service.ingest(unknownUserId, WearableSourceType.SIMULATOR, List.of(item)))
                .isInstanceOf(UserNotFoundException.class);
    }

    @Test
    void shouldGenerateReadingsViaSimulatorWhenConsentIsActive() {
        WearableReading generated = new WearableReading(
                userId,
                WearableReadingType.SLEEP_DURATION,
                new BigDecimal("7.0"),
                Instant.now(),
                WearableSourceType.SIMULATOR);
        when(simulator.fetch(eq(userId), any(), any())).thenReturn(List.of(generated));

        List<WearableReadingResponse> result = service.simulate(userId, 7);

        assertThat(result).hasSize(1);
        verify(simulator).fetch(eq(userId), any(), any());
    }

    @Test
    void shouldRejectSimulateWhenConsentIsNotActive() {
        when(consentService.isActive(userId, ConsentType.WEARABLE_INGESTION)).thenReturn(false);

        assertThatThrownBy(() -> service.simulate(userId, 7)).isInstanceOf(ConsentRequiredException.class);
    }

    private WearableReadingItem heartRateItem() {
        return new WearableReadingItem(WearableReadingType.HEART_RATE, new BigDecimal("70"), Instant.now());
    }
}
