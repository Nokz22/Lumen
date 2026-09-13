package dev.lumen.privacy;

import static dev.lumen.support.TestClients.distinctClient;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
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
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.containers.RabbitMQContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
@Testcontainers
class PrivacyIntegrationTest {

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
    private JdbcTemplate jdbcTemplate;

    /** Each test signs in as its own client, so they do not share one rate-limit budget. */
    private final RequestPostProcessor client = distinctClient();

    private record AuthenticatedUser(UUID userId, Cookie accessTokenCookie) {
    }

    private AuthenticatedUser registerUserWithConsent() throws Exception {
        RegisterRequest registerRequest = new RegisterRequest(
                "privacy-" + UUID.randomUUID() + "@lumen.dev",
                "SuperSecret123",
                "Privacy User",
                "en",
                "PT",
                LocalDate.of(1990, 1, 1));

        MvcResult result = mockMvc.perform(post("/api/v1/auth/register")
                        .with(csrf())
                        .with(client)
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
    void shouldExportTheDataThePersonActuallyProduced() throws Exception {
        AuthenticatedUser user = registerUserWithConsent();
        MoodCheckInRequest checkIn =
                new MoodCheckInRequest(MoodEmotion.ANXIOUS, 2, new BigDecimal("5.5"), 2, "a private note");
        mockMvc.perform(post("/api/v1/users/{userId}/mood-check-ins", user.userId())
                        .with(csrf())
                        .cookie(user.accessTokenCookie())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(checkIn)))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/v1/users/{userId}/privacy/export", user.userId())
                        .cookie(user.accessTokenCookie()))
                .andExpect(status().isOk())
                .andExpect(header().string(HttpHeaders.CONTENT_DISPOSITION, containsString("lumen-data-export.json")))
                .andExpect(jsonPath("$.account.email").exists())
                .andExpect(jsonPath("$.consents", hasSize(1)))
                .andExpect(jsonPath("$.moodCheckIns", hasSize(1)))
                .andExpect(jsonPath("$.moodCheckIns[0].note").value("a private note"));
    }

