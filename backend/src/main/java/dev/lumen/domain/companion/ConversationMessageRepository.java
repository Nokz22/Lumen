package dev.lumen.domain.companion;

import dev.lumen.domain.shared.PageQuery;
import dev.lumen.domain.shared.PagedResult;
import java.util.List;
import java.util.UUID;

public interface ConversationMessageRepository {

    ConversationMessage save(ConversationMessage message);

    List<ConversationMessage> findByUserIdOrderByCreatedAtAsc(UUID userId);

    void deleteByUserId(UUID userId);

    /**
     * Alongside the full read above, not instead of it: the data export must stay
     * complete (ADR-0011) and the correlation and LLM-context paths need whole windows.
     */
    PagedResult<ConversationMessage> findPageByUserIdOrderByCreatedAtDesc(UUID userId, PageQuery pageQuery);
}
