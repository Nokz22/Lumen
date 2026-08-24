package dev.lumen.infrastructure.persistence.user;

import dev.lumen.domain.user.RefreshToken;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

interface SpringDataRefreshTokenJpaRepository extends JpaRepository<RefreshToken, UUID> {

    Optional<RefreshToken> findByTokenHash(String tokenHash);

    List<RefreshToken> findByFamilyIdAndRevokedFalse(UUID familyId);

    @Modifying
    @Query("DELETE FROM RefreshToken t WHERE t.user.id = :userId")
    void deleteByUserId(@Param("userId") UUID userId);
}
