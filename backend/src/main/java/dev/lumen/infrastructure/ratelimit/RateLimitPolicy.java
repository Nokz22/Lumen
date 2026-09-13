package dev.lumen.infrastructure.ratelimit;

import java.time.Duration;

/**
 * What is limited, and — more importantly in this domain — what is not.
 *
 * <p>The crisis flow is deliberately absent. Presenting resources and acknowledging a
 * RiskEvent are never rate limited, at any rate, for any caller. Someone retrying because
 * a page did not load is the last person who should meet a 429, and no abuse scenario
 * against those two endpoints is worth that trade (project-brief section 6.2: from the
 * moment risk is signalled, safety is the default behaviour, not best effort).
 *
 * <p>Reading and writing check-ins, instruments and history are also unlimited: they are
 * authenticated, scoped to the caller's own data, and cost nothing worth defending.
 */
public enum RateLimitPolicy {

    /**
     * Unauthenticated and reachable by anyone, so the limit is per client address. Sized to
     * be invisible to a person mistyping a password and expensive for a credential-stuffing
     * script.
     */
    AUTHENTICATION(10, Duration.ofMinutes(1)),

    /**
     * Per user, not per address: this one costs real money on every call, and the account
     * is the thing being spent. Generous enough for a long conversation in one sitting.
     */
    COMPANION_MESSAGE(30, Duration.ofHours(1));

    private final int capacity;
    private final Duration window;

    RateLimitPolicy(int capacity, Duration window) {
        this.capacity = capacity;
        this.window = window;
    }

    public int capacity() {
        return capacity;
    }

    public Duration window() {
        return window;
    }
}
