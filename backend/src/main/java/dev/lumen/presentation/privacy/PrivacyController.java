package dev.lumen.presentation.privacy;

import dev.lumen.application.privacy.AccountErasureService;
import dev.lumen.application.privacy.DataExportService;
import dev.lumen.application.privacy.UserDataExport;
import dev.lumen.presentation.auth.AuthCookies;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletResponse;
import java.util.UUID;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Self-scoped with no ADMIN bypass, like every other endpoint over personal data. An
 * administrator exporting or erasing someone else's account would be the single most
 * damaging thing this API could offer, and the check here is identity, never role.
 */
@Tag(
        name = "Privacy",
        description = "Data-subject rights: access/portability (GDPR Article 15/20) and erasure (Article 17)."
                + " Self-scoped — an ADMIN cannot reach another person's data through these endpoints.")
@RestController
@RequestMapping("/api/v1/users/{userId}/privacy")
@PreAuthorize("#userId == authentication.principal.userId()")
public class PrivacyController {

    private final DataExportService dataExportService;
    private final AccountErasureService accountErasureService;
    private final AuthCookies authCookies;

    public PrivacyController(
            DataExportService dataExportService,
            AccountErasureService accountErasureService,
            AuthCookies authCookies) {
        this.dataExportService = dataExportService;
        this.accountErasureService = accountErasureService;
        this.authCookies = authCookies;
    }

    @Operation(
            summary = "Download everything held about you",
            description = "Returns every category of personal data as one JSON document, with encrypted fields"
                    + " (check-in notes, chat messages, the conversation summary) decrypted so the file is"
                    + " actually readable. Deliberately not gated on health-data consent: someone who has just"
                    + " withdrawn consent is the person most likely to want their copy.")
    @GetMapping("/export")
    public ResponseEntity<UserDataExport> export(@PathVariable UUID userId) {
        UserDataExport export = dataExportService.export(userId);
        ContentDisposition disposition = ContentDisposition.attachment()
                .filename("lumen-data-export.json")
                .build();
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, disposition.toString())
                .contentType(MediaType.APPLICATION_JSON)
                .body(export);
    }

    @Operation(
            summary = "Permanently erase your account",
            description = "Deletes the account and every piece of data attached to it, in one transaction — not a"
                    + " soft-delete flag. Irreversible, and it takes the crisis history with it. What remains is"
                    + " the audit trail, holding ids that no longer resolve to anyone (ADR-0011).")
    @DeleteMapping("/account")
    public ResponseEntity<Void> eraseAccount(@PathVariable UUID userId, HttpServletResponse response) {
        accountErasureService.erase(userId);
        // The refresh tokens are gone with the account, but the access token cookie stays
        // valid for its remaining TTL and would authenticate as a user that no longer
        // exists. Clearing it here ends the session at the same moment as the account.
        authCookies.clear(response);
        return ResponseEntity.noContent().build();
    }
}
