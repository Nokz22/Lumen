package dev.lumen.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;

/**
 * Without this, @Async on CompanionResponseService.generateResponseAsync would be
 * silently ignored and the call would run synchronously on the caller's thread —
 * blocking the HTTP response on the LLM call, exactly what the async design exists
 * to avoid.
 */
@Configuration
@EnableAsync
public class AsyncConfig {
}
