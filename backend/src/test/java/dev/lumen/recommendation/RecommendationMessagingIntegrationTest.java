package dev.lumen.recommendation;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import dev.lumen.domain.moodcheckin.MoodEmotion;
import dev.lumen.domain.recommendation.MoodCheckInSubmittedEvent;
import dev.lumen.domain.recommendation.Recommendation;
import dev.lumen.domain.recommendation.RecommendationRepository;
import dev.lumen.infrastructure.messaging.RabbitMqDestinations;
import dev.lumen.presentation.auth.dto.RegisterRequest;
import dev.lumen.presentation.moodcheckin.dto.MoodCheckInRequest;
import jakarta.servlet.http.Cookie;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
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
 * Exercises the real check-in -> RabbitMQ -> recommendation flow against a real
 * broker and database, including the two properties the DoD for this phase demands:
 * an idempotent consumer and dead-letter routing for unprocessable messages.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
@Testcontainers
class RecommendationMessagingIntegrationTest {

    private static final int POLL_ATTEMPTS = 30;
    private static final long POLL_INTERVAL_MS = 200;
    private static final long DLQ_RECEIVE_TIMEOUT_MS = 10_000;

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

    @Autowired
    private RabbitTemplate rabbitTemplate;

    @Autowired
    private RecommendationRepository recommendationRepository;

    private record AuthenticatedUser(UUID userId, Cookie accessTokenCookie) {
    }

    private AuthenticatedUser registerUserWithConsent() throws Exception {
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

        mockMvc.perform(post("/api/v1/users/{userId}/consents/HEALTH_DATA_PROCESSING/grant", userId)
                        .with(csrf())
                        .cookie(accessTokenCookie))
                .andExpect(status().isNoContent());

        return new AuthenticatedUser(userId, accessTokenCookie);
    }

    private UUID submitAnxiousCheckIn(AuthenticatedUser user) throws Exception {
        MoodCheckInRequest request =
                new MoodCheckInRequest(MoodEmotion.ANXIOUS, 4, new BigDecimal("7.0"), 3, null);

        MvcResult result = mockMvc.perform(post("/api/v1/users/{userId}/mood-check-ins", user.userId())
                        .with(csrf())
                        .cookie(user.accessTokenCookie())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andReturn();

        JsonNode json = objectMapper.readTree(result.getResponse().getContentAsString());
        return UUID.fromString(json.get("id").asText());
    }

    private List<Recommendation> waitForRecommendations(UUID userId, int expectedCount) throws InterruptedException {
        List<Recommendation> recommendations = List.of();
        for (int attempt = 0; attempt < POLL_ATTEMPTS; attempt++) {
            recommendations = recommendationRepository.findByUserIdOrderByCreatedAtDesc(userId);
            if (recommendations.size() >= expectedCount) {
                return recommendations;
            }
            Thread.sleep(POLL_INTERVAL_MS);
        }
        return recommendations;
    }

    @Test
    void shouldGenerateRecommendationsAsynchronouslyAfterAnAnxiousCheckIn() throws Exception {
        AuthenticatedUser user = registerUserWithConsent();

        submitAnxiousCheckIn(user);

        List<Recommendation> recommendations = waitForRecommendations(user.userId(), 2);
        assertThat(recommendations).hasSize(2);
    }

    @Test
    void shouldNotDuplicateRecommendationsWhenTheSameEventIsRedelivered() throws Exception {
        AuthenticatedUser user = registerUserWithConsent();
        UUID moodCheckInId = submitAnxiousCheckIn(user);
        waitForRecommendations(user.userId(), 2);

        rabbitTemplate.convertAndSend(new MoodCheckInSubmittedEvent(
                moodCheckInId, user.userId(), MoodEmotion.ANXIOUS, 4, new BigDecimal("7.0"), 3));
        Thread.sleep(POLL_INTERVAL_MS * 5);

        List<Recommendation> recommendations = recommendationRepository.findByUserIdOrderByCreatedAtDesc(user.userId());
        assertThat(recommendations).hasSize(2);
    }

    @Test
    void shouldRouteUnprocessableMessagesToTheDeadLetterQueueAfterRetriesAreExhausted() {
        rabbitTemplate.convertAndSend("not-a-valid-event-payload");

        Message deadLettered = rabbitTemplate.receive(RabbitMqDestinations.DEAD_LETTER_QUEUE, DLQ_RECEIVE_TIMEOUT_MS);

        assertThat(deadLettered).isNotNull();
    }
}
