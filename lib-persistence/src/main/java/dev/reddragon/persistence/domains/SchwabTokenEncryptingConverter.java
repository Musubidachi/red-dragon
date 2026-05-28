package dev.reddragon.persistence.domains;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.SecureRandom;
import java.util.Base64;

/**
 * Encrypts persisted Schwab OAuth token values while keeping entity accessors
 * compatible with the plain token strings expected by callers.
 */
@Converter(autoApply = false)
public class SchwabTokenEncryptingConverter implements AttributeConverter<String, String> {

    static final String PROPERTY_NAME = "red-dragon.persistence.schwab-token-encryption-key";
    static final String ENV_NAME = "RED_DRAGON_PERSISTENCE_SCHWAB_TOKEN_ENCRYPTION_KEY";
    static final String CIPHERTEXT_PREFIX = "rdg:v1:";

    private static final String KEY_PREFIX = "base64:";
    private static final String AES = "AES";
    private static final String AES_GCM = "AES/GCM/NoPadding";
    private static final int AES_256_KEY_BYTES = 32;
    private static final int GCM_IV_BYTES = 12;
    private static final int GCM_TAG_BITS = 128;
    private static final SecureRandom RANDOM = new SecureRandom();
    private static final Base64.Encoder URL_ENCODER = Base64.getUrlEncoder().withoutPadding();
    private static final Base64.Decoder URL_DECODER = Base64.getUrlDecoder();

    @Override
    public String convertToDatabaseColumn(String attribute) {
        if (attribute == null) {
            return null;
        }

        byte[] iv = new byte[GCM_IV_BYTES];
        RANDOM.nextBytes(iv);

        try {
            Cipher cipher = Cipher.getInstance(AES_GCM);
            cipher.init(Cipher.ENCRYPT_MODE, encryptionKey(), new GCMParameterSpec(GCM_TAG_BITS, iv));
            byte[] ciphertext = cipher.doFinal(attribute.getBytes(StandardCharsets.UTF_8));
            return CIPHERTEXT_PREFIX + URL_ENCODER.encodeToString(iv) + ":" + URL_ENCODER.encodeToString(ciphertext);
        } catch (GeneralSecurityException e) {
            throw new IllegalStateException("Unable to encrypt Schwab token for persistence", e);
        }
    }

    @Override
    public String convertToEntityAttribute(String dbData) {
        if (dbData == null || !dbData.startsWith(CIPHERTEXT_PREFIX)) {
            return dbData;
        }

        String payload = dbData.substring(CIPHERTEXT_PREFIX.length());
        String[] parts = payload.split(":", 2);
        if (parts.length != 2 || parts[0].isBlank() || parts[1].isBlank()) {
            throw new IllegalStateException("Persisted Schwab token ciphertext has an unsupported format");
        }

        try {
            byte[] iv = URL_DECODER.decode(parts[0]);
            byte[] ciphertext = URL_DECODER.decode(parts[1]);
            Cipher cipher = Cipher.getInstance(AES_GCM);
            cipher.init(Cipher.DECRYPT_MODE, encryptionKey(), new GCMParameterSpec(GCM_TAG_BITS, iv));
            return new String(cipher.doFinal(ciphertext), StandardCharsets.UTF_8);
        } catch (IllegalArgumentException | GeneralSecurityException e) {
            throw new IllegalStateException("Unable to decrypt persisted Schwab token", e);
        }
    }

    private SecretKey encryptionKey() {
        String configured = configuredKey();
        String encoded = configured.regionMatches(true, 0, KEY_PREFIX, 0, KEY_PREFIX.length())
                ? configured.substring(KEY_PREFIX.length())
                : configured;

        byte[] keyBytes;
        try {
            keyBytes = Base64.getDecoder().decode(encoded);
        } catch (IllegalArgumentException e) {
            throw new IllegalStateException("Schwab token encryption key must be base64-encoded", e);
        }

        if (keyBytes.length != AES_256_KEY_BYTES) {
            throw new IllegalStateException("Schwab token encryption key must decode to 32 bytes for AES-256");
        }
        return new SecretKeySpec(keyBytes, AES);
    }

    private String configuredKey() {
        String property = System.getProperty(PROPERTY_NAME);
        if (property != null && !property.isBlank()) {
            return property.trim();
        }

        String environment = System.getenv(ENV_NAME);
        if (environment != null && !environment.isBlank()) {
            return environment.trim();
        }

        throw new IllegalStateException("Schwab token encryption key is required to persist encrypted tokens; set "
                + PROPERTY_NAME + " or " + ENV_NAME + " to base64:<32-byte AES key>");
    }
}
