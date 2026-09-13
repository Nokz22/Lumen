package dev.lumen.domain.shared;

public class InvalidPageRequestException extends RuntimeException {

    public InvalidPageRequestException(String message) {
        super(message);
    }
}
