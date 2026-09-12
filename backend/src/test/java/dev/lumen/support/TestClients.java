package dev.lumen.support;

import java.util.concurrent.atomic.AtomicInteger;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

/**
 * Gives each test its own client address.
 *
 * <p>The authentication limit is ten requests a minute keyed on the caller's address
 * ({@code RateLimitPolicy.AUTHENTICATION}). MockMvc reports every request as arriving from
 * 127.0.0.1, so a whole test class spends one shared budget: twenty sign-ins in two seconds
 * from a single address is exactly the credential-stuffing shape the limiter exists to
 * stop, and the later tests meet a 429 that has nothing to do with what they assert.
 *
 * <p>Raising the limit under test, or leaving the filter out of the chain, would hide that
 * path from the integration suite entirely. Each test is a different person signing in, so
 * address them that way and leave the limiter running. JUnit builds a new test instance per
 * method, which means an instance field holds one address for exactly one test.
 */
public final class TestClients {

    /** RFC 5737 TEST-NET-2, reserved for documentation and never routable. */
    private static final String TEST_NET_2 = "198.51.100.";

    private static final int HOSTS = 254;

    private static final AtomicInteger ISSUED = new AtomicInteger();

    private TestClients() {
    }

    /**
     * One address, fixed for the life of the returned post-processor.
     *
     * @return a post-processor that makes every request it touches come from that address
     */
    public static RequestPostProcessor distinctClient() {
        String address = TEST_NET_2 + (ISSUED.getAndIncrement() % HOSTS + 1);
        return request -> {
            request.setRemoteAddr(address);
            return request;
        };
    }
}
