package com.be.config.crypto;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.security.SecureRandom;
import java.util.Base64;

import static org.junit.jupiter.api.Assertions.*;

class EncryptedStringConverterTest {

    private EncryptedStringConverter converter;

    @BeforeEach
    void setUp() {
        converter = new EncryptedStringConverter();
        byte[] key = new byte[32];
        new SecureRandom().nextBytes(key);
        ReflectionTestUtils.setField(converter, "base64Key", Base64.getEncoder().encodeToString(key));
        converter.init();
    }

    @Test
    void encryptThenDecrypt_returnsOriginalValue() {
        String iban = "DE89370400440532013000";
        String stored = converter.convertToDatabaseColumn(iban);

        assertNotNull(stored);
        assertNotEquals(iban, stored, "ciphertext must not equal plaintext");
        assertEquals(iban, converter.convertToEntityAttribute(stored));
    }

    @Test
    void sameValueEncryptedTwice_producesDifferentCiphertext() {
        String taxId = "DE123456789";
        String stored1 = converter.convertToDatabaseColumn(taxId);
        String stored2 = converter.convertToDatabaseColumn(taxId);

        assertNotEquals(stored1, stored2, "GCM must use a fresh IV per call — deterministic ciphertext would be a bug");
        assertEquals(taxId, converter.convertToEntityAttribute(stored1));
        assertEquals(taxId, converter.convertToEntityAttribute(stored2));
    }

    @Test
    void nullPassesThroughBothDirections() {
        assertNull(converter.convertToDatabaseColumn(null));
        assertNull(converter.convertToEntityAttribute(null));
    }

    @Test
    void init_throwsIfKeyMissing() {
        EncryptedStringConverter fresh = new EncryptedStringConverter();
        ReflectionTestUtils.setField(fresh, "base64Key", "");
        assertThrows(IllegalStateException.class, fresh::init);
    }

    @Test
    void init_throwsIfKeyWrongLength() {
        EncryptedStringConverter fresh = new EncryptedStringConverter();
        byte[] shortKey = new byte[16]; // AES-128 length, we require 32 (AES-256)
        ReflectionTestUtils.setField(fresh, "base64Key", Base64.getEncoder().encodeToString(shortKey));
        assertThrows(IllegalStateException.class, fresh::init);
    }
}
