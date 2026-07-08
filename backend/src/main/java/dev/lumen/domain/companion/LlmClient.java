package dev.lumen.domain.companion;

/**
 * completeStreaming is for the reply the person actually sees (chunks pushed live over
 * WebSocket); complete is for internal, non-visible work (summarizing older messages,
 * see ADR-0009) where nothing needs to stream. Both real (Anthropic) and mock
 * implementations provide both — mockable so tests never depend on, or pay for, a
 * real network call.
 */
public interface LlmClient {

    void completeStreaming(LlmPrompt request, LlmStreamHandler handler);

    String complete(LlmPrompt request);
}
