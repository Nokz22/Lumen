package dev.lumen.application.auth;

import dev.lumen.domain.user.Role;
import java.time.Instant;
import java.util.UUID;

public record AuthTokens(
        String accessToken,
        String refreshToken,
        Instant accessTokenExpiresAt,
        Instant refreshTokenExpiresAt,
        UUID userId,
        Role role) {
}
