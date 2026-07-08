package dev.lumen.application.companion;

import dev.lumen.domain.companion.ConversationRole;
import java.time.Instant;
import java.util.UUID;

public record ConversationMessageResponse(UUID id, ConversationRole role, String content, Instant createdAt) {
}
