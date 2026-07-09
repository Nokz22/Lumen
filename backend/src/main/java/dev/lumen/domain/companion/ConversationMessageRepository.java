package dev.lumen.domain.companion;

import java.util.List;
import java.util.UUID;

public interface ConversationMessageRepository {

    ConversationMessage save(ConversationMessage message);

    List<ConversationMessage> findByUserIdOrderByCreatedAtAsc(UUID userId);
}
