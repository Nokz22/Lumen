package dev.lumen.application.companion;

import dev.lumen.domain.companion.CompanionStreamNotifier;
import dev.lumen.domain.companion.ConversationMessage;
import dev.lumen.domain.companion.ConversationMessageRepository;
import dev.lumen.domain.companion.ConversationRole;
import dev.lumen.domain.companion.ConversationSummary;
import dev.lumen.domain.companion.ConversationSummaryRepository;
import dev.lumen.domain.companion.LlmClient;
import dev.lumen.domain.companion.LlmPrompt;
import dev.lumen.domain.companion.LlmStreamHandler;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

/**
 * Runs off the request thread (see ConversationService.scheduleResponseAfterCommit).
 * Deliberately buffers the model's streamed output and verifies the assembled text
 * before anything reaches the person, rather than forwarding raw chunks live: with
 * true token-by-token forwarding, a guardrail failure caught only at the end would
 * mean the unsafe partial text was already on screen. Re-chunking the verified text
 * afterwards keeps the live-typing feel without ever risking that (see ADR-0010).
 * LlmClient.completeStreaming's onChunk callback is still exercised end to end (a
 * future incremental-verification design could use it directly) — this consumer
 * just doesn't act on it.
 */
@Service
public class CompanionResponseService {

    private static final Logger LOG = LoggerFactory.getLogger(CompanionResponseService.class);
    private static final String LLM_UNAVAILABLE_FALLBACK =
            "I'm having trouble responding right now — please try again in a moment.";

    private final ConversationMessageRepository conversationMessageRepository;
    private final ConversationSummaryRepository conversationSummaryRepository;
    private final ConversationContextBuilder conversationContextBuilder;
    private final LlmClient llmClient;
    private final ChatOutputVerifier chatOutputVerifier;
    private final CompanionStreamNotifier companionStreamNotifier;

    public CompanionResponseService(
            ConversationMessageRepository conversationMessageRepository,
            ConversationSummaryRepository conversationSummaryRepository,
            ConversationContextBuilder conversationContextBuilder,
            LlmClient llmClient,
            ChatOutputVerifier chatOutputVerifier,
            CompanionStreamNotifier companionStreamNotifier) {
        this.conversationMessageRepository = conversationMessageRepository;
        this.conversationSummaryRepository = conversationSummaryRepository;
        this.conversationContextBuilder = conversationContextBuilder;
        this.llmClient = llmClient;
        this.chatOutputVerifier = chatOutputVerifier;
        this.companionStreamNotifier = companionStreamNotifier;
    }

    @Async
    public void generateResponseAsync(UUID userId) {
        LlmPrompt prompt = conversationContextBuilder.buildChatPrompt(userId);
        try {
            llmClient.completeStreaming(prompt, new LlmStreamHandler() {
                @Override
                public void onChunk(String textChunk) {
                    // Intentionally unused — see class-level note.
                }

                @Override
                public void onComplete(String fullText) {
                    finalizeResponse(userId, fullText);
                }

                @Override
                public void onError(Throwable error) {
                    handleError(userId, error);
                }
            });
        } catch (RuntimeException e) {
            handleError(userId, e);
        }
    }

    private void finalizeResponse(UUID userId, String fullText) {
        String safeText = chatOutputVerifier.isSafe(fullText) ? fullText : ChatOutputVerifier.SAFE_FALLBACK_MESSAGE;

        for (String word : safeText.split(" ")) {
            companionStreamNotifier.sendChunk(userId, word + " ");
        }

        ConversationMessage assistantMessage = conversationMessageRepository.save(
                new ConversationMessage(userId, ConversationRole.ASSISTANT, safeText));
        maybeSummarize(userId);
        companionStreamNotifier.sendComplete(userId, assistantMessage.getId());
    }

    private void handleError(UUID userId, Throwable error) {
        LOG.error("Companion LLM call failed for a conversation turn", error);
        conversationMessageRepository.save(
                new ConversationMessage(userId, ConversationRole.ASSISTANT, LLM_UNAVAILABLE_FALLBACK));
        companionStreamNotifier.sendError(userId, LLM_UNAVAILABLE_FALLBACK);
    }

    private void maybeSummarize(UUID userId) {
        conversationContextBuilder.planSummarizationIfNeeded(userId).ifPresent(plan -> {
            String summaryText = llmClient.complete(plan.prompt());
            ConversationSummary summary = conversationSummaryRepository
                    .findByUserId(userId)
                    .map(existing -> {
                        existing.update(summaryText, plan.throughMessageId());
                        return existing;
                    })
                    .orElseGet(() -> new ConversationSummary(userId, summaryText, plan.throughMessageId()));
            conversationSummaryRepository.save(summary);
        });
    }
}
