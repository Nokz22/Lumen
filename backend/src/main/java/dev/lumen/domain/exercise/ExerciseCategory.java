package dev.lumen.domain.exercise;

/**
 * Extensible on purpose (project-brief.md §4): new categories can be added without
 * touching RecommendationService's rule structure, only the rule-to-category mapping.
 */
public enum ExerciseCategory {
    BREATHING,
    WALKING,
    STRETCHING,
    SLEEP_HYGIENE,
    BEHAVIORAL_ACTIVATION,
    GROUNDING
}
