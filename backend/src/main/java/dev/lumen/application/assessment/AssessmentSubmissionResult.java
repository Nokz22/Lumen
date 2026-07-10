package dev.lumen.application.assessment;

/**
 * Never both — a submission either yields a wellbeing score, or interrupts into a
 * crisis flow that withholds the score until the RiskEvent is acknowledged
 * (docs/constitution.md invariant: item 9 must trigger the crisis flow before any score is shown).
 */
public sealed interface AssessmentSubmissionResult permits ScoredAssessmentResult, CrisisTriggeredResult {
}
