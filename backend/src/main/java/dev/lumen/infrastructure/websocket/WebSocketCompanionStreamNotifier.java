package dev.lumen.infrastructure.websocket;

import dev.lumen.domain.companion.CompanionStreamNotifier;
import java.util.UUID;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;

/** Same authenticated per-user destination pattern as WebSocketRecommendationNotifier (Fase 4). */
@Component
class WebSocketCompanionStreamNotifier implements CompanionStreamNotifier {

    static final String COMPANION_DESTINATION = "/queue/companion";

    private final SimpMessagingTemplate messagingTemplate;

    WebSocketCompanionStreamNotifier(SimpMessagingTemplate messagingTemplate) {
        this.messagingTemplate = messagingTemplate;
    }

    @Override
    public void sendChunk(UUID userId, String chunk) {
        messagingTemplate.convertAndSendToUser(
                userId.toString(), COMPANION_DESTINATION, CompanionStreamMessage.chunk(chunk));
    }

    @Override
    public void sendComplete(UUID userId, UUID messageId) {
        messagingTemplate.convertAndSendToUser(
                userId.toString(), COMPANION_DESTINATION, CompanionStreamMessage.complete(messageId));
    }

    @Override
    public void sendError(UUID userId, String errorMessage) {
        messagingTemplate.convertAndSendToUser(
                userId.toString(), COMPANION_DESTINATION, CompanionStreamMessage.error(errorMessage));
    }
}
