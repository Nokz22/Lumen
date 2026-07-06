package dev.lumen.presentation.moodcheckin;

import dev.lumen.application.moodcheckin.MoodCheckInResponse;
import dev.lumen.application.moodcheckin.MoodCheckInService;
import dev.lumen.presentation.moodcheckin.dto.MoodCheckInRequest;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * No authentication yet (Phase 2) — {userId} is taken from the path explicitly. Phase 2
 * keeps this path shape and adds a check that it matches the authenticated principal.
 */
@RestController
@RequestMapping("/api/v1/users/{userId}/mood-check-ins")
public class MoodCheckInController {

    private final MoodCheckInService moodCheckInService;

    public MoodCheckInController(MoodCheckInService moodCheckInService) {
        this.moodCheckInService = moodCheckInService;
    }

    @PostMapping
    public MoodCheckInResponse checkIn(@PathVariable UUID userId, @Valid @RequestBody MoodCheckInRequest request) {
        return moodCheckInService.checkIn(
                userId,
                request.emotion(),
                request.energyLevel(),
                request.sleepHours(),
                request.sleepQuality(),
                request.note());
    }

    @GetMapping
    public List<MoodCheckInResponse> history(@PathVariable UUID userId) {
        return moodCheckInService.getHistory(userId);
    }
}
