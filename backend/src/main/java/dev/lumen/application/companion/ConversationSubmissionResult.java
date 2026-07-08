package dev.lumen.application.companion;

/**
 * Never both — a submitted message either interrupts into the crisis flow (the LLM is
 * never called) or is accepted for asynchronous processing. Same shape philosophy as
 * AssessmentSubmissionResult from Fase 3.
 */
public sealed interface ConversationSubmissionResult permits ConversationCrisisResult, ConversationProcessingResult {
}
