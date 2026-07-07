package dev.lumen.domain.exercise;

import java.util.UUID;

public class ExerciseNotFoundException extends RuntimeException {

    public ExerciseNotFoundException(UUID id) {
        super("Exercise not found: " + id);
    }
}
