package dev.lumen.infrastructure.security;

import java.security.Principal;
import java.util.Map;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.server.support.DefaultHandshakeHandler;

/**
 * Turns the userId stashed by JwtHandshakeInterceptor into the STOMP session's
 * Principal, whose name is what SimpMessagingTemplate.convertAndSendToUser() matches
 * against — so it must be the raw userId string, the same value the REST API uses.
 */
public class UserPrincipalHandshakeHandler extends DefaultHandshakeHandler {

    @Override
    protected Principal determineUser(
            ServerHttpRequest request, WebSocketHandler wsHandler, Map<String, Object> attributes) {
        String userId = (String) attributes.get(JwtHandshakeInterceptor.USER_ID_ATTRIBUTE);
        return new StompUserPrincipal(userId);
    }

    private record StompUserPrincipal(String name) implements Principal {

        @Override
        public String getName() {
            return name;
        }
    }
}
