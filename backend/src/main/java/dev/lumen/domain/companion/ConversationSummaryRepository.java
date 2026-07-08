package dev.lumen.domain.companion;

import java.util.Optional;
import java.util.UUID;

public interface ConversationSummaryRepository {

    ConversationSummary save(ConversationSummary summary);

    Optional<ConversationSummary> findByUserId(UUID userId);
}
