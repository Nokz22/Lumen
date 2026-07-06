package dev.lumen.domain.crisis;

import java.util.UUID;

public class RiskEventNotFoundException extends RuntimeException {

    public RiskEventNotFoundException(UUID id) {
        super("RiskEvent not found: " + id);
    }
}
