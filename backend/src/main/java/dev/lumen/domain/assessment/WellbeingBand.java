package dev.lumen.domain.assessment;

/**
 * Non-diagnostic interpretation of a score, shared across instruments (ADR-0001).
 * GAD-7 only has 4 officially validated severity levels, so its scoring never
 * produces PRONOUNCED — see AssessmentService.
 */
public enum WellbeingBand {
    MINIMAL,
    MILD,
    MODERATE,
    PRONOUNCED,
    ELEVATED
}
