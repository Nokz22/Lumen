package dev.lumen.presentation.auth.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Past;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;

public record RegisterRequest(
        @Email @NotBlank String email,
        @NotBlank @Size(min = 10, max = 72) String password,
        @NotBlank @Size(max = 255) String displayName,
        @NotBlank String locale,
        @NotBlank String region,
        @NotNull @Past LocalDate dateOfBirth) {
}
