package dev.lumen.domain.companion;

import java.util.UUID;

/** Same shape as RecommendationNotifier (Fase 4) — a port for pushing to the user's own WebSocket session. */
public interface CompanionStreamNotifier {

    void sendChunk(UUID userId, String chunk);

    void sendComplete(UUID userId, UUID messageId);

    void sendError(UUID userId, String errorMessage);
}
