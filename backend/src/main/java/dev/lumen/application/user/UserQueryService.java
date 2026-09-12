package dev.lumen.application.user;

import dev.lumen.domain.shared.PageQuery;
import dev.lumen.domain.shared.PagedResult;
import dev.lumen.domain.user.User;
import dev.lumen.domain.user.UserNotFoundException;
import dev.lumen.domain.user.UserRepository;
import java.util.UUID;
import org.springframework.stereotype.Service;

@Service
public class UserQueryService {

    private final UserRepository userRepository;

    public UserQueryService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    public UserSummaryResponse getSummary(UUID userId) {
        return toSummary(userRepository.findById(userId).orElseThrow(() -> new UserNotFoundException(userId)));
    }

    public PagedResult<UserSummaryResponse> list(PageQuery pageQuery) {
        return userRepository.findPageOrderedByCreatedAt(pageQuery).map(this::toSummary);
    }

    private UserSummaryResponse toSummary(User user) {
        return new UserSummaryResponse(
                user.getId(), user.getEmail(), user.getDisplayName(), user.getRole(), user.getCreatedAt());
    }
}
