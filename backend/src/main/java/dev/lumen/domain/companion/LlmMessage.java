package dev.lumen.domain.companion;

public record LlmMessage(ConversationRole role, String content) {
}
