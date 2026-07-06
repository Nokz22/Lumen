package dev.lumen.infrastructure.persistence.user;

import dev.lumen.domain.user.User;
import dev.lumen.domain.user.UserRepository;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Repository;

@Repository
class UserRepositoryImpl implements UserRepository {

    private final SpringDataUserJpaRepository jpaRepository;

    UserRepositoryImpl(SpringDataUserJpaRepository jpaRepository) {
        this.jpaRepository = jpaRepository;
    }

    @Override
    public Optional<User> findById(UUID id) {
        return jpaRepository.findById(id);
    }

    @Override
    public Optional<User> findByEmail(String email) {
        return jpaRepository.findByEmail(email);
    }

    @Override
    public List<User> findAllOrderedByCreatedAt() {
        return jpaRepository.findAll(Sort.by(Sort.Direction.ASC, "createdAt"));
    }

    @Override
    public User save(User user) {
        return jpaRepository.save(user);
    }
}
