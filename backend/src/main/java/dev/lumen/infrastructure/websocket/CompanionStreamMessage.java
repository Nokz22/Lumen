package dev.lumen.infrastructure.websocket;

import java.util.UUID;

/** Wire shape for /user/queue/companion — the frontend switches on type. */
record CompanionStreamMessage(String type, String chunk, UUID messageId, String errorMessage) {

    static CompanionStreamMessage chunk(String chunk) {
        return new CompanionStreamMessage("CHUNK", chunk, null, null);
    }

    static CompanionStreamMessage complete(UUID messageId) {
        return new CompanionStreamMessage("COMPLETE", null, messageId, null);
    }

    static CompanionStreamMessage error(String errorMessage) {
        return new CompanionStreamMessage("ERROR", null, null, errorMessage);
    }
}
