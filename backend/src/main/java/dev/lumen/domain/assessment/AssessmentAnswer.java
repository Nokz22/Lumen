package dev.lumen.domain.assessment;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.util.UUID;

/**
 * itemNumber is 1-based, matching the official instrument numbering (e.g. "item 9" of
 * the PHQ-9 always refers to the same question this way).
 */
@Entity
@Table(name = "assessment_responses")
public class AssessmentAnswer {

    @Id
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "assessment_id", nullable = false)
    private Assessment assessment;

    @Column(name = "item_number", nullable = false)
    private int itemNumber;

    @Column(nullable = false)
    private int value;

    protected AssessmentAnswer() {
    }

    public AssessmentAnswer(Assessment assessment, int itemNumber, int value) {
        this.id = UUID.randomUUID();
        this.assessment = assessment;
        this.itemNumber = itemNumber;
        this.value = value;
    }

    public UUID getId() {
        return id;
    }

    public Assessment getAssessment() {
        return assessment;
    }

    public int getItemNumber() {
        return itemNumber;
    }

    public int getValue() {
        return value;
    }
}
