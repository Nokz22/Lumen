package dev.lumen.infrastructure.persistence.user;

import dev.lumen.domain.user.ConsentRecord;
import dev.lumen.domain.user.ConsentType;
import java.util.List;
import java.util.UUID;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

interface SpringDataConsentRecordJpaRepository extends JpaRepository<ConsentRecord, UUID> {

    @Query(
            "SELECT c FROM ConsentRecord c WHERE c.user.id = :userId AND c.consentType = :consentType "
                    + "ORDER BY c.createdAt DESC")
    List<ConsentRecord> findLatestByUserIdAndConsentType(
            @Param("userId") UUID userId, @Param("consentType") ConsentType consentType, Pageable pageable);
}
