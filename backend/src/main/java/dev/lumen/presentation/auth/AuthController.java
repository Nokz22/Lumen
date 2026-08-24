package dev.lumen.presentation.auth;

import dev.lumen.application.auth.AuthService;
import dev.lumen.application.auth.AuthTokens;
import dev.lumen.application.auth.AuthenticatedPrincipal;
import dev.lumen.application.user.UserQueryService;
import dev.lumen.application.user.UserSummaryResponse;
import dev.lumen.presentation.auth.dto.LoginRequest;
import dev.lumen.presentation.auth.dto.RegisterRequest;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.CookieValue;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@Tag(
        name = "Authentication",
        description = "Registration with an 18+ age gate, login, refresh-token rotation and logout. Tokens travel as"
                + " httpOnly cookies (ADR-0004).")
@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {

    private static final String REFRESH_TOKEN_COOKIE = AuthCookies.REFRESH_TOKEN_COOKIE;

    private final AuthService authService;
    private final UserQueryService userQueryService;
    private final AuthCookies authCookies;

    public AuthController(AuthService authService, UserQueryService userQueryService, AuthCookies authCookies) {
        this.authService = authService;
        this.userQueryService = userQueryService;
        this.authCookies = authCookies;
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
        authCookies.set(response, tokens);
        return userQueryService.getSummary(tokens.userId());
    }

    @PostMapping("/login")
    public UserSummaryResponse login(@Valid @RequestBody LoginRequest request, HttpServletResponse response) {
        AuthTokens tokens = authService.login(request.email(), request.password());
        authCookies.set(response, tokens);
        return userQueryService.getSummary(tokens.userId());
    }

    @PostMapping("/refresh")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void refresh(@CookieValue(REFRESH_TOKEN_COOKIE) String refreshToken, HttpServletResponse response) {
        authCookies.set(response, authService.refresh(refreshToken));
    }

    @PostMapping("/logout")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void logout(
            @CookieValue(value = REFRESH_TOKEN_COOKIE, required = false) String refreshToken,
            HttpServletResponse response) {
        if (refreshToken != null) {
            authService.logout(refreshToken);
        }
        authCookies.clear(response);
    }

    @GetMapping("/me")
    public UserSummaryResponse me(@AuthenticationPrincipal AuthenticatedPrincipal principal) {
        return userQueryService.getSummary(principal.userId());
    }
}
