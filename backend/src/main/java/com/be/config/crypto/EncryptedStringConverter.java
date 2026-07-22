package com.be.config.crypto;

import jakarta.annotation.PostConstruct;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.util.Base64;

/**
 * Transparent AES-256-GCM encryption for JPA columns holding GoBD/DSGVO
 * "besonders schützenswert" data (bank/tax identifiers). Apply via
 * {@code @Convert(converter = EncryptedStringConverter.class)} on the entity
 * field.
 * <p>
 * Do NOT apply to columns used in equality lookups (e.g. {@code User.email})
 * — GCM uses a random IV per call, so the same plaintext never produces the
 * same ciphertext twice, which breaks {@code WHERE column = ?} queries.
 */
@Component
@Converter
public class EncryptedStringConverter implements AttributeConverter<String, String> {

    private static final Logger log = LoggerFactory.getLogger(EncryptedStringConverter.class);
    private static final String TRANSFORMATION = "AES/GCM/NoPadding";
    private static final int GCM_IV_LENGTH = 12;
    private static final int GCM_TAG_LENGTH_BITS = 128;

    @Value("${app.field-encryption.key:}")
    private String base64Key;

    private SecretKeySpec key;

    @PostConstruct
    public void init() {
        if (base64Key == null || base64Key.isBlank()) {
            log.error("FIELD_ENCRYPTION_KEY is not set. Set env var FIELD_ENCRYPTION_KEY " +
                    "(base64-encoded, must decode to exactly 32 raw bytes for AES-256).");
            throw new IllegalStateException("Field encryption key is not configured");
        }
        byte[] keyBytes;
        try {
            keyBytes = Base64.getDecoder().decode(base64Key);
        } catch (IllegalArgumentException ex) {
            throw new IllegalStateException("FIELD_ENCRYPTION_KEY is not valid base64", ex);
        }
        if (keyBytes.length != 32) {
            log.error("FIELD_ENCRYPTION_KEY decodes to {} bytes, need exactly 32 (AES-256).", keyBytes.length);
            throw new IllegalStateException("Field encryption key must decode to 32 bytes (AES-256)");
        }
        this.key = new SecretKeySpec(keyBytes, "AES");
        log.info("EncryptedStringConverter initialized (AES-256-GCM)");
    }

    @Override
    public String convertToDatabaseColumn(String plainText) {
        if (plainText == null) return null;
        requireKey();
        try {
            byte[] iv = new byte[GCM_IV_LENGTH];
            new SecureRandom().nextBytes(iv);
            Cipher cipher = Cipher.getInstance(TRANSFORMATION);
            cipher.init(Cipher.ENCRYPT_MODE, key, new GCMParameterSpec(GCM_TAG_LENGTH_BITS, iv));
            byte[] cipherText = cipher.doFinal(plainText.getBytes(StandardCharsets.UTF_8));
            byte[] combined = new byte[iv.length + cipherText.length];
            System.arraycopy(iv, 0, combined, 0, iv.length);
            System.arraycopy(cipherText, 0, combined, iv.length, cipherText.length);
            return Base64.getEncoder().encodeToString(combined);
        } catch (Exception ex) {
            throw new IllegalStateException("Failed to encrypt field value", ex);
        }
    }

    @Override
    public String convertToEntityAttribute(String storedValue) {
        if (storedValue == null) return null;
        requireKey();
        try {
            byte[] combined = Base64.getDecoder().decode(storedValue);
            byte[] iv = new byte[GCM_IV_LENGTH];
            System.arraycopy(combined, 0, iv, 0, GCM_IV_LENGTH);
            byte[] cipherText = new byte[combined.length - GCM_IV_LENGTH];
            System.arraycopy(combined, GCM_IV_LENGTH, cipherText, 0, cipherText.length);
            Cipher cipher = Cipher.getInstance(TRANSFORMATION);
            cipher.init(Cipher.DECRYPT_MODE, key, new GCMParameterSpec(GCM_TAG_LENGTH_BITS, iv));
            byte[] plain = cipher.doFinal(cipherText);
            return new String(plain, StandardCharsets.UTF_8);
        } catch (Exception ex) {
            throw new IllegalStateException("Failed to decrypt field value", ex);
        }
    }

    private void requireKey() {
        if (key == null) {
            throw new IllegalStateException("EncryptedStringConverter used before Spring initialized it (@PostConstruct not run yet)");
        }
    }
}
