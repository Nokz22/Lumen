package dev.lumen.domain.user;

import java.util.Optional;
import java.util.UUID;

public interface RefreshTokenRepository {

    Optional<RefreshToken> findByTokenHash(String tokenHash);

    void revokeFamily(UUID familyId);

    RefreshToken save(RefreshToken refreshToken);
}
