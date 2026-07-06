package dev.lumen.infrastructure.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * Spring Security's CSRF token is loaded lazily and is only ever materialized (and its
 * cookie written) when something actually calls {@code getToken()} — a pure JSON API has
 * nothing that does that on its own, so without this filter the frontend would never
 * receive an XSRF-TOKEN cookie to echo back. This is Spring Security's own documented
 * workaround for SPA + cookie CSRF.
 */
public class CsrfCookieFilter extends OncePerRequestFilter {

    @Override
    protected void doFilterInternal(
            HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        // Only GET needs this: it's the request that hands a fresh token to a client that
        // doesn't have one yet. Forcing it on state-changing requests too was clearing the
        // cookie after every successful POST — CsrfFilter had already verified that
        // request's token, and re-materializing it re-triggered a fresh save.
        if ("GET".equalsIgnoreCase(request.getMethod())) {
            CsrfToken csrfToken = (CsrfToken) request.getAttribute(CsrfToken.class.getName());
            if (csrfToken != null) {
                csrfToken.getToken();
            }
        }
        filterChain.doFilter(request, response);
    }
}
