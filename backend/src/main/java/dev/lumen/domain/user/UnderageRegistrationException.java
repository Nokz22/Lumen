package dev.lumen.domain.user;

public class UnderageRegistrationException extends RuntimeException {

    public UnderageRegistrationException() {
        super("Lumen is only available to adults (18+)");
    }
}
