package dev.lumen.infrastructure.security;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.context.HttpRequestResponseHolder;
import org.springframework.security.web.context.SecurityContextRepository;

/**
 * {@code JwtAuthenticationFilter} re-authenticates every single request from its cookie —
 * there is no session-backed context to load or save, ever. Without this, Spring
 * Security's default stateless repository always reports "no context found," which makes
 * {@code SessionManagementFilter} treat every authenticated request as a brand new login
 * and re-run {@code CsrfAuthenticationStrategy}, rotating (and clearing) the CSRF cookie
 * after every request. Reporting "already handled" here is what actually stops that.
 */
public class StatelessSecurityContextRepository implements SecurityContextRepository {

    // The interface still requires this overload; only its parameter type is deprecated
    // in favor of loadDeferredContext(HttpServletRequest), which has a default we don't
    // need to override.
    @SuppressWarnings("deprecation")
    @Override
    public SecurityContext loadContext(HttpRequestResponseHolder requestResponseHolder) {
        return SecurityContextHolder.createEmptyContext();
    }

    @Override
    public void saveContext(SecurityContext context, HttpServletRequest request, HttpServletResponse response) {
        // Nothing to persist — JwtAuthenticationFilter rebuilds the context every request.
    }

    @Override
    public boolean containsContext(HttpServletRequest request) {
        return true;
    }
}
