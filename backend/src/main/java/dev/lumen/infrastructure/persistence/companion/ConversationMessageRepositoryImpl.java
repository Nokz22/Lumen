package dev.lumen.infrastructure.persistence.companion;

import dev.lumen.domain.companion.ConversationMessage;
import dev.lumen.domain.companion.ConversationMessageRepository;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Repository;

@Repository
class ConversationMessageRepositoryImpl implements ConversationMessageRepository {

    private final SpringDataConversationMessageJpaRepository jpaRepository;

    ConversationMessageRepositoryImpl(SpringDataConversationMessageJpaRepository jpaRepository) {
        this.jpaRepository = jpaRepository;
    }

    @Override
    public ConversationMessage save(ConversationMessage message) {
        return jpaRepository.save(message);
    }

    @Override
    public List<ConversationMessage> findByUserIdOrderByCreatedAtAsc(UUID userId) {
        return jpaRepository.findByUserIdOrderByCreatedAtAsc(userId);
    }
}
