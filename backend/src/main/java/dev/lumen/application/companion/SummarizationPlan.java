package dev.lumen.application.companion;

import dev.lumen.domain.companion.LlmPrompt;
import java.util.UUID;

record SummarizationPlan(LlmPrompt prompt, UUID throughMessageId) {
}
