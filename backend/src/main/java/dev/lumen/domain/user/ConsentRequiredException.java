package dev.lumen.domain.user;

public class ConsentRequiredException extends RuntimeException {

    public ConsentRequiredException(ConsentType consentType) {
        super("Active consent required: " + consentType);
    }
}
