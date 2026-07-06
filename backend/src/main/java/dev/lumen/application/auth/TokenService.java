package dev.lumen.application.auth;

import dev.lumen.domain.user.Role;
import dev.lumen.domain.user.User;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Instant;
import java.util.Base64;
import java.util.HexFormat;
import java.util.Optional;
import java.util.UUID;
import javax.crypto.SecretKey;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

/**
 * Access tokens are short-lived JWTs (stateless, cheap to verify on every request).
 * Refresh tokens are deliberately NOT JWTs — they are opaque random values, stored hashed,
 * so they can be revoked and rotation-reuse can be detected (a stateless JWT can't be
 * revoked without extra infrastructure).
 */
@Service
public class TokenService {

    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    private final SecretKey signingKey;

    public TokenService(@Value("${app.jwt.secret}") String secret) {
        this.signingKey = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
    }

    public String generateAccessToken(User user, Instant now, Instant expiresAt) {
        return Jwts.builder()
                .subject(user.getId().toString())
                .claim("role", user.getRole().name())
                .issuedAt(java.util.Date.from(now))
                .expiration(java.util.Date.from(expiresAt))
                .signWith(signingKey)
                .compact();
    }

    public Optional<AuthenticatedPrincipal> parseAccessToken(String token) {
        try {
            Claims claims =
                    Jwts.parser().verifyWith(signingKey).build().parseSignedClaims(token).getPayload();
            UUID userId = UUID.fromString(claims.getSubject());
            Role role = Role.valueOf(claims.get("role", String.class));
            return Optional.of(new AuthenticatedPrincipal(userId, role));
        } catch (JwtException | IllegalArgumentException e) {
            return Optional.empty();
        }
    }

    public String generateRefreshTokenValue() {
        byte[] bytes = new byte[32];
        SECURE_RANDOM.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    public String hashToken(String rawToken) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(rawToken.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 not available", e);
        }
    }
}
