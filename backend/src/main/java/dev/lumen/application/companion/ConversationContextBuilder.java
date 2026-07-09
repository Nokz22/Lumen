package dev.lumen.application.companion;

import dev.lumen.domain.companion.ConversationMessage;
import dev.lumen.domain.companion.ConversationMessageRepository;
import dev.lumen.domain.companion.ConversationRole;
import dev.lumen.domain.companion.ConversationSummary;
import dev.lumen.domain.companion.ConversationSummaryRepository;
import dev.lumen.domain.companion.LlmMessage;
import dev.lumen.domain.companion.LlmPrompt;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Component;

/**
 * History grows without bound (every ConversationMessage is kept forever), but what
 * gets sent to the model each turn is deliberately capped — see ADR-0009. The last
 * WINDOW_SIZE messages always go in raw; once the unsummarized tail grows past
 * SUMMARIZE_TRIGGER, everything older than the window is folded into
 * ConversationSummary and dropped from future prompts.
 */
@Component
class ConversationContextBuilder {

    static final int WINDOW_SIZE = 20;
    static final int SUMMARIZE_TRIGGER = 30;

    private static final String SUMMARIZATION_SYSTEM_PROMPT =
            "Summarize the conversation so far in a few concise sentences, preserving important "
                    + "emotional context and any recurring themes. Do not diagnose or use clinical labels.";

    private final ConversationMessageRepository conversationMessageRepository;
    private final ConversationSummaryRepository conversationSummaryRepository;

    ConversationContextBuilder(
            ConversationMessageRepository conversationMessageRepository,
            ConversationSummaryRepository conversationSummaryRepository) {
        this.conversationMessageRepository = conversationMessageRepository;
        this.conversationSummaryRepository = conversationSummaryRepository;
    }

    LlmPrompt buildChatPrompt(UUID userId) {
        List<ConversationMessage> unsummarized = unsummarizedMessages(userId);
        Optional<ConversationSummary> summary = conversationSummaryRepository.findByUserId(userId);

        List<LlmMessage> messages = new ArrayList<>();
        summary.ifPresent(s -> messages.add(
                new LlmMessage(ConversationRole.ASSISTANT, "Summary of earlier conversation: " + s.getSummaryText())));

        int fromIndex = Math.max(0, unsummarized.size() - WINDOW_SIZE);
        unsummarized
                .subList(fromIndex, unsummarized.size())
                .forEach(m -> messages.add(new LlmMessage(m.getRole(), m.getContent())));

        return new LlmPrompt(CompanionSystemPrompt.TEXT, messages);
    }

    Optional<SummarizationPlan> planSummarizationIfNeeded(UUID userId) {
        List<ConversationMessage> unsummarized = unsummarizedMessages(userId);
        if (unsummarized.size() <= SUMMARIZE_TRIGGER) {
            return Optional.empty();
        }

        int keepRawCount = Math.min(WINDOW_SIZE, unsummarized.size());
        List<ConversationMessage> toFold = unsummarized.subList(0, unsummarized.size() - keepRawCount);
        if (toFold.isEmpty()) {
            return Optional.empty();
        }

        Optional<ConversationSummary> existing = conversationSummaryRepository.findByUserId(userId);
        List<LlmMessage> messages = new ArrayList<>();
        existing.ifPresent(s ->
                messages.add(new LlmMessage(ConversationRole.ASSISTANT, "Existing summary: " + s.getSummaryText())));
        toFold.forEach(m -> messages.add(new LlmMessage(m.getRole(), m.getContent())));

        LlmPrompt prompt = new LlmPrompt(SUMMARIZATION_SYSTEM_PROMPT, messages);
        UUID throughMessageId = toFold.get(toFold.size() - 1).getId();
        return Optional.of(new SummarizationPlan(prompt, throughMessageId));
    }

    private List<ConversationMessage> unsummarizedMessages(UUID userId) {
        List<ConversationMessage> all = conversationMessageRepository.findByUserIdOrderByCreatedAtAsc(userId);
        Optional<ConversationSummary> summary = conversationSummaryRepository.findByUserId(userId);
        if (summary.isEmpty()) {
            return all;
        }

        UUID throughId = summary.get().getSummarizedThroughMessageId();
        for (int i = 0; i < all.size(); i++) {
            if (all.get(i).getId().equals(throughId)) {
                return all.subList(i + 1, all.size());
            }
        }
        return all;
    }
}
