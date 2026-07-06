package dev.lumen.application.moodcheckin;

import dev.lumen.domain.moodcheckin.MoodCheckIn;
import dev.lumen.domain.moodcheckin.MoodCheckInRepository;
import dev.lumen.domain.moodcheckin.MoodEmotion;
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

    public MoodCheckInService(
            MoodCheckInRepository moodCheckInRepository, UserRepository userRepository, MoodCheckInMapper mapper) {
        this.moodCheckInRepository = moodCheckInRepository;
        this.userRepository = userRepository;
        this.mapper = mapper;
    }

    @Transactional
    public MoodCheckInResponse checkIn(
            UUID userId, MoodEmotion emotion, int energyLevel, BigDecimal sleepHours, int sleepQuality, String note) {
        User user = userRepository.findById(userId).orElseThrow(() -> new UserNotFoundException(userId));
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
        return moodCheckInRepository.findByUserIdOrderByCheckInDateDesc(userId).stream()
                .map(mapper::toResponse)
                .toList();
    }
}
