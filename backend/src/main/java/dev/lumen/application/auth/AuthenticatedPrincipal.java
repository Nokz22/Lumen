package dev.lumen.application.auth;

import dev.lumen.domain.user.Role;
import java.util.UUID;

public record AuthenticatedPrincipal(UUID userId, Role role) {
}
