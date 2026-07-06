package dev.lumen.assessment;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import dev.lumen.presentation.assessment.dto.SubmitAssessmentRequest;
import dev.lumen.presentation.auth.dto.RegisterRequest;
import jakarta.servlet.http.Cookie;
import java.time.LocalDate;
import java.util.List;
import java.util.Locale;
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
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

/**
 * The crisis path here is the highest-value test in the whole codebase: it proves,
 * against a real database and the real HTTP stack, that a positive PHQ-9 item 9 never
 * lets a score reach the response before the RiskEvent is acknowledged.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
@Testcontainers
class AssessmentIntegrationTest {

    private static final List<String> DIAGNOSTIC_TERMS =
            List.of("depress", "disorder", "anxiety disorder", "diagnos", "clinical");

    @Container
    @ServiceConnection
    static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:16-alpine");

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

    private MvcResult submit(AuthenticatedUser user, String assessmentType, List<Integer> responses)
            throws Exception {
        SubmitAssessmentRequest request = new SubmitAssessmentRequest(responses);
        return mockMvc.perform(post("/api/v1/users/{userId}/assessments/{type}", user.userId(), assessmentType)
                        .with(csrf())
                        .cookie(user.accessTokenCookie())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andReturn();
    }

    @Test
    void shouldScoreAPhq9SubmissionInWellbeingLanguageWhenItem9IsZero() throws Exception {
        AuthenticatedUser user = registerUserWithConsent();
        List<Integer> responses = List.of(0, 0, 0, 0, 0, 0, 0, 0, 0);

        MvcResult result = submit(user, "PHQ9", responses);
        String body = result.getResponse().getContentAsString();

        assertThat(result.getResponse().getStatus()).isEqualTo(200);
        assertThat(body).contains("\"wellbeingBand\":\"MINIMAL\"");
        assertThat(body).doesNotContain("riskEventId");
        assertNoDiagnosticVocabulary(body);
    }

    @Test
    void shouldInterruptWithCrisisFlowBeforeAnyScoreWhenItem9IsPositive() throws Exception {
        AuthenticatedUser user = registerUserWithConsent();
        List<Integer> responses = List.of(1, 1, 1, 1, 1, 1, 0, 0, 1);

        MvcResult submitResult = submit(user, "PHQ9", responses);
        String body = submitResult.getResponse().getContentAsString();

        assertThat(submitResult.getResponse().getStatus()).isEqualTo(200);
        assertThat(body).doesNotContain("wellbeingBand");
        assertThat(body).doesNotContain("totalScore");
        assertThat(body).contains("riskEventId");
        assertThat(body).contains("SNS 24");

        JsonNode json = objectMapper.readTree(body);
        UUID riskEventId = UUID.fromString(json.get("riskEventId").asText());

        MvcResult acknowledgeResult = mockMvc.perform(post(
                        "/api/v1/users/{userId}/risk-events/{riskEventId}/acknowledge", user.userId(), riskEventId)
                        .with(csrf())
                        .cookie(user.accessTokenCookie()))
                .andExpect(status().isOk())
                .andReturn();

        String acknowledgeBody = acknowledgeResult.getResponse().getContentAsString();
        assertThat(acknowledgeBody).contains("\"wellbeingBand\":\"MILD\"");
    }

    @Test
    void shouldNeverTriggerCrisisFromGad7RegardlessOfScore() throws Exception {
        AuthenticatedUser user = registerUserWithConsent();
        List<Integer> maxResponses = List.of(3, 3, 3, 3, 3, 3, 3);

        MvcResult result = submit(user, "GAD7", maxResponses);
        String body = result.getResponse().getContentAsString();

        assertThat(result.getResponse().getStatus()).isEqualTo(200);
        assertThat(body).contains("\"wellbeingBand\":\"ELEVATED\"");
        assertThat(body).doesNotContain("riskEventId");
    }

    @Test
    void shouldRejectSecondPhq9SubmissionWithinThirtyDays() throws Exception {
        AuthenticatedUser user = registerUserWithConsent();
        List<Integer> responses = List.of(0, 0, 0, 0, 0, 0, 0, 0, 0);
        submit(user, "PHQ9", responses);

        MvcResult secondAttempt = submit(user, "PHQ9", responses);

        assertThat(secondAttempt.getResponse().getStatus()).isEqualTo(403);
    }

    @Test
    void shouldRejectWrongNumberOfResponsesForInstrument() throws Exception {
        AuthenticatedUser user = registerUserWithConsent();
        List<Integer> tooFewResponses = List.of(0, 0, 0);

        MvcResult result = submit(user, "PHQ9", tooFewResponses);

        assertThat(result.getResponse().getStatus()).isEqualTo(400);
    }

    private void assertNoDiagnosticVocabulary(String responseBody) {
        String normalized = responseBody.toLowerCase(Locale.ROOT);
        for (String term : DIAGNOSTIC_TERMS) {
            assertThat(normalized).doesNotContain(term);
        }
    }
}
