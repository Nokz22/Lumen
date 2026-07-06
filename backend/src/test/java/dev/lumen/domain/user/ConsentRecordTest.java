package dev.lumen.domain.user;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;
import org.junit.jupiter.api.Test;

class ConsentRecordTest {

    private final User user =
            new User("jane@lumen.dev", "hashed-password", "Jane", "en", "PT", LocalDate.of(1990, 1, 1), Role.USER);

    @Test
    void shouldStampGrantedAtAndLeaveRevokedAtNullWhenGranted() {
        ConsentRecord consent = new ConsentRecord(user, ConsentType.HEALTH_DATA_PROCESSING, true, 1);

        assertThat(consent.isGranted()).isTrue();
        assertThat(consent.getGrantedAt()).isNotNull();
        assertThat(consent.getRevokedAt()).isNull();
        assertThat(consent.getUser()).isEqualTo(user);
        assertThat(consent.getConsentType()).isEqualTo(ConsentType.HEALTH_DATA_PROCESSING);
        assertThat(consent.getConsentVersion()).isEqualTo(1);
        assertThat(consent.getId()).isNotNull();
        assertThat(consent.getCreatedAt()).isNotNull();
    }

    @Test
    void shouldStampRevokedAtAndLeaveGrantedAtNullWhenNotGranted() {
        ConsentRecord consent = new ConsentRecord(user, ConsentType.LLM_PROCESSING, false, 2);

        assertThat(consent.isGranted()).isFalse();
        assertThat(consent.getRevokedAt()).isNotNull();
        assertThat(consent.getGrantedAt()).isNull();
    }
}
