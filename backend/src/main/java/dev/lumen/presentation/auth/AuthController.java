package dev.lumen.presentation.auth;

import dev.lumen.application.auth.AuthService;
import dev.lumen.application.auth.AuthTokens;
import dev.lumen.application.auth.AuthenticatedPrincipal;
import dev.lumen.application.user.UserQueryService;
import dev.lumen.application.user.UserSummaryResponse;
import dev.lumen.presentation.auth.dto.LoginRequest;
import dev.lumen.presentation.auth.dto.RegisterRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import java.time.Duration;
import java.time.Instant;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseCookie;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.CookieValue;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {

    private static final String ACCESS_TOKEN_COOKIE = "access_token";
    private static final String REFRESH_TOKEN_COOKIE = "refresh_token";
    private static final String REFRESH_TOKEN_PATH = "/api/v1/auth";

    private final AuthService authService;
    private final UserQueryService userQueryService;

    public AuthController(AuthService authService, UserQueryService userQueryService) {
        this.authService = authService;
        this.userQueryService = userQueryService;
    }

    @PostMapping("/register")
    @ResponseStatus(HttpStatus.CREATED)
    public UserSummaryResponse register(@Valid @RequestBody RegisterRequest request, HttpServletResponse response) {
        AuthTokens tokens = authService.register(
                request.email(),
                request.password(),
                request.displayName(),
                request.locale(),
                request.region(),
                request.dateOfBirth());
        setAuthCookies(response, tokens);
        return userQueryService.getSummary(tokens.userId());
    }

    @PostMapping("/login")
    public UserSummaryResponse login(@Valid @RequestBody LoginRequest request, HttpServletResponse response) {
        AuthTokens tokens = authService.login(request.email(), request.password());
        setAuthCookies(response, tokens);
        return userQueryService.getSummary(tokens.userId());
    }

    @PostMapping("/refresh")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void refresh(@CookieValue(REFRESH_TOKEN_COOKIE) String refreshToken, HttpServletResponse response) {
        setAuthCookies(response, authService.refresh(refreshToken));
    }

    @PostMapping("/logout")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void logout(
            @CookieValue(value = REFRESH_TOKEN_COOKIE, required = false) String refreshToken,
            HttpServletResponse response) {
        if (refreshToken != null) {
            authService.logout(refreshToken);
        }
        clearAuthCookies(response);
    }

    @GetMapping("/me")
    public UserSummaryResponse me(@AuthenticationPrincipal AuthenticatedPrincipal principal) {
        return userQueryService.getSummary(principal.userId());
    }

    private void setAuthCookies(HttpServletResponse response, AuthTokens tokens) {
        Instant now = Instant.now();
        response.addHeader(
                HttpHeaders.SET_COOKIE,
                buildCookie(
                                ACCESS_TOKEN_COOKIE,
                                tokens.accessToken(),
                                "/",
                                Duration.between(now, tokens.accessTokenExpiresAt()))
                        .toString());
        response.addHeader(
                HttpHeaders.SET_COOKIE,
                buildCookie(
                                REFRESH_TOKEN_COOKIE,
                                tokens.refreshToken(),
                                REFRESH_TOKEN_PATH,
                                Duration.between(now, tokens.refreshTokenExpiresAt()))
                        .toString());
    }

    private void clearAuthCookies(HttpServletResponse response) {
        response.addHeader(
                HttpHeaders.SET_COOKIE, buildCookie(ACCESS_TOKEN_COOKIE, "", "/", Duration.ZERO).toString());
        response.addHeader(
                HttpHeaders.SET_COOKIE,
                buildCookie(REFRESH_TOKEN_COOKIE, "", REFRESH_TOKEN_PATH, Duration.ZERO).toString());
    }

    private ResponseCookie buildCookie(String name, String value, String path, Duration maxAge) {
        return ResponseCookie.from(name, value)
                .httpOnly(true)
                .secure(true)
                .sameSite("Strict")
                .path(path)
                .maxAge(maxAge)
                .build();
    }
}
