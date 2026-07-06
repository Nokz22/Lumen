package dev.lumen.domain.assessment;

public class InvalidAssessmentSubmissionException extends RuntimeException {

    public InvalidAssessmentSubmissionException(AssessmentType assessmentType, int expected, int actual) {
        super(assessmentType + " requires exactly " + expected + " responses, got " + actual);
    }
}
