package dev.reddragon.persistence.domains;

import jakarta.persistence.Convert;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.util.Base64;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SchwabTokenEncryptingConverterTest {

    private static final String KEY_A = "base64:" + Base64.getEncoder().encodeToString(keyBytes(1));
    private static final String KEY_B = "base64:" + Base64.getEncoder().encodeToString(keyBytes(65));

    private final SchwabTokenEncryptingConverter converter = new SchwabTokenEncryptingConverter();

    @AfterEach
    void clearKey() {
        System.clearProperty(SchwabTokenEncryptingConverter.PROPERTY_NAME);
    }

    @Test
    void encryptsAndDecryptsTokenValues() {
        System.setProperty(SchwabTokenEncryptingConverter.PROPERTY_NAME, KEY_A);

        String stored = converter.convertToDatabaseColumn("access-token-value");

        assertTrue(stored.startsWith(SchwabTokenEncryptingConverter.CIPHERTEXT_PREFIX));
        assertFalse(stored.contains("access-token-value"));
        assertEquals("access-token-value", converter.convertToEntityAttribute(stored));
    }

    @Test
    void usesFreshNonceForEachWrite() {
        System.setProperty(SchwabTokenEncryptingConverter.PROPERTY_NAME, KEY_A);

        String first = converter.convertToDatabaseColumn("refresh-token-value");
        String second = converter.convertToDatabaseColumn("refresh-token-value");

        assertNotEquals(first, second);
        assertEquals("refresh-token-value", converter.convertToEntityAttribute(first));
        assertEquals("refresh-token-value", converter.convertToEntityAttribute(second));
    }

    @Test
    void legacyPlaintextRowsRemainReadableWithoutKey() {
        assertEquals("legacy-token", converter.convertToEntityAttribute("legacy-token"));
    }

    @Test
    void persistingTokensRequiresConfiguredKey() {
        IllegalStateException thrown = assertThrows(IllegalStateException.class,
                () -> converter.convertToDatabaseColumn("token"));

        assertTrue(thrown.getMessage().contains(SchwabTokenEncryptingConverter.PROPERTY_NAME));
    }

    @Test
    void encryptedRowsRequireSameKeyForRead() {
        System.setProperty(SchwabTokenEncryptingConverter.PROPERTY_NAME, KEY_A);
        String stored = converter.convertToDatabaseColumn("refresh-token-value");
        System.setProperty(SchwabTokenEncryptingConverter.PROPERTY_NAME, KEY_B);

        assertThrows(IllegalStateException.class, () -> converter.convertToEntityAttribute(stored));
    }

    @Test
    void schwabTokenFieldsUseEncryptingConverter() throws NoSuchFieldException {
        assertFieldConverter("accessToken");
        assertFieldConverter("refreshToken");
    }

    private static void assertFieldConverter(String fieldName) throws NoSuchFieldException {
        Field field = SchwabTokenEntity.class.getDeclaredField(fieldName);
        Convert convert = field.getAnnotation(Convert.class);

        assertNotNull(convert);
        assertEquals(SchwabTokenEncryptingConverter.class, convert.converter());
    }

    private static byte[] keyBytes(int start) {
        byte[] key = new byte[32];
        for (int i = 0; i < key.length; i++) {
            key[i] = (byte) (start + i);
        }
        return key;
    }
}