    /**
     * Consent gates new processing; it does not gate the right to a copy of what was
     * already collected. Withdrawing it must not lock a person out of their own record.
     */
    @Test
    void shouldStillExportAfterHealthDataConsentIsWithdrawn() throws Exception {
        AuthenticatedUser user = registerUserWithConsent();
        mockMvc.perform(post("/api/v1/users/{userId}/consents/HEALTH_DATA_PROCESSING/revoke", user.userId())
                        .with(csrf())
                        .cookie(user.accessTokenCookie()))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/api/v1/users/{userId}/mood-check-ins", user.userId())
                        .cookie(user.accessTokenCookie()))
                .andExpect(status().isForbidden());
        mockMvc.perform(get("/api/v1/users/{userId}/privacy/export", user.userId())
                        .cookie(user.accessTokenCookie()))
                .andExpect(status().isOk());
    }

    /**
     * The invariant, asserted against the live schema rather than against a list written
     * by hand: whatever table carries a user_id, erasure has to empty it. A table added
     * later and forgotten fails here instead of leaving data behind in production.
     */
    @Test
    void shouldLeaveNoRowBehindInAnyTableKeyedByUser() throws Exception {
        AuthenticatedUser user = registerUserWithConsent();
        seedOneRowInEveryUserOwnedTable(user.userId());

        mockMvc.perform(delete("/api/v1/users/{userId}/privacy/account", user.userId())
                        .with(csrf())
                        .cookie(user.accessTokenCookie()))
                .andExpect(status().isNoContent());

        List<String> tables = jdbcTemplate.queryForList(
                "SELECT table_name FROM information_schema.columns "
                        + "WHERE table_schema = 'public' AND column_name = 'user_id' ORDER BY table_name",
                String.class);
        assertThat(tables).isNotEmpty();
        for (String table : tables) {
            Integer remaining = jdbcTemplate.queryForObject(
                    "SELECT count(*) FROM " + table + " WHERE user_id = ?", Integer.class, user.userId());
            assertThat(remaining).as("rows left in %s after erasure", table).isZero();
        }

        assertThat(jdbcTemplate.queryForObject(
                        "SELECT count(*) FROM users WHERE id = ?", Integer.class, user.userId()))
                .isZero();
    }

    /** Children of assessments carry no user_id of their own, so they need their own check. */
    @Test
    void shouldLeaveNoAssessmentAnswersOrScoresBehind() throws Exception {
        AuthenticatedUser user = registerUserWithConsent();
        seedOneRowInEveryUserOwnedTable(user.userId());

        mockMvc.perform(delete("/api/v1/users/{userId}/privacy/account", user.userId())
                        .with(csrf())
                        .cookie(user.accessTokenCookie()))
                .andExpect(status().isNoContent());

        assertThat(jdbcTemplate.queryForObject(
                        "SELECT count(*) FROM assessment_responses r "
                                + "WHERE NOT EXISTS (SELECT 1 FROM assessments a WHERE a.id = r.assessment_id)",
                        Integer.class))
                .isZero();
        assertThat(jdbcTemplate.queryForObject(
                        "SELECT count(*) FROM assessment_scores s "
                                + "WHERE NOT EXISTS (SELECT 1 FROM assessments a WHERE a.id = s.assessment_id)",
                        Integer.class))
                .isZero();
    }

    /**
     * Accountability under Article 5(2) outlives the account: the trail keeps saying an
     * erasure happened, while the id it names resolves to nobody.
     */
    @Test
    void shouldKeepTheAuditTrailOfTheErasureItself() throws Exception {
        AuthenticatedUser user = registerUserWithConsent();

        mockMvc.perform(delete("/api/v1/users/{userId}/privacy/account", user.userId())
                        .with(csrf())
                        .cookie(user.accessTokenCookie()))
                .andExpect(status().isNoContent());

        assertThat(jdbcTemplate.queryForObject(
                        "SELECT count(*) FROM audit_log_entries WHERE subject_user_id = ? AND action = 'ERASE_ACCOUNT'",
                        Integer.class,
                        user.userId()))
                .isEqualTo(1);
    }

    /**
     * Regression guard. The audit insert happens inside the export's own transaction, and
     * marking that transaction read-only leaves Hibernate in FlushMode.MANUAL, which drops
     * the insert with no error at all — the access reads as recorded while nothing is.
     */
    @Test
    void shouldRecordTheExportInTheAuditTrail() throws Exception {
        AuthenticatedUser user = registerUserWithConsent();

        mockMvc.perform(get("/api/v1/users/{userId}/privacy/export", user.userId())
                        .cookie(user.accessTokenCookie()))
                .andExpect(status().isOk());

        assertThat(jdbcTemplate.queryForObject(
                        "SELECT count(*) FROM audit_log_entries WHERE subject_user_id = ?"
                                + " AND action = 'EXPORT_PERSONAL_DATA'",
                        Integer.class,
                        user.userId()))
                .isEqualTo(1);
    }

    @Test
    void shouldForbidExportingOrErasingAnotherPersonsAccount() throws Exception {
        AuthenticatedUser userA = registerUserWithConsent();
        AuthenticatedUser userB = registerUserWithConsent();

        mockMvc.perform(get("/api/v1/users/{userId}/privacy/export", userB.userId())
                        .cookie(userA.accessTokenCookie()))
                .andExpect(status().isForbidden());
        mockMvc.perform(delete("/api/v1/users/{userId}/privacy/account", userB.userId())
                        .with(csrf())
                        .cookie(userA.accessTokenCookie()))
                .andExpect(status().isForbidden());
    }

    private void seedOneRowInEveryUserOwnedTable(UUID userId) {
        UUID exerciseId = jdbcTemplate.queryForObject("SELECT id FROM exercises LIMIT 1", UUID.class);
        UUID checkInId = UUID.randomUUID();
        UUID assessmentId = UUID.randomUUID();
        UUID messageId = UUID.randomUUID();

        seedSelfCareRows(userId, exerciseId, checkInId);
        seedAssessmentRows(userId, assessmentId);
        seedCompanionRows(userId, messageId);
    }

    private void seedSelfCareRows(UUID userId, UUID exerciseId, UUID checkInId) {
        jdbcTemplate.update(
                "INSERT INTO mood_check_ins (id, user_id, emotion, energy_level, sleep_hours, sleep_quality, note,"
                        + " check_in_date, created_at, version) VALUES (?, ?, 'SAD', 2, 6.0, 3, 'note', ?, now(), 0)",
                checkInId,
                userId,
                LocalDate.of(2026, 1, 4));
        jdbcTemplate.update(
                "INSERT INTO recommendations (id, user_id, mood_check_in_id, exercise_id, reason, created_at)"
                        + " VALUES (?, ?, ?, ?, 'because', now())",
                UUID.randomUUID(),
                userId,
                checkInId,
                exerciseId);
        jdbcTemplate.update(
                "INSERT INTO exercise_completions (id, user_id, exercise_id, completed_at) VALUES (?, ?, ?, now())",
                UUID.randomUUID(),
                userId,
                exerciseId);
        jdbcTemplate.update(
                "INSERT INTO wearable_readings (id, user_id, type, value, recorded_at, source, created_at)"
                        + " VALUES (?, ?, 'HEART_RATE', 62, now(), 'SIMULATOR', now())",
                UUID.randomUUID(),
                userId);
    }

    private void seedAssessmentRows(UUID userId, UUID assessmentId) {
        jdbcTemplate.update(
                "INSERT INTO assessments (id, user_id, assessment_type, status, created_at, version)"
                        + " VALUES (?, ?, 'PHQ9', 'SCORED', now(), 0)",
                assessmentId,
                userId);
        jdbcTemplate.update(
                "INSERT INTO assessment_responses (id, assessment_id, item_number, value) VALUES (?, ?, 9, 1)",
                UUID.randomUUID(),
                assessmentId);
        jdbcTemplate.update(
                "INSERT INTO assessment_scores (id, assessment_id, total_score, wellbeing_band, created_at)"
                        + " VALUES (?, ?, 7, 'MILD', now())",
                UUID.randomUUID(),
                assessmentId);
        jdbcTemplate.update(
                "INSERT INTO risk_events (id, user_id, assessment_id, trigger_source, status, detected_at, version)"
                        + " VALUES (?, ?, ?, 'PHQ9_ITEM9', 'DETECTED', now(), 0)",
                UUID.randomUUID(),
                userId,
                assessmentId);
    }

    private void seedCompanionRows(UUID userId, UUID messageId) {
        jdbcTemplate.update(
                "INSERT INTO conversation_messages (id, user_id, role, content, created_at)"
                        + " VALUES (?, ?, 'USER', 'ciphertext', now())",
                messageId,
                userId);
        jdbcTemplate.update(
                "INSERT INTO conversation_summaries (id, user_id, summary_text, summarized_through_message_id,"
                        + " updated_at, version) VALUES (?, ?, 'ciphertext', ?, now(), 0)",
                UUID.randomUUID(),
                userId,
                messageId);
    }
}
