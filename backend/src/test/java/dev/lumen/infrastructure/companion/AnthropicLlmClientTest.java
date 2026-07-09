package dev.lumen.infrastructure.companion;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.anthropic.client.AnthropicClient;
import com.anthropic.models.messages.ContentBlock;
import com.anthropic.models.messages.Message;
import com.anthropic.models.messages.MessageCreateParams;
import com.anthropic.models.messages.TextBlock;
import com.anthropic.services.blocking.MessageService;
import dev.lumen.domain.companion.ConversationRole;
import dev.lumen.domain.companion.LlmMessage;
import dev.lumen.domain.companion.LlmPrompt;
import dev.lumen.domain.companion.LlmStreamHandler;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import io.github.resilience4j.retry.RetryConfig;
import io.github.resilience4j.retry.RetryRegistry;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Proves the resilience wiring behaves as designed: complete() (summarization) retries
 * a transient failure and gives up after the configured attempts, while
 * completeStreaming() (the visible chat reply) never retries — a retry there could
 * duplicate content a person is already mid-way through seeing. The chunk-assembly
 * happy path is exercised by CannedLlmClientTest and the Testcontainers
 * ConversationIntegrationTest instead of here: the SDK's Kotlin-generated stream event
 * value types (TextDelta and friends) have final methods that Mockito's inline mock
 * maker cannot stub, so deep-mocking a streamed response isn't a viable unit test for
 * this SDK.
 */
class AnthropicLlmClientTest {

    private final AnthropicClient client = mock(AnthropicClient.class);
    private final MessageService messageService = mock(MessageService.class);
    private final LlmPrompt prompt = new LlmPrompt("system", List.of(new LlmMessage(ConversationRole.USER, "hi")));

    private AnthropicLlmClient anthropicLlmClient;

    @BeforeEach
    void setUp() {
        when(client.messages()).thenReturn(messageService);
        RetryRegistry retryRegistry = RetryRegistry.of(Map.of(
                AnthropicLlmClient.RESILIENCE_INSTANCE_NAME,
                RetryConfig.custom()
                        .maxAttempts(3)
                        .waitDuration(Duration.ofMillis(5))
                        .build()));
        anthropicLlmClient =
                new AnthropicLlmClient(client, "claude-sonnet-5", retryRegistry, CircuitBreakerRegistry.ofDefaults());
    }

    @Test
    void shouldRetryCompleteOnceOnATransientFailureThenSucceed() {
        when(messageService.create(any(MessageCreateParams.class)))
                .thenThrow(new RuntimeException("transient"))
                .thenReturn(messageWithText("Summary text"));

        String result = anthropicLlmClient.complete(prompt);

        assertThat(result).isEqualTo("Summary text");
        verify(messageService, times(2)).create(any(MessageCreateParams.class));
    }

    @Test
    void shouldGiveUpCompleteAfterExhaustingRetries() {
        when(messageService.create(any(MessageCreateParams.class))).thenThrow(new RuntimeException("boom"));

        assertThatThrownBy(() -> anthropicLlmClient.complete(prompt)).isInstanceOf(RuntimeException.class);
        verify(messageService, times(3)).create(any(MessageCreateParams.class));
    }

    @Test
    void shouldNotRetryCompleteStreamingAndShouldReportTheErrorInstead() {
        when(messageService.createStreaming(any(MessageCreateParams.class))).thenThrow(new RuntimeException("boom"));
        LlmStreamHandler handler = mock(LlmStreamHandler.class);

        anthropicLlmClient.completeStreaming(prompt, handler);

        verify(messageService, times(1)).createStreaming(any(MessageCreateParams.class));
        verify(handler).onError(any());
        verify(handler, never()).onComplete(any());
    }

    private Message messageWithText(String text) {
        TextBlock textBlock = mock(TextBlock.class);
        when(textBlock.text()).thenReturn(text);
        ContentBlock contentBlock = mock(ContentBlock.class);
        when(contentBlock.isText()).thenReturn(true);
        when(contentBlock.asText()).thenReturn(textBlock);
        Message message = mock(Message.class);
        when(message.content()).thenReturn(List.of(contentBlock));
        return message;
    }
}
