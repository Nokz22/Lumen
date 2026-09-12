package dev.lumen.domain.user;

import dev.lumen.domain.shared.PageQuery;
import dev.lumen.domain.shared.PagedResult;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface UserRepository {

    Optional<User> findById(UUID id);

    Optional<User> findByEmail(String email);

    List<User> findAllOrderedByCreatedAt();

    User save(User user);

    void deleteById(UUID id);

    PagedResult<User> findPageOrderedByCreatedAt(PageQuery pageQuery);
}
