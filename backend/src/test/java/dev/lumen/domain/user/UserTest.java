package dev.lumen.domain.user;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;
import org.junit.jupiter.api.Test;

class UserTest {

    @Test
    void shouldExposeConstructorValuesThroughGetters() {
        User user = new User(
                "jane@lumen.dev", "hashed-password", "Jane", "en", "PT", LocalDate.of(1990, 1, 1), Role.USER);

        assertThat(user.getId()).isNotNull();
        assertThat(user.getEmail()).isEqualTo("jane@lumen.dev");
        assertThat(user.getPasswordHash()).isEqualTo("hashed-password");
        assertThat(user.getDisplayName()).isEqualTo("Jane");
        assertThat(user.getLocale()).isEqualTo("en");
        assertThat(user.getRegion()).isEqualTo("PT");
        assertThat(user.getDateOfBirth()).isEqualTo(LocalDate.of(1990, 1, 1));
        assertThat(user.getRole()).isEqualTo(Role.USER);
        assertThat(user.getCreatedAt()).isNotNull();
        assertThat(user.getVersion()).isZero();
    }
}
