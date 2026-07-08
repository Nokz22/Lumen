package dev.lumen.application.companion;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import dev.lumen.application.consent.ConsentService;
import dev.lumen.application.crisis.CrisisTriggerOutcome;
import dev.lumen.application.crisis.RiskEventTriggerService;
import dev.lumen.domain.companion.ConversationMessage;
import dev.lumen.domain.companion.ConversationMessageRepository;
import dev.lumen.domain.user.ConsentRequiredException;
import dev.lumen.domain.user.ConsentType;
import dev.lumen.domain.user.Role;
import dev.lumen.domain.user.User;
import dev.lumen.domain.user.UserNotFoundException;
import dev.lumen.domain.user.UserRepository;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

/**
 * The most important tests in the whole codebase: they exist to make it impossible
 * for a future change to silently break CLAUDE.md's defining invariant — "O LLM
 * NUNCA é invocado quando o classificador de risco de entrada dispara" — without a
 * test failing.
 */
class ConversationServiceTest {

    private final ConversationMessageRepository conversationMessageRepository =
            mock(ConversationMessageRepository.class);
    private final UserRepository userRepository = mock(UserRepository.class);
    private final ConsentService consentService = mock(ConsentService.class);
    private final ChatRiskClassifier chatRiskClassifier = mock(ChatRiskClassifier.class);
    private final RiskEventTriggerService riskEventTriggerService = mock(RiskEventTriggerService.class);
    private final CompanionResponseService companionResponseService = mock(CompanionResponseService.class);

    private final ConversationService service = new ConversationService(
            conversationMessageRepository,
            userRepository,
            consentService,
            chatRiskClassifier,
            riskEventTriggerService,
            companionResponseService);

    private final UUID userId = UUID.randomUUID();
    private User user;

    @BeforeEach
    void setUp() {
        user = new User("jane@lumen.dev", "hashed-password", "Jane", "en", "PT", LocalDate.of(1990, 1, 1), Role.USER);
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(consentService.isActive(userId, ConsentType.LLM_PROCESSING)).thenReturn(true);
        when(conversationMessageRepository.save(any(ConversationMessage.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        TransactionSynchronizationManager.initSynchronization();
    }

    @AfterEach
    void tearDown() {
        TransactionSynchronizationManager.clearSynchronization();
    }

    @Test
    void shouldRejectSubmissionWhenConsentIsNotActive() {
        when(consentService.isActive(userId, ConsentType.LLM_PROCESSING)).thenReturn(false);

        assertThatThrownBy(() -> service.submitMessage(userId, "hello"))
                .isInstanceOf(ConsentRequiredException.class);
    }

    @Test
    void shouldThrowWhenUserDoesNotExist() {
        UUID unknownUserId = UUID.randomUUID();
        when(userRepository.findById(unknownUserId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.submitMessage(unknownUserId, "hello"))
                .isInstanceOf(UserNotFoundException.class);
    }

    @Test
    void shouldPersistTheUserMessageRegardlessOfRiskOutcome() {
        when(chatRiskClassifier.isHighRisk(any())).thenReturn(false);

        service.submitMessage(userId, "I had an okay day");

        verify(conversationMessageRepository).save(any(ConversationMessage.class));
    }

    @Test
    void shouldTriggerCrisisAndNeverScheduleAnLlmResponseWhenRiskIsDetected() {
        when(chatRiskClassifier.isHighRisk(any())).thenReturn(true);
        when(riskEventTriggerService.trigger(any(), any(), any(), any()))
                .thenReturn(new CrisisTriggerOutcome(UUID.randomUUID(), List.of()));

        ConversationSubmissionResult result = service.submitMessage(userId, "I want to kill myself");
        runAfterCommitCallbacks();

        assertThat(result).isInstanceOf(ConversationCrisisResult.class);
        verify(riskEventTriggerService).trigger(any(), any(), any(), any());
        verify(companionResponseService, never()).generateResponseAsync(any());
    }

    @Test
    void shouldDetectCrisisEvenWhenTheCompanionResponseServiceWouldFail() {
        when(chatRiskClassifier.isHighRisk(any())).thenReturn(true);
        when(riskEventTriggerService.trigger(any(), any(), any(), any()))
                .thenReturn(new CrisisTriggerOutcome(UUID.randomUUID(), List.of()));
        doThrow(new IllegalStateException("LLM provider unreachable"))
                .when(companionResponseService)
                .generateResponseAsync(any());

        ConversationSubmissionResult result = service.submitMessage(userId, "I don't want to be alive anymore");
        runAfterCommitCallbacks();

        assertThat(result).isInstanceOf(ConversationCrisisResult.class);
        verify(companionResponseService, never()).generateResponseAsync(any());
    }

    @Test
    void shouldOnlyScheduleTheAsyncResponseAfterCommitWhenNoRiskIsDetected() {
        when(chatRiskClassifier.isHighRisk(any())).thenReturn(false);

        ConversationSubmissionResult result = service.submitMessage(userId, "Today was fine");

        assertThat(result).isInstanceOf(ConversationProcessingResult.class);
        verify(companionResponseService, never()).generateResponseAsync(any());

        runAfterCommitCallbacks();

        verify(companionResponseService).generateResponseAsync(userId);
    }

    private void runAfterCommitCallbacks() {
        for (TransactionSynchronization synchronization : TransactionSynchronizationManager.getSynchronizations()) {
            synchronization.afterCommit();
        }
    }
}
