package dev.lumen.application.companion;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import dev.lumen.domain.companion.ConversationMessage;
import dev.lumen.domain.companion.ConversationMessageRepository;
import dev.lumen.domain.companion.ConversationRole;
import dev.lumen.domain.companion.ConversationSummary;
import dev.lumen.domain.companion.ConversationSummaryRepository;
import dev.lumen.domain.companion.LlmPrompt;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class ConversationContextBuilderTest {

    private final ConversationMessageRepository conversationMessageRepository =
            mock(ConversationMessageRepository.class);
    private final ConversationSummaryRepository conversationSummaryRepository =
            mock(ConversationSummaryRepository.class);
    private final ConversationContextBuilder contextBuilder =
            new ConversationContextBuilder(conversationMessageRepository, conversationSummaryRepository);

    private final UUID userId = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        when(conversationSummaryRepository.findByUserId(userId)).thenReturn(Optional.empty());
    }

    @Test
    void shouldIncludeAllMessagesInChatPromptWhenBelowWindowSize() {
        List<ConversationMessage> messages = messages(5);
        when(conversationMessageRepository.findByUserIdOrderByCreatedAtAsc(userId)).thenReturn(messages);

        LlmPrompt prompt = contextBuilder.buildChatPrompt(userId);

        assertThat(prompt.messages()).hasSize(5);
        assertThat(prompt.systemPrompt()).isEqualTo(CompanionSystemPrompt.TEXT);
    }

    @Test
    void shouldOnlyIncludeLastWindowSizeMessagesWhenThereAreMore() {
        List<ConversationMessage> messages = messages(25);
        when(conversationMessageRepository.findByUserIdOrderByCreatedAtAsc(userId)).thenReturn(messages);

        LlmPrompt prompt = contextBuilder.buildChatPrompt(userId);

        assertThat(prompt.messages()).hasSize(ConversationContextBuilder.WINDOW_SIZE);
    }

    @Test
    void shouldPrependExistingSummaryToChatPrompt() {
        ConversationSummary summary =
                new ConversationSummary(userId, "They have been discussing sleep.", UUID.randomUUID());
        when(conversationSummaryRepository.findByUserId(userId)).thenReturn(Optional.of(summary));
        when(conversationMessageRepository.findByUserIdOrderByCreatedAtAsc(userId)).thenReturn(messages(3));

        LlmPrompt prompt = contextBuilder.buildChatPrompt(userId);

        assertThat(prompt.messages().get(0).content()).contains("They have been discussing sleep.");
    }

    @Test
    void shouldNotPlanSummarizationAtOrBelowThreshold() {
        List<ConversationMessage> messages = messages(ConversationContextBuilder.SUMMARIZE_TRIGGER);
        when(conversationMessageRepository.findByUserIdOrderByCreatedAtAsc(userId)).thenReturn(messages);

        Optional<SummarizationPlan> plan = contextBuilder.planSummarizationIfNeeded(userId);

        assertThat(plan).isEmpty();
    }

    @Test
    void shouldPlanSummarizationAboveThresholdCoveringOnlyTheOldestMessages() {
        List<ConversationMessage> messages = messages(ConversationContextBuilder.SUMMARIZE_TRIGGER + 5);
        when(conversationMessageRepository.findByUserIdOrderByCreatedAtAsc(userId)).thenReturn(messages);

        Optional<SummarizationPlan> plan = contextBuilder.planSummarizationIfNeeded(userId);

        assertThat(plan).isPresent();
        int expectedFoldedCount = messages.size() - ConversationContextBuilder.WINDOW_SIZE;
        assertThat(plan.get().prompt().messages()).hasSize(expectedFoldedCount);
        assertThat(plan.get().throughMessageId())
                .isEqualTo(messages.get(expectedFoldedCount - 1).getId());
    }

    @Test
    void shouldOnlyConsiderMessagesAfterTheExistingSummarizedThroughPoint() {
        List<ConversationMessage> messages = messages(10);
        UUID throughId = messages.get(4).getId();
        ConversationSummary summary = new ConversationSummary(userId, "earlier context", throughId);
        when(conversationSummaryRepository.findByUserId(userId)).thenReturn(Optional.of(summary));
        when(conversationMessageRepository.findByUserIdOrderByCreatedAtAsc(userId)).thenReturn(messages);

        LlmPrompt prompt = contextBuilder.buildChatPrompt(userId);

        // 1 synthetic summary message + the 5 messages after index 4
        assertThat(prompt.messages()).hasSize(6);
    }

    private List<ConversationMessage> messages(int count) {
        List<ConversationMessage> messages = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            ConversationRole role = i % 2 == 0 ? ConversationRole.USER : ConversationRole.ASSISTANT;
            messages.add(new ConversationMessage(userId, role, "message " + i));
        }
        return messages;
    }
}
