package dev.lumen.domain.assessment;

/**
 * itemCount drives request validation in AssessmentService — a submission with the
 * wrong number of responses for its type is rejected before anything is persisted.
 */
public enum AssessmentType {
    PHQ9(9),
    GAD7(7);

    private final int itemCount;

    AssessmentType(int itemCount) {
        this.itemCount = itemCount;
    }

    public int getItemCount() {
        return itemCount;
    }
}
