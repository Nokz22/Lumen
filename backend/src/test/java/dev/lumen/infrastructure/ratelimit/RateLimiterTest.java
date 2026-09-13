package dev.lumen.infrastructure.ratelimit;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class RateLimiterTest {

    private final RateLimiter rateLimiter = new RateLimiter();

    @Test
    void shouldAllowUpToTheCapacityAndRefuseTheNextRequest() {
        int capacity = RateLimitPolicy.AUTHENTICATION.capacity();

        for (int attempt = 1; attempt <= capacity; attempt++) {
            assertThat(rateLimiter.check(RateLimitPolicy.AUTHENTICATION, "198.51.100.7").allowed())
                    .as("attempt %d of %d should be allowed", attempt, capacity)
                    .isTrue();
        }

        assertThat(rateLimiter
                        .check(RateLimitPolicy.AUTHENTICATION, "198.51.100.7")
                        .allowed())
                .isFalse();
    }

    /** One caller exhausting their quota must not lock anybody else out. */
    @Test
    void shouldTrackEachCallerSeparately() {
        for (int i = 0; i < RateLimitPolicy.AUTHENTICATION.capacity(); i++) {
            rateLimiter.check(RateLimitPolicy.AUTHENTICATION, "198.51.100.7");
        }

        assertThat(rateLimiter
                        .check(RateLimitPolicy.AUTHENTICATION, "203.0.113.9")
                        .allowed())
                .isTrue();
    }

    /** Spending the login quota must not cost anyone their conversation. */
    @Test
    void shouldTrackEachPolicySeparatelyForTheSameCaller() {
        for (int i = 0; i < RateLimitPolicy.AUTHENTICATION.capacity(); i++) {
            rateLimiter.check(RateLimitPolicy.AUTHENTICATION, "same-caller");
        }

        assertThat(rateLimiter
                        .check(RateLimitPolicy.COMPANION_MESSAGE, "same-caller")
                        .allowed())
                .isTrue();
    }

    @Test
    void shouldTellARefusedCallerHowLongToWait() {
        for (int i = 0; i < RateLimitPolicy.AUTHENTICATION.capacity(); i++) {
            rateLimiter.check(RateLimitPolicy.AUTHENTICATION, "198.51.100.7");
        }

        RateLimiter.Decision refused = rateLimiter.check(RateLimitPolicy.AUTHENTICATION, "198.51.100.7");

        assertThat(refused.allowed()).isFalse();
        assertThat(refused.retryAfterSeconds())
                .isBetween(0L, RateLimitPolicy.AUTHENTICATION.window().toSeconds());
    }
}
