package dev.lumen.infrastructure.persistence.user;

import dev.lumen.domain.user.RefreshToken;
import dev.lumen.domain.user.RefreshTokenRepository;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Repository;

@Repository
class RefreshTokenRepositoryImpl implements RefreshTokenRepository {

    private final SpringDataRefreshTokenJpaRepository jpaRepository;

    RefreshTokenRepositoryImpl(SpringDataRefreshTokenJpaRepository jpaRepository) {
        this.jpaRepository = jpaRepository;
    }

    @Override
    public Optional<RefreshToken> findByTokenHash(String tokenHash) {
        return jpaRepository.findByTokenHash(tokenHash);
    }

    @Override
    public void revokeFamily(UUID familyId) {
        Instant now = Instant.now();
        jpaRepository.findByFamilyIdAndRevokedFalse(familyId).forEach(token -> {
            token.revoke(now);
            jpaRepository.save(token);
        });
    }

    @Override
    public RefreshToken save(RefreshToken refreshToken) {
        return jpaRepository.save(refreshToken);
    }
}
