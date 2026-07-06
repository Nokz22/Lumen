package dev.lumen.application.auth;

public class InvalidRefreshTokenException extends RuntimeException {

    public InvalidRefreshTokenException() {
        super("Invalid, expired or revoked refresh token");
    }
}
