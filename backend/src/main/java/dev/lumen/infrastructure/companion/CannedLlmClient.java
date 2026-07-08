package dev.lumen.infrastructure.companion;

import dev.lumen.domain.companion.LlmClient;
import dev.lumen.domain.companion.LlmPrompt;
import dev.lumen.domain.companion.LlmStreamHandler;
import java.util.List;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/**
 * Default provider (app.llm.provider=mock, or unset — matches the Fase 5
 * simulator-first pattern for WearableSource). Never calls a network, never costs
 * anything, and is what the test profile always uses (see application.yml).
 */
@Component
@ConditionalOnProperty(name = "app.llm.provider", havingValue = "mock", matchIfMissing = true)
class CannedLlmClient implements LlmClient {

    private static final List<String> CANNED_RESPONSES = List.of(
            "Thank you for sharing that. It sounds like today has had its ups and downs — would you like to"
                    + " talk more about what's been on your mind?",
            "I hear you. Noticing how you're feeling is worth something on its own. Is there anything specific"
                    + " you'd like support with right now?",
            "That makes sense. Small steps count, even on harder days. What's one thing that's felt manageable"
                    + " today?");

    @Override
    public void completeStreaming(LlmPrompt prompt, LlmStreamHandler handler) {
        String response = pickResponse(prompt);
        String[] words = response.split(" ");
        StringBuilder assembled = new StringBuilder();
        for (String word : words) {
            String chunk = assembled.isEmpty() ? word : " " + word;
            assembled.append(chunk);
            handler.onChunk(chunk);
        }
        handler.onComplete(assembled.toString());
    }

    @Override
    public String complete(LlmPrompt prompt) {
        return "Summary so far: the person has been checking in about their mood, sleep and daily patterns.";
    }

    private String pickResponse(LlmPrompt prompt) {
        int index = Math.floorMod(prompt.messages().size(), CANNED_RESPONSES.size());
        return CANNED_RESPONSES.get(index);
    }
}
