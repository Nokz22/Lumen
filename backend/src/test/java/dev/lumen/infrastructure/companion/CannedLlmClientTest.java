package dev.lumen.infrastructure.companion;

import static org.assertj.core.api.Assertions.assertThat;

import dev.lumen.domain.companion.ConversationRole;
import dev.lumen.domain.companion.LlmMessage;
import dev.lumen.domain.companion.LlmPrompt;
import dev.lumen.domain.companion.LlmStreamHandler;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

class CannedLlmClientTest {

    private final CannedLlmClient client = new CannedLlmClient();

    @Test
    void shouldStreamChunksThatAssembleIntoTheCompleteText() {
        LlmPrompt prompt = new LlmPrompt("system", List.of(new LlmMessage(ConversationRole.USER, "hello")));
        List<String> chunks = new ArrayList<>();
        StringBuilder completed = new StringBuilder();

        client.completeStreaming(prompt, new LlmStreamHandler() {
            @Override
            public void onChunk(String textChunk) {
                chunks.add(textChunk);
            }

            @Override
            public void onComplete(String fullText) {
                completed.append(fullText);
            }

            @Override
            public void onError(Throwable error) {
                throw new AssertionError("should not error", error);
            }
        });

        assertThat(chunks).isNotEmpty();
        assertThat(String.join("", chunks)).isEqualTo(completed.toString());
    }

    @Test
    void shouldReturnANonEmptySummary() {
        LlmPrompt prompt = new LlmPrompt("system", List.of());

        String summary = client.complete(prompt);

        assertThat(summary).isNotBlank();
    }
}
