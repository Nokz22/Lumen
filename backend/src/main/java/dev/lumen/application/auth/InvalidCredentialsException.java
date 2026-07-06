package dev.lumen.application.auth;

/**
 * Deliberately generic — never reveals whether the email exists, to avoid user
 * enumeration.
 */
public class InvalidCredentialsException extends RuntimeException {

    public InvalidCredentialsException() {
        super("Invalid email or password");
    }
}
