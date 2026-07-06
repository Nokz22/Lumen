package dev.lumen.domain.assessment;

public class AssessmentTooSoonException extends RuntimeException {

    public AssessmentTooSoonException(AssessmentType assessmentType) {
        super(assessmentType + " was already completed within the last 30 days");
    }
}
