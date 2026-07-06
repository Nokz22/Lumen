package dev.lumen.domain.assessment;

import java.util.UUID;

public class AssessmentNotFoundException extends RuntimeException {

    public AssessmentNotFoundException(UUID id) {
        super("Assessment not found: " + id);
    }
}
