package dev.lumen.wearable;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import dev.lumen.domain.moodcheckin.MoodCheckIn;
import dev.lumen.domain.moodcheckin.MoodCheckInRepository;
import dev.lumen.domain.moodcheckin.MoodEmotion;
import dev.lumen.domain.user.User;
import dev.lumen.domain.user.UserRepository;
import dev.lumen.presentation.auth.dto.RegisterRequest;
import jakarta.servlet.http.Cookie;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
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
 * Historical MoodCheckIn rows are inserted directly via the repository rather than
 * through the check-in HTTP endpoint, because MoodCheckInService.checkIn() always
 * writes to today's date — there is no way to backfill past days through the public
 * API, by design (one check-in per day, upserted). That is the only part of this test
 * that bypasses HTTP; everything wearable-related goes through the real endpoints.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
@Testcontainers
class WearableIntegrationTest {

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
    private MoodCheckInRepository moodCheckInRepository;

    @Autowired
    private UserRepository userRepository;

    private record AuthenticatedUser(UUID userId, Cookie accessTokenCookie) {
    }

    private AuthenticatedUser registerUserWithWearableConsent() throws Exception {
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

        mockMvc.perform(post("/api/v1/users/{userId}/consents/WEARABLE_INGESTION/grant", userId)
                        .with(csrf())
                        .cookie(accessTokenCookie))
                .andExpect(status().isNoContent());

        return new AuthenticatedUser(userId, accessTokenCookie);
    }

    @Test
    void shouldPersistSimulatedReadingsAndExposeThemThroughHistory() throws Exception {
        AuthenticatedUser user = registerUserWithWearableConsent();

        JsonNode simulated = simulateReadings(user, 14);
        assertThat(simulated).isNotEmpty();

        MvcResult historyResult = mockMvc.perform(
                        get("/api/v1/users/{userId}/wearable-readings", user.userId())
                                .cookie(user.accessTokenCookie()))
                .andExpect(status().isOk())
                .andReturn();
        JsonNode history = objectMapper.readTree(historyResult.getResponse().getContentAsString());
        assertThat(history.size()).isEqualTo(simulated.size());
    }

    @Test
    void shouldSurfaceACorrelationInsightWhenSleepAndEnergyTrackTogether() throws Exception {
        AuthenticatedUser user = registerUserWithWearableConsent();
        JsonNode simulated = simulateReadings(user, 14);

        backfillEnergyTrackingSleep(user.userId(), simulated);

        MvcResult insightsResult = mockMvc.perform(
                        get("/api/v1/users/{userId}/wearable-insights", user.userId())
                                .cookie(user.accessTokenCookie()))
                .andExpect(status().isOk())
                .andReturn();
        JsonNode insights = objectMapper.readTree(insightsResult.getResponse().getContentAsString());

        assertThat(insights).isNotEmpty();
        assertThat(containsSleepInsight(insights)).isTrue();
    }

    private JsonNode simulateReadings(AuthenticatedUser user, int days) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/v1/users/{userId}/wearable-readings/simulate", user.userId())
                        .with(csrf())
                        .cookie(user.accessTokenCookie())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"days\":" + days + "}"))
                .andExpect(status().isOk())
                .andReturn();
        return objectMapper.readTree(result.getResponse().getContentAsString());
    }

    /** Backfills one MoodCheckIn per simulated sleep reading, with energy above/below average sleep. */
    private void backfillEnergyTrackingSleep(UUID userId, JsonNode simulated) {
        User userEntity = userRepository.findById(userId).orElseThrow();
        double meanSleep = 0;
        int sleepCount = 0;
        for (JsonNode reading : simulated) {
            if ("SLEEP_DURATION".equals(reading.get("type").asText())) {
                meanSleep += reading.get("value").asDouble();
                sleepCount++;
            }
        }
        meanSleep /= sleepCount;

        for (JsonNode reading : simulated) {
            if (!"SLEEP_DURATION".equals(reading.get("type").asText())) {
                continue;
            }
            LocalDate sleepDay = LocalDate.ofInstant(Instant.parse(reading.get("recordedAt").asText()), ZoneOffset.UTC);
            int energyLevel = reading.get("value").asDouble() >= meanSleep ? 5 : 1;
            moodCheckInRepository.save(new MoodCheckIn(
                    userEntity,
                    MoodEmotion.NEUTRAL,
                    energyLevel,
                    new BigDecimal("7.0"),
                    3,
                    null,
                    sleepDay.plusDays(1)));
        }
    }

    private boolean containsSleepInsight(JsonNode insights) {
        for (JsonNode insight : insights) {
            if ("SLEEP_DURATION".equals(insight.get("metric").asText())) {
                return true;
            }
        }
        return false;
    }
}
