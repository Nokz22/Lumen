package dev.lumen.infrastructure.websocket;

import dev.lumen.domain.recommendation.RecommendationNotification;
import dev.lumen.domain.recommendation.RecommendationNotifier;
import java.util.UUID;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;

/**
 * convertAndSendToUser matches on Principal.getName(), which UserPrincipalHandshakeHandler
 * sets to the raw userId string — so the destination here must use the same
 * userId.toString() form, not any other user identifier.
 */
@Component
class WebSocketRecommendationNotifier implements RecommendationNotifier {

    static final String RECOMMENDATIONS_DESTINATION = "/queue/recommendations";

    private final SimpMessagingTemplate messagingTemplate;

    WebSocketRecommendationNotifier(SimpMessagingTemplate messagingTemplate) {
        this.messagingTemplate = messagingTemplate;
    }

    @Override
    public void notify(UUID userId, RecommendationNotification notification) {
        messagingTemplate.convertAndSendToUser(userId.toString(), RECOMMENDATIONS_DESTINATION, notification);
    }
}
