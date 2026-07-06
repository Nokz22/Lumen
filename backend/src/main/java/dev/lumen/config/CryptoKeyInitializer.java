package dev.lumen.config;

import dev.lumen.domain.moodcheckin.EncryptedStringConverter;
import jakarta.annotation.PostConstruct;
import java.util.Base64;
import javax.crypto.spec.SecretKeySpec;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

/**
 * JPA instantiates {@code EncryptedStringConverter} via reflection, not Spring DI — this
 * pushes the key into it once at startup instead, keeping the converter itself
 * framework-agnostic (see its Javadoc).
 */
@Configuration
public class CryptoKeyInitializer {

    private final String base64Key;

    public CryptoKeyInitializer(@Value("${app.encryption.key}") String base64Key) {
        this.base64Key = base64Key;
    }

    @PostConstruct
    void initializeEncryptionKey() {
        byte[] keyBytes = Base64.getDecoder().decode(base64Key);
        EncryptedStringConverter.setEncryptionKey(new SecretKeySpec(keyBytes, "AES"));
    }
}
