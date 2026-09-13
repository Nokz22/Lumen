package dev.lumen.infrastructure.ratelimit;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import io.github.bucket4j.ConsumptionProbe;
import java.time.Duration;
import org.springframework.stereotype.Component;

/**
 * In-memory token buckets, one per (policy, caller).
 *
 * <p>In-memory is a deliberate limit, not an oversight: the buckets live in one JVM, so two
 * instances behind a load balancer would each allow the full quota. Making that correct
 * needs a shared store, and Redis is on this project's list of things not to add without
 * earning it (standards section 15). One instance is what is deployed; when that stops
 * being true, this is the class that changes, and the comment is here so the next person
 * knows it is a known boundary rather than a bug they discovered.
 *
 * <p>Caffeine rather than a plain map because entries must expire. A map keyed by client
 * address grows for every address that ever calls, which is a slow memory leak with an
 * attacker-controlled key.
 */
@Component
public class RateLimiter {

    private static final int MAXIMUM_TRACKED_CALLERS = 100_000;

    private final Cache<String, Bucket> buckets;

    public RateLimiter() {
        this.buckets = Caffeine.newBuilder()
                .maximumSize(MAXIMUM_TRACKED_CALLERS)
                // Longer than the widest window, so an entry is only dropped once its
                // bucket would have refilled anyway and forgetting it changes nothing.
                .expireAfterAccess(Duration.ofHours(2))
                .build();
    }

    public Decision check(RateLimitPolicy policy, String caller) {
        Bucket bucket = buckets.get(policy.name() + ':' + caller, key -> newBucket(policy));
        ConsumptionProbe probe = bucket.tryConsumeAndReturnRemaining(1);
        return new Decision(
                probe.isConsumed(), Duration.ofNanos(probe.getNanosToWaitForRefill()).toSeconds());
    }

    private Bucket newBucket(RateLimitPolicy policy) {
        return Bucket.builder()
                .addLimit(Bandwidth.builder()
                        .capacity(policy.capacity())
                        .refillGreedy(policy.capacity(), policy.window())
                        .build())
                .build();
    }

    /** @param retryAfterSeconds how long until one token is available; 0 when allowed. */
    public record Decision(boolean allowed, long retryAfterSeconds) {
    }
}
