package dev.lumen.infrastructure.companion;

import com.anthropic.client.AnthropicClient;
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
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import io.github.resilience4j.decorators.Decorators;
import io.github.resilience4j.retry.Retry;
import io.github.resilience4j.retry.RetryRegistry;
import java.util.stream.Collectors;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/**
 * Real provider (app.llm.provider=anthropic). Timeout is enforced at the HTTP client
 * level (an explicit configured Duration, not Resilience4j's TimeLimiter — that would
 * mean bridging every blocking SDK call through a second thread pool just to get a
 * Future to time out, for no benefit over a plain socket timeout here). Retry and
 * circuit breaker are true Resilience4j decorators, applied programmatically rather
 * than via annotations because completeStreaming's callback shape doesn't fit the
 * annotation-over-return-value pattern the Resilience4j AOP aspects expect (see
 * ADR-0010). Retry only wraps complete(): retrying completeStreaming() risks
 * duplicating content a person may already be seeing mid-stream.
 */
@Component
@ConditionalOnProperty(name = "app.llm.provider", havingValue = "anthropic")
class AnthropicLlmClient implements LlmClient {

    static final String RESILIENCE_INSTANCE_NAME = "anthropic-llm";
    private static final long MAX_TOKENS = 1024;

    private final AnthropicClient client;
    private final String model;
    private final Retry retry;
    private final CircuitBreaker circuitBreaker;

    AnthropicLlmClient(
            AnthropicClient client,
            @Value("${app.llm.anthropic-model}") String model,
            RetryRegistry retryRegistry,
            CircuitBreakerRegistry circuitBreakerRegistry) {
        this.client = client;
        this.model = model;
        this.retry = retryRegistry.retry(RESILIENCE_INSTANCE_NAME);
        this.circuitBreaker = circuitBreakerRegistry.circuitBreaker(RESILIENCE_INSTANCE_NAME);
    }

    @Override
    public void completeStreaming(LlmPrompt prompt, LlmStreamHandler handler) {
        StringBuilder assembled = new StringBuilder();
        Runnable call = Decorators.ofRunnable(() -> streamOnce(prompt, handler, assembled))
                .withCircuitBreaker(circuitBreaker)
                .decorate();
        try {
            call.run();
        } catch (RuntimeException e) {
            handler.onError(e);
            return;
        }
        handler.onComplete(assembled.toString());
    }

    private void streamOnce(LlmPrompt prompt, LlmStreamHandler handler, StringBuilder assembled) {
        try (StreamResponse<RawMessageStreamEvent> response =
                client.messages().createStreaming(toParams(prompt))) {
            response.stream().forEach(event -> event.contentBlockDelta().ifPresent(delta -> {
                if (delta.delta().isText()) {
                    String textChunk = delta.delta().asText().text();
                    assembled.append(textChunk);
                    handler.onChunk(textChunk);
                }
            }));
        }
    }

    @Override
    public String complete(LlmPrompt prompt) {
        return Decorators.ofSupplier(() -> rawComplete(prompt))
                .withRetry(retry)
                .withCircuitBreaker(circuitBreaker)
                .decorate()
                .get();
    }

    private String rawComplete(LlmPrompt prompt) {
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
