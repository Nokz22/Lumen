package dev.lumen.infrastructure.companion;

import com.anthropic.client.AnthropicClient;
import com.anthropic.client.okhttp.AnthropicOkHttpClient;
import com.anthropic.core.http.StreamResponse;
import com.anthropic.models.messages.ContentBlock;
import com.anthropic.models.messages.Message;
import com.anthropic.models.messages.MessageCreateParams;
import com.anthropic.models.messages.RawMessageStreamEvent;
import dev.lumen.domain.companion.ConversationRole;
import dev.lumen.domain.companion.LlmClient;
import dev.lumen.domain.companion.LlmMessage;
import dev.lumen.domain.companion.LlmPrompt;
import dev.lumen.domain.companion.LlmStreamHandler;
import java.util.stream.Collectors;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/**
 * Real provider (app.llm.provider=anthropic). No resilience wrapping here yet — a
 * failed call surfaces as a RuntimeException; the caller (CompanionResponseService)
 * already catches around completeStreaming and turns it into a graceful fallback
 * message. Timeout/retry/circuit-breaker come next, wrapping these two methods.
 */
@Component
@ConditionalOnProperty(name = "app.llm.provider", havingValue = "anthropic")
class AnthropicLlmClient implements LlmClient {

    private static final long MAX_TOKENS = 1024;

    private final AnthropicClient client;
    private final String model;

    AnthropicLlmClient(
            @Value("${app.llm.anthropic-api-key}") String apiKey, @Value("${app.llm.anthropic-model}") String model) {
        this.client = AnthropicOkHttpClient.builder().apiKey(apiKey).build();
        this.model = model;
    }

    @Override
    public void completeStreaming(LlmPrompt prompt, LlmStreamHandler handler) {
        StringBuilder assembled = new StringBuilder();
        try (StreamResponse<RawMessageStreamEvent> response =
                client.messages().createStreaming(toParams(prompt))) {
            response.stream().forEach(event -> event.contentBlockDelta().ifPresent(delta -> {
                if (delta.delta().isText()) {
                    String textChunk = delta.delta().asText().text();
                    assembled.append(textChunk);
                    handler.onChunk(textChunk);
                }
            }));
        } catch (RuntimeException e) {
            handler.onError(e);
            return;
        }
        handler.onComplete(assembled.toString());
    }

    @Override
    public String complete(LlmPrompt prompt) {
        Message message = client.messages().create(toParams(prompt));
        return message.content().stream()
                .filter(ContentBlock::isText)
                .map(block -> block.asText().text())
                .collect(Collectors.joining());
    }

    private MessageCreateParams toParams(LlmPrompt prompt) {
        MessageCreateParams.Builder builder =
                MessageCreateParams.builder().model(model).maxTokens(MAX_TOKENS).system(prompt.systemPrompt());
        for (LlmMessage message : prompt.messages()) {
            if (message.role() == ConversationRole.USER) {
                builder.addUserMessage(message.content());
            } else {
                builder.addAssistantMessage(message.content());
            }
        }
        return builder.build();
    }
}
