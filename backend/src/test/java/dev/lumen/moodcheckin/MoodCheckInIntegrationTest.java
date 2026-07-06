package dev.lumen.moodcheckin;

import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import dev.lumen.domain.moodcheckin.MoodEmotion;
import dev.lumen.domain.user.User;
import dev.lumen.domain.user.UserRepository;
import dev.lumen.presentation.moodcheckin.dto.MoodCheckInRequest;
import java.math.BigDecimal;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
@Testcontainers
class MoodCheckInIntegrationTest {

    @Container
    @ServiceConnection
    static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:16-alpine");

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ObjectMapper objectMapper;

    private UUID createUser() {
        User user = userRepository.save(new User(
                "test-" + UUID.randomUUID() + "@lumen.dev", "Test User", "en", "PT"));
        return user.getId();
    }

    @Test
    void shouldCreateMoodCheckInWhenNoneExistsForToday() throws Exception {
        UUID userId = createUser();
        MoodCheckInRequest request = new MoodCheckInRequest(
                MoodEmotion.HAPPY, 4, new BigDecimal("7.5"), 4, "Feeling good");

        mockMvc.perform(post("/api/v1/users/{userId}/mood-check-ins", userId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.emotion").value("HAPPY"))
                .andExpect(jsonPath("$.energyLevel").value(4));
    }

    @Test
    void shouldUpdateSameDayCheckInInsteadOfCreatingDuplicate() throws Exception {
        UUID userId = createUser();
        MoodCheckInRequest first = new MoodCheckInRequest(
                MoodEmotion.NEUTRAL, 3, new BigDecimal("6.0"), 3, null);
        MoodCheckInRequest second = new MoodCheckInRequest(
                MoodEmotion.ANXIOUS, 2, new BigDecimal("5.0"), 2, "Rough day");

        mockMvc.perform(post("/api/v1/users/{userId}/mood-check-ins", userId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(first)))
                .andExpect(status().isOk());
        mockMvc.perform(post("/api/v1/users/{userId}/mood-check-ins", userId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(second)))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/v1/users/{userId}/mood-check-ins", userId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].emotion").value("ANXIOUS"));
    }

    @Test
    void shouldReturnNotFoundWhenUserDoesNotExist() throws Exception {
        mockMvc.perform(get("/api/v1/users/{userId}/mood-check-ins", UUID.randomUUID()))
                .andExpect(status().isNotFound());
    }

    @Test
    void shouldRejectEnergyLevelOutsideOneToFiveRange() throws Exception {
        UUID userId = createUser();
        MoodCheckInRequest invalid = new MoodCheckInRequest(
                MoodEmotion.HAPPY, 6, new BigDecimal("7.0"), 3, null);

        mockMvc.perform(post("/api/v1/users/{userId}/mood-check-ins", userId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalid)))
                .andExpect(status().isBadRequest());
    }
}
