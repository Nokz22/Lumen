package dev.lumen.domain.companion;

/**
 * onError never means "the person is in crisis" — a failed LLM call is a technical
 * problem, handled with a graceful fallback message, entirely separate from the
 * crisis flow (which never reaches this far in the first place when it fires).
 */
public interface LlmStreamHandler {

    void onChunk(String textChunk);

    void onComplete(String fullText);

    void onError(Throwable error);
}
