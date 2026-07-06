package dev.lumen.domain.moodcheckin;

import static org.assertj.core.api.Assertions.assertThat;

import java.security.SecureRandom;
import javax.crypto.KeyGenerator;
import javax.crypto.spec.SecretKeySpec;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

/**
 * The encryption key lives in a static field (see the class Javadoc) so it can be pushed
 * in once by Spring at startup without the converter depending on Spring. That statement
 * is also shared with every other test class in the same JVM, so this test saves and
 * restores whatever key was already there instead of leaving its own behind.
 */
class EncryptedStringConverterTest {

    private static SecretKeySpec previousKey;

    private final EncryptedStringConverter converter = new EncryptedStringConverter();

    @BeforeAll
    static void setUpKey() throws Exception {
        previousKey = EncryptedStringConverter.getEncryptionKey();
        KeyGenerator keyGenerator = KeyGenerator.getInstance("AES");
        keyGenerator.init(256, new SecureRandom());
        EncryptedStringConverter.setEncryptionKey(
                new SecretKeySpec(keyGenerator.generateKey().getEncoded(), "AES"));
    }

    @AfterAll
    static void restoreKey() {
        EncryptedStringConverter.setEncryptionKey(previousKey);
    }

    @Test
    void shouldRoundTripEncryptAndDecrypt() {
        String plaintext = "Feeling anxious about work today";

        String ciphertext = converter.convertToDatabaseColumn(plaintext);
        String decrypted = converter.convertToEntityAttribute(ciphertext);

        assertThat(decrypted).isEqualTo(plaintext);
    }

    @Test
    void shouldNotStorePlaintextInsideTheCiphertext() {
        String plaintext = "Feeling anxious about work today";

        String ciphertext = converter.convertToDatabaseColumn(plaintext);

        assertThat(ciphertext).doesNotContain(plaintext);
    }

    @Test
    void shouldProduceDifferentCiphertextForTheSamePlaintextEachTime() {
        String plaintext = "Same note twice";

        String first = converter.convertToDatabaseColumn(plaintext);
        String second = converter.convertToDatabaseColumn(plaintext);

        assertThat(first).isNotEqualTo(second);
    }

    @Test
    void shouldTreatNullAsNull() {
        assertThat(converter.convertToDatabaseColumn(null)).isNull();
        assertThat(converter.convertToEntityAttribute(null)).isNull();
    }
}
