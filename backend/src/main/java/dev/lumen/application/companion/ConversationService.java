package dev.lumen.application.companion;

import dev.lumen.application.consent.ConsentService;
import dev.lumen.application.crisis.CrisisTriggerOutcome;
import dev.lumen.application.crisis.RiskEventTriggerService;
import dev.lumen.domain.companion.ConversationMessage;
import dev.lumen.domain.companion.ConversationMessageRepository;
import dev.lumen.domain.companion.ConversationRole;
import dev.lumen.domain.crisis.TriggerSource;
import dev.lumen.domain.user.ConsentRequiredException;
import dev.lumen.domain.user.ConsentType;
import dev.lumen.domain.user.User;
import dev.lumen.domain.user.UserNotFoundException;
import dev.lumen.domain.user.UserRepository;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

/**
 * The one method in the whole codebase that most directly implements CLAUDE.md's
 * defining invariant: "O LLM NUNCA é invocado quando o classificador de risco de
 * entrada dispara." The risk check happens before anything that could call the LLM
 * even exists in the call stack — see submitMessage(). The async LLM call is
 * deliberately kicked off from an afterCommit transaction synchronization, not
 * directly inside this @Transactional method: firing it before commit risks
 * CompanionResponseService reading the just-saved user message on another thread
 * before the write is actually visible.
 */
@Service
public class ConversationService {

    private final ConversationMessageRepository conversationMessageRepository;
    private final UserRepository userRepository;
    private final ConsentService consentService;
    private final ChatRiskClassifier chatRiskClassifier;
    private final RiskEventTriggerService riskEventTriggerService;
    private final CompanionResponseService companionResponseService;

    public ConversationService(
            ConversationMessageRepository conversationMessageRepository,
            UserRepository userRepository,
            ConsentService consentService,
            ChatRiskClassifier chatRiskClassifier,
            RiskEventTriggerService riskEventTriggerService,
            CompanionResponseService companionResponseService) {
        this.conversationMessageRepository = conversationMessageRepository;
        this.userRepository = userRepository;
        this.consentService = consentService;
        this.chatRiskClassifier = chatRiskClassifier;
        this.riskEventTriggerService = riskEventTriggerService;
        this.companionResponseService = companionResponseService;
    }

    @Transactional
    public ConversationSubmissionResult submitMessage(UUID userId, String content) {
        User user = userRepository.findById(userId).orElseThrow(() -> new UserNotFoundException(userId));
        requireLlmConsent(userId);

        ConversationMessage userMessage =
                conversationMessageRepository.save(new ConversationMessage(userId, ConversationRole.USER, content));

        if (chatRiskClassifier.isHighRisk(content)) {
            CrisisTriggerOutcome outcome =
                    riskEventTriggerService.trigger(userId, null, TriggerSource.CHAT_MESSAGE, user.getRegion());
            return new ConversationCrisisResult(outcome.riskEventId(), outcome.resources());
        }

        scheduleResponseAfterCommit(userId);
        return new ConversationProcessingResult(userMessage.getId());
    }

    @Transactional(readOnly = true)
    public List<ConversationMessageResponse> getHistory(UUID userId) {
        if (userRepository.findById(userId).isEmpty()) {
            throw new UserNotFoundException(userId);
        }
        return conversationMessageRepository.findByUserIdOrderByCreatedAtAsc(userId).stream()
                .map(this::toResponse)
                .toList();
    }

    private void scheduleResponseAfterCommit(UUID userId) {
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                companionResponseService.generateResponseAsync(userId);
            }
        });
    }

    private void requireLlmConsent(UUID userId) {
        if (!consentService.isActive(userId, ConsentType.LLM_PROCESSING)) {
            throw new ConsentRequiredException(ConsentType.LLM_PROCESSING);
        }
    }

    private ConversationMessageResponse toResponse(ConversationMessage message) {
        return new ConversationMessageResponse(
                message.getId(), message.getRole(), message.getContent(), message.getCreatedAt());
    }
}
