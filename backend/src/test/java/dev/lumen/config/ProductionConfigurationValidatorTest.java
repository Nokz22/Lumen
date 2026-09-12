package dev.lumen.config;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;
import org.springframework.mock.env.MockEnvironment;

class ProductionConfigurationValidatorTest {

    private static final String COMMITTED_JWT_SECRET =
            "dev-only-insecure-secret-change-me-0123456789abcdefghijklmnop";
    private static final String COMMITTED_ENCRYPTION_KEY = "zxLVCSUFSFXj60TnIb6R7L+4Y0jAPDe62VKLIvTkjqI=";

    /** 32 bytes once decoded, which is what AES-256 requires. */
    private static final String VALID_AES_256_KEY = "MDEyMzQ1Njc4OWFiY2RlZjAxMjM0NTY3ODlhYmNkZWY=";

    private final ProductionConfigurationValidator validator = new ProductionConfigurationValidator();

    private MockEnvironment environmentWith(String... profiles) {
        MockEnvironment environment = new MockEnvironment();
        environment.setActiveProfiles(profiles);
        return environment;
    }

    private void withCompleteConfiguration(MockEnvironment environment) {
        environment.setProperty("DATABASE_JDBC_URL", "jdbc:postgresql://db:5432/lumen");
        environment.setProperty("DATABASE_USERNAME", "lumen");
        environment.setProperty("DATABASE_PASSWORD", "a-real-password");
        environment.setProperty("RABBITMQ_HOST", "broker");
        environment.setProperty("RABBITMQ_USERNAME", "lumen");
        environment.setProperty("RABBITMQ_PASSWORD", "a-real-password");
        environment.setProperty("JWT_SECRET", "a-real-deployment-secret-not-in-git-0123456789abcdef");
        environment.setProperty("ENCRYPTION_KEY", VALID_AES_256_KEY);
        environment.setProperty("CORS_ALLOWED_ORIGINS", "https://lumen.example");
    }

    @Test
    void shouldStayOutOfTheWayWhenTheProductionProfileIsNotActive() {
        assertThatCode(() -> validator.postProcessEnvironment(environmentWith("dev"), null))
                .doesNotThrowAnyException();
    }

    @Test
    void shouldStartWhenEveryProductionValueIsSupplied() {
        MockEnvironment environment = environmentWith("prod");
        withCompleteConfiguration(environment);

        assertThatCode(() -> validator.postProcessEnvironment(environment, null))
                .doesNotThrowAnyException();
    }

    /** One startup failure naming everything that is missing beats nine deploys in a row. */
    @Test
    void shouldNameEveryMissingVariableInASingleFailure() {
        assertThatThrownBy(() -> validator.postProcessEnvironment(environmentWith("prod"), null))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("the database location is not set")
                .hasMessageContaining("JWT_SECRET is not set")
                .hasMessageContaining("ENCRYPTION_KEY is not set")
                .hasMessageContaining("CORS_ALLOWED_ORIGINS is not set");
    }

    @Test
    void shouldRefuseTheJwtSecretThatIsCommittedInThisRepository() {
        MockEnvironment environment = environmentWith("prod");
        withCompleteConfiguration(environment);
        environment.setProperty("JWT_SECRET", COMMITTED_JWT_SECRET);

        assertThatThrownBy(() -> validator.postProcessEnvironment(environment, null))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("JWT_SECRET is set to the development value");
    }

    @Test
    void shouldRefuseTheEncryptionKeyThatIsCommittedInThisRepository() {
        MockEnvironment environment = environmentWith("prod");
        withCompleteConfiguration(environment);
        environment.setProperty("ENCRYPTION_KEY", COMMITTED_ENCRYPTION_KEY);

        assertThatThrownBy(() -> validator.postProcessEnvironment(environment, null))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("ENCRYPTION_KEY is set to the development value");
    }

    /**
     * Nothing reads the key at startup, so a malformed one boots green and only fails when
     * somebody first saves a note or a chat message. That has to be caught before traffic.
     */
    /** A blueprint wires host, port and name from its managed database; a person pastes a URL. */
    @Test
    void shouldAcceptTheDatabaseLocationGivenAsSeparateParts() {
        MockEnvironment environment = environmentWith("prod");
        withCompleteConfiguration(environment);
        environment.setProperty("DATABASE_JDBC_URL", "");
        environment.setProperty("DATABASE_HOST", "db.internal");
        environment.setProperty("DATABASE_NAME", "lumen");

        assertThatCode(() -> validator.postProcessEnvironment(environment, null))
                .doesNotThrowAnyException();
    }

    @Test
    void shouldRefuseHalfADatabaseLocation() {
        MockEnvironment environment = environmentWith("prod");
        withCompleteConfiguration(environment);
        environment.setProperty("DATABASE_JDBC_URL", "");
        environment.setProperty("DATABASE_HOST", "db.internal");

        assertThatThrownBy(() -> validator.postProcessEnvironment(environment, null))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("the database location is not set");
    }

    @Test
    void shouldRefuseAnEncryptionKeyThatIsNotUsableForAes256() {
        MockEnvironment environment = environmentWith("prod");
        withCompleteConfiguration(environment);
        environment.setProperty("ENCRYPTION_KEY", "Zm9yLXRlc3Rpbmctb25seS1ub3QtYS1yZWFsLWtleS0wMDAwMDA=");

        assertThatThrownBy(() -> validator.postProcessEnvironment(environment, null))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("must decode to exactly 32 bytes")
                .hasMessageContaining("decodes to 38");
    }

    @Test
    void shouldRefuseAnEncryptionKeyThatIsNotEvenBase64() {
        MockEnvironment environment = environmentWith("prod");
        withCompleteConfiguration(environment);
        environment.setProperty("ENCRYPTION_KEY", "not base64 at all !!!");

        assertThatThrownBy(() -> validator.postProcessEnvironment(environment, null))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("not valid base64");
    }

    /** Blank is the shape a forgotten value takes in a deployment UI, not absent. */
    @Test
    void shouldTreatABlankValueAsMissing() {
        MockEnvironment environment = environmentWith("prod");
        withCompleteConfiguration(environment);
        environment.setProperty("DATABASE_PASSWORD", "   ");

        assertThatThrownBy(() -> validator.postProcessEnvironment(environment, null))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("DATABASE_PASSWORD is not set");
    }
}
