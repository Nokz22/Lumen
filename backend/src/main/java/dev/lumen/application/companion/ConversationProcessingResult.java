package dev.lumen.application.companion;

import java.util.UUID;

public record ConversationProcessingResult(UUID userMessageId) implements ConversationSubmissionResult {
}
