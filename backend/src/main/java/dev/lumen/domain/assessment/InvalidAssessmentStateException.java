package dev.lumen.domain.assessment;

public class InvalidAssessmentStateException extends RuntimeException {

    public InvalidAssessmentStateException(AssessmentStatus actual, AssessmentStatus required) {
        super("Assessment is " + actual + ", expected " + required);
    }
}
