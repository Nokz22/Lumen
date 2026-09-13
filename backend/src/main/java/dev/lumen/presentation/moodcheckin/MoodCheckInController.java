package dev.lumen.presentation.moodcheckin;

import dev.lumen.application.moodcheckin.MoodCheckInResponse;
import dev.lumen.application.moodcheckin.MoodCheckInService;
import dev.lumen.presentation.moodcheckin.dto.MoodCheckInRequest;
import dev.lumen.presentation.shared.PageParameters;
import dev.lumen.presentation.shared.PageResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.UUID;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Self-scoped, no ADMIN bypass: "ADMIN nunca lê conteúdo emocional de um USER"
 * (project-brief §3) applies here too — the check is identity, not role.
 */
@Tag(
        name = "Daily check-in",
        description = "Self-reported mood, energy and sleep. One check-in per UTC day; re-submitting the same day"
                + " updates it. Requires HEALTH_DATA_PROCESSING consent.")
@RestController
@RequestMapping("/api/v1/users/{userId}/mood-check-ins")
@PreAuthorize("#userId == authentication.principal.userId()")
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
    public PageResponse<MoodCheckInResponse> history(@PathVariable UUID userId, PageParameters pageParameters) {
        return PageResponse.from(moodCheckInService.getHistory(userId, pageParameters.toPageQuery()));
    }
}
