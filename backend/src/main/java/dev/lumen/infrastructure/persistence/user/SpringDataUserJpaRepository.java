package dev.lumen.infrastructure.persistence.user;

import dev.lumen.domain.user.User;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

interface SpringDataUserJpaRepository extends JpaRepository<User, UUID> {
}
