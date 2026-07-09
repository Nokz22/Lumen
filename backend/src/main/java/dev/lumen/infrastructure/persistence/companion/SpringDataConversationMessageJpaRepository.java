package dev.lumen.infrastructure.persistence.companion;

import dev.lumen.domain.companion.ConversationMessage;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

interface SpringDataConversationMessageJpaRepository extends JpaRepository<ConversationMessage, UUID> {

    @Query("SELECT m FROM ConversationMessage m WHERE m.userId = :userId ORDER BY m.createdAt ASC")
    List<ConversationMessage> findByUserIdOrderByCreatedAtAsc(@Param("userId") UUID userId);
}
