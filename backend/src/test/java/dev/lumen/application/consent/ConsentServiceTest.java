package dev.lumen.application.consent;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import dev.lumen.domain.user.ConsentRecord;
import dev.lumen.domain.user.ConsentRecordRepository;
import dev.lumen.domain.user.ConsentType;
import dev.lumen.domain.user.Role;
import dev.lumen.domain.user.User;
import dev.lumen.domain.user.UserRepository;
import java.time.LocalDate;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class ConsentServiceTest {

    private final ConsentRecordRepository consentRecordRepository = mock(ConsentRecordRepository.class);
    private final UserRepository userRepository = mock(UserRepository.class);
    private final ConsentService service = new ConsentService(consentRecordRepository, userRepository);

    private User user;

    @BeforeEach
    void setUp() {
        user = new User(
                "jane@lumen.dev", "hashed-password", "Jane", "en", "PT", LocalDate.of(1990, 1, 1), Role.USER);
        when(userRepository.findById(user.getId())).thenReturn(Optional.of(user));
        when(consentRecordRepository.save(any(ConsentRecord.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
    }

    @Test
    void shouldBeInactiveWhenNoDecisionWasEverMade() {
        when(consentRecordRepository.findLatestByUserIdAndConsentType(user.getId(), ConsentType.HEALTH_DATA_PROCESSING))
                .thenReturn(Optional.empty());

        assertThat(service.isActive(user.getId(), ConsentType.HEALTH_DATA_PROCESSING)).isFalse();
    }

    @Test
    void shouldBecomeActiveAfterGranting() {
        when(consentRecordRepository.findLatestByUserIdAndConsentType(user.getId(), ConsentType.HEALTH_DATA_PROCESSING))
                .thenReturn(Optional.empty())
                .thenReturn(Optional.of(new ConsentRecord(user, ConsentType.HEALTH_DATA_PROCESSING, true, 1)));

        service.grant(user.getId(), ConsentType.HEALTH_DATA_PROCESSING);

        assertThat(service.isActive(user.getId(), ConsentType.HEALTH_DATA_PROCESSING)).isTrue();
        verify(consentRecordRepository).save(any(ConsentRecord.class));
    }

    @Test
    void shouldBecomeInactiveAfterRevokingAPreviouslyGrantedConsent() {
        ConsentRecord granted = new ConsentRecord(user, ConsentType.HEALTH_DATA_PROCESSING, true, 1);
        ConsentRecord revoked = new ConsentRecord(user, ConsentType.HEALTH_DATA_PROCESSING, false, 2);
        when(consentRecordRepository.findLatestByUserIdAndConsentType(user.getId(), ConsentType.HEALTH_DATA_PROCESSING))
                .thenReturn(Optional.of(granted))
                .thenReturn(Optional.of(revoked));

        service.revoke(user.getId(), ConsentType.HEALTH_DATA_PROCESSING);

        assertThat(service.isActive(user.getId(), ConsentType.HEALTH_DATA_PROCESSING)).isFalse();
    }

    @Test
    void shouldIncrementConsentVersionOnEachDecision() {
        ConsentRecord previous = new ConsentRecord(user, ConsentType.HEALTH_DATA_PROCESSING, true, 3);
        when(consentRecordRepository.findLatestByUserIdAndConsentType(user.getId(), ConsentType.HEALTH_DATA_PROCESSING))
                .thenReturn(Optional.of(previous));

        service.revoke(user.getId(), ConsentType.HEALTH_DATA_PROCESSING);

        verify(consentRecordRepository).save(argThatHasVersion(4));
    }

    private ConsentRecord argThatHasVersion(int expectedVersion) {
        return argThat(record -> record.getConsentVersion() == expectedVersion);
    }
}
