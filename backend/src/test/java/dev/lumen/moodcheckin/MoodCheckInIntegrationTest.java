package dev.lumen.moodcheckin;

import static org.hamcrest.Matchers.hasSize;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import dev.lumen.domain.moodcheckin.MoodEmotion;
import dev.lumen.presentation.auth.dto.RegisterRequest;
import dev.lumen.presentation.moodcheckin.dto.MoodCheckInRequest;
import jakarta.servlet.http.Cookie;
import java.math.BigDecimal;
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

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
@Testcontainers
class MoodCheckInIntegrationTest {

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

    @Test
    void shouldCreateMoodCheckInWhenNoneExistsForToday() throws Exception {
        AuthenticatedUser user = registerUserWithConsent();
        MoodCheckInRequest request =
                new MoodCheckInRequest(MoodEmotion.HAPPY, 4, new BigDecimal("7.5"), 4, "Feeling good");

        mockMvc.perform(post("/api/v1/users/{userId}/mood-check-ins", user.userId())
                        .with(csrf())
                        .cookie(user.accessTokenCookie())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.emotion").value("HAPPY"))
                .andExpect(jsonPath("$.energyLevel").value(4));
    }

    @Test
    void shouldUpdateSameDayCheckInInsteadOfCreatingDuplicate() throws Exception {
        AuthenticatedUser user = registerUserWithConsent();
        MoodCheckInRequest first = new MoodCheckInRequest(MoodEmotion.NEUTRAL, 3, new BigDecimal("6.0"), 3, null);
        MoodCheckInRequest second =
                new MoodCheckInRequest(MoodEmotion.ANXIOUS, 2, new BigDecimal("5.0"), 2, "Rough day");

        mockMvc.perform(post("/api/v1/users/{userId}/mood-check-ins", user.userId())
                        .with(csrf())
                        .cookie(user.accessTokenCookie())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(first)))
                .andExpect(status().isOk());
        mockMvc.perform(post("/api/v1/users/{userId}/mood-check-ins", user.userId())
                        .with(csrf())
                        .cookie(user.accessTokenCookie())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(second)))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/v1/users/{userId}/mood-check-ins", user.userId())
                        .cookie(user.accessTokenCookie()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].emotion").value("ANXIOUS"));
    }

    @Test
    void shouldRejectCheckInWithoutHealthDataConsent() throws Exception {
        RegisterRequest registerRequest = new RegisterRequest(
                "test-" + UUID.randomUUID() + "@lumen.dev",
                "SuperSecret123",
                "No Consent User",
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

        MoodCheckInRequest request = new MoodCheckInRequest(MoodEmotion.HAPPY, 4, new BigDecimal("7.5"), 4, null);
        mockMvc.perform(post("/api/v1/users/{userId}/mood-check-ins", userId)
                        .with(csrf())
                        .cookie(accessTokenCookie)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden());
    }

    @Test
    void shouldRejectEnergyLevelOutsideOneToFiveRange() throws Exception {
        AuthenticatedUser user = registerUserWithConsent();
        MoodCheckInRequest invalid = new MoodCheckInRequest(MoodEmotion.HAPPY, 6, new BigDecimal("7.0"), 3, null);

        mockMvc.perform(post("/api/v1/users/{userId}/mood-check-ins", user.userId())
                        .with(csrf())
                        .cookie(user.accessTokenCookie())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalid)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void shouldForbidAccessingAnotherUsersMoodCheckIns() throws Exception {
        AuthenticatedUser userA = registerUserWithConsent();
        AuthenticatedUser userB = registerUserWithConsent();

        mockMvc.perform(get("/api/v1/users/{userId}/mood-check-ins", userB.userId())
                        .cookie(userA.accessTokenCookie()))
                .andExpect(status().isForbidden());
    }
}
