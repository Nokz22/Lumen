package dev.lumen.domain.companion;

import java.util.List;

public record LlmPrompt(String systemPrompt, List<LlmMessage> messages) {
}
