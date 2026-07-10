package dev.lumen.auth;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import dev.lumen.domain.user.Role;
import dev.lumen.domain.user.User;
import dev.lumen.domain.user.UserRepository;
import dev.lumen.presentation.auth.dto.LoginRequest;
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
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.containers.RabbitMQContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
@Testcontainers
class AuthIntegrationTest {

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
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    private RegisterRequest newRegisterRequest() {
        return new RegisterRequest(
                "test-" + UUID.randomUUID() + "@lumen.dev",
                "SuperSecret123",
                "Test User",
                "en",
                "PT",
                LocalDate.of(1990, 1, 1));
    }

    @Test
    void shouldRegisterAndSetAuthCookies() throws Exception {
        mockMvc.perform(post("/api/v1/auth/register")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(newRegisterRequest())))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.role").value("USER"))
                .andExpect(result -> assertThat(result.getResponse().getCookie("access_token"))
                        .isNotNull())
                .andExpect(result -> assertThat(result.getResponse().getCookie("refresh_token"))
                        .isNotNull());
    }

    @Test
    void shouldRejectRegistrationWithDuplicateEmail() throws Exception {
        RegisterRequest request = newRegisterRequest();
        mockMvc.perform(post("/api/v1/auth/register")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated());

        mockMvc.perform(post("/api/v1/auth/register")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isConflict());
    }

    @Test
    void shouldRejectRegistrationUnderEighteen() throws Exception {
        RegisterRequest underage = new RegisterRequest(
                "test-" + UUID.randomUUID() + "@lumen.dev",
                "SuperSecret123",
                "Too Young",
                "en",
                "PT",
                LocalDate.now().minusYears(16));

        mockMvc.perform(post("/api/v1/auth/register")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(underage)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void shouldRejectLoginWithWrongPassword() throws Exception {
        RegisterRequest registerRequest = newRegisterRequest();
        mockMvc.perform(post("/api/v1/auth/register")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(registerRequest)))
                .andExpect(status().isCreated());

        mockMvc.perform(post("/api/v1/auth/login")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new LoginRequest(registerRequest.email(), "WrongPassword1"))))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void shouldRotateRefreshTokenAndDetectReuse() throws Exception {
        RegisterRequest registerRequest = newRegisterRequest();
        MvcResult registerResult = mockMvc.perform(post("/api/v1/auth/register")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(registerRequest)))
                .andExpect(status().isCreated())
                .andReturn();
        Cookie originalRefreshToken = registerResult.getResponse().getCookie("refresh_token");

        MvcResult refreshResult = mockMvc.perform(post("/api/v1/auth/refresh")
                        .with(csrf())
                        .cookie(originalRefreshToken))
                .andExpect(status().isNoContent())
                .andReturn();
        Cookie rotatedRefreshToken = refreshResult.getResponse().getCookie("refresh_token");
        assertThat(rotatedRefreshToken.getValue()).isNotEqualTo(originalRefreshToken.getValue());

        // A second refresh with the rotated token succeeds (normal usage).
        mockMvc.perform(post("/api/v1/auth/refresh")
                        .with(csrf())
                        .cookie(rotatedRefreshToken))
                .andExpect(status().isNoContent());

        // Reusing the ORIGINAL (already-rotated) token is treated as theft: the whole
        // family is revoked, so even the most recently rotated token stops working.
        mockMvc.perform(post("/api/v1/auth/refresh")
                        .with(csrf())
                        .cookie(originalRefreshToken))
                .andExpect(status().isUnauthorized());
        mockMvc.perform(post("/api/v1/auth/refresh")
                        .with(csrf())
                        .cookie(rotatedRefreshToken))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void shouldRejectRefreshWithoutCookie() throws Exception {
        mockMvc.perform(post("/api/v1/auth/refresh").with(csrf()))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void shouldInvalidateRefreshTokenOnLogout() throws Exception {
        RegisterRequest registerRequest = newRegisterRequest();
        MvcResult registerResult = mockMvc.perform(post("/api/v1/auth/register")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(registerRequest)))
                .andExpect(status().isCreated())
                .andReturn();
        Cookie accessToken = registerResult.getResponse().getCookie("access_token");
        Cookie refreshToken = registerResult.getResponse().getCookie("refresh_token");

        mockMvc.perform(post("/api/v1/auth/logout")
                        .with(csrf())
                        .cookie(accessToken)
                        .cookie(refreshToken))
                .andExpect(status().isNoContent());

        mockMvc.perform(post("/api/v1/auth/refresh").with(csrf()).cookie(refreshToken))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void shouldExposeCurrentUserViaMeEndpoint() throws Exception {
        RegisterRequest registerRequest = newRegisterRequest();
        MvcResult registerResult = mockMvc.perform(post("/api/v1/auth/register")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(registerRequest)))
                .andExpect(status().isCreated())
                .andReturn();
        Cookie accessToken = registerResult.getResponse().getCookie("access_token");

        mockMvc.perform(get("/api/v1/auth/me").cookie(accessToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value(registerRequest.email()));
    }

    @Test
    void shouldRejectMeEndpointWithoutAuthentication() throws Exception {
        mockMvc.perform(get("/api/v1/auth/me")).andExpect(status().isUnauthorized());
    }

    private Cookie loginAsAdminFixture() throws Exception {
        String email = "admin-" + UUID.randomUUID() + "@lumen.dev";
        String rawPassword = "SuperSecret123";
        userRepository.save(new User(
                email,
                passwordEncoder.encode(rawPassword),
                "Admin",
                "en",
                "PT",
                LocalDate.of(1985, 1, 1),
                Role.ADMIN));

        MvcResult loginResult = mockMvc.perform(post("/api/v1/auth/login")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new LoginRequest(email, rawPassword))))
                .andExpect(status().isOk())
                .andReturn();
        return loginResult.getResponse().getCookie("access_token");
    }

    @Test
    void shouldAllowAdminToListUsers() throws Exception {
        Cookie adminAccessToken = loginAsAdminFixture();

        mockMvc.perform(get("/api/v1/admin/users").cookie(adminAccessToken)).andExpect(status().isOk());
    }

    @Test
    void shouldForbidRegularUserFromListingUsers() throws Exception {
        RegisterRequest registerRequest = newRegisterRequest();
        MvcResult registerResult = mockMvc.perform(post("/api/v1/auth/register")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(registerRequest)))
                .andExpect(status().isCreated())
                .andReturn();
        Cookie accessToken = registerResult.getResponse().getCookie("access_token");

        mockMvc.perform(get("/api/v1/admin/users").cookie(accessToken)).andExpect(status().isForbidden());
    }

    @Test
    void shouldPreventAdminFromReadingUserMoodCheckIns() throws Exception {
        Cookie adminAccessToken = loginAsAdminFixture();
        RegisterRequest registerRequest = newRegisterRequest();
        MvcResult registerResult = mockMvc.perform(post("/api/v1/auth/register")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(registerRequest)))
                .andExpect(status().isCreated())
                .andReturn();
        JsonNode json = objectMapper.readTree(registerResult.getResponse().getContentAsString());
        UUID otherUserId = UUID.fromString(json.get("id").asText());

        mockMvc.perform(get("/api/v1/users/{userId}/mood-check-ins", otherUserId).cookie(adminAccessToken))
                .andExpect(status().isForbidden());
    }
}
