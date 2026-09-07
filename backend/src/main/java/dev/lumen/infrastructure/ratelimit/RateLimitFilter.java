package dev.lumen.infrastructure.ratelimit;

import com.fasterxml.jackson.databind.ObjectMapper;
import dev.lumen.application.auth.AuthenticatedPrincipal;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Optional;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ProblemDetail;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.util.UriTemplate;

/**
 * Sits after JwtAuthenticationFilter and before authorization, so the companion limit can
 * key on the authenticated user while the auth endpoints — which have no user yet — key on
 * the caller's address.
 *
 * <p>The address comes from {@code getRemoteAddr()} and never from an X-Forwarded-For
 * header read directly. Behind a proxy that value is the proxy's, which is what
 * {@code server.forward-headers-strategy} exists to correct; trusting the header here
 * instead would let any caller pick their own rate-limit key by sending one.
 */
public class RateLimitFilter extends OncePerRequestFilter {

    private static final UriTemplate COMPANION_MESSAGES =
            new UriTemplate("/api/v1/users/{userId}/conversation/messages");

    private final RateLimiter rateLimiter;
    private final ObjectMapper objectMapper;

    public RateLimitFilter(RateLimiter rateLimiter, ObjectMapper objectMapper) {
        this.rateLimiter = rateLimiter;
        this.objectMapper = objectMapper;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        Optional<Limited> limited = limitFor(request);
        if (limited.isEmpty()) {
            chain.doFilter(request, response);
            return;
        }

        RateLimiter.Decision decision =
                rateLimiter.check(limited.get().policy(), limited.get().caller());
        if (decision.allowed()) {
            chain.doFilter(request, response);
            return;
        }

        reject(response, decision.retryAfterSeconds());
    }

    private Optional<Limited> limitFor(HttpServletRequest request) {
        String path = request.getRequestURI();

        if ("POST".equals(request.getMethod()) && COMPANION_MESSAGES.matches(path)) {
            // Falls back to the address when there is no principal: an unauthenticated
            // call here is rejected moments later anyway, and keying it on null would put
            // every anonymous caller in one shared bucket.
            String caller = currentUserId().orElseGet(request::getRemoteAddr);
            return Optional.of(new Limited(RateLimitPolicy.COMPANION_MESSAGE, caller));
        }

        if (path.startsWith("/api/v1/auth/")) {
            return Optional.of(new Limited(RateLimitPolicy.AUTHENTICATION, request.getRemoteAddr()));
        }

        return Optional.empty();
    }

    private Optional<String> currentUserId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.getPrincipal() instanceof AuthenticatedPrincipal principal) {
            return Optional.of(principal.userId().toString());
        }
        return Optional.empty();
    }

    private void reject(HttpServletResponse response, long retryAfterSeconds) throws IOException {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(
                HttpStatus.TOO_MANY_REQUESTS, "Too many requests. Please wait a moment and try again.");
        response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
        response.setContentType(MediaType.APPLICATION_PROBLEM_JSON_VALUE);
        response.setHeader(HttpHeaders.RETRY_AFTER, Long.toString(Math.max(1, retryAfterSeconds)));
        objectMapper.writeValue(response.getOutputStream(), problem);
    }

    private record Limited(RateLimitPolicy policy, String caller) {
    }
}
