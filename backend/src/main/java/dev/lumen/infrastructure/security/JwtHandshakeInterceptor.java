package dev.lumen.infrastructure.security;

import dev.lumen.application.auth.AuthenticatedPrincipal;
import java.util.Map;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.server.HandshakeInterceptor;

/**
 * The WebSocket handshake is a plain HTTP GET request, so it already passed through
 * the normal security filter chain (JwtAuthenticationFilter, .anyRequest().authenticated())
 * before reaching this interceptor — the access_token cookie was already verified,
 * there is no second JWT-parsing path to keep in sync with the REST one. This only
 * carries the resulting userId forward into the WebSocket session attributes.
 */
public class JwtHandshakeInterceptor implements HandshakeInterceptor {

    static final String USER_ID_ATTRIBUTE = "userId";

    @Override
    public boolean beforeHandshake(
            ServerHttpRequest request,
            ServerHttpResponse response,
            WebSocketHandler wsHandler,
            Map<String, Object> attributes) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (!(authentication != null && authentication.getPrincipal() instanceof AuthenticatedPrincipal principal)) {
            return false;
        }
        attributes.put(USER_ID_ATTRIBUTE, principal.userId().toString());
        return true;
    }

    @Override
    public void afterHandshake(
            ServerHttpRequest request,
            ServerHttpResponse response,
            WebSocketHandler wsHandler,
            Exception exception) {
        // No-op: nothing to clean up after the handshake completes.
    }
}
