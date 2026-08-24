package dev.lumen.presentation.companion;

import dev.lumen.application.companion.ConversationMessageResponse;
import dev.lumen.application.companion.ConversationService;
import dev.lumen.application.companion.ConversationSubmissionResult;
import dev.lumen.presentation.companion.dto.SendMessageRequest;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * sendMessage() returns either a crisis interrupt or a processing acknowledgment
 * (ConversationSubmissionResult) — the assistant's actual reply, when there is one,
 * arrives afterwards over /user/queue/companion, not in this response.
 */
@Tag(
        name = "Companion",
        description = "The single continuous conversation per user. An independent risk classifier runs before the"
                + " model on every message (ADR-0010). Requires LLM_PROCESSING consent.")
@RestController
@RequestMapping("/api/v1/users/{userId}/conversation/messages")
@PreAuthorize("#userId == authentication.principal.userId()")
public class ConversationController {

    private final ConversationService conversationService;

    public ConversationController(ConversationService conversationService) {
        this.conversationService = conversationService;
    }

    @PostMapping
    @Operation(
            summary = "Send a message to the companion",
            description = "The input risk classifier runs first and independently of the model: when it fires, the"
                    + " crisis flow starts and the model is never called at all. Otherwise this returns an"
                    + " acknowledgment and the reply streams over /user/queue/companion — it is not in this response"
                    + " body.")
    public ConversationSubmissionResult sendMessage(
            @PathVariable UUID userId, @Valid @RequestBody SendMessageRequest request) {
        return conversationService.submitMessage(userId, request.content());
    }

    @GetMapping
    public List<ConversationMessageResponse> history(@PathVariable UUID userId) {
        return conversationService.getHistory(userId);
    }
}
