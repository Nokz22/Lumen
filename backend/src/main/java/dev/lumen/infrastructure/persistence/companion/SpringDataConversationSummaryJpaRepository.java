package dev.lumen.infrastructure.persistence.companion;

import dev.lumen.domain.companion.ConversationSummary;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

interface SpringDataConversationSummaryJpaRepository extends JpaRepository<ConversationSummary, UUID> {

    Optional<ConversationSummary> findByUserId(UUID userId);

    @Modifying
    @Query("DELETE FROM ConversationSummary s WHERE s.userId = :userId")
    void deleteByUserId(@Param("userId") UUID userId);
}
