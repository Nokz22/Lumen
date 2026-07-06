package dev.lumen.domain.moodcheckin;

import dev.lumen.domain.user.User;
import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

/**
 * One check-in per user per day (enforced by a unique constraint on user_id + check_in_date).
 * A same-day resubmission updates this row instead of creating a new one.
 */
@Entity
@Table(name = "mood_check_ins")
public class MoodCheckIn {

    @Id
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private MoodEmotion emotion;

    @Column(name = "energy_level", nullable = false)
    private int energyLevel;

    @Column(name = "sleep_hours", nullable = false)
    private BigDecimal sleepHours;

    @Column(name = "sleep_quality", nullable = false)
    private int sleepQuality;

    @Convert(converter = EncryptedStringConverter.class)
    @Column(length = 2000)
    private String note;

    @Column(name = "check_in_date", nullable = false)
    private LocalDate checkInDate;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Version
    private long version;

    protected MoodCheckIn() {
    }

    public MoodCheckIn(
            User user,
            MoodEmotion emotion,
            int energyLevel,
            BigDecimal sleepHours,
            int sleepQuality,
            String note,
            LocalDate checkInDate) {
        this.id = UUID.randomUUID();
        this.user = user;
        this.checkInDate = checkInDate;
        this.createdAt = Instant.now();
        updateDetails(emotion, energyLevel, sleepHours, sleepQuality, note);
    }

    public void updateDetails(
            MoodEmotion emotion, int energyLevel, BigDecimal sleepHours, int sleepQuality, String note) {
        this.emotion = emotion;
        this.energyLevel = energyLevel;
        this.sleepHours = sleepHours;
        this.sleepQuality = sleepQuality;
        this.note = note;
    }

    public UUID getId() {
        return id;
    }

    public User getUser() {
        return user;
    }

    public MoodEmotion getEmotion() {
        return emotion;
    }

    public int getEnergyLevel() {
        return energyLevel;
    }

    public BigDecimal getSleepHours() {
        return sleepHours;
    }

    public int getSleepQuality() {
        return sleepQuality;
    }

    public String getNote() {
        return note;
    }

    public LocalDate getCheckInDate() {
        return checkInDate;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public long getVersion() {
        return version;
    }
}
