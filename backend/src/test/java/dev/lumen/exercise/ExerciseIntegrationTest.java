package dev.lumen.exercise;

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
 * Covers the seeded V18 (breathing pattern) and V19 (guided-session steps) data,
 * verifying real content rather than in-memory fixtures — the exercise library is
 * shared reference content seeded via migration, not something the app constructs.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
@Testcontainers
class ExerciseIntegrationTest {

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

    private Cookie registerAndGetAccessTokenCookie() throws Exception {
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

        return result.getResponse().getCookie("access_token");
    }

    @Test
    void shouldExposeBreathingPatternForBreathingExercisesAndStepsForOtherCategories() throws Exception {
        Cookie accessTokenCookie = registerAndGetAccessTokenCookie();

        MvcResult result = mockMvc.perform(get("/api/v1/exercises").cookie(accessTokenCookie))
                .andExpect(status().isOk())
                .andReturn();
        JsonNode exercises = objectMapper.readTree(result.getResponse().getContentAsString());

        JsonNode breathing = findByName(exercises, "4-7-8 Breathing");
        assertThat(breathing.get("inhaleSeconds").asInt()).isEqualTo(4);
        assertThat(breathing.get("holdAfterInhaleSeconds").asInt()).isEqualTo(7);
        assertThat(breathing.get("exhaleSeconds").asInt()).isEqualTo(8);
        assertThat(breathing.get("steps")).isEmpty();

        JsonNode walk = findByName(exercises, "10-Minute Outdoor Walk");
        assertThat(walk.get("inhaleSeconds").isNull()).isTrue();
        assertThat(walk.get("steps")).isNotEmpty();
        assertThat(walk.get("steps").get(0).asText()).isNotBlank();
    }

    private JsonNode findByName(JsonNode exercises, String name) {
        for (JsonNode exercise : exercises) {
            if (name.equals(exercise.get("name").asText())) {
                return exercise;
            }
        }
        throw new AssertionError("Exercise not found: " + name);
    }
}
