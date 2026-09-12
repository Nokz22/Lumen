package dev.lumen.infrastructure.persistence.user;

import dev.lumen.domain.user.User;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

interface SpringDataUserJpaRepository extends JpaRepository<User, UUID> {

    Optional<User> findByEmail(String email);

    Page<User> findAllByOrderByCreatedAtAsc(Pageable pageable);
}
