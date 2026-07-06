package dev.lumen.application.user;

import dev.lumen.domain.user.Role;
import java.time.Instant;
import java.util.UUID;

/**
 * Never includes dateOfBirth or passwordHash — a summary is what any authenticated
 * context (the user themselves, or an ADMIN listing accounts) is allowed to see.
 */
public record UserSummaryResponse(UUID id, String email, String displayName, Role role, Instant createdAt) {
}
