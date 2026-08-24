package dev.lumen.presentation.consent;

import dev.lumen.application.consent.ConsentService;
import dev.lumen.domain.user.ConsentType;
import dev.lumen.presentation.consent.dto.ConsentStatusResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@Tag(
        name = "Consent",
        description = "Granular, revocable consent per processing purpose. Revoking disables the dependent feature in"
                + " the same request cycle (ADR-0005).")
@RestController
@RequestMapping("/api/v1/users/{userId}/consents/{consentType}")
@PreAuthorize("#userId == authentication.principal.userId()")
public class ConsentController {

    private final ConsentService consentService;

    public ConsentController(ConsentService consentService) {
        this.consentService = consentService;
    }

    @PostMapping("/grant")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void grant(@PathVariable UUID userId, @PathVariable ConsentType consentType) {
        consentService.grant(userId, consentType);
    }

    @PostMapping("/revoke")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void revoke(@PathVariable UUID userId, @PathVariable ConsentType consentType) {
        consentService.revoke(userId, consentType);
    }

    @GetMapping
    public ConsentStatusResponse status(@PathVariable UUID userId, @PathVariable ConsentType consentType) {
        return new ConsentStatusResponse(consentService.isActive(userId, consentType));
    }
}
