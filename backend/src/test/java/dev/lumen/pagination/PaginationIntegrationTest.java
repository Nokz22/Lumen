package dev.lumen.pagination;

import static dev.lumen.support.TestClients.distinctClient;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import dev.lumen.domain.shared.PageQuery;
import dev.lumen.presentation.auth.dto.RegisterRequest;
import jakarta.servlet.http.Cookie;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
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
class PaginationIntegrationTest {

    private static final int SEEDED_CHECK_INS = 55;

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

    private AuthenticatedUser registerUserWithHistory() throws Exception {
        RegisterRequest registerRequest = new RegisterRequest(
                "paging-" + UUID.randomUUID() + "@lumen.dev",
                "SuperSecret123",
                "Paging User",
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
        Cookie cookie = result.getResponse().getCookie("access_token");

        mockMvc.perform(post("/api/v1/users/{userId}/consents/HEALTH_DATA_PROCESSING/grant", userId)
                        .with(csrf())
                        .cookie(cookie))
                .andExpect(status().isNoContent());

        // Seeded directly: a check-in is unique per user per UTC day, so a history longer
        // than one page cannot be produced through the API inside a single test run.
        for (int daysAgo = 1; daysAgo <= SEEDED_CHECK_INS; daysAgo++) {
            jdbcTemplate.update(
                    "INSERT INTO mood_check_ins (id, user_id, emotion, energy_level, sleep_hours, sleep_quality,"
                            + " check_in_date, created_at, version) VALUES (?, ?, 'NEUTRAL', 3, 7.0, 3, ?, now(), 0)",
                    UUID.randomUUID(),
                    userId,
                    LocalDate.now().minusDays(daysAgo));
        }
        return new AuthenticatedUser(userId, cookie);
    }

    @Test
    void shouldReturnOneDefaultSizedPageInsteadOfEveryRow() throws Exception {
        AuthenticatedUser user = registerUserWithHistory();

        mockMvc.perform(get("/api/v1/users/{userId}/mood-check-ins", user.userId())
                        .cookie(user.accessTokenCookie()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(PageQuery.DEFAULT_SIZE)))
                .andExpect(jsonPath("$.page").value(0))
                .andExpect(jsonPath("$.size").value(PageQuery.DEFAULT_SIZE))
                .andExpect(jsonPath("$.totalElements").value(SEEDED_CHECK_INS))
                .andExpect(jsonPath("$.totalPages").value(3))
                .andExpect(jsonPath("$.hasNext").value(true));
    }

    @Test
    void shouldReportTheLastPageAsHavingNoNext() throws Exception {
        AuthenticatedUser user = registerUserWithHistory();

        mockMvc.perform(get("/api/v1/users/{userId}/mood-check-ins", user.userId())
                        .param("page", "2")
                        .cookie(user.accessTokenCookie()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(SEEDED_CHECK_INS - 2 * PageQuery.DEFAULT_SIZE)))
                .andExpect(jsonPath("$.hasNext").value(false));
    }

    /** Paging that drops or repeats a row is worse than no paging: it hides data silently. */
    @Test
    void shouldCoverEveryRowExactlyOnceAcrossPages() throws Exception {
        AuthenticatedUser user = registerUserWithHistory();
        List<String> seen = new ArrayList<>();

        for (int page = 0; page < 3; page++) {
            MvcResult result = mockMvc.perform(get("/api/v1/users/{userId}/mood-check-ins", user.userId())
                            .param("page", String.valueOf(page))
                            .cookie(user.accessTokenCookie()))
                    .andExpect(status().isOk())
                    .andReturn();
            for (JsonNode entry : objectMapper.readTree(result.getResponse().getContentAsString()).get("content")) {
                seen.add(entry.get("id").asText());
            }
        }

        assertThat(seen).hasSize(SEEDED_CHECK_INS).doesNotHaveDuplicates();
    }

    /**
     * Without the cap, size=1000000 turns a paginated endpoint straight back into the
     * unbounded one it replaced.
     */
    @Test
    void shouldRefuseAPageSizeAboveTheCap() throws Exception {
        AuthenticatedUser user = registerUserWithHistory();

        mockMvc.perform(get("/api/v1/users/{userId}/mood-check-ins", user.userId())
                        .param("size", String.valueOf(PageQuery.MAX_SIZE + 1))
                        .cookie(user.accessTokenCookie()))
                .andExpect(status().isBadRequest());
    }

    @Test
    void shouldRefuseANegativePage() throws Exception {
        AuthenticatedUser user = registerUserWithHistory();

        mockMvc.perform(get("/api/v1/users/{userId}/mood-check-ins", user.userId())
                        .param("page", "-1")
                        .cookie(user.accessTokenCookie()))
                .andExpect(status().isBadRequest());
    }

    /**
     * The export is the one read that must stay complete (ADR-0011). Pagination applies to
     * the screens, never to the copy of their own data a person is entitled to.
     */
    @Test
    void shouldStillExportEveryRowRegardlessOfPageSize() throws Exception {
        AuthenticatedUser user = registerUserWithHistory();

        mockMvc.perform(get("/api/v1/users/{userId}/privacy/export", user.userId())
                        .cookie(user.accessTokenCookie()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.moodCheckIns", hasSize(SEEDED_CHECK_INS)));
    }
}
