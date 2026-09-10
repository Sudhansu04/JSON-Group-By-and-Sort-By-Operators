package com.assignment.datasetops.persistence;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

/**
 * JPA attribute converter that stores a Jackson {@link JsonNode} as its JSON text representation.
 *
 * <p>Keeping the entity attribute typed as {@code JsonNode} means the rest of the application never
 * deals with raw JSON strings, while the column stays a plain text column that every relational
 * database supports.
 */
@Converter
public class JsonNodeAttributeConverter implements AttributeConverter<JsonNode, String> {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    @Override
    public String convertToDatabaseColumn(JsonNode attribute) {
        if (attribute == null) {
            return null;
        }
        try {
            return OBJECT_MAPPER.writeValueAsString(attribute);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Unable to serialise JSON payload for storage", e);
        }
    }

    @Override
    public JsonNode convertToEntityAttribute(String dbData) {
        if (dbData == null) {
            return null;
        }
        try {
            return OBJECT_MAPPER.readTree(dbData);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Stored payload is not valid JSON", e);
        }
    }
}
