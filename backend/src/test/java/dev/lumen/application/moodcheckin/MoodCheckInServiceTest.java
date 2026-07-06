package dev.lumen.application.moodcheckin;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import dev.lumen.domain.moodcheckin.MoodCheckIn;
import dev.lumen.domain.moodcheckin.MoodCheckInRepository;
import dev.lumen.domain.moodcheckin.MoodEmotion;
import dev.lumen.domain.user.User;
import dev.lumen.domain.user.UserNotFoundException;
import dev.lumen.domain.user.UserRepository;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class MoodCheckInServiceTest {

    private final UserRepository userRepository = mock(UserRepository.class);
    private final MoodCheckInRepository moodCheckInRepository = mock(MoodCheckInRepository.class);
    private final MoodCheckInService service =
            new MoodCheckInService(moodCheckInRepository, userRepository, new MoodCheckInMapperImpl());

    private User user;

    @BeforeEach
    void setUp() {
        user = new User("jane@lumen.dev", "Jane", "en", "PT");
        when(userRepository.findById(user.getId())).thenReturn(Optional.of(user));
    }

    @Test
    void shouldCreateNewCheckInWhenNoneExistsForToday() {
        when(moodCheckInRepository.findByUserIdAndCheckInDate(user.getId(), LocalDate.now(ZoneOffset.UTC)))
                .thenReturn(Optional.empty());
        when(moodCheckInRepository.save(any(MoodCheckIn.class))).thenAnswer(invocation -> invocation.getArgument(0));

        MoodCheckInResponse result =
                service.checkIn(user.getId(), MoodEmotion.HAPPY, 4, new BigDecimal("7.5"), 4, "Great day");

        assertThat(result.emotion()).isEqualTo(MoodEmotion.HAPPY);
        assertThat(result.energyLevel()).isEqualTo(4);
    }

    @Test
    void shouldUpdateExistingCheckInForTodayInsteadOfCreatingAnother() {
        MoodCheckIn existing = new MoodCheckIn(
                user, MoodEmotion.NEUTRAL, 3, new BigDecimal("6.0"), 3, null, LocalDate.now(ZoneOffset.UTC));
        when(moodCheckInRepository.findByUserIdAndCheckInDate(user.getId(), LocalDate.now(ZoneOffset.UTC)))
                .thenReturn(Optional.of(existing));
        when(moodCheckInRepository.save(any(MoodCheckIn.class))).thenAnswer(invocation -> invocation.getArgument(0));

        MoodCheckInResponse result =
                service.checkIn(user.getId(), MoodEmotion.ANXIOUS, 2, new BigDecimal("5.0"), 2, "Rough day");

        assertThat(result.emotion()).isEqualTo(MoodEmotion.ANXIOUS);
        verify(moodCheckInRepository).save(existing);
    }

    @Test
    void shouldThrowWhenUserDoesNotExist() {
        UUID unknownUserId = UUID.randomUUID();
        when(userRepository.findById(unknownUserId)).thenReturn(Optional.empty());

        assertThatThrownBy(
                        () -> service.checkIn(unknownUserId, MoodEmotion.HAPPY, 4, new BigDecimal("7.0"), 4, null))
                .isInstanceOf(UserNotFoundException.class);
    }
}
