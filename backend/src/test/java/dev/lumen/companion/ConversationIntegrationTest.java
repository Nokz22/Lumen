package dev.lumen.companion;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import dev.lumen.presentation.auth.dto.RegisterRequest;
import jakarta.servlet.http.Cookie;
import java.time.LocalDate;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.containers.RabbitMQContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

/**
 * Exercises the real chat flow end to end against a real database. The `test` Spring
 * profile hard-pins app.llm.provider=mock (application.yml), so this never calls the
 * real Anthropic API regardless of environment — CannedLlmClient is the only LlmClient
 * in play here, in CI and locally alike.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
@Testcontainers
class ConversationIntegrationTest {

    private static final int POLL_ATTEMPTS = 30;
    private static final long POLL_INTERVAL_MS = 200;

    @Container
    @ServiceConnection
    static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:16-alpine");

    @Container
    @ServiceConnection
    static final RabbitMQContainer RABBITMQ = new RabbitMQContainer("rabbitmq:3-management-alpine");

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    private record AuthenticatedUser(UUID userId, Cookie accessTokenCookie) {
    }

    private AuthenticatedUser registerUserWithLlmConsent() throws Exception {
        RegisterRequest registerRequest = new RegisterRequest(
                "test-" + UUID.randomUUID() + "@lumen.dev",
                "SuperSecret123",
                "Test User",
                "en",
                "PT",
                LocalDate.of(1990, 1, 1));

        MvcResult result = mockMvc.perform(post("/api/v1/auth/register")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(registerRequest)))
                .andExpect(status().isCreated())
                .andReturn();

        JsonNode json = objectMapper.readTree(result.getResponse().getContentAsString());
        UUID userId = UUID.fromString(json.get("id").asText());
        Cookie accessTokenCookie = result.getResponse().getCookie("access_token");

        mockMvc.perform(post("/api/v1/users/{userId}/consents/LLM_PROCESSING/grant", userId)
                        .with(csrf())
                        .cookie(accessTokenCookie))
                .andExpect(status().isNoContent());

        return new AuthenticatedUser(userId, accessTokenCookie);
    }

    private JsonNode sendMessage(AuthenticatedUser user, String content) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/v1/users/{userId}/conversation/messages", user.userId())
                        .with(csrf())
                        .cookie(user.accessTokenCookie())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new SendMessagePayload(content))))
                .andExpect(status().isOk())
                .andReturn();
        return objectMapper.readTree(result.getResponse().getContentAsString());
    }

    private JsonNode waitForAssistantReply(AuthenticatedUser user) throws Exception {
        for (int attempt = 0; attempt < POLL_ATTEMPTS; attempt++) {
            JsonNode history = history(user);
            for (JsonNode message : history) {
                if ("ASSISTANT".equals(message.get("role").asText())) {
                    return history;
                }
            }
            Thread.sleep(POLL_INTERVAL_MS);
        }
        return history(user);
    }

    private JsonNode history(AuthenticatedUser user) throws Exception {
        MvcResult result = mockMvc.perform(get("/api/v1/users/{userId}/conversation/messages", user.userId())
                        .cookie(user.accessTokenCookie()))
                .andExpect(status().isOk())
                .andReturn();
        return objectMapper.readTree(result.getResponse().getContentAsString());
    }

    @Test
    void shouldStreamAnAssistantReplyForAnOrdinaryMessage() throws Exception {
        AuthenticatedUser user = registerUserWithLlmConsent();

        JsonNode submission = sendMessage(user, "Today was a pretty calm day overall.");
        assertThat(submission.has("userMessageId")).isTrue();

        JsonNode history = waitForAssistantReply(user);

        assertThat(history).hasSize(2);
        assertThat(history.get(0).get("role").asText()).isEqualTo("USER");
        assertThat(history.get(1).get("role").asText()).isEqualTo("ASSISTANT");
        assertThat(history.get(1).get("content").asText()).isNotBlank();
    }

    @Test
    void shouldTriggerTheCrisisFlowAndNeverProduceAnAssistantReplyForARiskyMessage() throws Exception {
        AuthenticatedUser user = registerUserWithLlmConsent();

        JsonNode submission = sendMessage(user, "I want to kill myself and I don't see a way out.");
        assertThat(submission.has("riskEventId")).isTrue();
        assertThat(submission.get("resources")).isNotEmpty();

        // Give any wrongly-scheduled async work a fair chance to run before asserting its absence.
        Thread.sleep(POLL_INTERVAL_MS * 5);
        JsonNode history = history(user);

        assertThat(history).hasSize(1);
        assertThat(history.get(0).get("role").asText()).isEqualTo("USER");
    }

    @Test
    void shouldBlockChatWhenLlmProcessingConsentIsNotGranted() throws Exception {
        RegisterRequest registerRequest = new RegisterRequest(
                "test-" + UUID.randomUUID() + "@lumen.dev",
                "SuperSecret123",
                "Test User",
                "en",
                "PT",
                LocalDate.of(1990, 1, 1));

        MvcResult result = mockMvc.perform(post("/api/v1/auth/register")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(registerRequest)))
                .andExpect(status().isCreated())
                .andReturn();
        JsonNode json = objectMapper.readTree(result.getResponse().getContentAsString());
        UUID userId = UUID.fromString(json.get("id").asText());
        Cookie accessTokenCookie = result.getResponse().getCookie("access_token");
        AuthenticatedUser user = new AuthenticatedUser(userId, accessTokenCookie);

        mockMvc.perform(post("/api/v1/users/{userId}/conversation/messages", user.userId())
                        .with(csrf())
                        .cookie(user.accessTokenCookie())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new SendMessagePayload("Hello there"))))
                .andExpect(status().isForbidden());
    }

    private record SendMessagePayload(String content) {
    }
}
