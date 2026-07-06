package dev.lumen.domain.user;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class UserTest {

    @Test
    void shouldExposeConstructorValuesThroughGetters() {
        User user = new User("jane@lumen.dev", "Jane", "en", "PT");

        assertThat(user.getId()).isNotNull();
        assertThat(user.getEmail()).isEqualTo("jane@lumen.dev");
        assertThat(user.getDisplayName()).isEqualTo("Jane");
        assertThat(user.getLocale()).isEqualTo("en");
        assertThat(user.getRegion()).isEqualTo("PT");
        assertThat(user.getCreatedAt()).isNotNull();
        assertThat(user.getVersion()).isZero();
    }
}
