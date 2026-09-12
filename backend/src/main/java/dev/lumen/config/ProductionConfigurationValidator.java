package dev.lumen.config;

import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.env.EnvironmentPostProcessor;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.util.StringUtils;

/**
 * Stops a misconfigured production instance before anything else happens.
 *
 * <p>An {@link EnvironmentPostProcessor} rather than a bean, because it has to run before
 * the first bean is created. Left to the normal startup order, a missing database variable
 * surfaces as {@code 'url' must start with "jdbc"} from deep inside Hikari — technically a
 * failure, but one that says nothing about which variable was forgotten. This reports every
 * missing value at once, by name, before the first connection is attempted.
 *
 * <p>It also rejects the development secrets. Those values are committed in
 * application.yml, which puts them in this repository's history permanently, and makes them
 * the obvious thing to paste into a deployment when something will not start. A JWT signed
 * with a key anybody can read from a public repo can be forged by anybody who reads it, and
 * an encryption key in the same position means the emotional content encrypted at rest is
 * encrypted to a public key. Refusing to start is the only safe response: an instance that
 * warns and serves traffic looks healthy while handing out forgeable sessions.
 */
public class ProductionConfigurationValidator implements EnvironmentPostProcessor {

    private static final String PRODUCTION_PROFILE = "prod";
    private static final String ENCRYPTION_KEY = "ENCRYPTION_KEY";
    private static final int AES_256_KEY_BYTES = 32;

    private static final List<String> REQUIRED_VARIABLES = List.of(
            "DATABASE_JDBC_URL",
            "DATABASE_USERNAME",
            "DATABASE_PASSWORD",
            "RABBITMQ_HOST",
            "RABBITMQ_USERNAME",
            "RABBITMQ_PASSWORD",
            "JWT_SECRET",
            "ENCRYPTION_KEY",
            "CORS_ALLOWED_ORIGINS");

    private static final Map<String, String> COMMITTED_DEVELOPMENT_VALUES = Map.of(
            "JWT_SECRET", "dev-only-insecure-secret-change-me-0123456789abcdefghijklmnop",
            "ENCRYPTION_KEY", "zxLVCSUFSFXj60TnIb6R7L+4Y0jAPDe62VKLIvTkjqI=");

    @Override
    public void postProcessEnvironment(ConfigurableEnvironment environment, SpringApplication application) {
        if (!isProduction(environment)) {
            return;
        }

        List<String> problems = new ArrayList<>();
        for (String variable : REQUIRED_VARIABLES) {
            String value = environment.getProperty(variable);
            if (!StringUtils.hasText(value)) {
                problems.add(variable + " is not set");
            } else if (value.equals(COMMITTED_DEVELOPMENT_VALUES.get(variable))) {
                problems.add(variable + " is set to the development value committed in this repository;"
                        + " generate a new one, because the committed value is public and cannot be made"
                        + " secret again");
            }
        }
        describeUnusableEncryptionKey(environment.getProperty(ENCRYPTION_KEY)).ifPresent(problems::add);

        if (!problems.isEmpty()) {
            throw new IllegalStateException("Refusing to start with profile 'prod': " + String.join("; ", problems));
        }
    }

    /**
     * A key of the wrong length is worse than a missing one. Nothing reads it at startup —
     * the converter only uses it the first time somebody writes a check-in note or a chat
     * message — so a malformed key boots green, passes the health check, serves traffic,
     * and then fails on the first piece of emotional content anyone tries to save. Found
     * by deploying with a 38-byte key by accident and watching it start perfectly.
     */
    private Optional<String> describeUnusableEncryptionKey(String base64Key) {
        if (!StringUtils.hasText(base64Key) || base64Key.equals(COMMITTED_DEVELOPMENT_VALUES.get(ENCRYPTION_KEY))) {
            return Optional.empty();
        }
        byte[] decoded;
        try {
            decoded = Base64.getDecoder().decode(base64Key);
        } catch (IllegalArgumentException e) {
            return Optional.of(ENCRYPTION_KEY + " is not valid base64");
        }
        if (decoded.length != AES_256_KEY_BYTES) {
            return Optional.of(ENCRYPTION_KEY + " must decode to exactly " + AES_256_KEY_BYTES
                    + " bytes for AES-256, but decodes to " + decoded.length
                    + "; generate one with: openssl rand -base64 32");
        }
        return Optional.empty();
    }

    private boolean isProduction(ConfigurableEnvironment environment) {
        for (String profile : environment.getActiveProfiles()) {
            if (PRODUCTION_PROFILE.equals(profile)) {
                return true;
            }
        }
        return false;
    }
}
