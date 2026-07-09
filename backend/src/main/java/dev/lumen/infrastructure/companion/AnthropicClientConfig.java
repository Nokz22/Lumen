package dev.lumen.infrastructure.companion;

import com.anthropic.client.AnthropicClient;
import com.anthropic.client.okhttp.AnthropicOkHttpClient;
import java.time.Duration;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Split out from AnthropicLlmClient so the SDK client is a mockable constructor
 * argument in tests rather than something built inline behind a private field.
 */
@Configuration
@ConditionalOnProperty(name = "app.llm.provider", havingValue = "anthropic")
class AnthropicClientConfig {

    @Bean
    AnthropicClient anthropicClient(
            @Value("${app.llm.anthropic-api-key}") String apiKey,
            @Value("${app.llm.anthropic-timeout-seconds}") long timeoutSeconds) {
        return AnthropicOkHttpClient.builder()
                .apiKey(apiKey)
                .timeout(Duration.ofSeconds(timeoutSeconds))
                .build();
    }
}
