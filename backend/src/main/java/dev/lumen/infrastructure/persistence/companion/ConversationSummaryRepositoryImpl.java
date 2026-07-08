package dev.lumen.infrastructure.persistence.companion;

import dev.lumen.domain.companion.ConversationSummary;
import dev.lumen.domain.companion.ConversationSummaryRepository;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Repository;

@Repository
class ConversationSummaryRepositoryImpl implements ConversationSummaryRepository {

    private final SpringDataConversationSummaryJpaRepository jpaRepository;

    ConversationSummaryRepositoryImpl(SpringDataConversationSummaryJpaRepository jpaRepository) {
        this.jpaRepository = jpaRepository;
    }

    @Override
    public ConversationSummary save(ConversationSummary summary) {
        return jpaRepository.save(summary);
    }

    @Override
    public Optional<ConversationSummary> findByUserId(UUID userId) {
        return jpaRepository.findByUserId(userId);
    }
}
