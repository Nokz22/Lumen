package dev.lumen.application.companion;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import dev.lumen.domain.companion.CompanionStreamNotifier;
import dev.lumen.domain.companion.ConversationMessage;
import dev.lumen.domain.companion.ConversationMessageRepository;
import dev.lumen.domain.companion.ConversationRole;
import dev.lumen.domain.companion.ConversationSummaryRepository;
import dev.lumen.domain.companion.LlmClient;
import dev.lumen.domain.companion.LlmPrompt;
import dev.lumen.domain.companion.LlmStreamHandler;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

class CompanionResponseServiceTest {

    private final ConversationMessageRepository conversationMessageRepository =
            mock(ConversationMessageRepository.class);
    private final ConversationSummaryRepository conversationSummaryRepository =
            mock(ConversationSummaryRepository.class);
    private final ConversationContextBuilder conversationContextBuilder = mock(ConversationContextBuilder.class);
    private final LlmClient llmClient = mock(LlmClient.class);
    private final ChatOutputVerifier chatOutputVerifier = mock(ChatOutputVerifier.class);
    private final CompanionStreamNotifier companionStreamNotifier = mock(CompanionStreamNotifier.class);

    private final CompanionResponseService service = new CompanionResponseService(
            conversationMessageRepository,
            conversationSummaryRepository,
            conversationContextBuilder,
            llmClient,
            chatOutputVerifier,
            companionStreamNotifier);

    private final UUID userId = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        when(conversationContextBuilder.buildChatPrompt(userId)).thenReturn(new LlmPrompt("system", List.of()));
        when(conversationContextBuilder.planSummarizationIfNeeded(userId)).thenReturn(Optional.empty());
        when(conversationMessageRepository.save(any(ConversationMessage.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
    }

    @Test
    void shouldPersistAndStreamTheReplyWhenOutputVerificationPasses() {
        when(chatOutputVerifier.isSafe(anyString())).thenReturn(true);
        completeWith("It sounds like today was a lot. Thanks for sharing that.");

        service.generateResponseAsync(userId);

        ArgumentCaptor<ConversationMessage> captor = ArgumentCaptor.forClass(ConversationMessage.class);
        verify(conversationMessageRepository).save(captor.capture());
        assertThat(captor.getValue().getRole()).isEqualTo(ConversationRole.ASSISTANT);
        assertThat(captor.getValue().getContent())
                .isEqualTo("It sounds like today was a lot. Thanks for sharing that.");
        verify(companionStreamNotifier).sendComplete(eq(userId), any());
    }

    @Test
    void shouldSubstituteTheSafeFallbackWhenOutputVerificationFails() {
        when(chatOutputVerifier.isSafe(anyString())).thenReturn(false);
        completeWith("You have generalized anxiety disorder.");

        service.generateResponseAsync(userId);

        ArgumentCaptor<ConversationMessage> captor = ArgumentCaptor.forClass(ConversationMessage.class);
        verify(conversationMessageRepository).save(captor.capture());
        assertThat(captor.getValue().getContent()).isEqualTo(ChatOutputVerifier.SAFE_FALLBACK_MESSAGE);
    }

    @Test
    void shouldPersistAndSendAGracefulFallbackWhenTheLlmCallFails() {
        errorWith(new IllegalStateException("provider unreachable"));

        service.generateResponseAsync(userId);

        verify(conversationMessageRepository).save(any(ConversationMessage.class));
        verify(companionStreamNotifier).sendError(eq(userId), anyString());
        verify(companionStreamNotifier, never()).sendComplete(any(), any());
    }

    @Test
    void shouldTriggerSummarizationWhenTheContextBuilderPlansOne() {
        when(chatOutputVerifier.isSafe(anyString())).thenReturn(true);
        UUID throughId = UUID.randomUUID();
        SummarizationPlan plan = new SummarizationPlan(new LlmPrompt("summarize", List.of()), throughId);
        when(conversationContextBuilder.planSummarizationIfNeeded(userId)).thenReturn(Optional.of(plan));
        when(llmClient.complete(plan.prompt())).thenReturn("They discussed sleep and energy.");
        when(conversationSummaryRepository.findByUserId(userId)).thenReturn(Optional.empty());
        completeWith("Thanks for sharing.");

        service.generateResponseAsync(userId);

        verify(conversationSummaryRepository).save(any());
    }

    private void completeWith(String fullText) {
        doAnswer(invocation -> {
                    LlmStreamHandler handler = invocation.getArgument(1);
                    handler.onComplete(fullText);
                    return null;
                })
                .when(llmClient)
                .completeStreaming(any(), any());
    }

    private void errorWith(Throwable error) {
        doAnswer(invocation -> {
                    LlmStreamHandler handler = invocation.getArgument(1);
                    handler.onError(error);
                    return null;
                })
                .when(llmClient)
                .completeStreaming(any(), any());
    }
}
