package dev.lumen.domain.crisis;

public class InvalidRiskEventTransitionException extends RuntimeException {

    public InvalidRiskEventTransitionException(RiskEventStatus actual, RiskEventStatus required) {
        super("RiskEvent is " + actual + ", expected " + required);
    }
}
