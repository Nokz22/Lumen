package dev.lumen.domain.companion;

import dev.lumen.domain.moodcheckin.EncryptedStringConverter;
import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import java.time.Instant;
import java.util.UUID;

/**
 * One row per user (single continuous conversation, project-brief.md §8.1).
 * summarizedThroughMessageId marks the last ConversationMessage folded into
 * summaryText — the context window builder only sends raw messages after that point,
 * see ADR-0009.
 */
@Entity
@Table(name = "conversation_summaries")
public class ConversationSummary {

    @Id
    private UUID id;

    @Column(name = "user_id", nullable = false, unique = true)
    private UUID userId;

    @Convert(converter = EncryptedStringConverter.class)
    @Column(name = "summary_text", nullable = false, length = 6000)
    private String summaryText;

    @Column(name = "summarized_through_message_id", nullable = false)
    private UUID summarizedThroughMessageId;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @Version
    private long version;

    protected ConversationSummary() {
    }

    public ConversationSummary(UUID userId, String summaryText, UUID summarizedThroughMessageId) {
        this.id = UUID.randomUUID();
        this.userId = userId;
        this.summaryText = summaryText;
        this.summarizedThroughMessageId = summarizedThroughMessageId;
        this.updatedAt = Instant.now();
    }

    public void update(String summaryText, UUID summarizedThroughMessageId) {
        this.summaryText = summaryText;
        this.summarizedThroughMessageId = summarizedThroughMessageId;
        this.updatedAt = Instant.now();
    }

    public UUID getId() {
        return id;
    }

    public UUID getUserId() {
        return userId;
    }

    public String getSummaryText() {
        return summaryText;
    }

    public UUID getSummarizedThroughMessageId() {
        return summarizedThroughMessageId;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public long getVersion() {
        return version;
    }
}
