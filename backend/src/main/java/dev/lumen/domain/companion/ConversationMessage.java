package dev.lumen.domain.companion;

import dev.lumen.domain.moodcheckin.EncryptedStringConverter;
import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

/**
 * userId is a plain UUID, not a JPA relation — same pattern as every other
 * high-volume, loosely-coupled table in this codebase (RiskEvent, AuditLogEntry,
 * WearableReading). content is cifrado at rest with the same converter MoodCheckIn.note
 * already uses (docs/constitution.md: "conteúdo emocional... nunca aparecem em logs" applies just
 * as much to chat content).
 */
@Entity
@Table(name = "conversation_messages")
public class ConversationMessage {

    @Id
    private UUID id;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ConversationRole role;

    @Convert(converter = EncryptedStringConverter.class)
    @Column(nullable = false, length = 8000)
    private String content;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    protected ConversationMessage() {
    }

    public ConversationMessage(UUID userId, ConversationRole role, String content) {
        this.id = UUID.randomUUID();
        this.userId = userId;
        this.role = role;
        this.content = content;
        this.createdAt = Instant.now();
    }

    public UUID getId() {
        return id;
    }

    public UUID getUserId() {
        return userId;
    }

    public ConversationRole getRole() {
        return role;
    }

    public String getContent() {
        return content;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
