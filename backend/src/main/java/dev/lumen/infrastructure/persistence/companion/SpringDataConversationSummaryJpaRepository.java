package dev.lumen.infrastructure.persistence.companion;

import dev.lumen.domain.companion.ConversationSummary;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

interface SpringDataConversationSummaryJpaRepository extends JpaRepository<ConversationSummary, UUID> {

    Optional<ConversationSummary> findByUserId(UUID userId);
}
