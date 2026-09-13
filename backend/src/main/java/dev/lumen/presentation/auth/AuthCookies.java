package dev.lumen.presentation.auth;

import dev.lumen.application.auth.AuthTokens;
import jakarta.servlet.http.HttpServletResponse;
import java.time.Duration;
import java.time.Instant;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.stereotype.Component;

/**
 * The one place that knows how an authenticated session is carried (ADR-0004): httpOnly,
 * Secure, SameSite=Strict, and the refresh token scoped to the auth path so it is never
 * sent with an ordinary API call.
 *
 * <p>Extracted from AuthController once account erasure needed to end a session too.
 * Cookies cleared with attributes that do not match the ones they were set with are simply
 * ignored by the browser, which would leave a still-valid access token in place after the
 * account behind it no longer exists — a second caller getting this subtly wrong is
 * exactly the failure worth designing out.
 */
@Component
public class AuthCookies {

    static final String ACCESS_TOKEN_COOKIE = "access_token";
    static final String REFRESH_TOKEN_COOKIE = "refresh_token";
    private static final String REFRESH_TOKEN_PATH = "/api/v1/auth";
    private static final String ROOT_PATH = "/";

    public void set(HttpServletResponse response, AuthTokens tokens) {
        Instant now = Instant.now();
        response.addHeader(
                HttpHeaders.SET_COOKIE,
                build(
                                ACCESS_TOKEN_COOKIE,
                                tokens.accessToken(),
                                ROOT_PATH,
                                Duration.between(now, tokens.accessTokenExpiresAt()))
                        .toString());
        response.addHeader(
                HttpHeaders.SET_COOKIE,
                build(
                                REFRESH_TOKEN_COOKIE,
                                tokens.refreshToken(),
                                REFRESH_TOKEN_PATH,
                                Duration.between(now, tokens.refreshTokenExpiresAt()))
                        .toString());
    }

    public void clear(HttpServletResponse response) {
        response.addHeader(
                HttpHeaders.SET_COOKIE,
                build(ACCESS_TOKEN_COOKIE, "", ROOT_PATH, Duration.ZERO).toString());
        response.addHeader(
                HttpHeaders.SET_COOKIE,
                build(REFRESH_TOKEN_COOKIE, "", REFRESH_TOKEN_PATH, Duration.ZERO)
                        .toString());
    }

    private ResponseCookie build(String name, String value, String path, Duration maxAge) {
        return ResponseCookie.from(name, value)
                .httpOnly(true)
                .secure(true)
                .sameSite("Strict")
                .path(path)
                .maxAge(maxAge)
                .build();
    }
}
