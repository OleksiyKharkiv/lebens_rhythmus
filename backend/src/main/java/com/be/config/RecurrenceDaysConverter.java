package com.be.config;

import com.be.domain.entity.RecurrenceDay;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Group.recurrencePattern <-> JSON text (LR-081). Not Spring-managed
 * config (unlike EncryptedStringConverter) — plain Jackson, no key
 * material — a static ObjectMapper is enough, same @Component @Converter
 * shape as the encryption converter for consistency with how this
 * project applies custom AttributeConverters.
 */
@Component
@Converter
public class RecurrenceDaysConverter implements AttributeConverter<List<RecurrenceDay>, String> {

    private static final ObjectMapper MAPPER = new ObjectMapper().registerModule(new JavaTimeModule());

    @Override
    public String convertToDatabaseColumn(List<RecurrenceDay> attribute) {
        if (attribute == null || attribute.isEmpty()) {
            return null;
        }
        try {
            return MAPPER.writeValueAsString(attribute);
        } catch (Exception e) {
            throw new IllegalStateException("Failed to serialize recurrence pattern", e);
        }
    }

    @Override
    public List<RecurrenceDay> convertToEntityAttribute(String dbData) {
        if (dbData == null || dbData.isBlank()) {
            return List.of();
        }
        try {
            return MAPPER.readerForListOf(RecurrenceDay.class).readValue(dbData);
        } catch (Exception e) {
            throw new IllegalStateException("Failed to deserialize recurrence pattern", e);
        }
    }
}
