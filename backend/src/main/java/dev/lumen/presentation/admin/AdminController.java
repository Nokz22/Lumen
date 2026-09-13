package dev.lumen.presentation.admin;

import dev.lumen.application.user.UserQueryService;
import dev.lumen.application.user.UserSummaryResponse;
import dev.lumen.presentation.shared.PageParameters;
import dev.lumen.presentation.shared.PageResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Access is enforced at the filter-chain level (SecurityConfig: "/api/v1/admin/**" ->
 * hasRole("ADMIN")), not per-method — every endpoint here is admin-only by construction.
 * Deliberately never returns MoodCheckIn data: "ADMIN nunca lê conteúdo emocional de um
 * USER" (project-brief §3).
 */
@Tag(
        name = "Administration",
        description = "ADMIN-only technical administration. Deliberately exposes no emotional content of any USER.")
@RestController
@RequestMapping("/api/v1/admin")
public class AdminController {

    private final UserQueryService userQueryService;

    public AdminController(UserQueryService userQueryService) {
        this.userQueryService = userQueryService;
    }

    @GetMapping("/users")
    public PageResponse<UserSummaryResponse> listUsers(PageParameters pageParameters) {
        return PageResponse.from(userQueryService.list(pageParameters.toPageQuery()));
    }
}
